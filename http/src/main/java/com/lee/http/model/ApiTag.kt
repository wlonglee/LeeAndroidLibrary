package com.lee.http.model

/**
 * 如果接口请求的回调在指向同一个地方的时候，依据该值可以作为区分
 * 接口请求时设定的tag数据,接口回调会传递该值
 *@author lee
 *@date 2022/4/26
 */
class ApiTag(var tag: Any)