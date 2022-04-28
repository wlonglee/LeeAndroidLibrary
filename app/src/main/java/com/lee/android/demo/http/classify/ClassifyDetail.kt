package com.lee.android.demo.http.classify

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

/**
 * 分类数据详情
 *@author lee
 *@date 2021/1/28
 */
@Parcelize
class ClassifyDetail(
        var id: Int = -1,
        var pid: Int = -1,
        @SerializedName("val")
        var value: String = "",
        var name: String = ""
) : Parcelable{
        override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as ClassifyDetail

                if (id != other.id) return false

                return true
        }

        override fun hashCode(): Int {
                return id
        }
}