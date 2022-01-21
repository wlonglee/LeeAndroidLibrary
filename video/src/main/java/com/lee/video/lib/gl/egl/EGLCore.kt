package com.lee.video.lib.gl.egl

import android.graphics.SurfaceTexture
import android.opengl.*
import android.view.Surface

/**
 * egl处理,简化操作
 *@author lee
 *@date 2021/11/24
 */
class EGLCore {
    // EGL相关变量
    companion object {
        const val FLAG_RECORDABLE = 0x01
        const val EGL_RECORDABLE_ANDROID = 0x3142
    }
    private var mEGLDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var mEGLContext = EGL14.EGL_NO_CONTEXT
    private var mEGLConfig: EGLConfig? = null

    /**
     * 初始化EGLDisplay
     * @param eglContext 共享上下文
     */
    fun init(eglContext: EGLContext?, flags: Int) {
        if (mEGLDisplay !== EGL14.EGL_NO_DISPLAY) {
            throw RuntimeException("EGL already set up")
        }

        val sharedContext = eglContext ?: EGL14.EGL_NO_CONTEXT

        //创建 EGLDisplay
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (mEGLDisplay === EGL14.EGL_NO_DISPLAY) {
            throw RuntimeException("Unable to get EGL14 display")
        }

        //初始化 EGLDisplay
        val version = IntArray(2)
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            mEGLDisplay = EGL14.EGL_NO_DISPLAY
            throw RuntimeException("unable to initialize EGL14")
        }

        //初始化EGLConfig，EGLContext上下文
        if (mEGLContext === EGL14.EGL_NO_CONTEXT) {
            val config =
                getConfig(flags, 2) ?: throw RuntimeException("Unable to find a suitable EGLConfig")
            val attr2List = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
            val context = EGL14.eglCreateContext(
                mEGLDisplay, config, sharedContext,
                attr2List, 0
            )
            mEGLConfig = config
            mEGLContext = context
        }
    }

    /**
     * 获取EGL配置信息
     * @param flags 初始化标记
     * @param version EGL版本
     */
    fun getConfig(flags: Int, version: Int): EGLConfig? {
        var renderType = EGL14.EGL_OPENGL_ES2_BIT
        if (version >= 3) {
            // 配置EGL 3
            renderType = renderType or EGLExt.EGL_OPENGL_ES3_BIT_KHR
        }

        // 配置数组，主要是配置RAGA位数和深度位数
        // 两个为一对，前面是key，后面是value
        // 数组必须以EGL14.EGL_NONE结尾
        val attrList = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, renderType,
            EGL14.EGL_NONE, 0,
            EGL14.EGL_NONE
        )
        //配置Android指定的标记
        if (flags and FLAG_RECORDABLE != 0) {
            attrList[attrList.size - 3] = EGL_RECORDABLE_ANDROID
            attrList[attrList.size - 2] = 1
        }
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)

        //获取可用的EGL配置列表
        if (!EGL14.eglChooseConfig(
                mEGLDisplay, attrList, 0,
                configs, 0, configs.size,
                numConfigs, 0
            )
        ) {
            return null
        }

        //使用系统推荐的第一个配置
        return configs[0]
    }

    /**
     * 创建可显示的渲染缓存
     * @param surface 渲染窗口的surface
     */
    fun createWindowSurface(surface: Any): EGLSurface {
        if (surface !is Surface && surface !is SurfaceTexture) {
            throw RuntimeException("Invalid surface: $surface")
        }

        val surfaceAttr = intArrayOf(EGL14.EGL_NONE)

        return EGL14.eglCreateWindowSurface(
            mEGLDisplay, mEGLConfig, surface,
            surfaceAttr, 0
        ) ?: throw RuntimeException("Surface was null")
    }

    /**
     * 创建离屏渲染缓存
     * @param width 缓存窗口宽
     * @param height 缓存窗口高
     */
    fun createOffscreenSurface(width: Int, height: Int): EGLSurface {
        val surfaceAttr = intArrayOf(
            EGL14.EGL_WIDTH, width,
            EGL14.EGL_HEIGHT, height,
            EGL14.EGL_NONE
        )

        return EGL14.eglCreatePbufferSurface(
            mEGLDisplay, mEGLConfig,
            surfaceAttr, 0
        ) ?: throw RuntimeException("Surface was null")
    }

    /**
     * 将当前线程与上下文进行绑定
     */
    fun makeCurrent(eglSurface: EGLSurface) {
        if (mEGLDisplay === EGL14.EGL_NO_DISPLAY) {
            throw RuntimeException("EGLDisplay is null, call init first")
        }
        if (!EGL14.eglMakeCurrent(mEGLDisplay, eglSurface, eglSurface, mEGLContext)) {
            throw RuntimeException("makeCurrent(eglSurface) failed")
        }
    }

    /**
     * 将当前线程与上下文进行绑定
     */
    fun makeCurrent(drawSurface: EGLSurface, readSurface: EGLSurface) {
        if (mEGLDisplay === EGL14.EGL_NO_DISPLAY) {
            throw RuntimeException("EGLDisplay is null, call init first")
        }
        if (!EGL14.eglMakeCurrent(mEGLDisplay, drawSurface, readSurface, mEGLContext)) {
            throw RuntimeException("eglMakeCurrent(draw,read) failed")
        }
    }

    /**
     * 将缓存图像数据发送到设备进行显示
     */
    fun swapBuffers(eglSurface: EGLSurface): Boolean {
        return EGL14.eglSwapBuffers(mEGLDisplay, eglSurface)
    }

    /**
     * 设置当前帧的时间，单位：纳秒
     */
    fun setPresentationTime(eglSurface: EGLSurface, nanosecond: Long) {
        EGLExt.eglPresentationTimeANDROID(mEGLDisplay, eglSurface, nanosecond)
    }

    /**
     * 销毁EGLSurface，并解除上下文绑定
     */
    fun destroySurface(eglSurface: EGLSurface) {
        EGL14.eglMakeCurrent(
            mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_CONTEXT
        )
        EGL14.eglDestroySurface(mEGLDisplay, eglSurface);
    }

    /**
     * 释放资源
     */
    fun release() {
        if (mEGLDisplay !== EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(
                mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT
            )
            EGL14.eglDestroyContext(mEGLDisplay, mEGLContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(mEGLDisplay)
        }

        mEGLDisplay = EGL14.EGL_NO_DISPLAY
        mEGLContext = EGL14.EGL_NO_CONTEXT
        mEGLConfig = null
    }
}