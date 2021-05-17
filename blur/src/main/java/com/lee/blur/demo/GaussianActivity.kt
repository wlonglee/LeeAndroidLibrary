package com.lee.blur.demo

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.lee.blur.R
import kotlinx.android.synthetic.main.activity_gaussian.*

/**
 * 高斯模糊效果demo
 * 该activity未注册,请不要跳转至此！！！
 * 代码可拷贝后自行查看效果
 *@author lee
 *@date 2021/4/20
 */
class GaussianDemo : AppCompatActivity() {

    private var change = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gaussian)

        //设置渲染的图片,该函数同时可以更新图片
        gaussianView.setRender(BitmapFactory.decodeResource(resources, R.drawable.blur_1))

        //变更渲染图片
        changeBtn.setOnClickListener {
            change = !change
            gaussianView.setRender(
                BitmapFactory.decodeResource(
                    resources,
                    if (change) R.drawable.blur_2 else R.drawable.blur_1
                )
            )
        }

        seekBar.progress = 0
        seekBar.max = 100
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                //设置模糊进度
                gaussianView.updateSize(progress / 100f)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
    }

    override fun onDestroy() {
        gaussianView.release()
        super.onDestroy()
    }
}