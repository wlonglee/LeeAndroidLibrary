package com.lee.video.lib.repack

import android.media.MediaCodec
import java.nio.ByteBuffer

/**
 *@author lee
 *@date 2021/11/25
 */
class FrameObject {
    var buffer: ByteBuffer? = null
    var bufferInfo = MediaCodec.BufferInfo()
        private set

    fun setBufferInfo(info: MediaCodec.BufferInfo) {
        bufferInfo.set(info.offset, info.size, info.presentationTimeUs, info.flags)
    }
}