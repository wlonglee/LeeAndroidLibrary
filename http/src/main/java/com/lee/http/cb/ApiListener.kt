package com.lee.http.cb

import com.lee.http.model.ApiTag

/**
 * 请求成功的回调器,根据接口数据类型,实现其中之一的函数即可,多个接口返回数据相同的情况下,可以公用一个回调器,通过apiTag进行区分
 *@author lee
 *@date 2022/4/26
 */
interface ApiListener<T> {
    /**
     * 成功,数据为单独数据类型
     */
    fun onSuccess(data: T, tag: ApiTag? = null) {}

    /**
     * 成功,数据为列表数据类型
     */
    fun onSuccessList(data: List<T>, tag: ApiTag? = null) {}
}