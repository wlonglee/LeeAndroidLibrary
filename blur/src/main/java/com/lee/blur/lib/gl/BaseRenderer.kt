package com.lee.blur.lib.gl

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * 基础渲染器
 *@author lee
 *@date 2021/4/13
 */
abstract class BaseRenderer(var context: Context?) : GLSurfaceView.Renderer {
    /**
     * GL对象指针
     */
    protected var glProgram: Int = 0

    /**
     * 顶点对象指针
     */
    protected var position: Int = 0

    /**
     * 顶点数据
     */
    protected lateinit var positionBuffer: FloatBuffer

    /**
     * 纹理对象指针
     */
    protected var coordinate: Int = 0

    /**
     * 纹理数据
     */
    protected lateinit var coordinateBuffer: FloatBuffer

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        glProgram = ShaderUtil.createGL(context!!, getVertexShader(), getFragmentShader())
        ShaderUtil.linkGL(glProgram)

        position = ShaderUtil.getAttributePosition(glProgram)
        coordinate = ShaderUtil.getAttributeCoordinate(glProgram)

        positionBuffer = ShaderUtil.generatePositionBuffer()
        coordinateBuffer = ShaderUtil.generateCoordinateBuffer()

        config()
    }


    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        onSizeChange(width,height)
    }

    override fun onDrawFrame(gl: GL10) {
        if (useCustomRender()) {
            render()
        } else {
            ShaderUtil.clearScreen()
            ShaderUtil.useGL(glProgram, position, positionBuffer, coordinate, coordinateBuffer)
            render()
            ShaderUtil.drawGL(position, coordinate)
        }
        renderEnd()
    }

    /**
     * 释放资源
     */
    fun release() {
        context = null
    }

    open fun onSizeChange(width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    /**
     * 获取顶点shader
     */
    abstract fun getVertexShader(): Int

    /**
     * 获取片元shader
     */
    abstract fun getFragmentShader(): Int


    /**
     * 配置
     * 1.需要传递的值
     * 2.需要激活的纹理
     */
    abstract fun config()

    /**
     * 是否需要自行渲染
     */
    open fun useCustomRender(): Boolean {
        return false
    }

    /**
     * 渲染数据,非自行渲染的情况下
     * 1.设置需要传递的值
     * 2.设置需要传递的纹理
     * 自行渲染的情况下 由子类自行实现
     */
    abstract fun render()


    /**
     * 渲染结束，解绑资源
     */
    abstract fun renderEnd()
}