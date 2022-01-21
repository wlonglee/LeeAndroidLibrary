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
class BitmapDrawer(context: Context?, var bitmap: Bitmap) :
    BaseDrawer(context) {
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
        change = false
        //加载图片为纹理
        textureId = ShaderUtil.loadTexture(bitmap)
        texture = ShaderUtil.getUniform(glProgram, "uTexture")
        //绑定纹理
        ShaderUtil.bindTexture(0, textureId, texture)
    }

    override fun render() {
        if (change) {
            change = false
            textureId = ShaderUtil.loadTexture(bitmap)
            //绑定纹理
            ShaderUtil.bindTexture(0, textureId, texture)
        }
        //将最终的结果渲染显示
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
    }

    override fun release() {
        super.release()
        ShaderUtil.unBindTexture(0)
    }

    fun updateBitmap(bitmap: Bitmap) {
        this.bitmap = bitmap
        change = true
    }

    override fun useCustomWordSize(): Boolean {
        return true
    }
}