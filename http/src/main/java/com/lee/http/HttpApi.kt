package com.lee.http

import android.net.Uri
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.lee.http.cb.ApiBasicListener
import com.lee.http.cb.ApiListener
import com.lee.http.model.ApiError
import com.lee.http.model.ApiInfo
import com.lee.http.model.ApiResponse
import com.lee.http.model.ApiTag
import kotlinx.coroutines.*
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 *@author lee
 *@date 2022/4/26
 */
class HttpApi private constructor(
    private var baseHost: String,
    private var baseHeader: Map<String, String>,
    private var codeKey: String,
    private var dataKey: String,
    private var msgKey: String,
    private var successCode: Int,
    private var connectTimeout: Long,
    private var writeTimeout: Long,
    private var readTimeout: Long,
    private var logInterceptor: HttpLoggingInterceptor.Logger? = null
) {
    companion object {
        lateinit var instance: HttpApi

        /**
         * 初始化网络相关配置
         * @param baseHost 网络请求基础URL
         * @param codeKey  解析的状态码字段名称
         * @param dataKey  解析的数据字段名称
         * @param msgKey   解析的消息字段名称
         * @param successCode 状态码成功值
         * @param logInterceptor 请求日志输出
         */
        fun init(
            baseHost: String,
            baseHeader: Map<String, String>,
            codeKey: String = "code",
            dataKey: String = "data",
            msgKey: String = "msg",
            successCode: Int = 1000,
            connectTimeout: Long = 5,
            writeTimeout: Long = 30,
            readTimeout: Long = 30,
            logInterceptor: HttpLoggingInterceptor.Logger? = null
        ) {
            instance = HttpApi(
                baseHost,
                baseHeader,
                codeKey,
                dataKey,
                msgKey,
                successCode,
                connectTimeout,
                writeTimeout,
                readTimeout,
                logInterceptor
            )
        }
    }

    /**
     * 请求类型
     */
    private enum class RequestType {
        GET,
        POST,
        PUT
    }

    /**
     * 接口请求服务
     */
    private var apiInfo: ApiInfo

    /**
     * io 线程调度
     */
    private val ioCoroutineScope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.IO) }

    /**
     * 主线程调度
     */
    private val mainCoroutineScope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.Main) }

    /**
     * 请求头信息
     */
    private var hostHeader: Map<String, String> = mutableMapOf()

    /**
     * 请求地址
     */
    private var hostUrl = baseHost

    init {
        //配置retrofit
        val retrofit = Retrofit.Builder()
            .client(createOkHttpClient())
            .baseUrl(baseHost)
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .addConverterFactory(ConverterFactory())
            .build()
        apiInfo = retrofit.create(ApiInfo::class.java)
    }

    /**
     * 创建一个okHttp客户端对象
     */
    private fun createOkHttpClient(): OkHttpClient {
        //设置log拦截器
        val logging = logInterceptor?.let {
            HttpLoggingInterceptor(it)
        }
        val builder = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                //设定请求头信息
                val request = chain.request()
                val requestBuilder = request.newBuilder()
                //添加请求头
                for ((k, v) in hostHeader) {
                    requestBuilder.addHeader(k, v)
                }

                //自定义

                Uri.parse(hostUrl).host?.let {
                    requestBuilder.url(request.url.newBuilder().host(it).build())
                }
                chain.proceed(requestBuilder.build())
            })
            //https
            .hostnameVerifier(TrustAll.hostnameVerifier())
            //连接超时
            .connectTimeout(connectTimeout, TimeUnit.SECONDS)
            //写数据超时
            .writeTimeout(writeTimeout, TimeUnit.SECONDS)
            //读数据超时
            .readTimeout(readTimeout, TimeUnit.SECONDS)
        logging?.let {
            //log输出
            it.level = HttpLoggingInterceptor.Level.BODY
            builder.addInterceptor(it)
        }

        TrustAll.socketFactory()?.let {
            builder.sslSocketFactory(it, TrustAll.trustManager())
        }
        return builder.build()
    }


    /**
     * 处理请求
     * @param url 请求路径
     * @param tag 请求标识
     * @param basicListener 基础回调
     * @param dataListener 数据回调
     * @param isList 返回数据是否是数组类型
     * @param params 请求参数-封装在url之后, key=value & key=value方式
     * @param header 自定义的请求头
     * @param isNoKey  true该请求返回的数据直接解析为指定的数据类型
     * @param replaceHost 该请求替换的基础路径,为空表示不替换
     * @param cls 返回数据的类型
     */
    fun <T> get(
        lifeCoroutineScope: LifecycleCoroutineScope,
        url: String,
        tag: ApiTag? = null,
        basicListener: ApiBasicListener? = null,
        dataListener: ApiListener<T>? = null,
        isList: Boolean = false,
        params: Map<String, Any>? = null,
        header: Map<String, String>? = null,
        isNoKey: Boolean = false,
        replaceHost: String? = null,
        cls: Class<T>
    ) {
        lifeCoroutineScope.launch {
            withContext(ioCoroutineScope.coroutineContext) {
                basicListener?.onStart(tag)
                val response = getSync(url, tag, isList, params, header, isNoKey, replaceHost, cls)
                basicListener?.onCompleted(tag)
                if (response.error != null) {
                    basicListener?.onError(response.error!!, response.tag)
                } else {
                    if (isList) {
                        dataListener?.onSuccessList(response.dataList!!, response.tag)
                    } else {
                        dataListener?.onSuccess(response.data!!, response.tag)
                    }
                }
            }
        }
    }

    /**
     * 处理请求
     * @param url 请求路径
     * @param tag 请求标识
     * @param basicListener 基础回调
     * @param dataListener 数据回调
     * @param isList 返回数据是否是数组类型
     * @param params 请求参数-封装在url之后, key=value & key=value方式
     * @param body  请求参数-封装在post请求体之内
     * @param header 自定义的请求头
     * @param isNoKey  true该请求返回的数据直接解析为指定的数据类型
     * @param replaceHost 该请求替换的基础路径,为空表示不替换
     * @param cls 返回数据的类型
     */
    fun <T> post(
        lifeCoroutineScope: LifecycleCoroutineScope,
        url: String,
        tag: ApiTag? = null,
        basicListener: ApiBasicListener? = null,
        dataListener: ApiListener<T>? = null,
        isList: Boolean = false,
        params: Map<String, Any>? = null,
        body: Map<String, Any>? = null,
        header: Map<String, String>? = null,
        isNoKey: Boolean = false,
        replaceHost: String? = null,
        cls: Class<T>
    ) {
        lifeCoroutineScope.launch {
            withContext(ioCoroutineScope.coroutineContext) {
                basicListener?.onStart(tag)
                val response =
                    postSync(url, tag, isList, params, body, header, isNoKey, replaceHost, cls)
                basicListener?.onCompleted(tag)
                if (response.error != null) {
                    basicListener?.onError(response.error!!, response.tag)
                } else {
                    if (isList) {
                        dataListener?.onSuccessList(response.dataList!!, response.tag)
                    } else {
                        dataListener?.onSuccess(response.data!!, response.tag)
                    }
                }
            }
        }
    }

    /**
     * 处理请求
     * @param url 请求路径
     * @param tag 请求标识
     * @param basicListener 基础回调
     * @param dataListener 数据回调
     * @param isList 返回数据是否是数组类型
     * @param params 请求参数-封装在url之后, key=value & key=value方式
     * @param body  请求参数-封装在请求体之内
     * @param header 自定义的请求头
     * @param isNoKey  true该请求返回的数据直接解析为指定的数据类型
     * @param replaceHost 该请求替换的基础路径,为空表示不替换
     * @param cls 返回数据的类型
     */
    fun <T> put(
        lifeCoroutineScope: LifecycleCoroutineScope,
        url: String,
        tag: ApiTag? = null,
        basicListener: ApiBasicListener? = null,
        dataListener: ApiListener<T>? = null,
        isList: Boolean = false,
        params: Map<String, Any>? = null,
        body: Map<String, Any>? = null,
        header: Map<String, String>? = null,
        isNoKey: Boolean = false,
        replaceHost: String? = null,
        cls: Class<T>
    ) {
        lifeCoroutineScope.launch {
            withContext(ioCoroutineScope.coroutineContext) {
                basicListener?.onStart(tag)
                val response =
                    putSync(url, tag, isList, params, body, header, isNoKey, replaceHost, cls)
                basicListener?.onCompleted(tag)
                if (response.error != null) {
                    basicListener?.onError(response.error!!, response.tag)
                } else {
                    if (isList) {
                        dataListener?.onSuccessList(response.dataList!!, response.tag)
                    } else {
                        dataListener?.onSuccess(response.data!!, response.tag)
                    }
                }
            }
        }
    }

    /**
     * 处理请求
     * @param url 请求路径
     * @param tag 请求标识
     * @param isList 返回数据是否是数组类型
     * @param params 请求参数-封装在url之后, key=value & key=value方式
     * @param header 自定义的请求头
     * @param isNoKey  true该请求返回的数据直接解析为指定的数据类型
     * @param replaceHost 该请求替换的基础路径,为空表示不替换
     * @param cls 返回数据的类型
     */
    suspend fun <T> getSync(
        url: String,
        tag: ApiTag? = null,
        isList: Boolean = false,
        params: Map<String, Any>? = null,
        header: Map<String, String>? = null,
        isNoKey: Boolean = false,
        replaceHost: String? = null,
        cls: Class<T>
    ): ApiResponse<T> {
        return execute(
            requestType = RequestType.GET,
            url = url,
            tag = tag,
            isList = isList,
            params = params,
            header = header,
            isNoKey = isNoKey,
            replaceHost = replaceHost,
            cls = cls
        )
    }

    /**
     * 处理请求
     * @param url 请求路径
     * @param tag 请求标识
     * @param isList 返回数据是否是数组类型
     * @param params 请求参数-封装在url之后, key=value & key=value方式
     * @param body  请求参数-封装在post请求体之内
     * @param header 自定义的请求头
     * @param isNoKey  true该请求返回的数据直接解析为指定的数据类型
     * @param replaceHost 该请求替换的基础路径,为空表示不替换
     * @param cls 返回数据的类型
     */
    suspend fun <T> postSync(
        url: String,
        tag: ApiTag? = null,
        isList: Boolean = false,
        params: Map<String, Any>? = null,
        body: Map<String, Any>? = null,
        header: Map<String, String>? = null,
        isNoKey: Boolean = false,
        replaceHost: String? = null,
        cls: Class<T>
    ): ApiResponse<T> {
        return execute(
            requestType = RequestType.POST,
            url = url,
            tag = tag,
            isList = isList,
            params = params,
            body = body,
            header = header,
            isNoKey = isNoKey,
            replaceHost = replaceHost,
            cls = cls
        )
    }

    /**
     * 处理请求
     * @param url 请求路径
     * @param tag 请求标识
     * @param isList 返回数据是否是数组类型
     * @param params 请求参数-封装在url之后, key=value & key=value方式
     * @param body  请求参数-封装在post请求体之内
     * @param header 自定义的请求头
     * @param isNoKey  true该请求返回的数据直接解析为指定的数据类型
     * @param replaceHost 该请求替换的基础路径,为空表示不替换
     * @param cls 返回数据的类型
     */
    suspend fun <T> putSync(
        url: String,
        tag: ApiTag? = null,
        isList: Boolean = false,
        params: Map<String, Any>? = null,
        body: Map<String, Any>? = null,
        header: Map<String, String>? = null,
        isNoKey: Boolean = false,
        replaceHost: String? = null,
        cls: Class<T>
    ): ApiResponse<T> {
        return execute(
            requestType = RequestType.PUT,
            url = url,
            tag = tag,
            isList = isList,
            params = params,
            body = body,
            header = header,
            isNoKey = isNoKey,
            replaceHost = replaceHost,
            cls = cls
        )
    }

    /**
     * 处理请求
     * @param requestType 请求类型
     * @param replaceHost 该请求替换的基础路径,为空表示不替换
     * @param url 请求路径
     * @param tag 请求标识
     * @param isList 返回数据是否是数组类型
     * @param header 请求头
     * @param params 请求参数-封装在url之后, key=value & key=value方式
     * @param body  请求参数-封装在post请求体之内
     * @param isNoKey  true该请求返回的数据直接解析为指定的数据类型
     * @param cls 返回数据的类型
     */
    private suspend fun <T> execute(
        requestType: RequestType,
        replaceHost: String?,
        url: String,
        tag: ApiTag? = null,
        isList: Boolean = false,
        header: Map<String, String>? = null,
        params: Map<String, Any>? = null,
        body: Map<String, Any>? = null,
        isNoKey: Boolean = false,
        cls: Class<T>
    ): ApiResponse<T> {
        val response = ApiResponse<T>()
        response.tag = tag
        try {
            hostUrl = replaceHost ?: baseHost
            hostHeader = header ?: baseHeader
            //请求开始
            val r: Response<JsonObject> = when (requestType) {
                RequestType.GET -> {
                    executeGet(url, params)
                }
                RequestType.POST -> {
                    executePost(url, params, body)
                }
                RequestType.PUT -> {
                    executePut(url, params, body)
                }
            }
            //请求完成
            //检测请求是否成功
            if (!r.isSuccessful) {
                response.error = ApiError(r.code())
                return response
            }
            //获取返回数据
            val jsonObject = r.body()
            if (jsonObject == null || jsonObject.toString().isEmpty()) {
                response.error = ApiError(-0x01, "没有数据")
                return response
            }

            //不包含要解析的字段
            if (!checkSuccess(jsonObject)) {
                //判断是否设置了不需要这些字段
                if (!isNoKey) {
                    response.error = ApiError(-0x02, "不包含指定的数据字段")
                    return response
                }
                //直接解析并返回数据
                if (isList) {
                    val jsonArray = jsonObject.asJsonArray
                    val dataList = arrayListOf<T>()
                    val gson = Gson()
                    for (j in jsonArray) {
                        val d = gson.fromJson(j, cls)
                        dataList.add(d)
                    }
                    response.dataList = dataList
                } else {
                    val d = Gson().fromJson(jsonObject, cls)
                    response.data = d
                }
                return response
            }
            val code = jsonObject.get(codeKey).asInt
            if (code != successCode) {
                //成功码对应不上
                response.error = ApiError(code, jsonObject.get(msgKey)?.asString)
                return response
            }
            //请求成功,获取参数
            if (isList) {
                val jsonArray = jsonObject.getAsJsonArray(dataKey)
                val dataList = arrayListOf<T>()
                val gson = Gson()
                for (j in jsonArray) {
                    val d = gson.fromJson(j, cls)
                    dataList.add(d)
                }
                response.dataList = dataList
            } else {
                val jsonData = jsonObject.getAsJsonObject(dataKey)
                val d = Gson().fromJson(jsonData, cls)
                response.data = d
            }
            return response
        } catch (e: Throwable) {
            response.error = ApiError(-0x11, throwable = e)
            return response
        }
    }


    /**
     * 校验是否包含需要解析的字段
     */
    private fun checkSuccess(jsonObject: JsonObject): Boolean {
        return jsonObject.has(codeKey) && jsonObject.has(msgKey) && jsonObject.has(dataKey)
    }

    /**
     * 处理put请求
     */
    private suspend fun executePut(
        url: String,
        params: Map<String, Any>?,
        body: Map<String, Any>?
    ): Response<JsonObject> {
        return if (body != null && params != null) {
            val b = Gson().toJson(body)
                .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            apiInfo.executePut(url, params2String(params), b)
        } else if (body != null) {
            val b = Gson().toJson(body)
                .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            apiInfo.executePut(url, b)
        } else if (params != null) {
            apiInfo.executePut(url, params2String(params))
        } else {
            apiInfo.executePut(url)
        }
    }

    /**
     * 处理post请求
     */
    private suspend fun executePost(
        url: String,
        params: Map<String, Any>?,
        body: Map<String, Any>?
    ): Response<JsonObject> {
        return if (body != null && params != null) {
            val b = Gson().toJson(body)
                .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            apiInfo.executePost(url, params2String(params), b)
        } else if (body != null) {
            val b = Gson().toJson(body)
                .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            apiInfo.executePost(url, b)
        } else if (params != null) {
            apiInfo.executePost(url, params2String(params))
        } else {
            apiInfo.executePost(url)
        }
    }


    /**
     * 处理get请求
     */
    private suspend fun executeGet(
        url: String,
        params: Map<String, Any>?
    ): Response<JsonObject> {
        return if (params != null) {
            apiInfo.executeGet(url, params2String(params))
        } else {
            apiInfo.executeGet(url)
        }
    }

    /**
     * 将params的v值全部转变为string
     */
    private fun params2String(params: Map<String, Any>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for ((k, v) in params) {
            v.let {
                result[k] = v.toString()
            }
        }
        return result
    }

}