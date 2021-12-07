package com.lee.video.lib.gl.render.drawer

import android.graphics.SurfaceTexture

/**
 * 定义绘制器
 * @author lee
 * @date 2021/11/23
 */
interface IDrawer {
    /**
     * 配置
     */
    fun onConfig()

    /**
     * 设置画面的宽高
     */
    fun setSize(width: Int, height: Int)

    /**
     * 画面平移
     */
    fun translate(dx: Float, dy: Float)

    /**
     * GL大小
     */
    fun onWordSize(width: Int, height: Int)

    /**
     * 绘制
     */
    fun onDrawFrame()

    /**
     * 释放资源
     */
    fun release()

    fun getSurface(): SurfaceTexture?

}