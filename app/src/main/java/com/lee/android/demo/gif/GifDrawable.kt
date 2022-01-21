package com.lee.android.demo.gif

import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.util.Log
import java.io.InputStream

/**
 * gif Drawable绘制
 *@author lee
 *@date 2021/5/24
 */
class GifDrawable : Drawable() {
    /**
     * 默认画笔
     */
    private var paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * 绘制圆角的画笔
     */
    private val paintRound = Paint()

    /**
     * 避免缩放导致的图片锯齿
     */
    private val pfd = PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    /**
     * 圆角叠加模式
     */
    private val paintMode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

    /**
     * gif解码器
     */
    private var gifDecoder: StandardGifDecoder =
        StandardGifDecoder(StandardBitmapProvider.getInstance())

    init {
        paintRound.isAntiAlias = true
        paintRound.color = -0xbdbdbe
    }

    /**
     * gif资源是否就绪
     */
    private var ready: Boolean = false

    /**
     * 是否需要前进到下一帧
     */
    private var needAdvance: Boolean = false

    /**
     * 是否需要回到首帧
     */
    private var needGoFirst: Boolean = false

    /**
     * 动画是否开始
     */
    private var animStart: Boolean = false

    /**
     * 设置该值后会使用该值作为gif每帧的切换时间,单位毫秒值
     */
    var duration: Int = 0

    /**
     * 圆角
     */
    var roundPx: Float = 0f

    /**
     * 当前帧数据
     */
    lateinit var currentPixel: IntArray

    private var currentBitmap: Bitmap? = null

    /**
     * 当前帧索引
     */
    var currentFrame: Int = 0

    /**
     * 设置gif资源
     */
    fun setGifResource(inputStream: InputStream) {
        gifDecoder.read(inputStream, inputStream.available())
        if (gifDecoder.frameCount > 0) {
            ready = true
            needAdvance = true
        } else {
            Log.e("lee-gif", "gif size is:${gifDecoder.frameCount}")
        }
    }

    /**
     * 启动动画
     */
    fun startAnim() {
        unscheduleSelf(this::invalidateSelf)
        needAdvance = true
        animStart = true
        invalidateSelf()
    }

    /**
     * 停止动画,停在当前帧的位置
     */
    fun stopAnim() {
        unscheduleSelf(this::invalidateSelf)
        needAdvance = false
        animStart = false
        invalidateSelf()
    }

    /**
     * 停止动画,回到首帧
     */
    fun stopAnim2First() {
        unscheduleSelf(this::invalidateSelf)
        currentFrame = 0
        needAdvance = false
        animStart = false
        needGoFirst = true
        invalidateSelf()
    }


    /**
     * 跳转到指定帧进行渲染,不启动动画
     */
    fun seekTo(pos: Int, pixels: IntArray) {
        if (pos == 0) {
            unscheduleSelf(this::invalidateSelf)
            gifDecoder.resetFrameIndex()
            needAdvance = true
        } else {
            gifDecoder.seek(pos, pixels)
            currentFrame = pos
            currentPixel = pixels
            currentBitmap?.setPixels(
                pixels,
                0,
                gifDecoder.width,
                0,
                0,
                gifDecoder.width,
                gifDecoder.height
            )
        }
        invalidateSelf()
    }

    /**
     * 跳转到指定帧并启动动画
     */
    fun seekAndStart(pos: Int, pixels: IntArray) {
        gifDecoder.seek(pos, pixels)
        startAnim()
    }


    /**
     * 绘制
     */
    override fun draw(canvas: Canvas) {
        if (!ready) {
            //资源未就绪
            return
        }
        if (needAdvance) {
            //渲染数据
            drawNextFrame(canvas)
        } else {
            //判断是否需要回到首帧
            if (needGoFirst) {
                needGoFirst = false
                //重置解码器当前帧
                gifDecoder.resetFrameIndex()
                drawNextFrame(canvas)
            } else {
                //直接渲染当前资源
                currentBitmap?.let {
                    canvas.drawBitmap(getRoundBitmap(it), 0f, 0f, paint)
                }
            }
        }
    }

    private fun drawNextFrame(canvas: Canvas) {
        //前进到下一帧
        gifDecoder.advance()
        //记录索引
        currentFrame = gifDecoder.currentFrameIndex
        //记录图片
        currentBitmap = gifDecoder.nextFrame

        currentBitmap?.let {
            //记录帧数据
            currentPixel = gifDecoder.mainScratch
            canvas.drawBitmap(getRoundBitmap(it), 0f, 0f, paint)
            if (animStart) {
                scheduleSelf(
                    this::invalidateSelf,
                    if (duration > 0) {
                        SystemClock.uptimeMillis() + duration
                    } else {
                        SystemClock.uptimeMillis() + gifDecoder.nextDelay.toLong()
                    }
                )
            } else {
                needAdvance = false
            }
        }
    }

    private fun getRoundBitmap(bitmap: Bitmap): Bitmap {
        val bound = RectF(0f, 0f, bounds.right.toFloat(), bounds.bottom.toFloat())
        val scaleX = bounds.right.toFloat() / bitmap.width
        val scaleY = bounds.bottom.toFloat() / bitmap.height
        val output = Bitmap.createBitmap(
            bound.right.toInt(),
            bound.bottom.toInt(),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(output)
        canvas.drawARGB(Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT)
        paintRound.xfermode = null
        canvas.drawRoundRect(bound, roundPx, roundPx, paintRound)
        paintRound.xfermode = paintMode
        canvas.drawFilter = pfd
        canvas.scale(scaleX, scaleY)
        canvas.drawBitmap(bitmap, 0f, 0f, paintRound)
        return output
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

}