package com.lee.android.demo.audio

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.lee.android.R
import com.lee.android.lib.FileSaveUtil
import com.lee.android.lib.audio.AudioMixer
import com.lee.android.lib.audio.AudioUtil

class AudioActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val drum = FileSaveUtil("drum", "")

        val metronome = FileSaveUtil("metronome", "")

        val mix = FileSaveUtil("saveExtremeFinal", "", true)

        val mixer = AudioMixer.createExtremeMixer()

        var size: Int
        val drumData = ByteArray(4096)
        val metronomeData = ByteArray(2048)

        do {
            size = drum.readData(drumData)

            if(size<=0)
                break
            metronome.readData(metronomeData, size / 2)

            val mixData = mixer.mixAudio(2, drumData, AudioUtil.channel2DoubleB16(metronomeData))
            mix.writeData(mixData, size)
        } while (size > 0)

        Log.e("lee", "混合完成")

        drum.close()
        metronome.close()
        mix.close()
    }
}