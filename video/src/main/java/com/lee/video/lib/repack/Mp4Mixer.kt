package com.lee.video.lib.repack

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import java.nio.ByteBuffer

/**
 * mp4混合器封装
 *@author lee
 *@date 2021/11/25
 */
class Mp4Mixer(outPath: String) {
    private var mixer: MediaMuxer = MediaMuxer(outPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

    /**
     * 视频轨道索引
     */
    private var videoTrackIndex: Int = -1

    /**
     * 音频轨道索引
     */
    private var audioTrackIndex: Int = -1

    /**
     * 视频轨道是否添加
     */
    private var videoAdd = false

    /**
     * 音频轨道是否添加
     */
    private var audioAdd = false

    /**
     * 视频混合完成
     */
    private var videoEnd = false

    /**
     * 音频混合完成
     */
    private var audioEnd = false

    /**
     * 开始标志
     */
    var start = false

    fun addVideoTrack(format: MediaFormat) {
        if (videoAdd) return
        videoTrackIndex = try {
            mixer.addTrack(format)
        } catch (e: Exception) {
            return
        }
        videoAdd = true
        startMixer()
    }

    fun addAudioTrack(format: MediaFormat) {
        if (audioAdd) return
        audioTrackIndex = try {
            mixer.addTrack(format)
        } catch (e: Exception) {
            return
        }
        audioAdd = true
        startMixer()
    }


    /**
     * 设定不需要音频
     */
    fun setNoAudio() {
        audioAdd = true
        audioEnd = true
    }

    /**
     * 设定不需要视频
     */
    fun setNoVideo() {
        videoAdd = true
        videoEnd = true
    }

    /**
     * 启动混合器
     */
    private fun startMixer() {
        if (audioAdd && videoAdd) {
            start = true
            mixer.start()
        }
    }

    /**
     * 写入视频数据
     */
    fun writeVideoData(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        if (!start) {
            return
        }
        mixer.writeSampleData(videoTrackIndex, byteBuffer, bufferInfo)
    }

    /**
     * 写入音频数据
     */
    fun writeAudioData(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        if (!start) {
            return
        }
        mixer.writeSampleData(audioTrackIndex, byteBuffer, bufferInfo)
    }


    /**
     * 结束视频写入
     */
    fun releaseVideo() {
        videoEnd = true
        release()
    }

    /**
     * 结束音频写入
     */
    fun releaseAudio() {
        audioEnd = true
        release()
    }


    /**
     * 释放资源
     */
    private fun release() {
        if (videoEnd && audioEnd) {
            mixer.stop()
            mixer.release()
            start = false
            videoAdd = false
            audioAdd = false
        }
    }
}