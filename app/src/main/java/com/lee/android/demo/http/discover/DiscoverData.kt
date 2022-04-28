package com.lee.android.demo.http.discover

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * 发现接口返回数据
 *@author lee
 *@date 2021/2/10
 */
@Parcelize
class DiscoverData(
        var list: MutableList<DiscoverDetail> = mutableListOf()
) : Parcelable