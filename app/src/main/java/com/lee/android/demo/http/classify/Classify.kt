package com.lee.android.demo.http.classify

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * 分类数据
 *@author lee
 *@date 2021/1/28
 */
@Parcelize
class Classify(
        var data: MutableList<ClassifyData> = mutableListOf()
) : Parcelable