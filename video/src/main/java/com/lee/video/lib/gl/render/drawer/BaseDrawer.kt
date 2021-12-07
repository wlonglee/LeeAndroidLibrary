package com.lee.video.lib.gl.render.drawer

import android.content.Context
import android.opengl.GLES20
import android.util.Log
import com.lee.video.lib.gl.ShaderUtil
import java.nio.FloatBuffer

/**
 * 基础渲染器
 * @author lee
 * @date 2021/11/23
 */
abstract class BaseDrawer(var context: Context?) : IDrawer {
    /**
     * GL对象指针
     */
    protected var glProgram: Int = 0

    /**
     * 顶点对象指针
     */
    var position: Int = 0

    /**
     * 顶点数据
     */
    lateinit var positionBuffer: FloatBuffer

    /**
     * 纹理对象指针
     */
    var coordinate: Int = 0

    /**
     * 纹理数据
     */
    lateinit var coordinateBuffer: FloatBuffer

    override fun onConfig() {
        ShaderUtil.enableAlpha()
        glProgram = ShaderUtil.createGL(context!!, getVertexShader(), getFragmentShader())
        ShaderUtil.linkGL(glProgram)

        position = ShaderUtil.getAttributePosition(glProgram)
        coordinate = ShaderUtil.getAttributeCoordinate(glProgram)

        positionBuffer = ShaderUtil.generatePositionBuffer()
        coordinateBuffer = ShaderUtil.generateCoordinateBuffer()
        config()
    }

    override fun onWordSize(width: Int, height: Int) {

    }

    override fun onDrawFrame() {
        if (useCustomRender()) {
            render()
        } else {
            ShaderUtil.useGL(glProgram, position, positionBuffer, coordinate, coordinateBuffer)
            render()
            ShaderUtil.drawGL(position, coordinate)
        }
    }

    override fun release() {
        context = null
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
     * 自行渲染的情况下 由子类自行实现全部操作
     */
    abstract fun render()
}