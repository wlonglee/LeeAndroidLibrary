package com.lee.metronome

import android.content.Context
import android.media.AudioFormat
import com.lee.metronome.model.MetronomeData
import com.lee.metronome.model.MetronomeSoundData
import com.lee.metronome.type.BeatType
import com.lee.metronome.type.NoteType
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.Executors

/**
 *@author lee
 *@date 2022/4/22
 */
class Metronome {
    /**
     * 音频采样率
     */
    private var sampleRate = 44100

    /**
     * pcm位数类型
     */
    private var pcmBit = AudioFormat.ENCODING_PCM_16BIT

    /**
     * pcm位数
     */
    private var pcmNum = 16

    /**
     * 输出通道-单声道
     */
    private val channel = AudioFormat.CHANNEL_OUT_MONO


    /**
     * 设定的节拍信息
     */
    var metronomeData: MetronomeData

    /**
     * 设定的声音信息
     */
    val soundData = MetronomeSoundData()

    var listener: MetronomeListener? = null

    /**
     * 当节拍音频数据不参与混合运算时,由其播放音频
     */
    private var audioTrack: AudioTR? = null

    private var audioTrackVolume = 1f

    /**
     * 线程池
     */
    private var pool = Executors.newScheduledThreadPool(1)

    /**
     * 节拍任务
     */
    private var metronomeTask: MetronomeTask? = null

    /**
     * 是否触发了更新节拍、拍号等操作
     */
    @Volatile
    private var updateBpm = false

    /**
     * 读取raw目录下的滴答声
     * @param drip 强拍
     * @param drop 次强拍
     * @param tick 弱拍
     * @param bpm         每分钟的节拍
     * @param molecule    每小节有几拍
     * @param denominator 以几分音符为一拍
     * @param noteType    音符类型
     * @param recordSection 录制小节数量,根据录制单独生成对应的节拍数据
     */
    constructor(
        context: Context,
        drip: Int,
        drop: Int,
        tick: Int,
        bpm: Int,
        molecule: Int,
        denominator: Int,
        noteType: NoteType = NoteType.QUARTER,
        recordSection: Int = -1,
        listener: MetronomeListener?
    ) {
        this.listener = listener
        metronomeData = MetronomeData(bpm, molecule, denominator, noteType, recordSection)
        metronomeData.calculateMetronome(sampleRate, pcmNum)
        var file = File(context.cacheDir.absolutePath + "/drip.wav")
        if (file.exists()) {
            file.delete()
        }
        file = File(context.cacheDir.absolutePath + "/drop.wav")
        if (file.exists()) {
            file.delete()
        }
        file = File(context.cacheDir.absolutePath + "/tick.wav")
        if (file.exists()) {
            file.delete()
        }
        copyRaw(context, drip, "drip.wav")
        copyRaw(context, drop, "drop.wav")
        copyRaw(context, tick, "tick.wav")
        updateAudioPath(
            context.cacheDir.absolutePath + "/drip.wav",
            context.cacheDir.absolutePath + "/drop.wav",
            context.cacheDir.absolutePath + "/tick.wav"
        )
    }

    /**
     * 读取指定音频文件作为滴答声
     *
     * @param dripPath 强拍
     * @param dropPath 次强拍
     * @param tickPath 弱拍
     * @param bpm         每分钟的节拍
     * @param molecule    每小节有几拍
     * @param denominator 以几分音符为一拍
     * @param noteType    音符类型
     * @param recordSection 录制小节数量,根据录制单独生成对应的节拍数据
     */
    constructor(
        dripPath: String,
        dropPath: String,
        tickPath: String,
        bpm: Int,
        molecule: Int,
        denominator: Int,
        noteType: NoteType = NoteType.QUARTER,
        recordSection: Int = -1,
        listener: MetronomeListener?
    ) {
        this.listener = listener
        metronomeData = MetronomeData(bpm, molecule, denominator, noteType, recordSection)
        metronomeData.calculateMetronome(sampleRate, pcmNum)
        updateAudioPath(dripPath, dropPath, tickPath)
    }


    /**
     * 更新节拍器参数
     *
     * @param bpm         每分钟的节拍
     * @param molecule    每小节有几拍
     * @param denominator 以几分音符为一拍
     * @param noteType    音符类型
     * @param recordSection 录制小节数量,根据录制单独生成对应的节拍数据
     */
    fun updateMetronome(
        bpm: Int,
        molecule: Int,
        denominator: Int,
        noteType: NoteType = NoteType.QUARTER,
        recordSection: Int = -1
    ) {
        if (updateBpm)
            return

        updateBpm = true
        metronomeData = MetronomeData(bpm, molecule, denominator, noteType, recordSection)
        metronomeData.calculateMetronome(sampleRate, pcmNum)
        if (isReady()) {
            soundData.generateBeatData(metronomeData.beatSize)
            soundData.generateNoneData()
        }

        //没有启动任务的情况下,还原该配置
        if (metronomeTask == null)
            updateBpm = false
        metronomeTask?.stopTask()
    }


    private fun copyRaw(context: Context, raw: Int, name: String) {
        val inputStream = context.resources.openRawResource(raw)
        var fos: FileOutputStream? = null
        try {
            val buffer = ByteArray(4096)
            var length = inputStream.read(buffer)
            fos = FileOutputStream(File(context.cacheDir.absolutePath + "/" + name))
            while (length != -1) {
                fos.write(buffer, 0, length)
                length = inputStream.read(buffer)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                inputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            try {
                fos?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


    private fun updateAudioPath(dripPath: String, dropPath: String, tickPath: String) {
        soundData.resetAllSrc()
        val dripData = ArrayList<ByteArray>()
        val dropData = ArrayList<ByteArray>()
        val tickData = ArrayList<ByteArray>()
        decoderAudio(dripPath, object : AudioDecoder.SampleListener() {
            override fun onPrepare(sample: Int, pcm: Int, channels: Int) {
                sampleRate = sample
                pcmBit = pcm
                when (pcmBit) {
                    AudioFormat.ENCODING_PCM_8BIT -> pcmNum = 8
                    AudioFormat.ENCODING_PCM_16BIT -> pcmNum = 16
                }
            }

            override fun onAudioData(data: ByteArray) {
                soundData.addTotalSize(BeatType.DRIP, data.size)
                dripData.add(data)
            }

            override fun onEnd() {
                soundData.settingSrc(BeatType.DRIP, dripData)
                calculateAudioReady()
            }
        })
        decoderAudio(dropPath, object : AudioDecoder.SampleListener() {
            override fun onAudioData(data: ByteArray) {
                soundData.addTotalSize(BeatType.DROP, data.size)
                dropData.add(data)
            }

            override fun onEnd() {
                soundData.settingSrc(BeatType.DROP, dropData)
                calculateAudioReady()
            }
        })
        decoderAudio(tickPath, object : AudioDecoder.SampleListener() {
            override fun onAudioData(data: ByteArray) {
                soundData.addTotalSize(BeatType.TICK, data.size)
                tickData.add(data)
            }

            override fun onEnd() {
                soundData.settingSrc(BeatType.TICK, tickData)
                calculateAudioReady()
            }
        })
    }

    private fun calculateAudioReady() {
        if (isReady()) {
            soundData.generateBeatData(metronomeData.beatSize)
            soundData.generateNoneData()
            //音频文件准备就绪
            listener?.onReady()
        }
    }


    private fun isReady(): Boolean {
        return soundData.isSrcReady
    }

    /**
     * 解码音频文件
     */
    private fun decoderAudio(path: String, listener: AudioDecoder.AudioListener) {
        val file = File(path)
        if (!file.exists()) {
            throw RuntimeException("file not found:$path")
        }
        val audioDecoder = AudioDecoder()
        audioDecoder.listener = listener
        audioDecoder.setAudioPath(path)
    }

    /**
     * 启动一个倒计数的节拍器
     * @param countdown 倒计时多少个拍子
     */
    fun startMetronomeCountdown(
        countdown: Int,
        metronomeIcon: MetronomeIcon? = null,
        listener: MetronomeCountdownListener? = null
    ) {
        audioTrack = AudioTR.AudioTrackBuilder()
            .setChannel(channel)
            .setSampleRate(sampleRate)
            .setPcmEncodeBit(pcmBit)
            .setAuto(true)
            .build()
        metronomeTask = MetronomeTask(metronomeData, soundData)
        metronomeTask?.settingCountDown(countdown, object : MetronomeTask.CountdownListener {
            override fun onCountdown(count: Int) {
                listener?.onCountdown(count)
            }

            override fun onMetronomeData(
                data: ByteArray,
                size: Int,
                index: Int,
                moleculeIndex: Int,
                progress: Float
            ) {
                if (index % 2 == 0)
                    metronomeIcon?.updateProgress(progress / 100f)
                else
                    metronomeIcon?.updateProgress(1 - progress / 100f)
                audioTrack?.writeData(data, size)
            }

            override fun onCountdownEnd() {
                listener?.onCountdownEnd()
                metronomeTask = null
                audioTrack?.stopTrack()
            }
        })
        pool.execute(metronomeTask)
    }


    /**
     * 启动一个持续播放的节拍器
     */
    fun startMetronomeLoop(
        metronomeIcon: MetronomeIcon? = null,
        listener: MetronomeLoopListener? = null
    ) {
        audioTrack = AudioTR.AudioTrackBuilder()
            .setChannel(channel)
            .setSampleRate(sampleRate)
            .setPcmEncodeBit(pcmBit)
            .setAuto(true)
            .build()
        metronomeTask = MetronomeTask(metronomeData, soundData)
        metronomeTask?.settingLoop(object : MetronomeTask.LoopListener {
            override fun onMetronomeData(
                data: ByteArray,
                size: Int,
                index: Int,
                moleculeIndex: Int,
                progress: Float
            ) {
                if (index % 2 == 0)
                    metronomeIcon?.updateProgress(progress / 100f)
                else
                    metronomeIcon?.updateProgress(1 - progress / 100f)
                audioTrack?.writeData(data, size)
                listener?.onBeat(moleculeIndex)
            }

            override fun onLoopEnd() {
                audioTrack?.stopTrack()

                //触发了更新Bpm的操作
                if (updateBpm) {
                    startMetronomeLoop(metronomeIcon, listener)
                } else {
                    metronomeTask = null
                    listener?.onLoopEnd()
                }
            }
        })
        pool.execute(metronomeTask)
        updateBpm = false
    }

    /**
     * 停止倒计时
     */
    fun stopMetronome() {
        metronomeTask?.stopTask()
    }

    /**
     * 更新节拍器的播放声音大小
     */
    fun updateVolume(volume: Float) {
        audioTrackVolume = volume
        audioTrack?.setVolume(audioTrackVolume)
    }
}