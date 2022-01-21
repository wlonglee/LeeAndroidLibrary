package com.lee.video.lib.gl.egl

import android.opengl.EGLContext
import android.opengl.EGLSurface
import com.lee.video.lib.gl.egl.EGLCore.Companion.EGL_RECORDABLE_ANDROID

/**
 * egl操作对象
 *@author lee
 *@date 2021/11/24
 */
class EGLSurfaceHolder {
    /**
     * egl对象
     */
    private lateinit var eglCore: EGLCore

    /**
     * 渲染表面
     */
    private var eglSurface: EGLSurface? = null

    /**
     * 初始化
     */
    fun init(shareContext: EGLContext? = null) {
        eglCore = EGLCore()
        eglCore.init(shareContext, EGL_RECORDABLE_ANDROID)
    }

    /**
     * 将surface绑定到egl
     */
    fun createEGLSurface(surface: Any?, width: Int = -1, height: Int = -1) {
        eglSurface = if (surface != null) {
            eglCore.createWindowSurface(surface)
        } else {
            eglCore.createOffscreenSurface(width, height)
        }
    }

    /**
     * 与当前线程绑定
     */
    fun makeCurrent() {
        if (eglSurface != null) {
            eglCore.makeCurrent(eglSurface!!)
        }
    }

    /**
     * 交换渲染缓冲区
     */
    fun swapBuffers() {
        if (eglSurface != null) {
            eglCore.swapBuffers(eglSurface!!)
        }
    }

    /**
     * 销毁对象
     */
    fun destroyEGLSurface() {
        if (eglSurface != null) {
            eglCore.destroySurface(eglSurface!!)
            eglSurface = null
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        eglCore.release()
    }

    /**
     * 设定渲染时间戳-如果是重编码该值会影响生成的视频时间戳,如果是渲染显示没有影响
     * @param curTimestamp 设定时间值,单位微秒
     */
    fun setTimestamp(curTimestamp: Long) {
        if (eglSurface != null) {
            eglCore.setPresentationTime(eglSurface!!, curTimestamp * 1000) //egl要求为纳秒,所以需要乘以1000
        }
    }
}