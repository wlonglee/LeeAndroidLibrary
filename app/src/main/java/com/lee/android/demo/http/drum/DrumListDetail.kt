package com.lee.android.demo.http.drum

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize


/**
 * 鼓页面模块列表数据,不包含鼓详情数据
 *@author lee
 *@date 2021/1/20
 */
@Parcelize
class DrumListDetail(
        //鼓id
        var id: Int = -1,
        //鼓风格
        var style: String = "",

        var beat: String = "",
        var bar: String = "",

        //鼓图片
        var cover: String = "",
        //鼓试听地址
        var audio: String = "",

        //鼓收藏数量
        @SerializedName("collect_num")
        var collectNum: Int = -1,
        //鼓名称
        var name: String = "",
        @SerializedName("is_collect")
        var collect: Int = 0,


        var avatar: String = "",
        @SerializedName("user_code")
        var userCode: String = "",
        @SerializedName("nickname")
        var nickName: String = ""
) : Parcelable {
    fun isCollect(): Boolean {
        return collect != 0
    }
}
