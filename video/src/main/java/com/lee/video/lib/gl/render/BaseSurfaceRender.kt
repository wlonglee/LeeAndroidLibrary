package com.lee.video.lib.gl.render

import android.opengl.GLSurfaceView
import android.util.Log
import com.lee.video.lib.gl.ShaderUtil
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
    /**
     * 绘制器列表,每个绘制器都可干自己的绘制流程,当编码输出的时候,可以将所有效果叠加在一起进行输出
     */
    private val drawers = mutableListOf<IDrawer>()

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        ShaderUtil.clearColor()
        //配置数据
        drawers.forEach {
            it.onConfig()
        }
    }


    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        //设定GL区域大小
        drawers.forEach {
            if (!it.useCustomWordSize())
                it.onWordSize(width, height)
        }
    }

    /**
     * surface绘制
     */
    override fun onDrawFrame(gl: GL10) {
        // 清屏，否则会有画面残留
        ShaderUtil.cleanScreen()
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

    /**
     * 释放资源
     */
    fun release() {
        for (drawer in drawers) {
            drawer.release()
        }
    }
}