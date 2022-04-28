package com.lee.android.demo.http

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 *@author lee
 *@date 2022/4/26
 */
@Parcelize
class CountryInfo(
    var code: String="",
    var name: String="",
    var hasNext: Int=0
) : Parcelable