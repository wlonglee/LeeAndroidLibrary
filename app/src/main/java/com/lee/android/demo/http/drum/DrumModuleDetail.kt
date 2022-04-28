package com.lee.android.demo.http.drum

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * 鼓页面模块数据
 *@author lee
 *@date 2021/1/20
 */
@Parcelize
class DrumModuleDetail(
        var name: String = "",
        var flag: String = "",
        var list: MutableList<DrumListDetail> = mutableListOf()
) : Parcelable {

    fun isMaster(): Boolean {
        return flag == "kol"
    }
}