package com.lee.video.lib.codec.base

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import com.lee.video.lib.repack.FrameObject
import com.lee.video.lib.repack.Mp4Mixer
import java.nio.ByteBuffer
import java.util.concurrent.Executors


/**
 * 基础编码器
 *@author lee
 *@date 2021/11/25
 */
abstract class BaseEncoder {

    interface EncoderListener {
        /**
         * autoEncode为true的情况下不会触发该回调,需要自行启动编码
         */
        fun onReady()

        /**
         * 编码进度 0~100,保留2位小数
         */
        fun onProgress(p: Float)

        /**
         * 编码结束
         */
        fun onEnd()
    }


    open class SimpleEncoderListener : EncoderListener {
        override fun onReady() {
        }

        override fun onProgress(p: Float) {
        }

        override fun onEnd() {
        }

    }

    /**
     * 混合器
     */
    protected lateinit var mixer: Mp4Mixer

    protected var listener: EncoderListener? = null


    /**
     * 是否自动启动编码
     */
    protected var autoEncode = false

    /**
     * 编码器
     */
    protected var codec: MediaCodec? = null

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


    open fun isVideo(): Boolean {
        return false
    }

    /**
     * 压入编码数据
     */
    fun encoder(frameObject: FrameObject) {
        synchronized(frameQueue) {
            frameQueue.add(frameObject)
        }
        if (!isRun) {
            isRun = true
            pool.execute(coderTask)
            pool.execute(encoderTask)
        }
    }


    /**
     * 停止编码器
     */
    fun stopEncoder() {
        Log.e("lee", "停止了编码器:${frameQueue.size}")
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
                if (isVideo()) {
                    if (frame.buffer == null) {
                        Log.e("lee", "视频结束标志")
                        codec?.signalEndOfInputStream()
                        break
                    }
                } else {
                    val index = codec!!.dequeueInputBuffer(waitTime)
                    if (index < 0) {
                        continue
                    }
                    val inputBuffer = codec?.getInputBuffer(index) ?: continue
                    if (frame.buffer == null) {
                        Log.e("lee", "音频结束标志")
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
                    Log.e("lee", "终止任务")
                    codec!!.releaseOutputBuffer(index, false)
                    break
                }
                writeData(codec!!.getOutputBuffer(index)!!, bufferInfo)

                codec!!.releaseOutputBuffer(index, false)

            } else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                //参数发生了变化
                addTrack(codec!!.outputFormat)
            }
        }
        ros = true
        goEnd()
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

    abstract fun writeData(
        outputBuffer: ByteBuffer,
        bufferInfo: MediaCodec.BufferInfo
    )

    abstract fun addTrack(format: MediaFormat)

    fun startEncoder() {
        codec?.start()
    }


    private fun goEnd() {
        Log.e("lee", "ros:$ros,$dos")
        if (dos && ros) {
            codec?.stop()
            codec?.release()
            codec = null

            listener?.onEnd()
        }

    }

    private fun configEncoderWithCQ(codec: MediaCodec?, outputFormat: MediaFormat) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // 本部分手机不支持 BITRATE_MODE_CQ 模式，有可能会异常
            outputFormat.setInteger(
                MediaFormat.KEY_BITRATE_MODE,
                MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ
            )
        }
    }

    private fun configEncoderWithVBR(codec: MediaCodec?, outputFormat: MediaFormat) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            outputFormat.setInteger(
                MediaFormat.KEY_BITRATE_MODE,
                MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR
            )
        }
    }

    private fun configEncoderWithCBR(codec: MediaCodec?, outputFormat: MediaFormat) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            outputFormat.setInteger(
                MediaFormat.KEY_BITRATE_MODE,
                MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR
            )
        }
    }
}