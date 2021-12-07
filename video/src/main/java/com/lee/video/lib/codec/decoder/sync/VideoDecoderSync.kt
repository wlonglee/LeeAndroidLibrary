package com.lee.video.lib.codec.decoder.sync

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import android.view.Surface

/**
 * 视频画面解码-同步,便于与EGL绑定,用于视频重编码
 *@author lee
 *@date 2021/12/6
 */
class VideoDecoderSync {
    /**
     * 输入是否结束
     */
    @Volatile
    private var dos = false

    /**
     * 渲染是否结束
     */
    @Volatile
    private var ros = false

    /**
     * 管理egl
     */
    private var egl: EglVideo? = null

    /**
     * 预期导出FPS
     */
    var extractFps: Int = 30

    /**
     * 视频宽
     */
    var videoWidth: Int = 1

    /**
     * 视频高
     */
    var videoHeight: Int = 1

    /**
     * 视频时长
     */
    var duration: Long = 1L

    /**
     * 数据提取
     */
    private lateinit var extractor: MediaExtractor

    /**
     * 解码器
     */
    private lateinit var codec: MediaCodec

    /**
     * 解码任务等待时间-单位为微秒,1000微秒为1毫秒
     */
    private val waitTime: Long = 1000 * 1


    /**
     * 是否压缩帧率
     * 超过50帧的压缩为一半
     */
    private var enableAbandon = false

    /**
     * enableAbandon为true的情况下,隔帧丢弃数据标志
     */
    private var abandon = true

    private var format: MediaFormat? = null


    fun create(inPath: String) {
        extractor = MediaExtractor()
        extractor.setDataSource(inPath)
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime != null && mime.startsWith("video/")) {
                //获取配置信息
                videoWidth = format.getInteger(MediaFormat.KEY_WIDTH)
                videoHeight = format.getInteger(MediaFormat.KEY_HEIGHT)
                extractFps = format.getInteger(MediaFormat.KEY_FRAME_RATE)
                if (extractFps >= 50) {
                    extractFps /= 2
                    enableAbandon = true
                }
                duration = format.getLong(MediaFormat.KEY_DURATION)
                //选择指定轨道
                extractor.selectTrack(i)
                this.format = format
                //创建解码器
                codec = MediaCodec.createDecoderByType(mime)
                break
            }
        }
    }

    fun configEgl(egl: EglVideo) {
        this.egl = egl
        egl.setSize(videoWidth, videoHeight)
        codec.configure(format, Surface(egl.getSurface()), null, 0)
    }

    fun prepare() {
        dos = false
        ros = false
        codec.start()
    }

    /**
     * 解码数据
     */
    fun decoder() {
        if (dos)
            return
        val index = codec.dequeueInputBuffer(waitTime)
        if (index < 0) return
        val diInputBuffer = codec.getInputBuffer(index) ?: return
        val sampleSize = extractor.readSampleData(diInputBuffer, 0)
        if (sampleSize < 0) {
            //解码完毕
            codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            dos = true
        } else {
            codec.queueInputBuffer(index, 0, sampleSize, extractor.sampleTime, 0)
            //获取下一帧数据
            extractor.advance()
        }
    }

    fun render() {
        if (ros)
            return
        val bufferInfo = MediaCodec.BufferInfo()
        val index = codec.dequeueOutputBuffer(bufferInfo, waitTime)
        if (index >= 0) {
            val buffer = codec.getOutputBuffer(index)
            if (buffer == null) {
                codec.releaseOutputBuffer(index, false)
            } else {
                var render = true
                //启用FPS减半的情况下,做对应的丢帧处理
                if (enableAbandon) {
                    render = abandon
                    abandon = !abandon
                }

                if (render) {
                    //渲染输出
                    val time = bufferInfo.presentationTimeUs
                    egl?.swapBuffers(time)
                }

                codec.releaseOutputBuffer(index, render)
            }
        }
        //结束标志
        val flag = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM
        if (flag != 0) {
            ros = true
        }
    }

    fun isEnd(): Boolean {
        return dos && ros
    }

    fun release() {
        codec.stop()
        codec.release()
        extractor.release()
    }

}