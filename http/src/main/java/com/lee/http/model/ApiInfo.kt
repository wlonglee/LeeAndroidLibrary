package com.lee.http.model

import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * 网络接口定义,针对所有的网络请求 统一化,完全没必要一个请求接口在这加一个函数，因为这太操蛋了
 * 返回数据的转换由封装层实现,使用者不用关心
 *@author lee
 *@date 2022/4/26
 */
interface ApiInfo {

    //get请求
    @GET
    suspend fun executeGet(@Url url: String): Response<JsonObject>

    //get请求-包含参数
    @GET
    suspend fun executeGet(@Url url: String, @QueryMap params: Map<String, String>): Response<JsonObject>

    //post请求
    @POST
    suspend fun executePost(@Url url: String): Response<JsonObject>

    //post请求-包含body
    @POST
    suspend fun executePost(@Url url: String, @Body body: RequestBody): Response<JsonObject>

    //post请求-包含参数
    @FormUrlEncoded
    @POST
    suspend fun executePost(@Url url: String, @FieldMap params: Map<String, String>): Response<JsonObject>

    //post请求-包含参数和body
    @FormUrlEncoded
    @POST
    suspend fun executePost(@Url url: String, @FieldMap params: Map<String, String>,@Body body: RequestBody): Response<JsonObject>



    //put请求
    @PUT
    suspend fun executePut(@Url url: String): Response<JsonObject>

    //put请求-包含body
    @PUT
    suspend fun executePut(@Url url: String, @Body body: RequestBody): Response<JsonObject>

    //put请求-包含参数
    @FormUrlEncoded
    @PUT
    suspend fun executePut(@Url url: String, @FieldMap params: Map<String, String>): Response<JsonObject>

    //put请求-包含参数和body
    @FormUrlEncoded
    @PUT
    suspend fun executePut(@Url url: String, @FieldMap params: Map<String, String>,@Body body: RequestBody): Response<JsonObject>


}