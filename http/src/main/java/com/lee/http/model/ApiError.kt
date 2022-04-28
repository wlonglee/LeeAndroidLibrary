package com.lee.http.model

/**
 * 请求错误信息包装
 *@author lee
 *@date 2022/4/26
 */
class ApiError(
    //错误码
    // -0X01没有数据,返回的json为空
    // -0X11 表示解析出错
    var errorCode: Int = -100,
    //错误信息
    var errorMsg: String? = "",
    //错误异常
    var throwable: Throwable? = null
){
    override fun toString(): String {
        return "ApiError(errorCode=$errorCode, errorMsg=$errorMsg, throwable=$throwable)"
    }
}