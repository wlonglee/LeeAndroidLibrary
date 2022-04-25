package com.lee.metronome

import android.util.Log

/**
 *@author lee
 *@date 2022/4/22
 */
object MetronomeLog {
    /**
     * 是否输出log信息
     */
    var showLog=true

    fun log(msg:String){
        if(showLog){
            Log.e("lee_metronome",msg)
        }
    }
}