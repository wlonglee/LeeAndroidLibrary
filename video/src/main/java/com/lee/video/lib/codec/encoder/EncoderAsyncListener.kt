package com.lee.video.lib.codec.encoder

import android.media.MediaCodec
import android.media.MediaFormat
import java.nio.ByteBuffer

/**
 * 编码相关异步回调
 *@author lee
 *@date 2021/11/25
 */
interface EncoderAsyncListener {
    /**
     * autoEncode为true的情况下不会触发该回调
     */
    fun onReady()

    /**
     * 编码进度 0~100,保留2位小数
     */
    fun onProgress(p: Float)

    /**
     * 参数配置
     */
    fun onFormat(format: MediaFormat)

    /**
     * 编码好的数据
     */
    fun onData(buffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo)

    /**
     * 编码结束
     */
    fun onEnd()
}