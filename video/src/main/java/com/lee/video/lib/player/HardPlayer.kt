package com.lee.video.lib.player

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaExtractor
import android.media.MediaFormat
import android.opengl.GLSurfaceView
import android.util.Log
import com.lee.video.lib.codec.base.BaseDecoder
import com.lee.video.lib.codec.decoder.AudioDecoder
import com.lee.video.lib.codec.decoder.VideoDecoder
import com.lee.video.lib.gl.render.BaseSurfaceRender
import com.lee.video.lib.gl.render.drawer.IDrawer
import com.lee.video.lib.gl.render.drawer.VideoDrawer
import com.lee.video.lib.gl.render.drawer.VideoDrawer2
import com.lee.video.lib.gl.render.drawer.WaterMaskDrawer
import java.io.FileDescriptor
import kotlin.math.max

/**
 * 硬解播放器
 *@author lee
 *@date 2021/11/30
 */
class HardPlayer private constructor() {

    /**
     * 是否自动启动播放
     */
    private var autoPlay: Boolean = false

    /**
     * 是否循环播放
     */
    private var loop: Boolean = false

    /**
     * 音频解码
     */
    private var audioDecoder: AudioDecoder? = null

    /**
     * 视频解码
     */
    private var videoDecoder: VideoDecoder? = null

    /**
     * 视频配置需要的相关参数
     */
    private var context: Context? = null

    private var surfaceView: GLSurfaceView? = null

    private var render: BaseSurfaceRender? = null

    /**
     * 绘制视频画面
     */
    private var drawer: IDrawer? = null

    /**
     * 绘制水印
     */
    private var mask: IDrawer? = null

    /**
     * 监听器
     */
    private var listener: PlayListener? = null

    /**
     * 网络进度拦截器
     */
    private var interceptor: BaseDecoder.NetInterceptor? = null


    /**
     * 音视频是否加载就绪
     */
    private var prepareAudio = false
    private var prepareVideo = false

    /**
     * 音视频是否结束
     */
    private var endAudio = false
    private var endVideo = false

    /**
     * 播放进度
     */
    private var playProgress = 0f

    /**
     * 上一次渲染进度回调的时间值
     */
    private var lastPT: Long = 0

    /**
     * 进度触发频率
     */
    private var progressFreq: Long = 0


    /**
     * 进度以谁为准,该值通过比较音频与视频的长度决定
     * true 音频为准
     * false 视频为准
     */
    private var audioLongish = true

    /**
     * 音视频的长度
     */
    private var audioDuration = 0L
    private var videoDuration = 0L

    interface PlayListener {
        /**
         * 准备就绪,可以启动播放,当autoPlay=true时该函数不会触发
         */
        fun onReady()

        /**
         * 解码的音频数据,dealPcm为true时才会回调
         *
         * @param audioData 音频数据
         */
        fun onAudioData(audioData: ByteArray)

        /**
         * 播放进度0~100 保留2位小数
         */
        fun onProgress(p: Float)

        /**
         * 播放完成
         */
        fun onEnd()

        /**
         * 播放出错
         */
        fun onError(msg: String)
    }

    class Builder {

        /**
         * 是否自动启动播放
         * true-加载后将自动播放
         */
        private var autoPlay = false

        /**
         * 是否循环
         * true 将会进行循环播放,直到主动停止
         */
        private var loop = false

        /**
         * 是否解码音频
         * false 将不会解码音频,只会解码视频
         */
        private var decoderAudio = true

        /**
         * 是否解码视频
         * false 将不会解码视频,只会解码音频
         */
        private var decoderVideo = true

        /**
         * 进度回调频率,每隔多少毫秒回调一次播放进度
         */
        private var progressFreq: Long = 200


        //音频相关参数配置
        /**
         * 是否自行处理解码后的pcm流
         * true 音频解码产生的pcm数据将通过回调函数给出,需自行控制播放
         * false 使用内置的audioTrack进行音频的播放
         */
        private var dealPcm = false
        //音频相关参数配置

        //视频相关参数配置
        private var context: Context? = null

        private var surfaceView: GLSurfaceView? = null

        //视频相关参数配置

        /**
         * 网络进度对比拦截器,如果不是网络资源不需要实现该操作
         */
        private var interceptor: BaseDecoder.NetInterceptor? = null

        /**
         * 播放相关监听回调
         */
        private var listener: PlayListener? = null

        fun setAutoPlay(autoPlay: Boolean): Builder {
            this.autoPlay = autoPlay
            return this
        }

        fun setLoop(loop: Boolean): Builder {
            this.loop = loop
            return this
        }

        fun setDecoderAudio(decoderAudio: Boolean): Builder {
            this.decoderAudio = decoderAudio
            return this
        }

        fun setProgressFreq(progressFreq: Long): Builder {
            this.progressFreq = progressFreq
            return this
        }

        fun setDealPcm(dealPcm: Boolean): Builder {
            this.dealPcm = dealPcm
            return this
        }


        fun setContext(context: Context?): Builder {
            this.context = context
            return this
        }

        fun setSurface(surface: GLSurfaceView?): Builder {
            this.surfaceView = surface
            return this
        }

        fun setDecoderVideo(decoderVideo: Boolean): Builder {
            this.decoderVideo = decoderVideo
            return this
        }

        fun setListener(listener: PlayListener?): Builder {
            this.listener = listener
            return this
        }

        fun setInterceptor(interceptor: BaseDecoder.NetInterceptor?): Builder {
            this.interceptor = interceptor
            return this
        }


        fun build(path: String): HardPlayer {
            val player = HardPlayer()
            player.generate(
                autoPlay, loop, decoderAudio, decoderVideo, progressFreq,
                dealPcm,
                context, surfaceView,
                interceptor, listener,
                path = path
            )
            return player
        }

        fun build(afd: AssetFileDescriptor): HardPlayer {
            return build(afd.fileDescriptor, afd.startOffset, afd.length)
        }

        fun build(fd: FileDescriptor, offset: Long, length: Long): HardPlayer {
            val player = HardPlayer()
            player.generate(
                autoPlay, loop, decoderAudio, decoderVideo, progressFreq,
                dealPcm,
                context, surfaceView,
                interceptor, listener,
                fd = fd, offset = offset, length = length
            )
            return player
        }
    }


    private fun generate(
        autoPlay: Boolean,
        loop: Boolean,
        decoderAudio: Boolean,
        decoderVideo: Boolean,
        progressFreq: Long,
        dealPcm: Boolean,
        context: Context?,
        surfaceView: GLSurfaceView?,
        interceptor: BaseDecoder.NetInterceptor?,
        listener: PlayListener?,
        path: String? = null,
        fd: FileDescriptor? = null,
        offset: Long = 0,
        length: Long = 0
    ) {
        val extractor = MediaExtractor()
        if (path != null) {
            extractor.setDataSource(path)
        } else {
            extractor.setDataSource(fd!!, offset, length)
        }
        val hasAudio = checkAudio(extractor)
        val hasVideo = checkVideo(extractor)
        //及时释放资源
        extractor.release()
        var da = decoderAudio
        var dv = decoderVideo
        if (!hasAudio)
            da = false
        if (!hasVideo)
            dv = false
        check(da, dv, hasAudio, hasVideo)

        this.autoPlay = autoPlay
        this.loop = loop
        this.progressFreq = progressFreq
        this.context = context
        this.surfaceView = surfaceView
        this.interceptor = interceptor
        this.listener = listener

        if (da && dv) {
            prepareAudio = false
            prepareVideo = false
            endAudio = false
            endVideo = false
            initAudioEncoder(
                dealPcm,
                loop,
                progressFreq,
                path,
                fd,
                offset,
                length
            )
            initVideoEncoder(
                loop,
                progressFreq,
                path,
                fd,
                offset,
                length
            )
        } else if (!dv) {
            prepareVideo = true
            endVideo = true
            initAudioEncoder(
                dealPcm,
                loop,
                progressFreq,
                path,
                fd,
                offset,
                length
            )
        } else {
            prepareAudio = true
            endAudio = true
            initVideoEncoder(
                loop,
                progressFreq,
                path,
                fd,
                offset,
                length
            )
        }
    }


    private fun check(
        decoderAudio: Boolean,
        decoderVideo: Boolean,
        hasAudio: Boolean,
        hasVideo: Boolean
    ) {

        //这个资源既没有视频也没有音频
        if (!hasAudio && !hasVideo) {
            throw RuntimeException("illegal resource")
        }

        //设定音频和视频都不解码,显然不能这样干
        if (!decoderAudio && !decoderVideo) {
            throw RuntimeException("video and audio must be decoded either way")
        }

        //设定不解码音频,但是这个资源不包含视频
        if (!decoderAudio && !hasVideo) {
            throw RuntimeException("Decodes only the video, but the resource has no video")
        }

        //设定不解码视频,但是这个资源不包含音频
        if (!decoderVideo && !hasAudio) {
            throw RuntimeException("Decodes only the audio, but the resource has no audio")
        }
    }

    private fun initAudioEncoder(
        dealPcm: Boolean,
        loop: Boolean,
        progressFreq: Long,
        path: String? = null,
        fd: FileDescriptor? = null,
        offset: Long = 0,
        length: Long = 0
    ) {
        val builder = AudioDecoder.Builder()
            .setLoop(loop)
            .setDealPcm(dealPcm)
            .setProgressFreq(progressFreq)
            .setInterceptor(object : BaseDecoder.NetInterceptor {
                override fun progressComparison(decoderProgress: Float): Boolean {
                    return false
                }
            })
            .setListener(object : AudioDecoder.AudioListener {
                override fun onAudioFormat(
                    sample: Int,
                    pcmBit: Int,
                    channel: Int,
                    duration: Long
                ) {
                    audioDuration = duration
                    dealAudioLongish()
                }

                override fun onReady() {
                    prepareAudio = true
                    holdOn()
                }

                override fun onAudioProgress(p: Float) {
                    if (audioLongish)
                        dealPlayProgress(p)
                }

                override fun onAudioData(audioData: ByteArray) {
                    listener?.onAudioData(audioData)
                }

                override fun onEnd() {
                    endAudio = true
                    holdEnd()
                }

                override fun onError(msg: String) {
                    listener?.onError(msg)
                }
            })

        //创建音频解码器
        audioDecoder = if (path != null) {
            builder.build(path)
        } else {
            builder.build(fd!!, offset, length)
        }
    }


    private fun initVideoEncoder(
        loop: Boolean,
        progressFreq: Long,
        path: String? = null,
        fd: FileDescriptor? = null,
        offset: Long = 0,
        length: Long = 0
    ) {
        drawer = VideoDrawer2(context!!) {
            val builder = VideoDecoder.Builder()
                .setLoop(loop)
                .setProgressFreq(progressFreq)
                .setSurface(it)
                .setInterceptor(object : BaseDecoder.NetInterceptor {
                    override fun progressComparison(decoderProgress: Float): Boolean {
                        return false
                    }
                })
                .setListener(object : VideoDecoder.VideoListener {
                    override fun onVideoFormat(
                        videoWidth: Int,
                        videoHeight: Int,
                        videoFrame: Int,
                        duration: Long
                    ) {
                        videoDuration = duration
                        drawer?.setSize(videoWidth, videoHeight)
                        dealAudioLongish()
                    }

                    override fun onReady() {
                        prepareVideo = true
                        holdOn()
                    }

                    override fun onVideoProgress(p: Float) {
                        if (!audioLongish)
                            dealPlayProgress(p)
                    }

                    override fun onEnd() {
                        endVideo = true
                        holdEnd()
                    }

                    override fun onError(msg: String) {
                        listener?.onError(msg)
                    }
                })

            //创建视频解码器
            videoDecoder = if (path != null) {
                builder.build(path)
            } else {
                builder.build(fd!!, offset, length)
            }
            videoDecoder?.prepare()
        }
    }

    private fun holdOn() {
        //音视频加载完毕
        if (prepareAudio && prepareVideo) {
            if (autoPlay) {
                startPlay()
            } else {
                listener?.onReady()
            }
        }
    }

    private fun holdEnd() {
        //音视频播放完毕
        if (endAudio && endVideo) {
            listener?.onEnd()
        }
    }


    fun prepare(mask: WaterMaskDrawer) {
        //加载
        audioDecoder?.prepare()
        render = BaseSurfaceRender()
        render?.addDrawer(drawer)

        this.mask = mask

        render?.addDrawer(mask)
        surfaceView?.setEGLContextClientVersion(2)
        surfaceView?.setRenderer(render)
        surfaceView?.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY


    }

    fun translateMask(dx: Float, dy: Float) {
//        mask?.translate(dx, dy)
//        drawer?.translate(dx, dy)
    }

    fun translate(dx: Float, dy: Float) {
//        mask?.translate(dx, dy)
        drawer?.translate(dx, dy)
    }


    fun startPlay() {
        if (prepareAudio && prepareVideo) {
            videoDecoder?.startPlay()
            audioDecoder?.startPlay()
        }
    }

    fun stopPlay() {
        audioDecoder?.stop()
        videoDecoder?.stop()
    }


    private fun dealAudioLongish() {
        audioLongish = audioDuration > videoDuration
    }

    private fun dealPlayProgress(p: Float) {
        playProgress = max(playProgress, p)
        val ct = System.currentTimeMillis()
        if (ct - lastPT >= progressFreq) {
            //满足回调频率后进行回调
            listener?.onProgress(playProgress)
            lastPT = ct
        } else if (playProgress == 100f) {
            listener?.onProgress(playProgress)
        }
    }

    /**
     * 检测是否包含音频
     */
    private fun checkAudio(extractor: MediaExtractor): Boolean {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime != null && mime.startsWith("audio/")) {
                return true
            }
        }
        return false
    }

    /**
     * 检测是否包含视频
     */
    private fun checkVideo(extractor: MediaExtractor): Boolean {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime != null && mime.startsWith("video/")) {
                return true
            }
        }
        return false
    }
}