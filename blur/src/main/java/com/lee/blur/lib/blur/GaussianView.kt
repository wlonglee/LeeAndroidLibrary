package com.lee.blur.lib.blur

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import android.util.AttributeSet

/**
 * 高斯模糊
 *@author lee
 *@date 2021/4/13
 */
class GaussianView(context: Context?, attrs: AttributeSet?) : GLSurfaceView(context, attrs) {
    private var gaussianRender: GaussianRender? = null


    fun setRender(bitmap: Bitmap) {
        if (gaussianRender == null) {
            //设置GL版本
            setEGLContextClientVersion(2)
            //设置渲染器
            gaussianRender =
                GaussianRender(context, bitmap)
            setRenderer(gaussianRender)
            renderMode = RENDERMODE_WHEN_DIRTY
        } else {
            gaussianRender?.updateBitmap(bitmap)
            requestRender()
        }
    }


    fun updateSize(size: Float) {
        val s= when {
            size<0f -> {
                0f
            }
            size>1f -> {
                1f
            }
            else -> {
                size
            }
        }
        gaussianRender?.size = s
        requestRender()
    }

    fun release(){
        gaussianRender?.release()
    }
}