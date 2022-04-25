package com.lee.metronome.model

import com.lee.metronome.MetronomeLog
import com.lee.metronome.type.BeatType
import com.lee.metronome.type.DotType
import com.lee.metronome.type.NoteType

/**
 * 节拍器数据信息
 *@author lee
 *@date 2021/1/14
 */
class MetronomeData(
    //设定的每分钟节拍数
    var bpm: Int = 60,
    //每个小节有多少拍
    var molecule: Int = 4,
    //以什么音符为一拍, 1/4 2/4表示4分音符一拍，每小节有1 2拍
    var denominator: Int = 4,
    //音符类型
    var noteType: NoteType = NoteType.QUARTER,
    //录制小节数量
    var recordSection: Int = -1
) {

    /**
     * 小节内每个拍号的圆点规则
     */
    lateinit var dotArray: Array<DotType>

    /**
     * 一小节内每拍(包含子拍)数据对应的声音响动规则
     */
    lateinit var beatArray: Array<BeatType>

    /**
     * 计算出来的每拍(包含子拍)数据的字节长度
     */
    lateinit var bpmArray: IntArray

    var beatSize = 0

    init {
        //计算界面显示的圆点规则
        calculateDot()
        //计算每次响动的声音
        calculateBeat()
    }

    /**
     * 计算界面显示的圆点规则
     */
    private fun calculateDot() {

        //4分拍号  1/4 2/4 3/4 4/4
        //8分拍号  3/8 6/8 9/8 12/8
        dotArray = Array(molecule) { index ->
            when (index) {
                0 -> {
                    DotType.STRONG_BIG
                }
                else -> {
                    if (denominator == 4) {
                        //4分音符的都是大拍
                        DotType.BIG
                    } else {
                        //8分音符  3 6 9为大拍
                        if (index % 3 == 0) {
                            //3 6 9为大拍
                            DotType.BIG
                        } else {
                            DotType.SMALL
                        }
                    }
                }
            }
        }
    }

    /**
     * 计算每次响动的声音,每拍(无论多少子拍)时对应的圆点发亮,只亮一下-by产品
     */
    private fun calculateBeat() {
        //实际长度是小节拍子数量*每个拍子包含的子拍数量
        beatArray = Array(molecule * noteType.num) { index ->
            when (index) {
                0 -> {
                    //第一个都是强拍音
                    BeatType.DRIP
                }
                else -> {
                    if (denominator == 4) {
                        //4分音符剩下的都是弱拍
                        BeatType.TICK
                    } else {
                        if (index % 3 == 0) {
                            //3 6 9发次强声
                            BeatType.DROP
                        } else {
                            if (noteType == NoteType.QUARTER_DOT) {
                                BeatType.NONE
                            } else {
                                BeatType.TICK
                            }
                        }
                    }
                }
            }
        }
    }

    fun calculateMetronome(sample: Int, pcmNum: Int) {
        if (recordSection <= 0) {
            //bpm表示一分钟内响多少次, noteType.num表示响的这拍实际有几下
            val realCount = bpm * noteType.num
            calculateMetronome(sample, pcmNum, realCount, 1f)
        } else {
            //recordSection表示录制的小节数量
            //molecule表示每个小节有多少拍
            val realCount = recordSection * molecule * noteType.num
            //计算一分钟内的响动比值  例60bpm表示一分钟响60次  3/4拍 4小节 60bpm的,则一个循环只响12次,是一分钟的 1/5
            val realMolecule = recordSection * molecule * 1f / bpm
            calculateMetronome(sample, pcmNum, realCount, realMolecule)
        }
    }

    /**
     * 计算每一拍的数据信息
     * @param sample 采样率 44100、48000
     * @param pcmNum 8位传8 16位传16
     * @param realCount 实际有多少个拍子
     * @param realMolecule 默认下是1
     */
    private fun calculateMetronome(
        sample: Int,
        pcmNum: Int,
        realCount: Int,
        realMolecule: Float = 1f
    ) {
        bpmArray = IntArray(realCount)

        //计算数据的总长度
        // sample*pcmNum/8 表示一秒钟的数据大小  *60 表示一分钟的数据
        var dataTotalSize = (sample * pcmNum / 8 * 60 * realMolecule).toInt()

        //这两个音符,数据长度只有实际的1/3
        if (noteType == NoteType.QUARTER_DOT || noteType == NoteType.QUAVER_TRIPLET) {
            dataTotalSize /= 3
        }

        //保证总长度值为2的倍数
        if (dataTotalSize % 2 != 0) {
            dataTotalSize += 1
        }

        //计算每个拍子的长度
        //数据长度原理:1000-60 -->20*16+40*17
        //因为17为单数,音频数据需求偶数,需要进一步拆解  最终结果为  1000-60 -->40*16+20*18
        var high = 0
        var low: Int = dataTotalSize / realCount

        if (low % 2 != 0) {
            //保证low为2的整数倍
            low -= 1
        }
        if (low * realCount != dataTotalSize) {
            //存在high high比low2
            high = low + 2
        }

        //计算高位的数量,相当于求一元二次方程
        // x+y=realCount
        // x*low +y*high=dataTotalSize
        // low+2=high
        var y = 0
        if (high != 0) {
            y = (dataTotalSize - low * realCount) / (high - low)
        }
        val x = realCount - y

        //给数组赋值
        for (i in 0 until x) {
            bpmArray[i] = low
        }
        for (i in 0 until y) {
            bpmArray[x + i] = high
        }

        //存储拍子的长度值,以大的为准
        beatSize = low.coerceAtLeast(high)

        MetronomeLog.log("info:$bpm,$molecule,$denominator,${noteType.name},realCount:$realCount,$x*$low+$y*$high,beat:$beatSize")
    }
}