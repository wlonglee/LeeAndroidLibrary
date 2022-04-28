package com.lee.metronome

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import kotlin.math.cos

/**
 * 节拍器动画组合控件
 * @author ljq
 * @date 2021/4/19
 */
open class MetronomeIcon : RelativeLayout {
    /**
     * 摆针控件
     */
    private var needleView: NeedleView? = null

    /**
     * 节拍器图标固定的底图,可改变色值
     */
    private var ivBg: ImageView? = null

    /**
     * 包围指针的边缘色,该色值与背景图非三角区域同色,达到切割的效果
     */
    private var needleBgColor: Int = Color.BLACK

    /**
     * 指针颜色,该色值与背景图的三角同色
     */
    private var needleColor: Int = Color.WHITE

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        val obtainStyledAttributes =
            context.obtainStyledAttributes(attrs, R.styleable.MetronomeIcon)
        needleBgColor =
            obtainStyledAttributes.getInt(R.styleable.MetronomeIcon_needleBgColor, Color.BLACK)
        needleColor =
            obtainStyledAttributes.getInt(R.styleable.MetronomeIcon_needleColor, Color.WHITE)
        obtainStyledAttributes.recycle()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        post {
            //设定底图
            ivBg = ImageView(context)
            ivBg?.setImageResource(R.drawable.ic_metronome)
            //设定底图色值
            ivBg?.drawable!!.setTint(needleColor)

            //摆针
            needleView = NeedleView(context, needleBgColor, needleColor, width, height)

            val layoutParams = LayoutParams(width, height)
            layoutParams.addRule(CENTER_IN_PARENT)
            ivBg?.layoutParams = layoutParams
            needleView?.layoutParams = layoutParams
            addView(ivBg)
            addView(needleView)
        }
    }

    /**
     * needleBgColor 指针背景颜色
     * needleColor 指针颜色
     */
    fun setColor(needleBgColor: Int, needleColor: Int) {
        var delayTime = 0L
        if (needleView == null) delayTime = 50
        postDelayed({
            needleView?.needleBgColor = needleBgColor
            needleView?.needleColor = needleColor
            ivBg?.drawable?.setTint(needleColor)
            needleView?.invalidate()
        }, delayTime)
    }

    /**
     * 回到最左
     */
    fun toLeft() = needleView?.toLeft()

    /**
     * 回到最右
     */
    fun toRight() = needleView?.toRight()

    fun updateProgress(p: Float) = needleView?.updateProgress(p)

    /**
     * 摆针
     */
    @SuppressLint("ViewConstructor")
    class NeedleView(
        context: Context,
        bgColor: Int,
        needle_Color: Int,
        parentWidth: Int,
        parentHeight: Int
    ) : View(context) {
        /**
         * 指针起始坐标
         */
        private var needleStartX = 0f
        private var needleStartY = 0f

        /**
         * 指针长度
         */
        private var needleLong = 0f

        /**
         * 指针的末端点坐标
         */
        private var needleEndCoordinate: PointF

        /**
         * 指针在最左边时的末端点坐标
         */
        private var needleEndLeftCoordinate: PointF

        /**
         * 指针在最右边时的末端点坐标
         */
        private var needleEndRightCoordinate: PointF

        private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        /**
         * 指针背景颜色
         */
        var needleBgColor: Int = bgColor

        /**
         * 指针颜色
         */
        var needleColor: Int = needle_Color
        var mWidth: Int = parentWidth
        var mHeight: Int = parentHeight

        init {
            mPaint.strokeCap = Paint.Cap.ROUND
        }

        /**
         * 当前夹角度数
         */
        private var currentDegree = 45f

        init {
            // 根据宽高的特定比例决定
            needleStartX = mWidth / 2f
            needleStartY = mHeight * 3 / 4f

            needleLong = mHeight * 11 / 16f
            // cos 45° 的值
            val cos45 = cos(Math.PI / 4)
            val needleEndY = needleStartY - needleLong * cos45

            // 初始化指针终点坐标
            val needleEndLeftX = mWidth * 0.0625f
            needleEndCoordinate = PointF(needleEndLeftX, needleEndY.toFloat())
            needleEndLeftCoordinate = PointF(needleEndLeftX, needleEndY.toFloat())
            needleEndRightCoordinate = PointF(mWidth * (1 - 0.0625f), needleEndY.toFloat())
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            /**
             * 指针背景
             * 0.234 是固定比例
             */
            mPaint.strokeWidth = width * 0.234f
            mPaint.color = needleBgColor
            mPaint.strokeCap = Paint.Cap.ROUND
            canvas.drawLine(
                needleStartX,
                needleStartY,
                needleEndCoordinate.x,
                needleEndCoordinate.y,
                mPaint
            )

            // 指针前景
            mPaint.strokeWidth = width * 0.078f
            mPaint.color = needleColor
            canvas.drawLine(
                needleStartX,
                needleStartY,
                needleEndCoordinate.x,
                needleEndCoordinate.y,
                mPaint
            )

            /**
             * 遮掩的底线，坐标不用变,写死即可
             */
            mPaint.strokeCap = Paint.Cap.SQUARE
            canvas.drawLine(
                width * 0.1875f,
                width * 0.711f,
                width * 0.8125f,
                width * 0.711f,
                mPaint
            )
            mPaint.color = needleBgColor
            mPaint.strokeWidth = width * 0.11f
            canvas.drawLine(
                width * 0.25f, height * 0.805f,
                width * 0.75f, height * 0.805f, mPaint
            )

            mPaint.color = needleColor
            mPaint.strokeWidth = width * 0.078f
            canvas.drawLine(
                width * 0.25f, height * 0.9f,
                width * 0.75f, height * 0.9f, mPaint
            )
            updateCoordinateByDegree()
        }


        private fun updateCoordinateByDegree() {
            val cosXValue = cos(Math.PI / 180 * (90 - currentDegree))
            needleEndCoordinate.x = (width / 2 - needleLong * cosXValue).toFloat()

            val cosYValue = cos(Math.PI / 180 * currentDegree)
            val needleEndY = needleStartY - needleLong * cosYValue
            needleEndCoordinate.y = needleEndY.toFloat()
        }

        /**
         * 指针到最左边
         */
        fun toLeft() {
            currentDegree = 45f
            postInvalidate()
        }

        /**
         * 指针到最右边
         */
        fun toRight() {
            currentDegree = -45f
            postInvalidate()
        }

        /**
         * 指针移动进度 0~1
         * 从左到右 进度从0~1
         * 从右到左 进度从1~0
         */
        fun updateProgress(p: Float) {
            val degree = 45f - 90 * p
            if (degree == currentDegree) {
                return
            }
            currentDegree = 45f - 90 * p
            postInvalidate()
        }
    }
}