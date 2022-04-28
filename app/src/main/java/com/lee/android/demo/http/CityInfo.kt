package com.lee.android.demo.http

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 *@author lee
 *@date 2022/4/26
 */
@Parcelize
class CityInfo(
    var country:MutableList<CountryInfo> = mutableListOf()
): Parcelable