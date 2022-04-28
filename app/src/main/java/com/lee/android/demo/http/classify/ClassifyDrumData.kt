package com.lee.android.demo.http.classify

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.lee.android.demo.http.classify.ClassifyDetail
import kotlinx.android.parcel.Parcelize

/**
 * 分类界面的鼓数据
 *@author lee
 *@date 2021/1/29
 */
@Parcelize
class ClassifyDrumData(
    var id: Int = -1,
    @SerializedName("user_id")
    var userId: Int = -1,
    var cover: String = "",
    var audio: String = "",

    var style: ClassifyDetail = ClassifyDetail(),

    @SerializedName("sub_style")
    var subStyle: Int = -1,

    var beat: ClassifyDetail = ClassifyDetail(),
    var bar: ClassifyDetail = ClassifyDetail(),

    @SerializedName("min_speed")
    var minSpeed: Int = -1,

    @SerializedName("max_speed")
    var maxSpeed: Int = -1,

    @SerializedName("create_time")
    var createTime: Long = -1,

    var status: Int = -1,

    @SerializedName("nickname")
    var nickName: String = "",

    var avatar: String = "",

    @SerializedName("download_num")
    var downloadNum: Int = 0,

    var name: String = "",

    @SerializedName("collect_num")
    var collectNum: Int = 0,

    @SerializedName("is_collect")
    var collect: Int = 0
) : Parcelable