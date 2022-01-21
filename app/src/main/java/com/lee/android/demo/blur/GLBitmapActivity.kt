package com.lee.android.demo.blur

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.lee.android.R
import kotlinx.android.synthetic.main.activity_glbitmap.*

/**
 * 高斯模糊效果demo
 *@author lee
 *@date 2021/4/20
 */
class GLBitmapActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_glbitmap)
        //设置渲染的图片,该函数同时可以更新图片
//        glBitmapView.setRender(BitmapFactory.decodeResource(resources, R.drawable.blur_1))
//
//        glBitmapView.alpha=0f
//
//        changeBtn.setOnClickListener {
//            glBitmapView.setRender(BitmapFactory.decodeResource(resources, R.drawable.blur_2))
//        }
//
//        seekBar.progress = 0
//        seekBar.max = 100
//        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
//            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
//                //设置模糊进度
//                glBitmapView.alpha=(progress / 100f)
//            }
//
//            override fun onStartTrackingTouch(seekBar: SeekBar?) {
//            }
//
//            override fun onStopTrackingTouch(seekBar: SeekBar?) {
//            }
//        })
    }

    override fun onDestroy() {
//        glBitmapView.release()
        super.onDestroy()
    }
}