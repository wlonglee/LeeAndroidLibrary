package com.lee.video.lib.gl.render.drawer

import android.content.Context
import android.opengl.Matrix
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
     * 顶点坐标对象指针
     */
    protected var position: Int = 0

    /**
     * 纹理坐标对象指针
     */
    protected var coordinate: Int = 0

    /**
     * 顶点坐标数据
     */
    protected lateinit var positionBuffer: FloatBuffer

    /**
     * 纹理坐标数据，修改此数据可控制纹理的渲染区域-画面裁剪 即修改此数据
     */
    protected lateinit var coordinateBuffer: FloatBuffer

    /**
     * 矩阵对象指针
     */
    protected var matrixP: Int = 0

    /**
     * 矩阵--缩放、位移
     */
    protected var matrix = FloatArray(16)

    /**
     * 数据的宽
     */
    protected var width: Int = 0

    /**
     * 数据的高
     */
    protected var height: Int = 0

    /**
     * GL窗口的偏移量,相对于屏幕左侧的距离 ---水印会使用
     */
    protected var offsetX: Int = 0

    /**
     * GL窗口的偏移量,相对于屏幕底部的距离 ---水印会使用
     */
    protected var offsetY: Int = 0

    /**
     * GL的宽
     */
    protected var worldWidth: Int = 0

    /**
     * GL的高
     */
    protected var worldHeight: Int = 0

    /**
     * 宽度缩放比例
     */
    protected var widthRatio = 1f

    /**
     * 高度缩放比例
     */
    protected var heightRatio = 1f

    /**
     * 初始化配置
     */
    override fun onConfig() {
        //开启混合模式
        ShaderUtil.enableAlpha()

        //初始化shader
        glProgram = ShaderUtil.createGL(context!!, getVertexShader(), getFragmentShader())
        ShaderUtil.linkGL(glProgram)

        //获取顶点坐标与纹理坐标数据
        position = ShaderUtil.getAttributePosition(glProgram)
        coordinate = ShaderUtil.getAttributeCoordinate(glProgram)
        //使用默认的顶点数据与纹理数据
        positionBuffer = ShaderUtil.generatePositionBuffer()
        coordinateBuffer = ShaderUtil.generateCoordinateBuffer()
        //获取矩阵指针
        matrixP = ShaderUtil.getUniform(glProgram, "uMatrix")
        initMatrix()
        //子类自行配置自己所需数据
        config()
    }

    override fun setSize(width: Int, height: Int) {
        //设定新的宽高数据
        this.width = width
        this.height = height

        //更新矩阵配置
        if (fitScale() && isSizeOK())
            configMatrix()
    }

    override fun setOffset(x: Int, y: Int) {
        this.offsetX = x
        this.offsetY = y
    }

    override fun onWordSize(width: Int, height: Int) {
        //设定新的大小数据
        this.worldWidth = width
        this.worldHeight = height

        //更新矩阵配置
        if (fitScale() && isSizeOK())
            configMatrix()
    }

    override fun onDrawFrame() {
        if (useCustomRender()) {
            render()
        } else {
            ShaderUtil.useGL(glProgram, position, positionBuffer, coordinate, coordinateBuffer)
            ShaderUtil.setWord(offsetX, offsetY, worldWidth, worldHeight)
            ShaderUtil.setUniformMatrix4f(matrixP, matrix)
            render()
            ShaderUtil.drawGL(position, coordinate)
        }
    }

    override fun translate(dx: Float, dy: Float) {
        Matrix.translateM(
            matrix,
            0,
            dx / worldWidth * widthRatio * 2,
            -dy / worldHeight * heightRatio * 2,
            0f
        )
    }

    fun scale(sx: Float, sy: Float) {
        Matrix.scaleM(matrix, 0, sx, sy, 1f)
        widthRatio /= sx
        heightRatio /= sy
    }

    override fun release() {
        context = null
    }

    private fun isSizeOK(): Boolean {
        return !(width <= 0 || height <= 0 || worldWidth <= 0 || worldHeight <= 0)
    }

    /**
     * 配置矩阵
     * 子类可自行设定配置
     */
    open fun configMatrix() {
        val prjMatrix = FloatArray(16)
        val originRatio = width / height.toFloat()
        val worldRatio = worldWidth / worldHeight.toFloat()
        if (worldWidth > worldHeight) {
            if (originRatio > worldRatio) {
                heightRatio = originRatio / worldRatio
                Matrix.orthoM(
                    prjMatrix, 0,
                    -widthRatio, widthRatio,
                    -heightRatio, heightRatio,
                    3f, 5f
                )
            } else {
                // 原始比例小于窗口比例，缩放高度度会导致高度超出，因此，高度以窗口为准，缩放宽度
                widthRatio = worldRatio / originRatio
                Matrix.orthoM(
                    prjMatrix, 0,
                    -widthRatio, widthRatio,
                    -heightRatio, heightRatio,
                    3f, 5f
                )
            }
        } else {
            if (originRatio > worldRatio) {
                heightRatio = originRatio / worldRatio
                Matrix.orthoM(
                    prjMatrix, 0,
                    -widthRatio, widthRatio,
                    -heightRatio, heightRatio,
                    3f, 5f
                )
            } else {
                // 原始比例小于窗口比例，缩放高度会导致高度超出，因此，高度以窗口为准，缩放宽度
                widthRatio = worldRatio / originRatio
                Matrix.orthoM(
                    prjMatrix, 0,
                    -widthRatio, widthRatio,
                    -heightRatio, heightRatio,
                    3f, 5f
                )
            }
        }

        //设置相机位置
        val viewMatrix = FloatArray(16)
        Matrix.setLookAtM(
            viewMatrix, 0,
            0f, 0f, 5.0f,
            0f, 0f, 0f,
            0f, 1.0f, 0f
        )
        //计算变换矩阵
        Matrix.multiplyMM(matrix, 0, prjMatrix, 0, viewMatrix, 0)
    }

    /**
     * 设定默认矩阵
     */
    private fun initMatrix() {
        val prjMatrix = FloatArray(16)
        Matrix.orthoM(
            prjMatrix, 0,
            -1f, 1f,
            -1f, 1f,
            3f, 5f
        )
        //设置相机位置
        val viewMatrix = FloatArray(16)
        Matrix.setLookAtM(
            viewMatrix, 0,
            0f, 0f, 5.0f,
            0f, 0f, 0f,
            0f, 1.0f, 0f
        )
        //计算变换矩阵
        Matrix.multiplyMM(matrix, 0, prjMatrix, 0, viewMatrix, 0)
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
    open fun useCustomRender(): Boolean = false

    /**
     * 是否自动更新矩阵,true-使用数据的宽高的比例去计算矩阵,确保画面显示不产生拉伸
     */
    open fun fitScale(): Boolean = true

    /**
     * 渲染数据,非自行渲染的情况下
     * 1.设置需要传递的值
     * 2.设置需要传递的纹理
     * 自行渲染的情况下 由子类自行实现全部操作
     */
    abstract fun render()
}