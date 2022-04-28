package com.lee.android.demo.http.discover

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

/**
 *@author lee
 *@date 2021/2/10
 */
@Parcelize
class DiscoverInfo(
        var id: Int = -1,
        var pid: Int = -1,
        var name: String = "",
        var en_name: String = "",
        var trad_name: String = "",
        @SerializedName( "val")
        var value: String = ""
) : Parcelable