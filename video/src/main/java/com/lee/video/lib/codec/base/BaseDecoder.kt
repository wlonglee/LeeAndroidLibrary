package com.lee.video.lib.codec.base

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.util.concurrent.Executors

/**
 * 解码器基类
 * 网络资源播放需自行实现下载逻辑，解码器不负责下载存储处理,播放网络资源参考NetInterceptor说明,在拦截器中处理当前是否需要暂停解码和恢复解码即可(对比下载进度与解码进度)
 *@author lee
 *@date 2021/11/25
 */
abstract class BaseDecoder {

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

        //跳转
        SEEK,

        //停止
        STOP
    }

    /**
     * 是否自动启动解码
     * true-资源加载后将自动解码
     */
    protected var autoPlay = false

    /**
     * 是否自行处理视频画面
     */
    protected var dealVideo = false


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
     * 上一帧的渲染时间戳
     */
    protected var lastPts: Long = 0

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
                    break
                }

                //跳转状态
//                if (playStatus == State.SEEK) {
//                    extractor!!.seekTo(seekPos, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
//                    playStatus = State.PLAY
//                }

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
                while (netPause) {
                    //网络加载暂停中,进行休眠
                    sleep(90)
                }

                //停止指令
                if (playStatus == State.STOP) {
                    break
                }

                //获取索引
                val index = codec!!.dequeueOutputBuffer(bufferInfo!!, (1000 * 16).toLong())
                if (index >= 0) {
                    onRender(index)
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
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            onError("render err:$e")
        }
    }

//    open fun genBuffer(): MediaCodec.BufferInfo {
//        bufferInfo=MediaCodec.BufferInfo()
//        return bufferInfo!!
//    }

    @Throws(InterruptedException::class)
    fun sleep(millis: Long) {
        Thread.sleep(millis)
    }

    private fun goEnd() {
        if (dos && ros) {
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


//    /**
//     * 跳转播放
//     *
//     * @param p 跳转进度 0~1
//     */
//    fun seek(p: Float) {
//        var p = p
//        if (playStatus == State.SEEK) return
//        //限定跳转进度
//        p = Math.max(p, 0f)
//        p = Math.min(p, 1f)
//
//        //跳转的值需要是微秒,所以最后需乘1000
//        seekPos = (duration * p * 1000).toLong()
//        playStatus = State.SEEK
//    }

    /**
     * 暂停播放
     */
    fun pause() {
        if (playStatus == State.PLAY) {
            playStatus = State.PAUSE
        }
    }

    /**
     * 恢复播放
     */
    fun restore() {
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
     * 停止播放
     */
    fun stop() {
        if (playStatus != State.NO_PLAY) {
            playStatus = State.STOP
        }
    }
}