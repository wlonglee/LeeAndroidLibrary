package com.lee.http

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.io.BufferedReader
import java.lang.Exception
import java.lang.reflect.Type

/**
 * 数据转换器,获取返回的数据
 *@author lee
 *@date 2022/4/27
 */
internal class ConverterFactory : Converter.Factory() {
    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, JsonObject> {
        return object : Converter<ResponseBody, JsonObject> {
            override fun convert(value: ResponseBody): JsonObject {
                try {
                    val reader = value.charStream()
                    val r = BufferedReader(reader)
                    val sb = StringBuilder()
                    var line: String?
                    while (r.readLine().also { line = it } != null) {
                        sb.append(line)
                    }
                    return JsonParser().parse(sb.toString()).asJsonObject
                } catch (e: Exception) {
                    throw e
                } finally {
                    value.close()
                }
            }
        }
    }
}