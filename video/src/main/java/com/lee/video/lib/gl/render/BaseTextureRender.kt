package com.lee.video.lib.gl.render

import android.opengl.GLES20
import com.lee.video.lib.gl.render.drawer.IDrawer
import com.lee.video.lib.gl.GLTextureView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * GLTextureView基础渲染器
 * GLTextureView是在TextureView的基础上,封装了GL相关的操作
 * TextureView相较于SurfaceView,拥有了View的一切可操作属性,可以移动/缩放/透明度等等View的操作
 * 作为代价,其性能约降低了15%,经测试4k 50帧的视频,使用GLTextureView渲染持续丢帧,但使用GLSurfaceView渲染没有问题
 *@author lee
 *@date 2021/11/23
 */
class BaseTextureRender : GLTextureView.Renderer {

    private val drawers = mutableListOf<IDrawer>()

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        for (drawer in drawers) {
            drawer.onConfig()
        }
    }


    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        for (drawer in drawers) {
            drawer.onWordSize(width, height)
        }
    }

    override fun onDrawFrame(gl: GL10) {
        // 清屏，否则会有画面残留
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        for (drawer in drawers) {
            drawer.onDrawFrame()
        }
    }

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