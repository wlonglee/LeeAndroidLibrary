package com.lee.http.cb

import com.lee.http.model.ApiError
import com.lee.http.model.ApiTag

/**
 * 请求基础回调器,同一个界面，可公用一个基础回调器
 *@author lee
 *@date 2022/4/26
 */
interface ApiBasicListener{
    /**
     * 请求开始
     */
    fun onStart(tag: ApiTag? = null) {}

    /**
     * 请求完成
     */
    fun onCompleted(tag: ApiTag? = null) {}

    /**
     * 出错
     */
    fun onError(e: ApiError, tag: ApiTag? = null)
}