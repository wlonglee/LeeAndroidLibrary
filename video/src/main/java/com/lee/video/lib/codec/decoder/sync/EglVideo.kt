package com.lee.video.lib.codec.decoder.sync

import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.util.Log
import android.view.Surface
import com.lee.video.lib.gl.egl.EGLCore
import com.lee.video.lib.gl.egl.EGLSurfaceHolder
import com.lee.video.lib.gl.render.drawer.IDrawer

/**
 *@author lee
 *@date 2021/12/6
 */
class EglVideo {
    private lateinit var eglSurfaceHolder: EGLSurfaceHolder
    private lateinit var drawer: IDrawer
    private lateinit var drawer2: IDrawer
    private lateinit var inputSurface: Surface
    fun create(surface: Surface, drawer: IDrawer, drawer2: IDrawer, width: Int, height: Int) {
        //初始化egl
        inputSurface = surface
        eglSurfaceHolder = EGLSurfaceHolder()
        eglSurfaceHolder.init()
        eglSurfaceHolder.createEGLSurface(inputSurface)
        //绑定
        eglSurfaceHolder.makeCurrent()
        //设置视图大小
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        //配置绘制器
        this.drawer = drawer
        this.drawer.onWordSize(width, height)
        this.drawer.onConfig()
        this.drawer2 = drawer2
        this.drawer2.onWordSize(width, height)
        this.drawer2.onConfig()
    }

    fun swapBuffers(time: Long) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        drawer.onDrawFrame()
        if (time in 5000000..10000000)
            drawer2.onDrawFrame()
        eglSurfaceHolder.setTimestamp(time)
        eglSurfaceHolder.swapBuffers()
    }

    fun getSurface(): SurfaceTexture {
        return drawer.getSurface()!!
    }

    fun setSize(width: Int, height: Int) {
        drawer.setSize(width, height)
        drawer2.setSize(width, height)
    }

    fun release() {
        eglSurfaceHolder.destroyEGLSurface()
        eglSurfaceHolder.release()
        inputSurface.release()
    }
}