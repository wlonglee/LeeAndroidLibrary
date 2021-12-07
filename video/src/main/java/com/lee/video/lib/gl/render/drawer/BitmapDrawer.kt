package com.lee.video.lib.gl.render.drawer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import com.lee.video.R
import com.lee.video.lib.gl.ShaderUtil


/**
 * 渲染图片
 *@author lee
 *@date 2021/12/6
 */
class BitmapDrawer(context: Context?, var bitmap: Bitmap) : BaseDrawer(context) {
    /**
     * 纹理索引
     */
    private var textureId: Int = 0

    /**
     * 着色器中的纹理对象
     */
    private var texture: Int = 0

    /**
     * 是否需要更新图片
     */
    private var change = false

    override fun getVertexShader(): Int {
        return R.raw.vs_base
    }

    override fun getFragmentShader(): Int {
        return R.raw.fs_base
    }

    override fun config() {
        coordinateBuffer = ShaderUtil.generateCoordinateBuffer(512,512,256,256,52,256)
        change = false
        //加载图片为纹理
        textureId = ShaderUtil.loadTexture(bitmap)
        texture = ShaderUtil.getUniform(glProgram, "uTexture")
        //绑定纹理
        ShaderUtil.bindTexture(0, textureId, texture)
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

        //将最终的结果渲染显示
        ShaderUtil.clearScreen()
        ShaderUtil.useGL(glProgram, position, positionBuffer, coordinate, coordinateBuffer)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        ShaderUtil.drawGL(position, coordinate)
    }

    override fun release() {
        super.release()
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    override fun setSize(width: Int, height: Int) {
    }

    override fun translate(dx: Float, dy: Float) {
    }

    override fun getSurface(): SurfaceTexture? {
        return null
    }


    fun updateBitmap(bitmap: Bitmap) {
        this.bitmap = bitmap
        change = true
    }
}