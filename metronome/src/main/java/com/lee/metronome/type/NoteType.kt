package com.lee.metronome.type

/**
 * 音符选项
 * 影响节拍器发声规则
 *
 * @author lee
 * @date 2021/1/13
 */
enum class NoteType(
    //每个音符里包含的子拍数
    val num: Int
) {
    //####针对 1/4 2/4 3/4 4/4 5/4拍号
    //四分音符
    QUARTER(1),
    //八分音符
    QUAVER(2),
    //三连音
    TRIPLET(3),
    //十六分音符
    SEMIQUAVER(4),


    //####针对 3/8 6/8 9/8 12/8 拍号
    //四分附点音符
    QUARTER_DOT(1),
    //三个八分音符
    QUAVER_TRIPLET(1);

}