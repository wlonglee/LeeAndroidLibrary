package com.lee.video.lib.gl.render.drawer

import android.graphics.SurfaceTexture

/**
 * 定义绘制器
 * @author lee
 * @date 2021/11/23
 */
interface IDrawer {
    /**
     * 配置数据
     */
    fun onConfig()

    /**
     * 设置绘制数据的宽高
     */
    fun setSize(width: Int, height: Int)

    /**
     * 设定GL区域与屏幕的偏移量-水印会使用
     */
    fun setOffset(x: Int, y: Int)

    /**
     * 设定GL区域大小 -
     * GL区域大小 与 setSize确定数据正常显示的缩放比例
     */
    fun onWordSize(width: Int, height: Int)

    /**
     * 是否自行配置GL区域大小,该值返回true的情况下,渲染器将不会设定GL区域大小
     */
    fun useCustomWordSize(): Boolean = false

    /**
     * 绘制画面
     */
    fun onDrawFrame()

    /**
     * 释放资源
     */
    fun release()

    /**
     * 画面平移
     */
    fun translate(dx: Float, dy: Float)

    /**
     * 获取绘制表面
     */
    fun getSurface(): SurfaceTexture? = null
}