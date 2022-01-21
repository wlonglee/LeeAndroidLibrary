package com.lee.video.lib.gl.render.drawer

import android.content.Context
import android.graphics.SurfaceTexture
import com.lee.video.R
import com.lee.video.lib.gl.ShaderUtil

/**
 * 视频渲染
 * @author lee
 * @date 2021/11/23
 */
class VideoDrawer(context: Context, var callBack: ((st: SurfaceTexture) -> Unit)?) :
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

    override fun config() {
        //获取纹理指针
        textureP = ShaderUtil.getUniform(glProgram, "uTexture")
        //绑定纹理
        ShaderUtil.bindTexture(0, textureId, textureP)
        //创建surfaceTexture
        surfaceTexture = SurfaceTexture(textureId[0])
        callBack?.invoke(surfaceTexture!!)
    }

    override fun render() {
        //更新数据
        surfaceTexture?.updateTexImage()
    }

    override fun release() {
        super.release()
        ShaderUtil.unBindTexture(0)
        ShaderUtil.delTexture(1, textureId)
        ShaderUtil.delGL(glProgram)
    }
}