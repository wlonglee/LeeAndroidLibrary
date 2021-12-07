package com.lee.video.lib.gl.render.drawer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import com.lee.video.R
import com.lee.video.lib.gl.ShaderUtil

/**
 * 高斯模糊
 *@author lee
 *@date 2021/12/7
 */
class GaussianDrawer(context: Context?, var bitmap: Bitmap,var bitmapW:Int,var bitmapH: Int) :BaseDrawer(context){
    /**
     * 纹理索引
     */
    private var textureId: Int = 0

    /**
     * 着色器中的纹理对象
     */
    private var texture: Int = 0

    private var sizeId: Int = 0

    /**
     * 模糊进度  范围0~1
     */
    var size = 1f

    /**
     * 模糊范围
     */
    private val blurSize = 4.7f


    /**
     * 模糊次数
     */
    private val fboSize = 6

    private var frameBuffer = IntArray(fboSize)
    private var frameTexture = IntArray(fboSize)


    /**
     * 是否需要更新图片
     */
    private var change = false
    override fun getVertexShader(): Int {
        return R.raw.vs_base
    }

    override fun getFragmentShader(): Int {
        return R.raw.fs_blur
    }

    override fun config() {
        change = false
        //加载图片为纹理
        textureId = ShaderUtil.loadTexture(bitmap)
        texture = ShaderUtil.getUniform(glProgram, "uTexture")
        //绑定纹理
        ShaderUtil.bindTexture(0, textureId, texture)

        sizeId = ShaderUtil.getUniform(glProgram, "size")
        ShaderUtil.generateFrameBufferObject(frameBuffer, frameTexture, bitmapW, bitmapH, fboSize)
    }
    override fun useCustomRender(): Boolean {
        return true
    }

    override fun render() {
        if (change) {
            change = false

            textureId = ShaderUtil.loadTexture(bitmap)
            //绑定纹理
            ShaderUtil.bindTexture(0, textureId, texture)
        }

        //首次渲染 将图片渲染到FBO
        ShaderUtil.renderFrameBufferBegin(frameBuffer[0])
        ShaderUtil.useGL(glProgram, position, positionBuffer, coordinate, coordinateBuffer)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        ShaderUtil.setUniform1f(sizeId, size * blurSize)
        ShaderUtil.drawGL(position, coordinate)
        ShaderUtil.renderFrameBufferEnd()


        //将上一次的渲染结果继续FBO渲染,同时扩散模糊范围,达成更好的模糊效果
        for (i in 1 until fboSize) {
            ShaderUtil.renderFrameBufferBegin(frameBuffer[i])
            ShaderUtil.useGL(glProgram, position, positionBuffer, coordinate, coordinateBuffer)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameTexture[i - 1])
            ShaderUtil.setUniform1f(sizeId, size * blurSize * (i + 0.2f))
            ShaderUtil.drawGL(position, coordinate)
            ShaderUtil.renderFrameBufferEnd()
        }

        //将最终的结果渲染显示
        ShaderUtil.clearScreen()
        ShaderUtil.useGL(glProgram, position, positionBuffer, coordinate, coordinateBuffer)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameTexture[fboSize - 1])
        ShaderUtil.setUniform1f(sizeId, size * blurSize)
        ShaderUtil.drawGL(position, coordinate)

        //解绑纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    override fun setSize(width: Int, height: Int) {
    }

    override fun translate(dx: Float, dy: Float) {
    }

    override fun getSurface(): SurfaceTexture? {
        return null
    }

}