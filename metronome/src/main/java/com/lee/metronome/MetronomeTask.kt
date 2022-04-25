package com.lee.metronome

import com.lee.metronome.model.MetronomeData
import com.lee.metronome.model.MetronomeSoundData

/**
 * 节拍器播放处理任务
 *
 * @author lee
 * @date 2021/1/14
 */
internal class MetronomeTask(
    private val metronomeData: MetronomeData,
    private val soundData: MetronomeSoundData
) : Runnable {
    private var type = MetronomeType.COUNT_DOWN

    //节拍器任务类型
    private enum class MetronomeType {
        //倒计时
        COUNT_DOWN,

        //一直播放,直到主动停止
        LOOP
    }

    //停止播放标志
    private var stop = false

    //重复的小节数
    private var repeat = 4

    //倒计时多少个拍子
    private var countdownNum = 4

    private var countdownListener: CountdownListener? = null
    private var loopListener: LoopListener? = null

    /**
     * 倒计时监听器
     */
    interface CountdownListener {
        /**
         * 倒计时数字
         */
        fun onCountdown(count: Int)

        /**
         * 每拍(包含子拍)的音频数据
         * @param data 该拍的音频数据
         * @param size 音频数据的大小,不能以data.size为准
         * @param index 音频数据一个循环内的索引
         * @param moleculeIndex 小节内的索引,例如3/4,表示一个小节内有3个单拍,该值回调为0~2
         * @param progress 音频数据在该拍内的进度,从0~100
         */
        fun onMetronomeData(
            data: ByteArray,
            size: Int,
            index: Int,
            moleculeIndex: Int,
            progress: Float
        )

        fun onCountdownEnd()
    }

    interface LoopListener {
        fun onLoopStart()

        /**
         * 每拍(包含子拍)的音频数据
         * @param data 该拍的音频数据
         * @param size 音频数据的大小,不能以data.size为准
         * @param index 音频数据一个循环内的索引
         * @param moleculeIndex 小节内的索引,例如3/4,表示一个小节内有3个单拍,该值回调为0~2
         * @param progress 音频数据在该拍内的进度,从0~100
         */
        fun onMetronomeData(
            data: ByteArray,
            size: Int,
            index: Int,
            moleculeIndex: Int,
            progress: Float
        )

        fun onLoopEnd()
    }

    fun settingCountDown(countDownNum: Int, listener: CountdownListener) {
        this.countdownNum = countDownNum
        countdownListener = listener
        type = MetronomeType.COUNT_DOWN
    }

    fun settingLoop(listener: LoopListener) {
        loopListener = listener
        type = MetronomeType.LOOP
    }


    /**
     * 停止
     */
    fun stopTask() {
        stop = true
    }

    override fun run() {
        when (type) {
            MetronomeType.COUNT_DOWN -> {
                startCountDown()
            }
            MetronomeType.LOOP -> {
                startLoop()
            }
        }
    }

    /**
     * 节拍倒计时
     */
    private fun startCountDown() {
        val molecule = metronomeData.molecule
        MetronomeLog.log("countDown start")

        //记录小节内的索引,例如3/4,表示一个小节内有3个单拍
        var moleculeIndex = -1

        //记录一小节内的索引
        var soundIndex = 0
        //音频数据一个循环内的索引
        var bpmIndex = 0

        //单个拍子包含的子拍数量,默认是1个,表示该拍不需要切分多个子拍,用于计算声音在这个拍子内的进度
        val noteNum: Int = metronomeData.noteType.num
        //单拍内子拍的索引,例如NoteType.QUAVER表示子拍数量为2,该值为0~1
        var noteIndex = 0

        var currentSound: ByteArray
        var currentSoundSize: Int

        var p: Float
        while (!stop && countdownNum > 0) {
            //每经过一轮音符数量的洗礼,表示过去了一个拍子
            if (noteIndex % noteNum == 0) {
                noteIndex = 0
                if ((moleculeIndex + 1) == molecule) {
                    moleculeIndex = -1
                }
                moleculeIndex++
            }

            //每经历一个拍子,倒计时减1
            if (bpmIndex % noteNum == 0) {
                //通知倒计时变化
                countdownListener?.onCountdown(countdownNum)
                countdownNum--
            }

            //获取当前拍的声音数据
            currentSound = soundData.getBeat(metronomeData.beatArray[soundIndex])
            //获取当前拍的数据大小
            currentSoundSize = metronomeData.bpmArray[bpmIndex]


            //将声音数据切割为20ms一次的大小,然后计算在当前拍子内的进度值
            var audioClip = true
            var clipSize = 1764
            var clipTotalSize = 0
            val clipData = ByteArray(clipSize)
            while (audioClip) {
                if (stop) {
                    //在切割单拍音频的过程中可能触发停止指令,30BPM的时候一拍数据有2秒，不能停止的这么慢
                    break
                }

                //当前拍音频数据切割完成后终止循环
                if (clipTotalSize + clipSize >= currentSoundSize) {
                    audioClip = false
                    clipSize = currentSoundSize - clipTotalSize
                }

                //拷贝数据
                System.arraycopy(currentSound, clipTotalSize, clipData, 0, clipSize)
                clipTotalSize += clipSize

                p = clipTotalSize * 100f / currentSoundSize

                countdownListener?.onMetronomeData(
                    clipData,
                    clipSize,
                    bpmIndex / noteNum,
                    moleculeIndex,
                    p / noteNum + noteIndex * 100f / noteNum
                )
            }
            noteIndex++
            soundIndex++
            bpmIndex++

            if (soundIndex == metronomeData.beatArray.size) soundIndex = 0

            if (bpmIndex == metronomeData.bpmArray.size) bpmIndex = 0
        }


        MetronomeLog.log("countDown over")
        countdownListener?.onCountdownEnd()
    }

    private fun startLoop() {
        val molecule = metronomeData.molecule
        MetronomeLog.log("loop start")
        loopListener?.onLoopStart()

        //记录小节内的索引,例如3/4,表示一个小节内有3个单拍
        var moleculeIndex = -1

        //记录一小节内的索引
        var soundIndex = 0
        //音频数据一个循环内的索引
        var bpmIndex = 0

        //单个拍子包含的子拍数量,默认是1个,表示该拍不需要切分多个子拍,用于计算声音在这个拍子内的进度
        val noteNum: Int = metronomeData.noteType.num
        //单拍内子拍的索引,例如NoteType.QUAVER表示子拍数量为2,该值为0~1
        var noteIndex = 0

        var currentSound: ByteArray
        var currentSoundSize: Int

        var p: Float
        while (!stop) {
            //每经过一轮音符数量的洗礼,表示过去了一个拍子
            if (noteIndex % noteNum == 0) {
                noteIndex = 0
                if ((moleculeIndex + 1) == molecule) {
                    moleculeIndex = -1
                }
                moleculeIndex++
            }

            //获取当前拍的声音数据
            currentSound = soundData.getBeat(metronomeData.beatArray[soundIndex])
            //获取当前拍的数据大小
            currentSoundSize = metronomeData.bpmArray[bpmIndex]


            //将声音数据切割为20ms一次的大小,然后计算在当前拍子内的进度值
            var audioClip = true
            var clipSize = 1764
            var clipTotalSize = 0
            val clipData = ByteArray(clipSize)
            while (audioClip) {
                if (stop) {
                    //在切割单拍音频的过程中可能触发停止指令,30BPM的时候一拍数据有2秒，不能停止的这么慢
                    break
                }

                //当前拍音频数据切割完成后终止循环
                if (clipTotalSize + clipSize >= currentSoundSize) {
                    audioClip = false
                    clipSize = currentSoundSize - clipTotalSize
                }

                //拷贝数据
                System.arraycopy(currentSound, clipTotalSize, clipData, 0, clipSize)
                clipTotalSize += clipSize

                p = clipTotalSize * 100f / currentSoundSize

                loopListener?.onMetronomeData(
                    clipData,
                    clipSize,
                    bpmIndex / noteNum,
                    moleculeIndex,
                    p / noteNum + noteIndex * 100f / noteNum
                )
            }
            noteIndex++
            soundIndex++
            bpmIndex++

            if (soundIndex == metronomeData.beatArray.size) soundIndex = 0

            if (bpmIndex == metronomeData.bpmArray.size) bpmIndex = 0
        }


        MetronomeLog.log("loop over")
        loopListener?.onLoopEnd()
    }
}