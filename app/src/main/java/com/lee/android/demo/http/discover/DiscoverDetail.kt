package com.lee.android.demo.http.discover

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

/**
 * 发现接口返回数据
 *@author lee
 *@date 2021/2/10
 */
@Parcelize
class DiscoverDetail(
    //id
    var id: Int = -1,
    //用户id
    @SerializedName("user_id")
    var userId: Int = -1,
    //鼓id 0表示没有鼓数据
    @SerializedName("drums_id")
    var drumId: Int = 0,
    //该鼓使用的音频url
    @SerializedName("drums_url")
    var drumUrl: String = "",
    //该鼓速度信息  120,140,130   鼓的调节范围为120-140  当前速度为130
    @SerializedName("drums_detail")
    var drumDetail: String = "",

    //标题
    var title: String = "",
    //拍子
    var beat: DiscoverInfo = DiscoverInfo(),
    //小节
    var bar: DiscoverInfo = DiscoverInfo(),
    //用户创作时的速度
    var speed: Int = 0,
    //用户发表的音频
    var audio: String = "",
    //音频信息  1,1,1,0   0表示音轨没有数据 1表示音轨有数据  鼓有4个 无鼓有3个
    @SerializedName("audio_track")
    var audioTrack: String = "",
    //定位地址
    var location: String = "",
    //时长,单位毫秒
    var duration: Int = 0,
    //点赞次数
    @SerializedName("like_num")
    var likeNum: Int = 0,
    //分享次数
    @SerializedName("share_num")
    var shareNum: Int = 0,
    //0私有 1公开
    @SerializedName("is_publish")
    var isPublic: Int = 0,
    //设备id
    @SerializedName("device_id")
    var deviceId: String = "",
    @SerializedName("deal_user_id")
    var dealUserId: Int = 0,
    //时间，单位秒
    @SerializedName("create_time")
    var create_time: Long = 0,
    var status: Int = 0,
    //用户昵称
    @SerializedName("nickname")
    var nickName: String = "",
    //用户头像
    var avatar: String = "",
    //用户标志
    @SerializedName("corner_mark")
    var logo: String = "",
    //0 未点赞  1点赞
    @SerializedName("is_like")
    var isLike: Int = 0
) : Parcelable