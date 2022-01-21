package com.lee.android

import android.Manifest
import android.annotation.TargetApi
import android.app.WallpaperManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_test.*


@TargetApi(Build.VERSION_CODES.M)
class TestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        requestPermissions(
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            101
        )


        goBtn.setOnClickListener {
            imgLayout.setBackgroundDrawable(WallpaperManager.getInstance(this).drawable)
        }

        backBtn.setOnClickListener {

            //当调用者未处理的时候，toast提示
            val toast = Toast.makeText(this,"Hello",Toast.LENGTH_SHORT)
            toast.setGravity(Gravity.TOP,0,0)
//            toast.duration=Toast.LENGTH_SHORT
//            val layout: View = LayoutInflater.from(this).inflate(R.layout.toast_layout,null)
//            toast.view=layout
            toast.view!!.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            toast.show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode==101){
            if(grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED){

            }else{
                requestPermissions(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    101
                )
            }
        }

    }
}