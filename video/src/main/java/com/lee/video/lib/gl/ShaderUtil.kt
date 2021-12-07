package com.lee.video.lib.gl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLUtils
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * shader相关工具类,便捷操作GL
 *@author lee
 *@date 2021/4/12
 */
object ShaderUtil {

    /**
     * 顶点坐标
     * GL坐标系
     */
    private val positionData = floatArrayOf(
        -1.0f, 1.0f,    //左上角
        -1.0f, -1.0f,   //左下角
        1.0f, 1.0f,     //右上角
        1.0f, -1.0f     //右下角
    )

    /**
     * positionData数组中每个顶点的坐标数量
     *   -1.0f, 1.0f,       //左上角由2个点确定 ---->数量为2
     *   -1.0f, 1.0f,0f,    //左上角由3个点确定 ---->数量为3
     */
    private const val POS_PER_VERTEX = 2

    /**
     * 顶点数量
     */
    private var VERTEX_COUNT = positionData.size / POS_PER_VERTEX

    /**
     * 默认纹理坐标
     */
    private val coordinateData = floatArrayOf(
        0f, 0f,     //左上角  0
        0f, 1f,     //左下角  1
        1f, 0f,     //右上角  2
        1f, 1f      //右下角  3
    )

    /**
     * coordinateData数组中每个点的坐标数量
     */
    private const val COORDINATE_PER_VERTEX = 2

    /**
     * 三角形绘制顺序,与纹理坐标挂钩
     * 0-1-2组成一个三角形
     * 1-2-3组成一个三角形
     * 012/123这个是drawGL默认的绘制规则
     */
    private val drawOrder = shortArrayOf(0, 1, 2, 1, 2, 3)

    /**
     * 1.创建一个GL对象
     * @param vertexShader 顶点shader文件
     * @param fragmentShader 片元shader文件
     *
     * @return 创建好的GL对象指针
     */
    fun createGL(context: Context, vertexShader: Int, fragmentShader: Int): Int {
        //加载shader文件
        val vertex = loadShader(GLES20.GL_VERTEX_SHADER, readShader(context, vertexShader))
        val fragment = loadShader(GLES20.GL_FRAGMENT_SHADER, readShader(context, fragmentShader))

        //编译失败
        if (vertex == -1 || fragment == -1) {
            Log.e("lee", "gl compiled fail")
            throw RuntimeException("gl compiled fail")
        }
        //创建OpenGL ES程序，注意：需要在OpenGL渲染线程中创建，否则无法渲染
        val program = GLES20.glCreateProgram()
        //绑定着色器
        GLES20.glAttachShader(program, vertex)
        GLES20.glAttachShader(program, fragment)
        return program
    }

    /**
     * 读取shader文件流
     * @param resId 顶点或片元文件
     */
    private fun readShader(context: Context, resId: Int): String {
        val builder = StringBuilder()
        try {
            val inputStream = context.resources.openRawResource(resId)
            val streamReader = InputStreamReader(inputStream)
            val bufferedReader = BufferedReader(streamReader)
            var textLine: String?
            while (bufferedReader.readLine().also { textLine = it } != null) {
                builder.append(textLine)
                builder.append("\n")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Log.e("lee", builder.toString())
        return builder.toString()
    }

    /**
     * 加载shader
     * @param glShader shader标识
     * @param shader  shader文件
     */
    private fun loadShader(glShader: Int, shader: String): Int {
        //创建
        val glCreateShader = GLES20.glCreateShader(glShader)
        //加载
        GLES20.glShaderSource(glCreateShader, shader)
        //编译
        GLES20.glCompileShader(glCreateShader)

        //编译结果
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(glCreateShader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] != GLES20.GL_TRUE) {
            //编译失败
            Log.e("lee", GLES20.glGetShaderInfoLog(glCreateShader))
            GLES20.glDeleteShader(glCreateShader)
            return -1
        }
        //返回
        return glCreateShader
    }

    /**
     * 2.解析GL
     * @param program GL对象
     */
    fun linkGL(program: Int) {
        //连接到着色器程序
        GLES20.glLinkProgram(program)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES20.GL_TRUE) {
            //解析失败
            val info = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            Log.e("lee", "Could not link program: $info")
            throw RuntimeException("Could not link program: $info")
        }
    }

    /**
     * 3.获取顶点属性
     * @param program GL对象
     */
    fun getAttributePosition(program: Int): Int {
        return GLES20.glGetAttribLocation(program, "vPosition")
    }

    /**
     * 4.获取纹理属性
     * @param program GL对象
     */
    fun getAttributeCoordinate(program: Int): Int {
        return GLES20.glGetAttribLocation(program, "vCoordinate")
    }

    /**
     * 4.1获取需要传递的属性
     * @param name 属性名
     */
    fun getUniform(program: Int, name: String): Int {
        return GLES20.glGetUniformLocation(program, name)
    }


    /**
     * 获取顶点数据,一次即可
     */
    fun generatePositionBuffer(): FloatBuffer {
        val posBuffer = ByteBuffer.allocateDirect(positionData.size * 4) //一个float有4个字节
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(positionData)
        posBuffer.position(0)
        return posBuffer
    }

    /**
     * 获取纹理数据,一次即可
     */
    fun generateCoordinateBuffer(): FloatBuffer {
        val coordinateBuffer = ByteBuffer.allocateDirect(coordinateData.size * 4) //一个float有4个字节
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(coordinateData)
        coordinateBuffer.position(0)
        return coordinateBuffer
    }

    /**
     * 获取纹理数据,一次即可,通过预设的裁剪区域 对显示区域进行裁剪后显示
     * @param originalW 原始宽
     * @param originalH 原始高
     * @param clipW  裁剪的宽
     * @param clipH 裁剪的高
     * @param marginLeft 裁剪区域距离左边的距离
     * @param marginTop 裁剪区域距离顶部的距离
     */
    fun generateCoordinateBuffer(
        originalW: Int,
        originalH: Int,
        clipW: Int,
        clipH: Int,
        marginLeft: Int,
        marginTop: Int
    ): FloatBuffer {
        val dLeft = marginLeft * 1f / originalW
        val dTop = marginTop * 1f / originalH
        val dRight = (marginLeft + clipW) * 1f / originalW
        val dBottom = (marginTop + clipH) * 1f / originalH
        val coordinateData = floatArrayOf(
            dLeft, dTop,        //左上角  0
            dLeft, dBottom,     //左下角  1
            dRight, dTop,       //右上角  2
            dRight, dBottom     //右下角  3
        )

        val coordinateBuffer = ByteBuffer.allocateDirect(coordinateData.size * 4) //一个float有4个字节
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(coordinateData)
        coordinateBuffer.position(0)
        return coordinateBuffer
    }

    /**
     * 获取绘制数据,一次即可
     */
    fun generateDrawOrderBuffer(): ShortBuffer {
        val drawBuffer = ByteBuffer.allocateDirect(drawOrder.size * 2) //一个short有2个字节
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .put(drawOrder)
        drawBuffer.position(0)
        return drawBuffer
    }

    /**
     * 5.使用GL
     * @param program GL对象
     * @param position 顶点
     * @param positionBuffer 顶点数据
     * @param coordinate 纹理
     * @param coordinateBuffer 纹理数据
     */
    fun useGL(
        program: Int,
        position: Int,
        positionBuffer: FloatBuffer,
        coordinate: Int,
        coordinateBuffer: FloatBuffer
    ) {
        //启用gl
        GLES20.glUseProgram(program)

        //启用顶点
        GLES20.glEnableVertexAttribArray(position)
        //顶点赋值
        GLES20.glVertexAttribPointer(
            position,
            POS_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            POS_PER_VERTEX * 4, //每个点4个字节
            positionBuffer
        )

        //启用纹理
        GLES20.glEnableVertexAttribArray(coordinate)
        //纹理赋值
        GLES20.glVertexAttribPointer(
            coordinate,
            COORDINATE_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            COORDINATE_PER_VERTEX * 4,  //每个点4个字节
            coordinateBuffer
        )
    }


    /**
     * 5.1 给指定的属性赋值
     * @param attribute 属性
     * @param v 属性值, float类型
     */
    fun setUniform1f(attribute: Int, v: Float) {
        GLES20.glUniform1f(attribute, v)
    }

    /**
     * 5.1 给指定的属性赋值
     * @param attribute 属性
     * @param v 属性值, int类型
     */
    fun setUniform1i(attribute: Int, v: Int) {
        GLES20.glUniform1i(attribute, v)
    }

    /**
     * 5.1 给指定的属性赋值
     * @param attribute 属性
     * @param v 属性值,用于矩阵
     */
    fun setUniformMatrix4f(attribute: Int, v: FloatArray) {
        GLES20.glUniformMatrix4fv(attribute, 1, false, v, 0)
    }

    /**
     * 加载资源为GL纹理
     * @return 纹理索引
     */
    fun loadTexture(context: Context, resId: Int): Int {
        val texture = IntArray(1)

        //创建纹理
        GLES20.glGenTextures(1, texture, 0)
        //绑定纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0])

        //设置环绕和过滤方式
        //环绕(超出纹理坐标范围)(s==x t==y GL_REPEAT 重复)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
        //过滤(纹理像素映射到坐标点)(缩小、放大:GL_LINEAR线性)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        //设置图片
        val bitmap = BitmapFactory.decodeResource(context.resources, resId)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        bitmap.recycle()

        //解绑纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

        return texture[0]
    }

    /**
     * 加载图片为GL纹理
     * @return 纹理索引
     */
    fun loadTexture(bitmap: Bitmap): Int {
        val texture = IntArray(1)

        //创建纹理
        GLES20.glGenTextures(1, texture, 0)
        //绑定纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0])

        //设置环绕和过滤方式
        //环绕(超出纹理坐标范围)(s==x t==y GL_REPEAT 重复)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
        //过滤(纹理像素映射到坐标点)(缩小、放大:GL_LINEAR线性)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        //设置图片
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        bitmap.recycle()

        //解绑纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

        return texture[0]
    }


    /**
     * 5.2绑定纹理
     * @param offset 纹理编号 0~31
     * @param id 纹理索引
     * @param texture 纹理属性
     */
    fun bindTexture(offset: Int, id: Int, texture: Int) {
        //激活指定纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + offset)
        //绑定纹理索引
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D + offset, id)
        //设置
        GLES20.glUniform1i(texture, offset)
    }

    /**
     * 5.2绑定根据颜色值生成的纹理
     * @param offset 纹理编号 0~31
     * @param id 纹理索引
     * @param texture 纹理属性
     * @param buffer 色值
     * @param width 图片宽
     * @param height 图片高
     */
    fun bindTexture(
        offset: Int,
        id: Int,
        texture: Int,
        buffer: ByteBuffer,
        width: Int,
        height: Int
    ) {
        //激活指定纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + offset)
        //绑定纹理索引
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D + offset, id)

        //生成纹理
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_LUMINANCE,
            width,
            height,
            0,
            GLES20.GL_LUMINANCE,
            GLES20.GL_UNSIGNED_BYTE,
            buffer
        )

        //设置
        GLES20.glUniform1i(texture, offset)
    }


    /**
     * 5.2绑定扩展纹理,渲染视频
     * @param offset 纹理编号 0~31
     * @param id 纹理索引
     * @param texture 纹理属性
     */
    fun bindTexture(offset: Int, id: IntArray, texture: Int) {
        //激活指定纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + offset)
        //生成纹理 n:生成的纹理数量
        GLES20.glGenTextures(1, id, 0)
        //绑定
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, id[0])
        //设置
        GLES20.glUniform1i(texture, offset)
    }

    /**
     * 6.使用默认方式绘制三角形
     */
    fun drawGL(position: Int, coordinate: Int) {
        //绘制三角形
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTEX_COUNT)

        //禁用顶点
        GLES20.glDisableVertexAttribArray(position)
        //禁用纹理
        GLES20.glDisableVertexAttribArray(coordinate)
    }

    /**
     * 6.使用设定的顺序去绘制三角形
     */
    fun drawGL(drawBuffer: ShortBuffer, position: Int, coordinate: Int) {
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLE_STRIP,
            drawOrder.size,
            GLES20.GL_UNSIGNED_SHORT,
            drawBuffer
        )

        //禁用顶点
        GLES20.glDisableVertexAttribArray(position)
        //禁用纹理
        GLES20.glDisableVertexAttribArray(coordinate)
    }

    /**
     * 开启混合模式,这样透明度渐变过渡的效果才生效,否则有锯齿
     */
    fun enableAlpha() {
        // 开启混合模式
        GLES20.glEnable(GLES20.GL_BLEND)
        // 配置混合算法
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
    }

    /**
     * 清屏
     */
    fun clearScreen() {
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
    }

    /**
     * 创建一个FBO离屏缓存对象
     * @param frameBuffer fbo缓存索引
     * @param frameTexture fbo纹理索引
     * @param width fbo宽
     * @param height fbo高
     */
    fun generateFrameBufferObject(
        frameBuffer: IntArray,
        frameTexture: IntArray,
        width: Int,
        height: Int
    ) {
        //创建FBO
        GLES20.glGenFramebuffers(1, frameBuffer, 0)
        //生成纹理 n:生成的纹理数量
        GLES20.glGenTextures(1, frameTexture, 0)
        //绑定纹理索引
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameTexture[0])

        //生成纹理
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_RGBA,
            width,
            height,
            0,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            null
        )

        //设置环绕和过滤方式
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE.toFloat()
        )
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE.toFloat()
        )

        //绑定FBO索引
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0])
        //绑定纹理到FBO
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER,
            GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D,
            frameTexture[0],
            0
        )

        //解绑纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        //解绑FBO
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    /**
     * 生成指定数量的FBO
     * @param frameBuffer fbo索引
     * @param frameTexture fbo纹理索引
     * @param width fbo宽
     * @param height fbo高
     * @param size fbo数量
     */
    fun generateFrameBufferObject(
        frameBuffer: IntArray,
        frameTexture: IntArray,
        width: Int,
        height: Int,
        size: Int
    ) {
        GLES20.glGenFramebuffers(size, frameBuffer, 0)
        GLES20.glGenTextures(size, frameTexture, 0)
        for (i in 0 until size) {
            //绑定纹理索引
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameTexture[i])

            //生成纹理
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                GLES20.GL_RGBA,
                width,
                height,
                0,
                GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE,
                null
            )

            //设置环绕和过滤方式
            GLES20.glTexParameterf(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR.toFloat()
            )
            GLES20.glTexParameterf(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR.toFloat()
            )
            GLES20.glTexParameterf(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE.toFloat()
            )
            GLES20.glTexParameterf(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE.toFloat()
            )

            //绑定FBO索引
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[i])
            //绑定纹理到FBO
            GLES20.glFramebufferTexture2D(
                GLES20.GL_FRAMEBUFFER,
                GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D,
                frameTexture[i],
                0
            )

            //解绑纹理
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            //解绑FBO
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        }

    }

    /**
     * 开启FBO渲染
     */
    fun renderFrameBufferBegin(frameBuffer: Int) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer)
        GLES20.glClearColor(0f, 0f, 0f, 0f)  //清屏,保证fbo无残留
    }

    /**
     * 渲染数据到FBO,此处留白说明流程,不要调用该函数！不要调用该函数！！不要调用该函数！！！
     * 该函数的实现和GL的使用与绘制一致
     * 开启FBO渲染-->使用和绘制gl-->关闭FBO渲染
     * 经过这个步骤后,FBO的数据存在于frameTexture,可用于二次创作
     */
    private fun renderFrameBuffer() {
        //  useGL   使用gl
        //  setUniform1f  赋值
        //  drawGL  绘制
    }

    /**
     * 关闭FBO渲染
     */
    fun renderFrameBufferEnd() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_NONE)
    }


    /**
     * 释放相关,根据情况实现,不要调用该函数！不要调用该函数！！不要调用该函数！！！
     */
    private fun release(program: Int, position: Int, coordinate: Int, id: IntArray, idSize: Int) {
        //1.禁用顶点
        GLES20.glDisableVertexAttribArray(position)
        //2.禁用纹理
        GLES20.glDisableVertexAttribArray(coordinate)

        //3.解绑纹理
        //解绑0号纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        //解绑1号纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D + 1, 0)
        //3.删除生成的纹理索引  idSize:需要删除的数量
        GLES20.glDeleteTextures(idSize, id, 0)


        //4.解绑fbo idSize:需要删除的数量
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_NONE)
        //3.删除生成的fbo索引  idSize:需要删除的数量
        GLES20.glDeleteFramebuffers(idSize, id, 0)


        //5.删除gl
        GLES20.glDeleteProgram(program)
    }

}