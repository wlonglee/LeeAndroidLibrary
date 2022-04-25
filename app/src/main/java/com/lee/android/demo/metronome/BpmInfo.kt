package com.lee.android.demo.metronome

/**
 *@author lee
 *@date 2022/4/24
 */
class BpmInfo(
    //显示数据
    var info: ArrayList<String> = arrayListOf(),
    //数据宽度
    var itemW:ArrayList<Float> = arrayListOf(),
    //圆点所在位置
    var pos: Int = 0
)