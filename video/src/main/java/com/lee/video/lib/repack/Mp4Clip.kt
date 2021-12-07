package com.lee.video.lib.repack

import android.content.Context
import android.media.MediaCodec
import android.media.MediaFormat
import com.lee.video.lib.codec.decoder.sync.AudioExtractor
import com.lee.video.lib.codec.decoder.sync.EglVideo
import com.lee.video.lib.codec.decoder.sync.VideoDecoderSync
import com.lee.video.lib.codec.encoder.EncoderSyncListener
import com.lee.video.lib.codec.encoder.VideoEncoder
import com.lee.video.lib.gl.render.drawer.IDrawer
import com.lee.video.lib.gl.render.drawer.VideoClipDrawer
import java.lang.Exception
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import kotlin.math.min

/**
 *@author lee
 *@date 2021/12/2
 */
class Mp4Clip private constructor() {

    class Builder {
        private var context: Context? = null

        /**
         * 进度回调
         */
        private var listener: ClipListener? = null

        /**
         * 待编码的视频路径
         */
        private var inPath: String? = null

        /**
         * 编码后输出视频的路径
         */
        private var outPath: String? = null

        /**
         * 输出的视频宽-要求为2的倍数
         */
        private var resultWidth = 1280

        /**
         * 输出的视频高-要求为2的倍数
         */
        private var resultHeight = 720

        /**
         * 裁剪视频距离左侧的距离
         */
        private var marginLeft = 0

        /**
         * 裁剪视频距离顶部的距离
         */
        private var marginTop = 0

        /**
         * 生成的i帧间隔
         */
        private var iInterval = 0.1f

        /**
         * 取值范围0~1
         * 清晰度,该值越高视频画面越清晰
         */
        private var clarity = 0.3f

        fun setContext(context: Context): Builder {
            this.context = context
            return this
        }


        fun setInPath(inPath: String): Builder {
            this.inPath = inPath
            return this
        }

        fun setOutPath(outPath: String): Builder {
            this.outPath = outPath
            return this
        }

        fun setResultWidth(resultWidth: Int): Builder {
            this.resultWidth = resultWidth
            return this
        }

        fun setResultHeight(resultHeight: Int): Builder {
            this.resultHeight = resultHeight
            return this
        }

        fun setMarginLeft(marginLeft: Int): Builder {
            this.marginLeft = marginLeft
            return this
        }

        fun setMarginTop(marginTop: Int): Builder {
            this.marginTop = marginTop
            return this
        }

        fun setListener(listener: ClipListener): Builder {
            this.listener = listener
            return this
        }

        fun setIInterval(iInterval: Float): Builder {
            this.iInterval = iInterval
            return this
        }

        fun setClarity(clarity: Float): Builder {
            this.clarity = clarity
            return this
        }


        fun build(): Mp4Clip {
            val repack = Mp4Clip()
            repack.generate(
                context!!,
                inPath!!,
                outPath!!,
                resultWidth,
                resultHeight,
                marginLeft,
                marginTop,
                iInterval,
                clarity,
                listener
            )
            return repack
        }
    }

    interface ClipListener {
        /**
         * 编码开始
         */
        fun onStart()

        /**
         * 进度 0~100 保留2位小数
         */
        fun onProgress(p: Float)

        /**
         * 编码结束
         */
        fun onEnd()

        /**
         * 编码出错
         */
        fun onError(msg: String)
    }

    private val pool = Executors.newScheduledThreadPool(1)

    private var listener: ClipListener? = null

    /**
     * 混合器
     */
    private lateinit var mixer: Mp4Mixer

    /**
     * 视频编码器
     */
    private lateinit var videoEncoder: VideoEncoder

    /**
     * 视频解码器
     */
    private lateinit var videoDecoder: VideoDecoderSync

    /**
     * 视频解码后的渲染与效果处理
     */
    private lateinit var egl: EglVideo

    private lateinit var drawer: IDrawer

    /**
     * 音频数据提取器
     */
    private lateinit var audioExtractor: AudioExtractor

    /**
     * 输出的视频宽-要求为2的倍数
     */
    private var resultWidth = 1

    /**
     * 输出的视频高-要求为2的倍数
     */
    private var resultHeight = 1


    /**
     * 处理编码进度
     */
    private var lastP = 0f
    private var audioP = 0f
    private var videoP = 0f


    private var isRun = false

    private fun generate(
        context: Context,
        inPath: String,
        outPath: String,
        resultWidth: Int,
        resultHeight: Int,
        marginLeft: Int,
        marginTop: Int,
        iInterval: Float,
        clarity: Float,
        listener: ClipListener?
    ) {
        this.listener = listener
        this.resultWidth = resultWidth
        this.resultHeight = resultHeight
        try {
            mixer = Mp4Mixer(outPath)
            //创建解码器
            videoDecoder = VideoDecoderSync()
            videoDecoder.create(inPath)

            //创建编码器
            videoEncoder = VideoEncoder.Builder()
                .setVideoWidth(resultWidth)
                .setVideoHeight(resultHeight)
                .setOriginalInfo(videoDecoder.extractFps, videoDecoder.duration)
                .setIInterval(iInterval)  //该值越小,要求Clarity越大
                .setClarity(clarity) //该值越小,要求IInterval越大,否则视频画面惨不忍睹
                .setListener(object : EncoderSyncListener {
                    override fun onEncoderProgress(p: Float) {
                        onExtractProgress(p, false)
                    }

                    override fun onFormat(format: MediaFormat) {
                        mixer.addVideoTrack(format)
                    }

                    override fun onData(buffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
                        mixer.writeVideoData(buffer, bufferInfo)
                    }

                })
                .build()

            //创建音频数据提取器
            audioExtractor = AudioExtractor.Builder()
                .setPath(inPath)
                .setListener(object : EncoderSyncListener {
                    override fun onEncoderProgress(p: Float) {
                        onExtractProgress(p)
                    }

                    override fun onFormat(format: MediaFormat) {
                        mixer.addAudioTrack(format)
                    }

                    override fun onData(buffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
                        mixer.writeAudioData(buffer, bufferInfo)
                    }

                })
                .build()

            /**
             * 创建绘制器
             */
            drawer = VideoClipDrawer(
                context,
                videoDecoder.videoWidth,
                videoDecoder.videoHeight,
                resultWidth,
                resultHeight,
                marginLeft,
                marginTop
            )
        } catch (e: Exception) {
            e.printStackTrace()
            this.listener?.onError(e.toString())
        }


    }

    /**
     * 启动重编码任务
     */
    fun startTask() {
        if (!isRun) {
            pool.execute(task)
            isRun = true
        }
    }

    private var task = Runnable {
        try {
            //创建EGL相关数据
            egl = EglVideo()
            egl.create(videoEncoder.inputSurface, drawer, resultWidth, resultHeight)
            videoDecoder.configEgl(egl)

            //启动解码
            videoDecoder.prepare()
            //启动编码
            audioExtractor.prepare()
            videoEncoder.prepare()
            listener?.onStart()
            while (true) {
                //1 解码视频数据
                videoDecoder.decoder()
                //2 获取解码数据
                videoDecoder.render()
                if (videoDecoder.isEnd()) {
                    videoEncoder.stopEncoder()
                }
                //3 获取编码数据
                videoEncoder.render()

                //4提取音频数据
                if (mixer.start)
                    audioExtractor.extract()

                //判断是否完成
                if (videoDecoder.isEnd() && videoEncoder.isEnd() && audioExtractor.isEnd()) {
                    break
                }
            }

            //释放资源
            videoDecoder.release()
            audioExtractor.release()
            videoEncoder.release()

            mixer.releaseAudio()
            mixer.releaseVideo()

            egl.release()

            isRun = false

            listener?.onEnd()
        } catch (e: Exception) {
            e.printStackTrace()
            listener?.onError(e.toString())
        }
    }

    private fun onExtractProgress(progress: Float, isAudio: Boolean = true) {
        if (isAudio) {
            audioP = progress
        } else {
            videoP = progress
        }
        //计算音视频两者中 进度比较小的值,作为回调给出
        val p = min(audioP, videoP)
        if (p - lastP >= 1f || p >= 100f) {
            lastP = p
            listener?.onProgress(p)
        }
    }

}