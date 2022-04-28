package com.lee.http.model

/**
 * 同步请求返回数据包装
 *@author lee
 *@date 2022/4/26
 */
class ApiResponse<T>(
    //设定的tag
    var tag: ApiTag? = null,
    //错误封装
    var error: ApiError? = null,
    //数据类型非数组时有值
    var data: T? = null,
    //数据类型为数组时有值
    var dataList: List<T>? = null
) {
    override fun toString(): String {
        return "ApiResponse(tag=$tag, error=$error, data=$data, dataList=$dataList)"
    }

    /**
     * 请求是否成功
     */
    fun isSuccess(): Boolean {
        //出错了会存在错误封装数据
        return error == null
    }
}