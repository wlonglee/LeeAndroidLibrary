package com.lee.metronome

/**
 * 节拍倒计数回调
 *@author lee
 *@date 2022/4/24
 */
interface MetronomeLoopListener {
    /**
     * 在小节内索引,从0开始
     */
    fun onBeat(index: Int)

    /**
     * 倒计时结束
     */
    fun onLoopEnd()
}