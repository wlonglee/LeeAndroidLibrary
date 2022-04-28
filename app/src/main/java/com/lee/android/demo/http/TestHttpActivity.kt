package com.lee.android.demo.http

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lee.android.R
import com.lee.android.demo.http.classify.ClassifyData
import com.lee.android.demo.http.classify.ClassifyDrum
import com.lee.android.demo.http.discover.DiscoverData
import com.lee.android.demo.http.drum.DrumData
import com.lee.http.model.ApiError
import com.lee.http.HttpApi
import com.lee.http.cb.ApiListener
import com.lee.http.model.ApiTag
import com.lee.http.cb.ApiBasicListener
import com.lee.http.model.ApiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.logging.HttpLoggingInterceptor


class TestHttpActivity : AppCompatActivity() {

    fun logI(msg: String) {
        Log.i("lee", msg)
    }

    fun logE(msg: String) {
        Log.e("lee", msg)
    }

    var basicListener: ApiBasicListener = object : ApiBasicListener {
        override fun onStart(tag: ApiTag?) {
            logE("onStart--->${tag?.tag}")
        }

        override fun onCompleted(tag: ApiTag?) {
            logE("onCompleted--->${tag?.tag}")
        }

        override fun onError(e: ApiError, tag: ApiTag?) {
            logE("onError--->$e")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_http)

        val header = mutableMapOf<String, String>()
        header["security_key"] =
            "YOuYb3XkActoXGNurdMCWwFw39h0JNnMB9XyYcIjbk%252FD0iZLgTXZEg%253D%253D"

        //初始化 配置请求信息
        HttpApi.init(
            "https://overseas.lavatest.com",  //请求host
            header, //默认请求头
            logInterceptor = object : HttpLoggingInterceptor.Logger {
                override fun log(message: String) {
                    logI(message)  //网络log输出
                }
            })

        //ApiBasicListener包含 onStart，onCompleted，onError
        //ApiListener包含 onSuccess onSuccessList
        // 请求回调执行顺序
        // onStart-->onCompleted
        // 成功后 onSuccess/onSuccessList (两个函数只会调用一个,根据返回数据类型决定)
        // 失败后 onError
    }

    fun get1(view: View) {
        //get请求,获取首页数据  https://overseas.lavamusic.com/loop/index/indexnew
        HttpApi.instance.get(
            lifecycleScope,
            url = "/loop/index/indexnew",
            tag = ApiTag("请求首页数据"),
            basicListener = basicListener,
            dataListener = object : ApiListener<DrumData> {
                override fun onSuccess(data: DrumData, tag: ApiTag?) {
                    logE("onSuccess:$data")
                }
            },
            cls = DrumData::class.java
        )
    }

    fun get2(view: View) {
        //get请求,指定分类数据查询 https://overseas.lavamusic.com/loop/index/search?style=[50]
        val params = mutableMapOf<String, Any>()
        val array = mutableListOf<Int>()
        array.add(50)
        params["style"] = array
        HttpApi.instance.get(
            lifecycleScope,
            url = "/loop/index/search",
            tag = ApiTag("指定分类数据查询"),
            params = params,
            basicListener = basicListener,
            dataListener = object : ApiListener<ClassifyDrum> {
                override fun onSuccess(data: ClassifyDrum, tag: ApiTag?) {
                    logE("onSuccess:$data")
                }
            },
            cls = ClassifyDrum::class.java
        )
    }

    fun get3(view: View) {
        //get请求,返回数据是列表类型
        HttpApi.instance.get(
            lifecycleScope,
            tag = ApiTag("查询分类列表"),
            url = "/loop/index/category",
            basicListener = basicListener,
            dataListener = object : ApiListener<ClassifyData> {
                override fun onSuccessList(data: List<ClassifyData>, tag: ApiTag?) {
                    logE("onSuccessList:${data.size},$data")
                }
            },
            isList = true, //返回数据是数组列表
            cls = ClassifyData::class.java
        )
    }

    fun get4(view: View) {
        //get请求,替换了请求host,并且不解码默认字段
        HttpApi.instance.get(
            lifecycleScope,
            replaceHost = "https://d11d0svgv78lor.cloudfront.net", //修改请求的host
            header = mutableMapOf(), //不要请求头
            url = "/i18n/en/country.json",
            basicListener = object : ApiBasicListener {  //自行处理 开始 结束 出错，不走统一的
                override fun onStart(tag: ApiTag?) {
                    logE("城市信息onStart")
                }

                override fun onError(e: ApiError, tag: ApiTag?) {
                    logE("城市信息onError")
                }

                override fun onCompleted(tag: ApiTag?) {
                    logE("城市信息onCompleted")
                }
            },
            dataListener = object : ApiListener<CityInfo> {
                override fun onSuccess(data: CityInfo, tag: ApiTag?) {
                    logE("onSuccess:$data")
                }
            },
            isNoKey = true,  //返回的数据，不需要校验code data  请求成功直接解析
            cls = CityInfo::class.java
        )
    }

    fun get5(view: View) {
        //get请求,封装多个请求,顺序执行
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                logE("请求1开始")
                val r1: ApiResponse<DrumData> = HttpApi.instance.getSync(
                    url = "/loop/index/indexnew",
                    tag = ApiTag("请求1"),
                    cls = DrumData::class.java
                )
                logE("请求1结果:${r1}")

                if (r1.isSuccess()) {
                    logE("请求1结果成功,有数据:${r1.data}")
                }

                logE("请求2开始")
                val r2: ApiResponse<ClassifyData> = HttpApi.instance.getSync(
                    tag = ApiTag("请求1"),
                    url = "/loop/index/category",
                    isList = true, //返回数据是数组列表
                    cls = ClassifyData::class.java
                )
                logE("请求2结果:${r2}")

                if (r2.isSuccess()) {
                    logE("请求2结果成功,有数据:${r2.dataList}")
                }
            }
        }
    }

    fun post1(view: View) {
        //post请求
        HttpApi.instance.post(
            lifecycleScope,
            url = "/loop/index/indexnew",
            tag = ApiTag("首页数据post请求"),
            basicListener = basicListener,
            dataListener = object : ApiListener<DrumData> {
                override fun onSuccess(data: DrumData, tag: ApiTag?) {
                    logE("onSuccess:$data")
                }
            },
            cls = DrumData::class.java
        )
    }

    fun post2(view: View) {
        //post请求-params参数
        val params = mutableMapOf<String, Any>()
        val array = mutableListOf<Int>()
        array.add(50)
        params["style"] = array
        HttpApi.instance.post(
            lifecycleScope,
            url = "/loop/index/search",
            tag = ApiTag("分类数据post查询"),
            params = params,
            basicListener = basicListener,
            dataListener = object : ApiListener<ClassifyDrum> {
                override fun onSuccess(data: ClassifyDrum, tag: ApiTag?) {
                    logE("onSuccess:$data")
                }
            },
            cls = ClassifyDrum::class.java
        )
    }

    fun post3(view: View) {
        //post请求-body参数
        val body = mutableMapOf<String, Any>()
        body["page"] = 1
        HttpApi.instance.post(
            lifecycleScope,
            url = "/loop/publish/list",
            tag = ApiTag("获取发现列表数据"),
            body = body,
            basicListener = basicListener,
            dataListener = object : ApiListener<DiscoverData> {
                override fun onSuccess(data: DiscoverData, tag: ApiTag?) {
                    logE("onSuccess:$data")
                }
            },
            cls = DiscoverData::class.java
        )
    }
}