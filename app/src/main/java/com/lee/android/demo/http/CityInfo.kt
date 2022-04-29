package com.lee.android.demo.http

import android.os.Parcelable
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

/**
 *@author lee
 *@date 2022/4/26
 */
@Parcelize
class CityInfo(
    var country: MutableList<CountryInfo> = mutableListOf(),
    //格式为 { "NL":[CountryInfo,CountryInfo], "RU":[CountryInfo,CountryInfo] }
    var provice: MutableMap<String,MutableList<CountryInfo>> = mutableMapOf(),
    //同上
    var city: MutableMap<String,MutableList<CountryInfo>> = mutableMapOf()
) : Parcelable