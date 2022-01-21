package com.lee.android.demo.blur

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.lee.android.R

/**
 * 高斯模糊效果demo
 *@author lee
 *@date 2021/4/20
 */
class GaussianActivity : AppCompatActivity() {

    private var change = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gaussian)
        //设置渲染的图片,该函数同时可以更新图片
//        gaussianView.setRender(BitmapFactory.decodeResource(resources, R.drawable.blur_1))
//        gaussianView.updateSize(1f)
//
//        gaussianView.alpha=0f
//
//        changeBtn.setOnClickListener {
//            gaussianView.setRender(BitmapFactory.decodeResource(resources, R.drawable.blur_2))
//        }
//
//        seekBar.progress = 0
//        seekBar.max = 100
//        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
//            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
//                //设置模糊进度
//                gaussianView.alpha=(progress / 100f)
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
//        gaussianView.release()
        super.onDestroy()
    }
}