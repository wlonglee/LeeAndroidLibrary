package com.lee.video.lib.codec.encoder

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.view.Surface
import kotlin.math.max
import kotlin.math.min

/**
 * 视频编码器
 * 视频重编码需要考虑gl效果、裁剪等功能,需要与EGL绑定,从EGL中获取视频帧,不存在压入数据的操作
 *@author lee
 *@date 2021/12/6
 */
class VideoEncoder private constructor() {
    /**
     * 视频数据输入表面
     */
    lateinit var inputSurface: Surface

    /**
     * 编码器
     */
    private lateinit var codec: MediaCodec

    /**
     * 监听器
     */
    private var listener: EncoderSyncListener? = null

    /**
     * 原始视频的长度值,计算编码进度
     */
    private var duration: Long = 1

    /**
     * 输入是否结束
     */
    @Volatile
    private var eos = false

    /**
     * 编码是否结束
     */
    @Volatile
    private var ros = false

    /**
     * 编解码任务等待时间-单位为微秒,1000微秒为1毫秒
     */
    private val waitTime: Long = 1000 * 1

    /**
     * 构建器
     */
    class Builder {

        var listener: EncoderSyncListener? = null

        /**
         * 生成的视频宽
         */
        private var videoWidth = 720

        /**
         * 生成的视频高
         */
        private var videoHeight = 1280

        /**
         * 生成的视频帧率
         */
        private var fps = 30

        /**
         * 原始视频的长度
         */
        private var duration = 1L

        /**
         * 生成的i帧间隔
         */
        private var iInterval = 0.2f

        /**
         * 取值范围0~1
         * 清晰度,该值越高视频画面越清晰
         */
        private var clarity = 0.3f

        fun setListener(listener: EncoderSyncListener): Builder {
            this.listener = listener
            return this
        }


        fun setVideoWidth(videoWidth: Int): Builder {
            this.videoWidth = videoWidth
            return this
        }

        fun setVideoHeight(videoHeight: Int): Builder {
            this.videoHeight = videoHeight
            return this
        }

        fun setOriginalInfo(fps: Int, duration: Long): Builder {
            this.fps = fps
            this.duration = duration
            return this
        }

        fun setIInterval(iInterval: Float): Builder {
            this.iInterval = iInterval
            return this
        }

        fun setClarity(clarity: Float): Builder {
            this.clarity = min(max(0f,clarity),1f)
            return this
        }

        fun build(): VideoEncoder {
            val encoder = VideoEncoder()
            encoder.generateEncoder(
                videoWidth,
                videoHeight,
                fps,
                duration,
                iInterval,
                clarity,
                listener
            )
            return encoder
        }
    }

    private fun generateEncoder(
        videoWidth: Int,
        videoHeight: Int,
        fps: Int,
        duration: Long,
        iInterval: Float,
        clarity: Float,
        listener: EncoderSyncListener?
    ) {
        this.duration = duration
        this.listener = listener
        val bitrate = (videoWidth * videoHeight * fps * clarity).toInt()
        val format =
            MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, videoWidth, videoHeight)
        format.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, fps)
        format.setFloat(
            MediaFormat.KEY_I_FRAME_INTERVAL, //帧/秒  关键帧间隔的时间 -值表示只有第一帧是关键帧,0表示每一帧都是关键帧,大于0表示每 iInterval*fps出现一个关键帧
            iInterval
        )
        codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        //创建输入表面
        inputSurface = codec.createInputSurface()
    }


    /**
     * 启动编码
     */
    fun prepare() {
        eos = false
        ros = false
        codec.start()
    }

    fun render() {
        if (ros)
            return
        val bufferInfo = MediaCodec.BufferInfo()
        val index = codec.dequeueOutputBuffer(bufferInfo, waitTime)
        if (index >= 0) {
            //结束标志
            val flag = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM
            if (flag != 0) {
                listener?.onEncoderProgress(100f)
                ros = true
            } else {
                listener?.onEncoderProgress(min(((bufferInfo.presentationTimeUs * 1f / duration) * 10000).toInt() / 100f,100f))
                listener?.onData(codec.getOutputBuffer(index)!!, bufferInfo)
            }
            codec.releaseOutputBuffer(index, false)
        } else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            //参数发生了变化
            listener?.onFormat(codec.outputFormat)
        }
    }

    fun isEnd(): Boolean {
        return ros
    }

    /**
     * 停止编码器
     */
    fun stopEncoder() {
        if (eos)
            return
        eos = true
        codec.signalEndOfInputStream()
    }

    fun release() {
        codec.stop()
        codec.release()
    }
}