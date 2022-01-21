package com.lee.video.lib.gl.render.drawer

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.util.Log
import com.lee.video.R
import com.lee.video.lib.gl.ShaderUtil

/**
 * 支持裁剪视频画面
 * @author lee
 * @date 2021/12/6
 */
class VideoClipDrawer(context: Context,
                      var originalW: Int,
                      var originalH: Int,
                      var clipW: Int,
                      var clipH: Int,
                      var marginLeft: Int,
                      var marginTop: Int) :
    BaseDrawer(context) {
    /**
     * 纹理对象指针
     */
    private var textureP: Int = 0

    /**
     * 纹理对象id
     */
    private var textureId = IntArray(1)

    private var surfaceTexture: SurfaceTexture? = null

    override fun getSurface(): SurfaceTexture? {
        return surfaceTexture
    }

    override fun getVertexShader(): Int {
        return R.raw.vs_base
    }

    override fun getFragmentShader(): Int {
        return R.raw.fs_video
    }

    override fun setSize(width: Int, height: Int) {
    }

    override fun onWordSize(width: Int, height: Int) {
    }


    override fun config() {
        coordinateBuffer = ShaderUtil.generateCoordinateBuffer(originalW,originalH,clipW,clipH,marginLeft,marginTop)
        //获取纹理索引
        textureP = ShaderUtil.getUniform(glProgram, "uTexture")
        //绑定纹理
        ShaderUtil.bindTexture(0, textureId, textureP)
        //创建surfaceTexture
        surfaceTexture = SurfaceTexture(textureId[0])
    }


    override fun render() {
        //更新
        surfaceTexture?.updateTexImage()
    }

    override fun release() {
        super.release()
        //解绑纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glDeleteTextures(1, textureId, 0)
        //删除gl
        GLES20.glDeleteProgram(glProgram)
    }

    override fun translate(dx: Float, dy: Float) {
    }
}