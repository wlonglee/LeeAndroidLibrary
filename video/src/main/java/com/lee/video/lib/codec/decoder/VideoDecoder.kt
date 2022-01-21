package com.lee.video.lib.codec.decoder

import android.content.res.AssetFileDescriptor
import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import com.lee.video.lib.codec.base.BaseDecoder
import java.io.FileDescriptor
import java.io.IOException
import java.nio.ByteBuffer

/**
 * 视频画面解码-单独使用可编辑视频画面,支持几乎所有视频格式文件,可无缝循环
 *@author lee
 *@date 2021/11/25
 */
class VideoDecoder private constructor() : BaseDecoder() {

    interface VideoListener {
        /**
         * 视频参数回调
         */
        fun onVideoFormat(videoWidth: Int, videoHeight: Int, videoFrame: Int, duration: Long)

        /**
         * 准备就绪,可以启动播放,配置了自动播放(autoPlay为true)的情况下该函数不会触发
         */
        fun onReady()

        /**
         * 渲染进度 从0~100,保留两位小数
         */
        fun onVideoProgress(p: Float)

        /**
         * 播放器停止,资源也已释放,如果再次使用需要重新构建对象
         */
        fun onEnd()

        /**
         * 播放错误
         */
        fun onError(msg: String)

    }

    open class SimpleListener : VideoListener {
        override fun onVideoFormat(
            videoWidth: Int,
            videoHeight: Int,
            videoFrame: Int,
            duration: Long
        ) {
        }

        override fun onReady() {
        }

        override fun onVideoProgress(p: Float) {
        }

        override fun onEnd() {
        }

        override fun onError(msg: String) {
        }
    }

    /**
     * 显示视频画面
     */
    private var surface: Surface? = null

    /**
     * 监听器
     */
    private var listener: VideoListener? = null

    /**
     * 视频宽
     */
    private var videoWidth = -1

    /**
     * 视频高
     */
    private var videoHeight = -1

    /**
     * 视频帧率
     */
    private var videoFrame = -1

    /**
     * 构建器
     */
    class Builder {
        /**
         * 是否自动启动播放
         * true-加载后将自动播放
         */
        private var autoPlay = false

        /**
         * 是否循环
         * true 将会进行循环解码,直到主动停止
         */
        private var loop = false

        /**
         * 进度回调频率,每隔多少毫秒回调一次播放进度
         */
        private var progressFreq: Long = 200

        /**
         * 视频渲染表面
         */
        private var surface: Surface? = null

        /**
         * 监听器
         */
        private var listener: VideoListener? = null

        /**
         * 网络音频进度对比拦截器,如果不是网络音频不需要实现该操作
         */
        private var interceptor: NetInterceptor? = null
        fun setSurface(surface: Surface?): Builder {
            this.surface = surface
            return this
        }

        fun setSurface(surfaceTexture: SurfaceTexture?): Builder {
            surface = Surface(surfaceTexture)
            return this
        }

        fun setAutoPlay(autoPlay: Boolean): Builder {
            this.autoPlay = autoPlay
            return this
        }

        fun setLoop(loop: Boolean): Builder {
            this.loop = loop
            return this
        }

        fun setProgressFreq(progressFreq: Long): Builder {
            this.progressFreq = progressFreq
            return this
        }

        fun setListener(listener: VideoListener?): Builder {
            this.listener = listener
            return this
        }

        fun setInterceptor(interceptor: NetInterceptor?): Builder {
            this.interceptor = interceptor
            return this
        }

        /**
         * 指定本地播放路径
         */
        fun build(path: String): VideoDecoder {
            val player = VideoDecoder()
            player.generate(
                surface,
                autoPlay,
                loop,
                progressFreq,
                path,
                listener,
                interceptor
            )
            return player
        }

        /**
         * 指定资源目录下的文件
         */
        fun build(afd: AssetFileDescriptor): VideoDecoder {
            return build(afd.fileDescriptor, afd.startOffset, afd.length)
        }

        fun build(fd: FileDescriptor, offset: Long, length: Long): VideoDecoder {
            val player = VideoDecoder()
            player.generate(
                surface,
                autoPlay,
                loop,
                progressFreq,
                fd,
                offset,
                length,
                listener
            )
            return player
        }

    }

    private fun generate(
        surface: Surface?,
        autoPlay: Boolean,
        loop: Boolean,
        progressFreq: Long,
        path: String,
        listener: VideoListener?,
        interceptor: NetInterceptor?
    ) {
        this.surface = surface
        this.autoPlay = autoPlay
        this.loop = loop
        this.progressFreq = progressFreq
        this.listener = listener
        this.interceptor = interceptor

        extractor = MediaExtractor()
        playStatus = State.NO_PLAY
        try {
            extractor?.setDataSource(path)
        } catch (e: IOException) {
            onError("setDataSource error:$e")
        }
    }

    private fun generate(
        surface: Surface?,
        autoPlay: Boolean,
        loop: Boolean,
        progressFreq: Long,
        fd: FileDescriptor,
        offset: Long,
        length: Long,
        listener: VideoListener?
    ) {
        this.surface = surface
        this.autoPlay = autoPlay
        this.loop = loop
        this.progressFreq = progressFreq
        this.listener = listener
        extractor = MediaExtractor()
        playStatus = State.NO_PLAY
        try {
            extractor?.setDataSource(fd, offset, length)
        } catch (e: IOException) {
            onError("setDataSource error:$e")
        }
    }

    override fun prepare() {
        //遍历获取轨道
        for (i in 0 until extractor!!.trackCount) {
            val format = extractor!!.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime != null && mime.startsWith("video/")) {
                this.format = format
                //获取配置信息
                configCodec(false)
                //选择指定轨道
                extractor?.selectTrack(i)
                //创建解码器
                codec = MediaCodec.createDecoderByType(mime)
                codec!!.configure(format, surface, null, 0)
                //创建缓存区
                bufferInfo = MediaCodec.BufferInfo()
                break
            }
        }
        when {
            codec == null -> {
                extractor?.release()
                extractor = null
                onError("no video resource")
            }
            autoPlay -> {
                extractor?.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                startPlay()
            }
            else -> {
                onReady()
            }
        }
    }

    override fun configCodec(flag: Boolean) {
        if (format!!.containsKey(MediaFormat.KEY_DURATION)) {
            //视频时长
            duration = format!!.getLong(MediaFormat.KEY_DURATION) / 1000
        }

        if (format!!.containsKey(MediaFormat.KEY_WIDTH)) {
            //视频宽
            videoWidth = format!!.getInteger(MediaFormat.KEY_WIDTH)
        }

        if (format!!.containsKey(MediaFormat.KEY_HEIGHT)) {
            //视频高
            videoHeight = format!!.getInteger(MediaFormat.KEY_HEIGHT)
        }

        if (format!!.containsKey(MediaFormat.KEY_FRAME_RATE)) {
            //视频帧率
            videoFrame = format!!.getInteger(MediaFormat.KEY_FRAME_RATE)
        }
        if (flag) {
            Log.e("lee", "视频信息:$videoWidth*$videoHeight,fps:$videoFrame,duration:$duration")
            onVideoFormat()
        }
    }

    override fun onRender(index: Int) {
        codec?.getOutputBuffer(index) ?: return
        //数据帧的时间值
        val pts = bufferInfo!!.presentationTimeUs / 1000
        val p = (pts * 100f / duration * 100).toInt() / 100f //保留2位小数
        //进度回调
        val ct = System.currentTimeMillis()
        if (ct - lastPT >= progressFreq) {
            //满足回调频率后进行回调
            listener?.onVideoProgress(p)
            lastPT = ct
        } else if (p >= 100f) {
            listener?.onVideoProgress(100f)
        }

        //记录开始节点
        if (startTime == 0L || lastPts > pts && loop) {
            startTime = System.currentTimeMillis()
            Log.e("lee", "视频开始了")
        }
        //更新上一帧时间
        lastPts = pts
        val s = System.currentTimeMillis() - startTime - pts
        val fps = (1000 / videoFrame).toLong()
        var render = true
        if (s < -fps) {
            //时间较早,休眠一会再送入渲染
            val expectedTime = System.currentTimeMillis() - s
            do {
                try {
                    //由于cpu调度，休眠并不保证在指定时间后一定醒来，所以每次休眠几毫秒
                    sleep(2)
                } catch (e: InterruptedException) {
                    //
                }
            } while (System.currentTimeMillis() < expectedTime - 2)
        } else if (s > fps) {
            //超时,直接丢弃
            render = false
            Log.e("lee","超时,直接丢弃")
        }
        codec?.releaseOutputBuffer(index, render)
    }

    private fun onVideoFormat() {
        listener?.onVideoFormat(videoWidth, videoHeight, videoFrame, duration)
    }

    private fun onReady() {
        listener?.onReady()
    }

    override fun onError(s: String) {
        listener?.onError(s)
    }

    override fun onEnd() {
        surface = null
        listener?.onEnd()
    }
}