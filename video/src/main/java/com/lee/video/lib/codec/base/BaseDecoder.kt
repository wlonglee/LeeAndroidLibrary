package com.lee.video.lib.codec.base

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.util.concurrent.Executors

/**
 * 解码器基类
 * 网络资源播放需自行实现下载逻辑，解码器不负责下载存储处理,播放网络资源参考NetInterceptor说明,在拦截器中处理当前是否需要暂停解码和恢复解码即可(对比下载进度与解码进度)
 *@author lee
 *@date 2021/11/25
 */
abstract class BaseDecoder {
    var showLog = true

    fun log(msg: String) {
        if (showLog) {
            Log.e("lee", msg)
        }
    }

    /**
     * 网络进度对比拦截
     * 针对网络资源,有两种播放方式
     * 1文件下载完成后设置播放路径,即普通的播放方式,不需要实现该拦截器
     * 2下载达到一定进度后设置文件下载路径进行解码播放,该方式需要实现该拦截器进行进度的实时比对校验.
     * !!!需要注意的是，如果使用方式2,文件下载的时候创建文件需指定大小,解码器创建时传输的文件句柄大小是多少则只会解码这样多的数据
     * !!!例如文件大小5M。但是创建的时候没指定大小,下载一点数据文件就追加一点数据,下载了10%后进行2操作播放,此时解码器认为该文件只有0.5M,解码至此将结束,该处理位于C底层无法更改
     */
    interface NetInterceptor {
        /**
         * 进行解码进度与下载进度的比对
         *
         * @param decoderProgress 当前解码进度,范围0~100保留2位小数
         * @return  true 解码进度与网络进度的差值在限定范围内,将暂停解码例如解码进度 56%,下载进度 60%,限定范围为5%,则应该暂停解码,需要注意的是如果解码进度96%下载进度100%,既下载完成后不需要暂停
         *          false 持续解码
         */
        fun progressComparison(decoderProgress: Float): Boolean
    }

    /**
     * 解码状态信息
     */
    enum class State {
        //未开始
        NO_PLAY,

        //播放中
        PLAY,

        //暂停
        PAUSE,

        //进入跳转状态
        SEEK_START,

        //跳转中
        SEEK,

        //单独处理数据时,进入暂停状态
        DATA_WAIT,

        //停止
        STOP
    }

    /**
     * 跳转状态
     */
    enum class SeekState {
        //正在朝前跳转
        SEEK_BEFORE,

        //正在朝后跳转
        SEEK_AFTER,

        //正常播放
        SEEK_NONE
    }

    /**
     * 是否自动启动解码
     * true-资源加载后将自动解码
     */
    protected var autoPlay = false

    /**
     * 是否循环
     * true 将会进行循环解码,直到主动停止
     */
    protected var loop = false

    /**
     * 网络资源进度对比拦截器,如果不是网络资源不需要赋值
     */
    protected var interceptor: NetInterceptor? = null

    /**
     * 解码器
     */
    protected var codec: MediaCodec? = null

    /**
     * 文件提取器
     */
    protected var extractor: MediaExtractor? = null

    /**
     * 格式参数信息
     */
    protected var format: MediaFormat? = null

    /**
     * 缓冲区
     */
    protected var bufferInfo: MediaCodec.BufferInfo? = null


    /**
     * 播放标志
     */
    @Volatile
    protected var playStatus = State.NO_PLAY

    /**
     * 记录跳转前的状态,跳转完成后,还原该状态
     */
    private var seekBeforeStatus = State.NO_PLAY

    /**
     * 跳转标志
     */
    @Volatile
    protected var seekStatus = SeekState.SEEK_NONE

    /**
     * 记录上一帧的pts,朝前跳转时,由于缓存帧的存在，只要当前帧小于上一帧，则认为跳转到前边了
     */
    @Volatile
    protected var lastPts: Long = 0

    /**
     * 朝后跳转时到达的pts,只要当前帧大于该帧，则认为跳到后边了
     */
    @Volatile
    private var goPts: Long = 0

    protected var needSeek = false

    /**
     * 网络资源暂停标志
     */
    @Volatile
    private var netPause = false

    /**
     * 解码线程是否完成
     */
    @Volatile
    private var dos = false

    /**
     * 渲染线程是否完成
     */
    @Volatile
    private var ros = false

    /**
     * 资源时长,单位为毫秒值
     */
    protected var duration: Long = 1

    /**
     * 渲染进度回调频率,每隔多少毫秒回调一次播放进度
     */
    protected var progressFreq: Long = 0

    /**
     * 上一次渲染进度回调的时间值
     */
    protected var lastPT: Long = 0

    /**
     * 线程池,执行解码与渲染任务
     */
    private val pool = Executors.newScheduledThreadPool(2)

    /**
     * 资源播放开始的时间节点
     */
    protected var startTime = 0L

    /**
     * 解码线程,执行解码操作
     */
    private val decoderTask = Runnable {
        try {
            dos = false
            //解码进度
            var decoderProgress = 0f
            while (true) {
                while (playStatus == State.PAUSE) {
                    //暂停中,进行休眠
                    sleep(16)
                }

                while (playStatus == State.DATA_WAIT) {
                    //暂停中,进行休眠
                    sleep(16)
                    onDataWait()
                }

                if (playStatus == State.SEEK_START) {
                    dealSeek()
                }

                //有网络加载的情况下,判断是否需要进入暂停
                if (interceptor != null) {
                    netPause = interceptor!!.progressComparison(decoderProgress)
                }
                while (netPause) {
                    //网络视频加载中,进行休眠
                    sleep(100)
                    //判断是否可以继续
                    netPause = interceptor!!.progressComparison(decoderProgress)
                }

                //停止指令
                if (playStatus == State.STOP) {
                    log("解码停止")
                    break
                }


                //获取输入索引
                val index = codec!!.dequeueInputBuffer((1000 * 16).toLong())
                if (index < 0) {
                    continue
                }
                val inputBuffer = codec!!.getInputBuffer(index) ?: continue
                //读取数据
                var sampleSize = extractor!!.readSampleData(inputBuffer, 0)
                if (sampleSize < 0) {
                    //数据读取结束
                    if (!loop) {
                        //压入结束标志
                        codec!!.queueInputBuffer(
                            index,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        //停止解码
                        break
                    } else {
                        //跳转至开头
                        extractor!!.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                        sampleSize = extractor!!.readSampleData(inputBuffer, 0)
                        val sampleTime = extractor!!.sampleTime
                        codec!!.queueInputBuffer(
                            index,
                            0,
                            sampleSize,
                            sampleTime,
                            0
                        )
                        //获取下一帧数据
                        extractor!!.advance()
                    }
                } else {
                    //压入数据
                    val sampleTime = extractor!!.sampleTime
                    decoderProgress =
                        (sampleTime * 0.1f / duration * 100).toInt() / 100f //保留2位小数
                    codec!!.queueInputBuffer(
                        index,
                        0,
                        sampleSize,
                        sampleTime,
                        0
                    )
                    //获取下一帧数据
                    extractor!!.advance()
                }
            }
            dos = true
            goEnd()
        } catch (e: Exception) {
            e.printStackTrace()
            onError("decoder err:$e")
        }
    }

    private fun dealSeek() {
        playStatus = State.SEEK
        val current = extractor!!.sampleTime
        //跳转单位是微秒
        extractor?.seekTo(goPts * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
        val go = extractor!!.sampleTime
        goPts = go / 1000
        log("real seek:$goPts")
        val goBefore = go < current
        seekStatus = if (goBefore) SeekState.SEEK_BEFORE
        else SeekState.SEEK_AFTER
        log("dealSeek:${seekStatus.name}")
        needSeek = false
    }

    /**
     * 渲染线程,获取解码好的数据执行播放操作
     * 不用mediaCodeC异步的方式是因为在某些特定机型上异步的无法release,会出现anr,瑞芯微给出的理由是底层代码未适配
     */
    private val renderTask = Runnable {
        try {
            ros = false
            while (true) {
                while (playStatus == State.PAUSE) {
                    //暂停中,进行休眠
                    sleep(16)
                }

                while (playStatus == State.DATA_WAIT) {
                    //暂停中,进行休眠
                    sleep(16)
                }

                while (netPause) {
                    //网络加载暂停中,进行休眠
                    sleep(90)
                }

                //停止指令
                if (playStatus == State.STOP) {
                    log("渲染停止")
                    break
                }

                //获取索引
                val index = codec!!.dequeueOutputBuffer(bufferInfo!!, (1000 * 16).toLong())
                if (index >= 0) {
                    render(index, bufferInfo!!.presentationTimeUs / 1000)
                } else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    //参数发生了变化
                    format = codec!!.outputFormat
                    configCodec(true)
                }
                val flag = bufferInfo!!.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM
                if (flag != 0 && !loop) {
                    break
                }
            }
            ros = true
            goEnd()
        } catch (e: Exception) {
            e.printStackTrace()
            onError("render err:$e")
        }
    }

    private fun render(index: Int, pts: Long) {
        when (seekStatus) {
            SeekState.SEEK_BEFORE -> {
                log("SEEK_BEFORE:$pts----$lastPts")
                if (pts <= lastPts) {
                    seekStatus = SeekState.SEEK_NONE
                    playStatus = seekBeforeStatus
                    log("seek before over")
                }
                abandonIndex(index)
            }
            SeekState.SEEK_AFTER -> {
                log("SEEK_AFTER:$pts----$goPts")
                if (pts >= goPts) {
                    seekStatus = SeekState.SEEK_NONE
                    playStatus = seekBeforeStatus
                    log("seek after over")
                }
                abandonIndex(index)
            }
            else -> {
                onRender(index)
            }
        }

    }

    private fun abandonIndex(index: Int) {
        codec?.releaseOutputBuffer(index, false)
    }

    @Throws(InterruptedException::class)
    fun sleep(millis: Long) {
        Thread.sleep(millis)
    }

    private fun goEnd() {
        if (dos && ros) {
            log("播放停止")
            //停止解码器,释放资源
            codec?.stop()
            codec?.release()
            extractor?.release()
            codec = null
            extractor = null
            format = null
            bufferInfo = null
            playStatus = State.NO_PLAY
            onEnd()
        }
    }

    /**
     * 加载资源,就绪后会启动播放(配置了自动播放)或触发onReady回调(没有设定自动播放)
     */
    abstract fun prepare()

    /**
     * format参数信息发生变化
     */
    abstract fun configCodec(flag: Boolean)

    /**
     * 渲染数据
     */
    abstract fun onRender(index: Int)

    /**
     * 自行渲染数据进入等待状态后触发该回调
     */
    abstract fun onDataWait()

    /**
     * 错误信息
     */
    abstract fun onError(s: String)

    /**
     * 渲染结束
     */
    abstract fun onEnd()

    /**
     * 启动播放
     */
    fun startPlay() {
        log("启动播放:${playStatus.name}")
        if (playStatus == State.NO_PLAY) {
            if (codec != null) {
                //启动解码器
                codec!!.start()
                //进入播放状态
                playStatus = State.PLAY
                pool.execute(decoderTask)
                pool.execute(renderTask)
            }
        }
    }

    /**
     * 暂停播放
     */
    fun pause() {
        log("暂停播放:${playStatus.name}")
        if (playStatus == State.PLAY) {
            playStatus = State.PAUSE
        }
    }

    /**
     * 恢复播放
     */
    fun restore() {
        log("恢复播放:${playStatus.name}")
        if (playStatus == State.PAUSE) {
            playStatus = State.PLAY
        }
    }

    /**
     * 如果在播放,则暂停
     * 如果暂停中,则播放
     */
    fun pauseOrRestore() {
        if (playStatus == State.PLAY) {
            playStatus = State.PAUSE
        } else if (playStatus == State.PAUSE) {
            playStatus = State.PLAY
        }
    }

    /**
     * 跳转到指定进度，0~1
     */
    fun seek(p: Float) {
        val progress = 0f.coerceAtLeast(1f.coerceAtMost(p))
        seek((duration * progress).toLong())
    }

    /**
     * 跳转到指定时间，毫秒值
     */
    fun seek(pos: Long) {
        if (playStatus == State.SEEK_START)
            return
        log("expected seek:$pos")
        goPts = pos
        needSeek = true
        seekBeforeStatus = playStatus
        playStatus = State.SEEK_START
    }

    /**
     * 停止播放
     */
    fun stop() {
        log("停止播放:${playStatus.name}")
        if (playStatus != State.NO_PLAY) {
            playStatus = State.STOP
        }
    }

    fun getCurrentPlayStatus(): State {
        return playStatus
    }
}