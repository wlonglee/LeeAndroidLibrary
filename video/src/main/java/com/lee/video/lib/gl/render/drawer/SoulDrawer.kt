package com.lee.video.lib.gl.render.drawer

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.opengl.Matrix
import com.lee.video.R
import com.lee.video.lib.gl.ShaderUtil

/**
 * 灵魂出窍效果
 * @author lee
 * @date 2021/11/24
 */
class SoulDrawer(context: Context, var callBack: (st: SurfaceTexture) -> Unit) :
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

    /**
     * 进度对象指针
     */
    private var progressP: Int = 0

    /**
     * FBO标记对象指针
     */
    private var drawFboP: Int = 0

    /**
     * 灵魂出窍纹理对象指针
     */
    private var soulTextureP: Int = 0

    /**
     * 纹理对象id
     */
    private var textureId = IntArray(1)

    /**
     * fbo缓存对象id
     */
    private var fboBuffer = IntArray(1)

    /**
     * fbo纹理对象id
     */
    private var fboTextureId = IntArray(1)

    /**
     * 灵魂出窍持续时间
     */
    private var soulTime = 500L

    /**
     * 透明度
     */
    private var alpha: Float = 1f

    private var drawFbo: Int = 0

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
        return R.raw.fs_soul
    }

    /**
     * 设定视频的宽和高
     */
    override fun setSize(width: Int, height: Int) {
        videoWidth = width
        videoHeight = height
        initMatrix()
    }

    override fun onWordSize(width: Int, height: Int) {
        worldWidth = width
        worldHeight = height
        initMatrix()
    }


    override fun config() {
        alphaP = ShaderUtil.getUniform(glProgram, "inAlpha")
        matrixP = ShaderUtil.getUniform(glProgram, "uMatrix")

        progressP = ShaderUtil.getUniform(glProgram, "progress")
        drawFboP = ShaderUtil.getUniform(glProgram, "drawFbo")
        soulTextureP = ShaderUtil.getUniform(glProgram, "uSoulTexture")

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
            matrix = FloatArray(16)
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


            //创建fbo纹理
            ShaderUtil.generateFrameBufferObject(fboBuffer, fboTextureId, videoWidth, videoHeight)
            //绑定fbo纹理
            ShaderUtil.bindTexture(1, fboTextureId, soulTextureP)
        }
    }

    override fun render() {
        updateFBO()
        surfaceTexture?.updateTexImage()
        //设定矩阵
        ShaderUtil.setUniformMatrix4f(matrixP, matrix)
        //设定透明度
        ShaderUtil.setUniform1f(alphaP, alpha)
        //设定进度
        ShaderUtil.setUniform1f(progressP, (System.currentTimeMillis() - soulTime) / 500f)
        //设定是否绘制fbo
        ShaderUtil.setUniform1i(drawFboP, drawFbo)
    }

    private fun updateFBO() {
        if (System.currentTimeMillis() - soulTime > 500) {
            soulTime = System.currentTimeMillis()
            drawFbo = 1
            //启用FBO渲染
            ShaderUtil.renderFrameBufferBegin(fboBuffer[0])
            //获取视频画面
            surfaceTexture?.updateTexImage()
            //设定矩阵
            ShaderUtil.setUniformMatrix4f(matrixP, matrix)
            //设定透明度
            ShaderUtil.setUniform1f(alphaP, alpha)
            //设定进度
            ShaderUtil.setUniform1f(progressP, (System.currentTimeMillis() - soulTime) / 500f)
            //设定绘制fbo
            ShaderUtil.setUniform1i(drawFboP, drawFbo)
            //终止FBO渲染
            ShaderUtil.renderFrameBufferEnd()
            drawFbo = 0
        }
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