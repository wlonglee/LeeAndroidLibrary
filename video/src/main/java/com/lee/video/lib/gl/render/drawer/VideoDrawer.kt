package com.lee.video.lib.gl.render.drawer

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import com.lee.video.R
import com.lee.video.lib.gl.ShaderUtil

/**
 * 视频渲染
 * @author lee
 * @date 2021/11/23
 */
class VideoDrawer(context: Context, var callBack: (st: SurfaceTexture) -> Unit) :
    BaseDrawer(context) {
    /**
     * 纹理对象指针
     */
    private var textureP: Int = 0

    /**
     * 矩阵对象指针
     */
    private var matrixP: Int = 0

    /**
     * 透明度对象指针
     */
    private var alphaP: Int = 0

    private var timeP: Int = 0

    /**
     * 纹理对象id
     */
    private var textureId = IntArray(1)

    /**
     * 透明度
     */
    private var alpha: Float = 1f

    /**
     * 矩阵--缩放、位移
     */
    private var matrix = FloatArray(16)

    /**
     * openGL的宽与高
     */
    private var worldWidth: Int = -1
    private var worldHeight: Int = -1

    private var videoWidth: Int = -1
    private var videoHeight: Int = -1

    /**
     * 视频的宽高缩放比例
     */
    private var widthRatio: Float = 1f
    private var heightRatio: Float = 1f

    private var surfaceTexture: SurfaceTexture? = null

    override fun getSurface(): SurfaceTexture? {
        return surfaceTexture
    }

    override fun getVertexShader(): Int {
        return R.raw.vs_video
    }

    override fun getFragmentShader(): Int {
        return R.raw.fs_video
    }

    /**
     * 设定视频的宽和高
     */
    override fun setSize(width: Int, height: Int) {
        Log.e("lee", "视频大小: $width*$height")
        videoWidth = width
        videoHeight = height
        initMatrix()
    }

    override fun onWordSize(width: Int, height: Int) {
        Log.e("lee", "视图大小: $width*$height")
        worldWidth = width
        worldHeight = height
        initMatrix()
    }


    override fun config() {
        alphaP = ShaderUtil.getUniform(glProgram, "inAlpha")
        timeP = ShaderUtil.getUniform(glProgram, "uTime")
        matrixP = ShaderUtil.getUniform(glProgram, "uMatrix")

        //获取纹理索引
        textureP = ShaderUtil.getUniform(glProgram, "uTexture")
        //绑定纹理
        ShaderUtil.bindTexture(0, textureId, textureP)
        //创建surfaceTexture
        surfaceTexture = SurfaceTexture(textureId[0])
        callBack.invoke(surfaceTexture!!)
    }

    private fun initMatrix() {
        if (videoWidth != -1 && videoHeight != -1 && worldWidth != -1 && worldHeight != -1) {
            val prjMatrix = FloatArray(16)
            val originRatio = videoWidth / videoHeight.toFloat()
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
                } else {// 原始比例小于窗口比例，缩放高度度会导致高度超出，因此，高度以窗口为准，缩放宽度
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
                } else {// 原始比例小于窗口比例，缩放高度会导致高度超出，因此，高度以窗口为准，缩放宽度
                    widthRatio = worldRatio / originRatio
                    Matrix.orthoM(
                        prjMatrix, 0,
                        -widthRatio, widthRatio,
                        -heightRatio, heightRatio,
                        3f, 5f
                    )
                }
            }

            Log.e("lee","matrix:${widthRatio},${heightRatio}")
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
    }

    private var time = 0f

    override fun render() {
        time += 0.016f
        //更新
        surfaceTexture?.updateTexImage()

        //设定矩阵
        ShaderUtil.setUniformMatrix4f(matrixP, matrix)
        //设定透明度
        ShaderUtil.setUniform1f(alphaP, alpha)
        ShaderUtil.setUniform1f(timeP, time)

    }

    override fun release() {
        super.release()
        //解绑纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glDeleteTextures(1, textureId, 0)
        //删除gl
        GLES20.glDeleteProgram(glProgram)
    }

    fun setSurfaceAlpha(alpha: Float) {
        this.alpha = alpha
    }

    /**
     * 画面平移
     */
    override fun translate(dx: Float, dy: Float) {
        Matrix.translateM(
            matrix,
            0,
            dx / worldWidth * widthRatio * 2,
            -dy / worldHeight * heightRatio * 2,
            0f
        )
    }

    /**
     * 画面缩放
     */
    fun scale(s: Float) {
        scale(s, s)
    }

    /**
     * 画面缩放
     */
    fun scale(sx: Float, sy: Float) {
        Matrix.scaleM(matrix, 0, sx, sy, 1f)
        widthRatio /= sx
        heightRatio /= sy
    }

}