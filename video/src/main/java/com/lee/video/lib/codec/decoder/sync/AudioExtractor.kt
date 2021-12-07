package com.lee.video.lib.codec.decoder.sync

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import com.lee.video.lib.codec.encoder.EncoderSyncListener
import java.nio.ByteBuffer
import kotlin.math.min

/**
 * 音频数据提取器
 *@author lee
 *@date 2021/12/6
 */
class AudioExtractor private constructor() {
    var listener: EncoderSyncListener? = null

    /**
     * 提取音频数据
     */
    private lateinit var extractor: MediaExtractor

    /**
     * 缓存区
     */
    private var buffer = ByteBuffer.allocate(500 * 1024)
    private var bufferInfo = MediaCodec.BufferInfo()

    /**
     * 音频时长,用于计算提取进度
     */
    private var duration: Long = 1L

    /**
     * 提取是否完成
     */
    private var dos = false

    class Builder {
        var listener: EncoderSyncListener? = null

        /**
         * 文件路径
         */
        private var path: String? = null

        fun setListener(listener: EncoderSyncListener): Builder {
            this.listener = listener
            return this
        }

        fun setPath(path: String): Builder {
            this.path = path
            return this
        }

        fun build(): AudioExtractor {
            val audioExtractor = AudioExtractor()
            audioExtractor.generateEncoder(path!!, listener)
            return audioExtractor
        }
    }

    private fun generateEncoder(path: String, listener: EncoderSyncListener?) {
        this.listener = listener
        extractor = MediaExtractor()
        extractor.setDataSource(path)
    }

    fun prepare() {
        dos = false
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime != null && mime.startsWith("audio/")) {
                //选择指定轨道
                extractor.selectTrack(i)
                duration = format.getLong(MediaFormat.KEY_DURATION)
                listener?.onFormat(format)
                break
            }
        }
    }


    /**
     * 提取数据
     */
    fun extract() {
        if (dos)
            return
        buffer.clear()
        val readSampleCount = extractor.readSampleData(buffer, 0)
        if (readSampleCount < 0) {
            listener?.onEncoderProgress(100f)
            dos = true
        } else {
            listener?.onEncoderProgress(
                min(
                    ((bufferInfo.presentationTimeUs * 1f / duration) * 10000).toInt() / 100f,
                    100f
                )
            )
            bufferInfo.set(
                0,
                readSampleCount,
                extractor.sampleTime,
                extractor.sampleFlags
            )
            listener?.onData(buffer, bufferInfo)
            extractor.advance()
        }
    }


    fun isEnd(): Boolean {
        return dos
    }

    fun release() {
        extractor.release()
    }
}