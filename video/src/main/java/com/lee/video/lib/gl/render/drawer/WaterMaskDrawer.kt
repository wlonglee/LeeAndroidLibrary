package com.lee.video.lib.gl.render.drawer

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import com.lee.video.R
import com.lee.video.lib.gl.ShaderUtil


/**
 * 渲染水印
 *@author lee
 *@date 2021/12/9
 */
class WaterMaskDrawer(context: Context?, var bitmap: Bitmap) :
    BaseDrawer(context) {
    /**
     * 纹理索引
     */
    private var textureId: Int = 0

    /**
     * 着色器中的纹理对象
     */
    private var texture: Int = 0

    override fun getVertexShader(): Int {
        return R.raw.vs_base
    }

    override fun getFragmentShader(): Int {
        return R.raw.fs_base
    }

    override fun config() {
        //加载图片为纹理
        textureId = ShaderUtil.loadTexture(bitmap)
        texture = ShaderUtil.getUniform(glProgram, "uTexture")
        //绑定纹理
        ShaderUtil.bindTexture(0, textureId, texture)
    }

    override fun render() {
        //将最终的结果渲染显示
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
    }

    override fun release() {
        super.release()
        ShaderUtil.unBindTexture(0)
    }

    override fun useCustomWordSize(): Boolean {
        return true
    }


    override fun translate(dx: Float, dy: Float) {
        offsetX += dx.toInt()
        //gl坐标系与屏幕坐标系相反,所以需要取负
        offsetY -= dy.toInt()
    }
}