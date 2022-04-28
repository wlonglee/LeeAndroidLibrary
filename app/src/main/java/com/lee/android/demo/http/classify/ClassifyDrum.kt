package com.lee.android.demo.http.classify

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 *@author lee
 *@date 2021/1/29
 */
@Parcelize
class ClassifyDrum(
        var list: MutableList<ClassifyDrumData> = mutableListOf()
) : Parcelable {
}