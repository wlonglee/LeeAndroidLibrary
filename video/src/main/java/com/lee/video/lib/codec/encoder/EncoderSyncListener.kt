package com.lee.video.lib.codec.encoder

import android.media.MediaCodec
import android.media.MediaFormat
import java.nio.ByteBuffer

/**
 * 同步回调
 *@author lee
 *@date 2021/12/7
 */
interface EncoderSyncListener {
    /**
     * 编码进度 0~100,保留2位小数
     */
    fun onEncoderProgress(p: Float)

    /**
     * 参数配置
     */
    fun onFormat(format: MediaFormat)

    /**
     * 编码好的数据
     */
    fun onData(buffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo)
}