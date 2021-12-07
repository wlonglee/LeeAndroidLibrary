package com.lee.video.lib.gl.render

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.lee.video.lib.gl.render.drawer.IDrawer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * GLSurfaceView基础渲染器
 * GLSurfaceView在SurfaceView的基础上实现了GL相关操作
 * 优点：渲染高效
 * 缺点：渲染在单独的线程,无法实现View相关的操作
 *@author lee
 *@date 2021/11/23
 */
class BaseSurfaceRender : GLSurfaceView.Renderer {

    private val drawers = mutableListOf<IDrawer>()

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        drawers.forEach {
            it.onConfig()
        }
    }


    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        drawers.forEach {
            it.onWordSize(width, height)
        }
    }

    override fun onDrawFrame(gl: GL10) {
        // 清屏，否则会有画面残留
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        drawers.forEach {
            it.onDrawFrame()
        }
    }

    /**
     * 添加绘制器
     */
    fun addDrawer(drawer: IDrawer?) {
        if (drawer != null)
            drawers.add(drawer)
    }

    fun release() {
        for (drawer in drawers) {
            drawer.release()
        }
    }
}