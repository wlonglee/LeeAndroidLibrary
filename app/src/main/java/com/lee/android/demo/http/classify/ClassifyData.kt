package com.lee.android.demo.http.classify

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

/**
 * 分类数据
 *@author lee
 *@date 2021/1/28
 */
@Parcelize
class ClassifyData(
        //id 1表示风格  表示节拍
        var id: Int = -1,
        var pid: Int = -1,
        @SerializedName("val")
        var value: String = "",
        var name: String = "",
        var list: MutableList<ClassifyDetail> = mutableListOf()
) : Parcelable