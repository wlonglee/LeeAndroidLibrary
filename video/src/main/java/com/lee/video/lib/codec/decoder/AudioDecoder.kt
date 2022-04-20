package com.lee.video.lib.codec.decoder

import android.content.res.AssetFileDescriptor
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import com.lee.video.lib.audio.AudioTR
import com.lee.video.lib.audio.AudioUtil
import com.lee.video.lib.codec.base.BaseDecoder
import java.io.FileDescriptor
import java.io.IOException

/**
 * 音频信息解码-单独使用可作为音频播放器,支持几乎所有音频格式文件,可无缝循环
 *@author lee
 *@date 2021/11/25
 */
class AudioDecoder private constructor() : BaseDecoder() {
    interface AudioListener {
        /**
         * 音轨参数回调
         *
         * @param sample   采样率  44100、48000 ...
         * @param pcmBit   pcm位数- AudioFormat.ENCODING_PCM_16BIT、AudioFormat.ENCODING_PCM_8BIT ...
         * @param channel  音轨通道- AudioFormat.CHANNEL_OUT_MONO、AudioFormat.CHANNEL_OUT_STEREO ...
         * @param duration 音频时长 单位为毫秒值
         */
        fun onAudioFormat(sample: Int, pcmBit: Int, channel: Int, duration: Long)

        /**
         * 准备就绪,可以启动播放,配置了自动播放(autoPlay为true)的情况下该函数不会触发
         */
        fun onReady()

        /**
         * 音频播放进度 从0~100,保留两位小数
         */
        fun onAudioProgress(p: Float)

        /**
         * 解码的音频数据,dealPcm为true时才会回调
         *
         * @param audioData 音频数据
         * @param p 该帧音频的进度值 0~100,保留两位小数,用于自行处理音频时记录该帧数据的进度
         */
        fun onAudioData(audioData: ByteArray, p: Float)

        /**
         * 播放器停止,资源也已释放,如果再次使用需要重新构建对象
         */
        fun onEnd()

        /**
         * 播放错误
         */
        fun onError(msg: String)

    }

    /**
     * 空实现
     */
    open class SimpleListener : AudioListener {
        override fun onAudioFormat(sample: Int, pcmBit: Int, channel: Int, duration: Long) {
        }

        override fun onReady() {
        }

        override fun onAudioProgress(p: Float) {
        }

        override fun onAudioData(audioData: ByteArray, p: Float) {
        }

        override fun onEnd() {
        }

        override fun onError(msg: String) {
        }
    }

    /**
     * 是否自行处理解码后的pcm流
     * true 音频解码产生的pcm数据将通过回调函数给出,不会进行播放
     * false 使用内置的audioTrack进行音频的播放,音频流不会给出
     */
    private var dealPcm = false

    /**
     * 音轨通道- AudioFormat.CHANNEL_OUT_MONO、AudioFormat.CHANNEL_OUT_STEREO ...
     */
    private var channel = 0

    /**
     * 采样率  44100、48000 ...
     */
    private var sample = 0

    /**
     * pcm位数- AudioFormat.ENCODING_PCM_16BIT、AudioFormat.ENCODING_PCM_8BIT ...
     */
    private var pcmBit = 0

    /**
     * 播放音频
     */
    private var track: AudioTR? = null

    /**
     * 监听器
     */
    private var listener: AudioListener? = null

    /**
     * 解码的音频文件路径
     */
    var path: String? = null

    /**
     * 构建器
     */
    class Builder {
        /**
         * 是否自动启动播放
         * true-音频加载后将自动播放
         */
        private var autoPlay = false

        /**
         * 是否自行处理解码后的pcm流
         * true 音频解码产生的pcm数据将通过回调函数给出,需自行控制播放
         * false 使用内置的audioTrack进行音频的播放
         */
        private var dealPcm = false

        /**
         * 是否循环
         * true 音频将会进行循环解码,直到主动停止
         */
        private var loop = false

        /**
         * 进度回调频率,每隔多少毫秒回调一次播放进度
         */
        private var progressFreq: Long = 200

        /**
         * 监听器
         */
        private var listener: AudioListener? = null

        /**
         * 网络音频进度对比拦截器,如果不是网络音频不需要实现该操作
         */
        private var interceptor: NetInterceptor? = null


        fun setAutoPlay(autoPlay: Boolean): Builder {
            this.autoPlay = autoPlay
            return this
        }

        fun setDealPcm(dealPcm: Boolean): Builder {
            this.dealPcm = dealPcm
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

        fun setListener(listener: AudioListener?): Builder {
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
        fun build(path: String): AudioDecoder {
            val player = AudioDecoder()
            player.generate(
                autoPlay,
                dealPcm,
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
        fun build(afd: AssetFileDescriptor): AudioDecoder {
            return build(afd.fileDescriptor, afd.startOffset, afd.length)
        }

        fun build(fd: FileDescriptor, offset: Long, length: Long): AudioDecoder {
            val player = AudioDecoder()
            player.generate(
                autoPlay,
                dealPcm,
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
        autoPlay: Boolean,
        dealPcm: Boolean,
        loop: Boolean,
        progressFreq: Long,
        path: String,
        listener: AudioListener?,
        interceptor: NetInterceptor?
    ) {
        this.autoPlay = autoPlay
        this.dealPcm = dealPcm
        this.loop = loop
        this.progressFreq = progressFreq
        this.listener = listener
        this.interceptor = interceptor
        this.path = path

        extractor = MediaExtractor()
        playStatus = State.NO_PLAY
        try {
            extractor?.setDataSource(path)
        } catch (e: IOException) {
            onError("setDataSource error:$e")
        }
    }

    private fun generate(
        autoPlay: Boolean,
        dealPcm: Boolean,
        loop: Boolean,
        progressFreq: Long,
        fd: FileDescriptor,
        offset: Long,
        length: Long,
        listener: AudioListener?
    ) {
        this.autoPlay = autoPlay
        this.dealPcm = dealPcm
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
        try {
            //遍历获取音轨轨道
            for (i in 0 until extractor!!.trackCount) {
                val format = extractor!!.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime != null && mime.startsWith("audio/")) {
                    this.format = format
                    //获取配置信息
                    configCodec(false)
                    //选择指定轨道
                    extractor?.selectTrack(i)
                    //创建解码器
                    codec = MediaCodec.createDecoderByType(mime)
                    codec!!.configure(format, null, null, 0)
                    //创建缓存区
                    bufferInfo = MediaCodec.BufferInfo()
                    break
                }
            }
            //从最开始进行提取
            extractor?.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            when {
                codec == null -> {
                    extractor?.release()
                    extractor = null
                    onError("no audio resource")
                }
                autoPlay -> {
                    startPlay()
                }
                else -> {
                    onReady()
                }
            }
        } catch (e: IOException) {
            onError("setDataSource error:$e")
        }
    }

    override fun configCodec(flag: Boolean) {
        if (format!!.containsKey(MediaFormat.KEY_DURATION)) {
            //音频时长
            duration = format!!.getLong(MediaFormat.KEY_DURATION) / 1000
        }

        if (format!!.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
            //音轨是多少声道的
            val channels = format!!.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            //将通道数转换为声道的配置,该值可用于AudioTrack创建
            channel = AudioUtil.getChannelOut(channels)
        }

        if (format!!.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
            //获取采样率,该值可用于AudioTrack创建
            sample = format!!.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        }

        if (format!!.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
            //获取pcm位数,该值可用于AudioTrack创建
            pcmBit = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                format!!.getInteger(MediaFormat.KEY_PCM_ENCODING)
            } else {
                AudioFormat.ENCODING_PCM_16BIT
            }
        }


        if (pcmBit == AudioFormat.ENCODING_INVALID) {
            pcmBit = AudioFormat.ENCODING_PCM_16BIT
        }

        if (flag) {
            log("音频信息:$sample,$pcmBit,$channel,duration:$duration")
            onAudioFormat()
            if (!dealPcm) {
                track = AudioTR.AudioTrackBuilder()
                    .setAuto(true)
                    .setSampleRate(sample)
                    .setPcmEncodeBit(pcmBit)
                    .setChannel(channel)
                    .build()
            }
        }
    }


    override fun onRender(index: Int) {
        val buffer = codec?.getOutputBuffer(index) ?: return
        //数据帧的时间值
        val pts = bufferInfo!!.presentationTimeUs / 1000
        val p = (pts * 100f / duration * 100).toInt() / 100f //保留2位小数
        //进度回调
        val ct = System.currentTimeMillis()
        if (ct - lastPT >= progressFreq) {
            //满足回调频率后进行回调
            listener?.onAudioProgress(p)
            lastPT = ct
        } else if (p >= 100f) {
            listener?.onAudioProgress(100f)
        }

        //记录开始节点
        if (startTime == 0L || lastPts > pts && loop) {
            startTime = System.currentTimeMillis()
//            Log.e("lee", "音频开始了")
        }
        //更新上一帧时间
        lastPts = pts

        //获取音频信息
        val chunk = ByteArray(bufferInfo!!.size)
        buffer.get(chunk)
        buffer.clear()

        if (dealPcm) {
            //自行处理音频,通过回调给出
            listener?.onAudioData(chunk, p.coerceAtMost(100f))
        } else {
            //播放音频
            track?.writeData(chunk, chunk.size)
        }
        codec?.releaseOutputBuffer(index, false)
    }

    private fun onAudioFormat() {
        listener?.onAudioFormat(sample, pcmBit, channel, duration)
    }

    private fun onReady() {
        listener?.onReady()
    }

    override fun onError(s: String) {
        listener?.onError(s)
    }

    override fun onEnd() {
        listener?.onEnd()
    }
}