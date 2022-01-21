package com.lee.video.lib.gl.render.drawer

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.Matrix
import android.util.Log
import com.lee.video.R
import com.lee.video.lib.gl.ShaderUtil
import kotlin.math.max
import kotlin.math.min

/**
 * 视频渲染
 * @author lee
 * @date 2021/11/23
 */
class VideoDrawer2(context: Context, var callBack: ((st: SurfaceTexture) -> Unit)?) :
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

    /**
     * 位移的最小值  缩放后由于居中显示的原因,此时偏移量为负数
     */
    private var minDx = 0f
    private var minDy = 0f

    /**
     * 位移的最大值
     */
    private var maxDx = 0f
    private var maxDy = 0f

    /**
     * 当前移动距离
     */
    private var currentDx = 0f
    private var currentDy = 0f

    private var lastDx = 0f
    private var lastDy = 0f

    private var useHeight=0

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

    override fun configMatrix() {
        useHeight = worldHeight
        val prjMatrix = FloatArray(16)
        if (width > height) {
            //定死比例 4:3   16:9

            //计算出视频的显示区域
            val rh = (worldWidth / 4f * 3).toInt()
            val rw = (rh * width.toFloat() / height).toInt()

            val hr = rh / worldHeight.toFloat()
            val hw = rw / worldWidth.toFloat()

            Log.e("lee", "rh:$rh  rw:$rw  $hr  $hw")

            Matrix.orthoM(
                prjMatrix, 0,
                -1f / hw, 1f / hw,
                -1f / hr, 1f / hr,
                3f, 5f
            )

            if(hw!=1f){
                //限定位移距离
                if (rw < width) {
                    //进行缩放后的宽比原始视频宽度小,需要计算缩放值
                    minDx = -(rw - worldWidth) / 2f / hw
                    maxDx = (rw - worldWidth) / 2f / hw
                } else {

                    //计算左右的移动距离
                    minDx = -(rw - worldWidth) / 2f
                    maxDx = (rw - worldWidth) / 2f
                }
            }

            //纵向不允许移动
            minDy = 0f
            maxDy = 0f

        } else {
            //定死比例 1:1.2
            val rw = worldWidth
            val rh = (rw * height.toFloat() / width).toInt()
            worldHeight = (worldWidth * 1.2f).toInt()
            offsetY = (useHeight - worldHeight) / 2
            val hr = rh / worldHeight.toFloat()
            Log.e("lee", "rh:$rh  rw:$rw  $hr $worldWidth  $worldHeight")
            Matrix.orthoM(
                prjMatrix, 0,
                -1f, 1f,
                -1f / hr, 1f / hr,
                3f, 5f
            )

            if(hr!=1f){
                //限定位移距离
                if (rh < height) {
                    minDy = -(rh - worldHeight) / 4f / hr
                    maxDy = (rh - worldHeight) / 4f / hr
                } else {
                    minDy = -(rh - worldHeight) / 4f
                    maxDy = (rh - worldHeight) / 4f
                }
            }



            //横向不允许移动
            minDx = 0f
            maxDx = 0f
        }
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

    override fun translate(dx: Float, dy: Float) {


        //记录移动距离
        currentDx += dx
        currentDy += dy

        currentDx = min(max(minDx, currentDx), maxDx)
        currentDy = min(max(minDy, currentDy), maxDy)

        Matrix.translateM(
            matrix,
            0,
            (currentDx-lastDx)/ worldWidth * widthRatio * 2,
            -(currentDy-lastDy) / useHeight * heightRatio * 2,
            0f
        )

        lastDx = currentDx
        lastDy = currentDy
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