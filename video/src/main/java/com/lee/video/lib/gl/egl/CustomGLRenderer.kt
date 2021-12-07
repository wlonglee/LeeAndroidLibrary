package com.lee.video.lib.gl.egl

import android.opengl.GLES20
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import com.lee.video.lib.gl.egl.EGLCore.Companion.EGL_RECORDABLE_ANDROID
import com.lee.video.lib.gl.render.drawer.IDrawer
import java.lang.ref.WeakReference

/**
 * 包含单独的渲染线程
 *@author lee
 *@date 2021/11/24
 */
class CustomGLRenderer : SurfaceHolder.Callback {

    //OpenGL渲染线程
    private val renderThread = RenderThread()

    //页面上的SurfaceView弱引用
    private var surfaceView: WeakReference<SurfaceView>? = null

    private var surface: Surface? = null

    //所有的绘制器
    private val drawers = mutableListOf<IDrawer>()

    init {
        //启动渲染线程
        renderThread.start()
    }

    /**
     * 设置SurfaceView
     */
    fun setSurface(surface: SurfaceView) {
        surfaceView = WeakReference(surface)
        surface.holder.addCallback(this)

        surface.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewDetachedFromWindow(v: View?) {
                stopSurface()
            }

            override fun onViewAttachedToWindow(v: View?) {
            }
        })
        setRenderMode(RenderMode.RENDER_CONTINUOUSLY)
    }

    /**
     * 设置surface
     */
    fun setSurface(surface: Surface, width: Int, height: Int){
        this.surface = surface
        renderThread.onSurfaceCreate()
        renderThread.onSurfaceChange(width, height)
        setRenderMode(RenderMode.RENDER_WHEN_DIRTY)
    }

    /**
     * 停止
     */
    fun stopSurface(){
        renderThread.onSurfaceStop()
        surface=null
    }

    fun setRenderMode(mode: RenderMode) {
        renderThread.setRenderMode(mode)
    }

    fun notifyTimeUs(timeUs: Long){
        renderThread.notifyTimeUs(timeUs)
    }

    fun notifySwap(timeUs: Long) {
        renderThread.notifySwap(timeUs)
    }

    /**
     * 添加绘制器
     */
    fun addDrawer(drawer: IDrawer?) {
        if (drawer != null)
            drawers.add(drawer)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        surface=holder.surface
        renderThread.onSurfaceCreate()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        renderThread.onSurfaceChange(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        renderThread.onSurfaceDestroy()
    }

    /**
     * 渲染状态
     *@author lee
     *@date 2021/11/24
     */
    enum class RenderState {
        NO_SURFACE, //没有有效的surface
        FRESH_SURFACE, //持有一个未初始化的新的surface
        SURFACE_CHANGE, // surface尺寸变化
        RENDERING, //初始化完毕，可以开始渲染
        SURFACE_DESTROY, //surface销毁
        STOP //停止绘制
    }


    enum class RenderMode {
        RENDER_CONTINUOUSLY,
        RENDER_WHEN_DIRTY
    }

    inner class RenderThread : Thread() {

        // 渲染状态
        private var state = RenderState.NO_SURFACE

        private var eglSurfaceHolder: EGLSurfaceHolder? = null

        // 是否绑定了EGLSurface
        private var bindEGLContext = false

        //是否已经新建过EGL上下文
        private var createEglContext = true

        private var worldWidth = 0
        private var worldHeight = 0

        private var mCurTimestamp = 0L

        private var mLastTimestamp = 0L

        private var mRenderMode = RenderMode.RENDER_WHEN_DIRTY

        private val lock = Object()

        //------------第1部分：线程等待与解锁-----------------
        private fun holdOn() {
            synchronized(lock) {
                lock.wait()
            }
        }

        private fun notifyGo() {
            synchronized(lock) {
                lock.notify()
            }
        }

        //------------第2部分：Surface声明周期转发函数------------
        fun onSurfaceCreate() {
            state = RenderState.FRESH_SURFACE
            notifyGo()
        }

        fun onSurfaceChange(width: Int, height: Int) {
            worldWidth = width
            worldHeight = height
            state = RenderState.SURFACE_CHANGE
            notifyGo()
        }

        fun onSurfaceDestroy() {
            state = RenderState.SURFACE_DESTROY
            notifyGo()
        }

        fun onSurfaceStop() {
            state = RenderState.STOP
            notifyGo()
        }
        fun setRenderMode(mode: RenderMode) {
            mRenderMode = mode
        }
        fun notifyTimeUs(timeUs: Long) {
            synchronized(mCurTimestamp) {
                mCurTimestamp = timeUs
            }
        }

        fun notifySwap(timeUs: Long) {
            synchronized(mCurTimestamp) {
                mCurTimestamp = timeUs
            }
            notifyGo()
        }


        //------------第3部分：OpenGL渲染循环------------
        override fun run() {
            //1初始化EGL
            initEGL()
            while (true) {
                when (state) {
                    RenderState.FRESH_SURFACE -> {
                        //2使用surface初始化EGLSurface，并绑定上下文
                        createEGLSurfaceFirst()
                        holdOn()
                    }
                    RenderState.SURFACE_CHANGE -> {
                        createEGLSurfaceFirst()
                        //3初始化OpenGL世界坐标系宽高
                        GLES20.glViewport(0, 0, worldWidth, worldHeight)
                        configWordSize()
                        state = RenderState.RENDERING
                    }
                    RenderState.RENDERING -> {
                        //4进入循环渲染
                        render()
                        if (mRenderMode == RenderMode.RENDER_WHEN_DIRTY) {
                            holdOn()
                        }
                    }
                    RenderState.SURFACE_DESTROY -> {
                        //5销毁EGLSurface，并解绑上下文
                        destroyEGLSurface()
                        state = RenderState.NO_SURFACE
                    }
                    RenderState.STOP -> {
                        //6释放所有资源
                        releaseEGL()
                        return
                    }
                    else -> {
                        holdOn()
                    }
                }
                if (mRenderMode == RenderMode.RENDER_CONTINUOUSLY) {
                    sleep(16)
                }
            }
        }

        //------------第4部分：EGL相关操作------------
        private fun initEGL() {
            eglSurfaceHolder = EGLSurfaceHolder()
            eglSurfaceHolder?.init(null, EGL_RECORDABLE_ANDROID)
        }

        private fun createEGLSurfaceFirst() {
            if (!bindEGLContext) {
                bindEGLContext = true
                createEGLSurface()
                if (createEglContext) {
                    createEglContext = false
                    onSurfaceCreated()
                }
            }
        }

        private fun createEGLSurface() {
            eglSurfaceHolder?.createEGLSurface(surface)
            eglSurfaceHolder?.makeCurrent()
        }

        private fun destroyEGLSurface() {
            eglSurfaceHolder?.destroyEGLSurface()
            bindEGLContext = false
        }

        private fun releaseEGL() {
            eglSurfaceHolder?.release()
        }

        //------------第5部分：OpenGL ES相关操作-------------
        private fun onSurfaceCreated() {
            GLES20.glClearColor(0f, 0f, 0f, 0f)
            drawers.forEach {
                it.onConfig()
            }
        }

        private fun configWordSize() {
            drawers.forEach { it.onWordSize(worldWidth, worldHeight) }
        }

        private fun render() {
            val render = if (mRenderMode == RenderMode.RENDER_CONTINUOUSLY) {
                true
            } else {
                synchronized(mCurTimestamp) {
                    if (mCurTimestamp > mLastTimestamp) {
                        mLastTimestamp = mCurTimestamp
                        true
                    } else {
                        false
                    }
                }
            }
            if (render) {
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
                drawers.forEach { it.onDrawFrame() }
                eglSurfaceHolder?.setTimestamp(mCurTimestamp)
                eglSurfaceHolder?.swapBuffers()
            }


        }
    }


}