package com.lee.android

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.lee.android.demo.gif.GifDrawable
import com.lee.video.lib.gl.egl.CustomGLRenderer
import com.lee.video.lib.gl.render.drawer.GaussianDrawer
import kotlinx.android.synthetic.main.activity_test3.*


class TestActivity3 : AppCompatActivity() {
    var gifDrawable: GifDrawable? = null
    var drawer: GaussianDrawer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test3)
        gifDrawable = GifDrawable()
        gifDrawable?.setGifResource(assets.open("gif_2.gif"))
        gifDrawable?.listener = object : GifDrawable.OnBitmapChange {
            override fun onChange(bitmap: Bitmap?) {
                drawer?.updateBitmap(bitmap!!)
            }

        }
        gifView.background=gifDrawable
        gifDrawable?.startAnim()


        drawer = GaussianDrawer(
            this,
            BitmapFactory.decodeResource(resources, R.drawable.blur_1),
            272,
            360
        )
        val render = CustomGLRenderer()
        render.addDrawer(drawer)
        render.setSurface(gaussianView)

        seekBar.progress = 0
        seekBar.max = 100
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                //设置模糊进度
                drawer?.size = (progress / 100f)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })


    }

    override fun onDestroy() {
        super.onDestroy()
        gifDrawable?.stopAnim()
        gifDrawable?.clear()
    }

    private var isPause=false

    fun pauseGif(view: View) {
        if(isPause){
            gifDrawable?.startAnim()
        }else{
            gifDrawable?.stopAnim()
        }
        isPause=!isPause
    }

}