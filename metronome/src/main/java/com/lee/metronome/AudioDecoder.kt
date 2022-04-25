package com.lee.metronome

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build

/**
 * 解码音频文件,提取pcm流
 *
 * @author lee by 2020/8/13
 */
internal class AudioDecoder {
    interface AudioListener {
        fun onPrepare(sample: Int, pcm: Int, channels: Int)
        fun onDecoderError(error: String)
        fun onAudioData(data: ByteArray)
        fun onEnd()
    }

    open class SampleListener:AudioListener{
        override fun onPrepare(sample: Int, pcm: Int, channels: Int) {
        }

        override fun onDecoderError(error: String) {
        }

        override fun onAudioData(data: ByteArray) {
        }

        override fun onEnd() {
        }
    }

    var listener: AudioListener? = null
    private var codec: MediaCodec? = null
    private var extractor: MediaExtractor? = null
    private var format: MediaFormat? = null
    private var bufferInfo: MediaCodec.BufferInfo? = null
    private var channels = 2
    private var sample = 48000
    private var pcm = AudioFormat.ENCODING_PCM_16BIT
    private var decoderTask: DecoderThread? = null

    /**
     * 设置解析的音频文件路径
     */
    fun setAudioPath(path: String) {
        extractor = MediaExtractor()
        try {
            extractor?.setDataSource(path)
            loadAudio()
        } catch (e: Exception) {
            e.printStackTrace()
            onError("setDataSource error:$e")
            MetronomeLog.log("setDataSource error:$e")
        }
    }

    private fun onError(msg: String) {
        listener?.onDecoderError(msg)
    }

    private fun onAudioData(data: ByteArray) {
        listener?.onAudioData(data)
    }


    private fun loadAudio() {
        for (i in 0 until extractor!!.trackCount) {
            val format = extractor!!.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime != null && mime.startsWith("audio/")) {
                this.format = format
                configCodec(i, mime)
                break
            }
        }
        extractor?.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        if (codec == null) {
            //未加载到资源
            extractor?.release()
            extractor = null
            onError("no audio resource")
            MetronomeLog.log("no audio resource")
        } else {
            startTask()
        }
    }

    private fun configCodec(index: Int, mime: String) {
        if (format!!.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
            channels = format!!.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
//            MetronomeLog.log("channels:$channels")
        }
        if (format!!.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
            sample = format!!.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        }
        if (format!!.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
            pcm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                format!!.getInteger(MediaFormat.KEY_PCM_ENCODING)
            } else {
                AudioFormat.ENCODING_PCM_16BIT
            }
        }
        extractor?.selectTrack(index)
        try {
            codec = MediaCodec.createDecoderByType(mime)
            codec?.configure(format, null, null, 0)
            bufferInfo = MediaCodec.BufferInfo()
            decoderTask = DecoderThread()
            listener?.onPrepare(sample, pcm, 1)
        } catch (e: Exception) {
            e.printStackTrace()
            onError("create decoder error:$e")
            MetronomeLog.log("create decoder error:$e")
        }
    }

    private fun startTask() {
        codec?.start()
        decoderTask?.start()
    }

    private fun release() {
        codec?.stop()
        codec?.release()
        extractor?.release()
        codec = null
        extractor = null
        format = null
        decoderTask = null
        listener?.onEnd()
    }

    private inner class DecoderThread : Thread() {
        @Volatile
        private var dos = false

        @Volatile
        private var ros = false
        override fun run() {
            try {
                decoder()
            } catch (e: Exception) {
                e.printStackTrace()
                onError("decoder err:$e")
                MetronomeLog.log("decoder err:$e")
            }
        }

        private fun decoder() {
            while (true) {
                if (!dos) dealDecoderData()
                if (!ros) dealRenderData()
                if (dos && ros) {
                    break
                }
            }
//            MetronomeLog.log("task end")
            //解码完成
            release()
        }

        private fun dealDecoderData() {
            val index = codec!!.dequeueInputBuffer((1000 * 16).toLong())
            if (index >= 0) {
                val inputBuffer = codec?.getInputBuffer(index)
                if (inputBuffer == null) {
                    MetronomeLog.log("inputBuffer is null")
                    return
                }
                val sampleSize = extractor!!.readSampleData(inputBuffer, 0)
                if (sampleSize < 0) {
                    codec?.queueInputBuffer(
                        index,
                        0,
                        0,
                        0,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    )
                    dos = true
                } else {
                    val sampleTime = extractor!!.sampleTime
                    codec?.queueInputBuffer(
                        index,
                        0,
                        sampleSize,
                        sampleTime,
                        0
                    )
                    extractor?.advance()
                }
            }
        }

        private fun dealRenderData() {
            val index = codec!!.dequeueOutputBuffer(bufferInfo!!, (1000 * 16).toLong())
            if (index >= 0) {
                val pts = bufferInfo!!.presentationTimeUs
                if (pts >= 0) {
                    render(index)
                }
                val flag = bufferInfo!!.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM
                if (flag != 0) {
                    ros = true
                }
            }
        }

        private fun render(index: Int) {
            val buffer = codec?.getOutputBuffer(index)
            if (buffer == null) {
                MetronomeLog.log("buffer is null")
                return
            }
            val chunk = ByteArray(bufferInfo!!.size)
            buffer[chunk]
            buffer.clear()
            if (channels == 1) {
                onAudioData(chunk)
            } else {
                onAudioData(channelSingleLeft(chunk))
            }
            codec?.releaseOutputBuffer(index, false)
        }

        private fun channelSingleLeft(chunk: ByteArray): ByteArray {
            val singleChunk = ByteArray(chunk.size / channels)
            var i = 0
            var count = 0
            while (i < chunk.size) {
                singleChunk[count] = chunk[i]
                singleChunk[count + 1] = chunk[i + 1]
                count += 2
                i += channels * 2
            }
            return singleChunk
        }
    }
}