package com.lee.video.lib.codec.encoder

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import com.lee.video.lib.repack.FrameObject
import com.lee.video.lib.repack.Mp4Mixer
import java.util.concurrent.Executors

/**
 * 音频编码器
 * 音频编码器启用两个线程,一个线程持续压入待编码的数据,另一个线程持续取出编码好的数据
 *@author lee
 *@date 2021/11/25
 */
class AudioEncoder private constructor() {

    /**
     * 混合器
     */
    private lateinit var mixer: Mp4Mixer

    /**
     * 是否自动启动编码
     */
    private var autoEncode = false

    /**
     * 编码器
     */
    private var codec: MediaCodec? = null

    private var listener: EncoderAsyncListener? = null

    /**
     * 编解码任务是否处于运行中
     */
    private var isRun = false

    /**
     * 压入线程是否终止
     */
    @Volatile
    private var dos = false

    /**
     * 取出线程是否终止
     */
    @Volatile
    private var ros = false

    /**
     * 编解码任务等待时间-单位为微秒,1000微秒为1毫秒
     */
    private val waitTime: Long = 1000 * 16

    /**
     * 编解码数据队列-缓存当前待编解码的数据
     */
    private val frameQueue = arrayListOf<FrameObject>()

    /**
     * 线程池-执行编解码任务
     */
    private val pool = Executors.newScheduledThreadPool(2)

    /**
     * 构建器
     */
    class Builder {
        /**
         * 混合器对象
         */
        private lateinit var mixer: Mp4Mixer

        var listener: EncoderAsyncListener? = null

        /**
         * 是否自动启动编码
         */
        private var autoEncode = false

        /**
         * 采样率
         */
        private var sample = 48000

        /**
         * 通道数量 1-单声道 2-双声道
         */
        private var channelCount = 2

        /**
         * 比特率率, 96kbps fm音质
         */
        private var bitRate = 96000


        fun setListener(listener: EncoderAsyncListener): Builder {
            this.listener = listener
            return this
        }

        fun setMp4Mixer(mixer: Mp4Mixer): Builder {
            this.mixer = mixer
            return this
        }

        fun setAutoEncode(autoEncode: Boolean): Builder {
            this.autoEncode = autoEncode
            return this
        }

        fun setSample(sample: Int): Builder {
            this.sample = sample
            return this
        }

        fun setChannelCount(channelCount: Int): Builder {
            this.channelCount = channelCount
            return this
        }

        fun setBitRate(bitRate: Int): Builder {
            this.bitRate = bitRate
            return this
        }


        fun build(): AudioEncoder {
            val encoder = AudioEncoder()
            encoder.generateEncoder(mixer, sample, channelCount, bitRate, autoEncode, listener)
            return encoder
        }
    }

    private fun generateEncoder(
        mixer: Mp4Mixer,
        sample: Int,
        channelCount: Int,
        bitRate: Int,
        autoEncode: Boolean,
        listener: EncoderAsyncListener?
    ) {
        val aacProfile = MediaCodecInfo.CodecProfileLevel.AACObjectLC
        this.mixer = mixer
        this.autoEncode = autoEncode
        this.listener = listener
        val format =
            MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sample, channelCount)
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, aacProfile)

        format.setInteger(
            MediaFormat.KEY_CHANNEL_MASK,
            if (channelCount == 2) AudioFormat.CHANNEL_IN_STEREO else AudioFormat.CHANNEL_IN_MONO
        )
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 4 * 1024)
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channelCount)
        codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        codec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)


        //生成混合器所需的format
//        val trackFormat = MediaFormat.createAudioFormat(
//            MediaFormat.MIMETYPE_AUDIO_AAC,
//            sample,
//            channelCount
//        )
//        trackFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
//        trackFormat.setInteger("max-bitrate", bitRate)
//        trackFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channelCount)
//        trackFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, sample)
//
//        val aacSample = getADTSSample(sample)
//        val data = ByteArray(2)
//        data[0] = (aacProfile shl 3 or (aacSample ushr 1)).toByte()
//        data[1] = ((aacSample and 0x01 shl 7) + (channelCount shl 3)).toByte()
//        val csd = ByteBuffer.wrap(data)
//        trackFormat.setByteBuffer("csd-0", csd)
//        addTrack(trackFormat)
    }

    /**
     * 加载资源,就绪后会启动编码(配置了自动编码)或触发onReady回调(没有设定自动编码)
     */
    fun prepare() {
        if (autoEncode) {
            startEncoder()
        } else {
            listener?.onReady()
        }
    }

    /**
     * 启动编码
     */
    fun startEncoder() {
        codec?.start()
    }

    /**
     * 压入编码数据
     */
    fun encoder(frameObject: FrameObject) {
        synchronized(frameQueue) {
            frameQueue.add(frameObject)
        }
        if(!isRun){
            isRun=true
//            Log.e("lee","启动音频编码")
            pool.execute(coderTask)
            pool.execute(encoderTask)
        }
    }

    /**
     * 停止编码器
     */
    fun stopEncoder() {
        //压入一个空数据
        encoder(FrameObject())
//        Log.e("lee", "停止了音频编码器,剩余待处理数据:${frameQueue.size}")
    }

    /**
     * 压入编码数据任务
     */
    private val coderTask = Runnable {
        dos = false
        while (true) {
            val empty = synchronized(frameQueue) {
                frameQueue.isEmpty()
            }
            if (empty)
                continue

            if (frameQueue.isNotEmpty()) {
                val frame = synchronized(frameQueue) {
                    frameQueue.removeAt(0)
                }
                val index = codec!!.dequeueInputBuffer(waitTime)
                if (index < 0) {
                    continue
                }
                val inputBuffer = codec?.getInputBuffer(index) ?: continue
                if (frame.buffer == null) {
//                    Log.e("lee", "音频结束标志")
                    codec?.queueInputBuffer(
                        index,
                        0,
                        0,
                        0,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    )
                    break
                } else {
                    inputBuffer.put(frame.buffer!!)
                    codec?.queueInputBuffer(
                        index,
                        0,
                        frame.bufferInfo.size,
                        frame.bufferInfo.presentationTimeUs,
                        0
                    )
                }
            }
        }
        dos = true
        goEnd()
    }


    /**
     * 取出编码好的数据
     */
    private val encoderTask = Runnable {
        ros = false
        while (true) {
            //获取索引
            val bufferInfo = MediaCodec.BufferInfo()
            val index = codec!!.dequeueOutputBuffer(bufferInfo, waitTime)
            if (index >= 0) {
                //结束标志
                val flag = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM
                if (flag != 0) {
                    codec!!.releaseOutputBuffer(index, false)
                    break
                }
                mixer.writeAudioData(codec!!.getOutputBuffer(index)!!, bufferInfo)
                codec!!.releaseOutputBuffer(index, false)
            } else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                //参数发生了变化
                mixer.addAudioTrack(codec!!.outputFormat)
            }
        }
        ros = true
        goEnd()
    }

    private fun goEnd() {
        if (dos && ros) {
            codec?.stop()
            codec?.release()
            codec = null
//            Log.e("lee", "音频编码任务完成")
            listener?.onEnd()
        }
    }
    private fun getADTSSample(sample: Int): Int {
        var rate = 4
        when (sample) {
            96000 -> rate = 0
            88200 -> rate = 1
            64000 -> rate = 2
            48000 -> rate = 3
            44100 -> rate = 4
            32000 -> rate = 5
            24000 -> rate = 6
            22050 -> rate = 7
            16000 -> rate = 8
            12000 -> rate = 9
            11025 -> rate = 10
            8000 -> rate = 11
            7350 -> rate = 12
        }
        return rate
    }
}