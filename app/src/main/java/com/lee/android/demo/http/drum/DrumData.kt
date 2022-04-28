package com.lee.android.demo.http.drum

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * 鼓页面模块数据
 *@author lee
 *@date 2021/1/20
 */
@Parcelize
class DrumData(
        var module: MutableList<DrumModuleDetail> = mutableListOf()
) : Parcelable