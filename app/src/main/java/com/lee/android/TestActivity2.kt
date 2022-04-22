package com.lee.android

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Environment
import android.text.TextPaint
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.lee.video.lib.codec.decoder.AudioDecoder
import com.lee.video.lib.gl.render.drawer.WaterMaskDrawer
import com.lee.video.lib.player.HardPlayer
import com.lee.video.lib.repack.Mp4Clip
import kotlinx.android.synthetic.main.activity_test2.*
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.random.Random


class TestActivity2 : AppCompatActivity() {

    var startX = 0f
    var startY = 0f


    var player: HardPlayer? = null


    var audioPlayer:AudioDecoder?=null

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                val moveX = event.x - startX
                val moveY = event.y - startY
//                player?.translate(moveX, moveY)
                startX = event.x
                startY = event.y
            }
            MotionEvent.ACTION_UP -> {
                startX = 0f
                startY = 0f
            }
        }


        return true
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test2)

        Log.e("lee","onCreate")

        //设置渲染的图片,该函数同时可以更新图片

//        val supportVideoType = arrayListOf<String>()
//        val c = MediaCodecList.getCodecCount()
//        for (i in 0 until c) {
//            val codec = MediaCodecList.getCodecInfoAt(i)
//
//            if (!codec.isEncoder)
//                continue
//            val types: Array<String> = codec.supportedTypes
//            for (j in types.indices) {
//                if (types[j].startsWith("video/") && !supportVideoType.contains(types[j])) {
//                    supportVideoType.add(types[j])
//                }
//            }
//        }
//
//        val extractor = MediaExtractor()
//        extractor.setDataSource(Environment.getExternalStorageDirectory().absolutePath + "/Pictures/sht.mp4")
//        for (i in 0 until extractor.trackCount) {
//            val format = extractor.getTrackFormat(i)
//            val mime = format.getString(MediaFormat.KEY_MIME)
//            if (mime != null && mime.startsWith("video/")) {
//                if (supportVideoType.contains(mime)){
//                    Log.e("lee","格式支持")
//                }else{
//                    Log.e("lee","格式不支持")
//                }
//            }
//        }
//        extractor.release()

//        testClip()
//        testPlay()


//        val render = CustomGLRenderer()
//        render.addDrawer(
//            BitmapDrawer(
//                this,
//                BitmapFactory.decodeResource(resources, R.drawable.blur_2)
//            )
//        )
//        render.addDrawer(
//            BitmapDrawer(
//                this,
//                BitmapFactory.decodeResource(resources, R.drawable.blur_1),true
//            )
//        )
//        render.setSurface(surfaceView)


//        val path = Environment.getExternalStorageDirectory().absolutePath + "/Pictures/4.mp4"
//        val path = Environment.getExternalStorageDirectory().absolutePath + "/aserbaoCamera/1620877098625.mp4"
//        Log.e("lee","path:$path")
//

//        player = HardPlayer.Builder()
//            .setContext(this)
//            .setDecoderAudio(false)
//            .setDealAudio(true)
//            .setDealVideo(true)
//            .setDecoderVideo(true)
//            .setLoop(true)
//            .setDecoderAudio(false)
//            .setSurface(mp4Repack!!.getSurface(),vW,vH)
//            .setSurface(surfaceView)
//            .setListener(object : HardPlayer.PlayListener {
//                override fun onReady() {
//                    Log.e("lee", "启动播放")
//                    player?.startPlay()
//                }
//
//                override fun onProgress(p: Float) {
//                }
//
//                override fun onEnd() {
//                    Log.e("lee", "播放完成")
//                }
//
//                override fun onError(msg: String) {
//
//                }
//
//                override fun onDealAudio(buffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
//                }
//
//                override fun onDealVideo(buffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
//                }
//
//            })
//            .build(path)
//        player?.prepare()


    }

    fun testPlay(v:View) {
        val path = Environment.getExternalStorageDirectory().absolutePath + "/Pictures/4.mp3"
        audioPlayer=AudioDecoder.Builder().setAutoPlay(true).build(path)
        audioPlayer?.prepare()
//        player = HardPlayer.Builder()
//            .setAutoPlay(true)
//            .setContext(this)
//            .setLoop(true)
//            .setSurface(surfaceView)
//            .setListener(object : HardPlayer.PlayListener {
//                override fun onReady() {
//                }
//
//                override fun onAudioData(audioData: ByteArray) {
//
//                }
//
//                override fun onProgress(p: Float) {
//                }
//
//                override fun onEnd() {
//                }
//
//                override fun onError(msg: String) {
//                }
//
//            })
//            .build(path)
//
//
////        val bitmap=BitmapFactory.decodeResource(resources, R.drawable.b)
//        val bitmap = generateBitmap("我是水印", 30, Color.RED)
//        //添加一个水印
//        val mask = WaterMaskDrawer(this, bitmap)
//        //设定默认偏移量
//        mask.setOffset(480, 320)
//        //设定显示大小
//        mask.onWordSize(bitmap.width, bitmap.height)
//        player?.prepare(mask)
//        drawer2.scale(0.5f,0.5f)

//        drawer2 = VideoDrawer(this) {
//            video2 = VideoDecoder.Builder()
//                .setSurface(it)
//                .setAutoPlay(true)
//                .setLoop(false)
//                .setListener(object : VideoDecoder.SimpleListener() {
//                    override fun onVideoFormat(
//                        videoWidth: Int,
//                        videoHeight: Int,
//                        videoFrame: Int,
//                        duration: Long
//                    ) {
//                        drawer2?.setSize(videoWidth, videoHeight)
//                        drawer2?.setSurfaceAlpha(0.8f)
//                        drawer2?.scale(0.5f,0.8f)
//                    }
//                })
//                .build(path2)
//            video2.prepare()
//        }
    }

    fun testSeek(v:View){
        audioPlayer?.seek(Random(System.currentTimeMillis()).nextFloat())
//        audioPlayer?.stop()
    }


    private fun testClip() {
        val mp4Clip = Mp4Clip.Builder()
            .setContext(this)
            .setInPath(Environment.getExternalStorageDirectory().absolutePath + "/Pictures/4.mp4")
            .setOutPath(Environment.getExternalStorageDirectory().absolutePath + "/Pictures/encode.mp4")
            .setResultWidth(720)
            .setResultHeight(1280)
            .setMarginLeft(0)
            .setMarginTop(0)
            .setIInterval(1f)
            .setClarity(0.15f)
            .setListener(object : Mp4Clip.ClipListener {
                override fun onStart() {

                }

                override fun onProgress(p: Float) {
                    repack.post {
                        repack.text = "$p"
                    }
                }

                override fun onEnd() {

                }

                override fun onError(msg: String) {
                }

            })
            .build()
        mp4Clip.startTask()
    }

    override fun onStop() {
        Log.e("lee", "onStop")
        super.onStop()
        player?.stopPlay()
    }

    private fun generateBitmap(text: String, textSizePx: Int, textColor: Int): Bitmap {
        val textPaint = TextPaint()
        textPaint.textSize = textSizePx.toFloat()
        textPaint.color = textColor
        textPaint.isAntiAlias = true
        val width = ceil(textPaint.measureText(text).toDouble()).toInt()
        val fontMetrics: Paint.FontMetrics = textPaint.fontMetrics
        val height = ceil(abs(fontMetrics.bottom) + abs(fontMetrics.top))
            .toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawText(text, 0f, abs(fontMetrics.ascent), textPaint)
        return bitmap
    }


}