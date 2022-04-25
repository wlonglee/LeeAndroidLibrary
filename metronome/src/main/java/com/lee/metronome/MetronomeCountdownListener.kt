package com.lee.metronome

/**
 * 节拍倒计数回调
 *@author lee
 *@date 2022/4/24
 */
interface MetronomeCountdownListener {
    /**
     * 倒计时数字
     */
    fun onCountdown(count: Int)

    /**
     * 倒计时结束
     */
    fun onCountdownEnd()
}