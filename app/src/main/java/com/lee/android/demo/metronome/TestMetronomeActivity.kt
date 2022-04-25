package com.lee.android.demo.metronome

import android.graphics.Paint
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.lee.android.R
import com.lee.metronome.Metronome
import com.lee.metronome.MetronomeListener
import com.lee.metronome.MetronomeLoopListener
import com.lee.metronome.type.NoteType
import kotlinx.android.synthetic.main.activity_test_metronome.*

/**
 * 节拍器测试
 */
class TestMetronomeActivity : AppCompatActivity() {
    private var metronome: Metronome? = null

    /**
     * 当前选择的条目
     */
    private var currentType = CurrentType.TIME_SIGNATURE

    /**
     * 拍号
     */
    private var bpmInfoTime = BpmInfo(
        arrayListOf(
            "2/4",
            "3/4",
            "4/4",
            "5/4",
            "3/8",
            "6/8",
            "9/8",
            "12/8"
        ),
        pos = 2
    )

    /**
     * bpm
     */
    private var bpmInfoBPM = BpmInfo()

    /**
     * 音符类型
     */
    private var bpmInfoType = BpmInfo(
        arrayListOf(
            "四分音符",
            "八分音符",
            "三连音",
            "十六分音符",
            "四分附点",
            "三个八分"
        )
    )

    /**
     * 当前选择的条目
     */
    enum class CurrentType {
        //拍号
        TIME_SIGNATURE,

        //bpm
        BPM,

        //音符类型
        TYPE
    }

    private var infoLocation1: InfoLocation? = null
    private var infoLocation2: InfoLocation? = null
    private var infoLocation3: InfoLocation? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_metronome)
        initView()
        initMetronome()
    }

    private fun initMetronome() {
//                metronome?.startMetronomeCountdown(
//                    15,
//                    metronomeIcon,
//                    object : MetronomeCountdownListener {
//                        override fun onCountdown(count: Int) {
//
//                        }
//
//                        override fun onCountdownEnd() {
//
//                        }
//                    })


        metronome = Metronome(
            this,
            R.raw.drip,
            R.raw.drop,
            R.raw.drop,
            60,
            4,
            4,
            NoteType.QUARTER,
            listener = MetronomeListener {

                metronome?.startMetronomeLoop(
                    metronomeIcon,
                    listener = object : MetronomeLoopListener {
                        override fun onLoopStart() {
                            beatHot.setBeat(metronome?.getMetronomeDotType())
                        }

                        override fun onBeat(index: Int) {
                            beatHot.current=index
                        }

                        override fun onLoopEnd() {
                        }

                    })
            }
        )
    }

    private fun initView() {
        beatHot.setCount(4)

        val paint = Paint()
        paint.textSize = 50f
        paint.style = Paint.Style.FILL

        //拍号
        for (i in 0 until bpmInfoTime.info.size) {
            bpmInfoTime.itemW.add(paint.measureText(bpmInfoTime.info[i]))
        }

        //设定bpm数据范围
        for (i in 30..240) {
            bpmInfoBPM.info.add(i.toString())
            bpmInfoBPM.itemW.add(paint.measureText(i.toString()))
        }
        bpmInfoBPM.pos = 30

        //设定音符类型
        for (i in 0 until bpmInfoType.info.size) {
            bpmInfoType.itemW.add(paint.measureText(bpmInfoType.info[i]))
        }

        bpmTimeSignature.text = bpmInfoTime.info[bpmInfoTime.pos]
        bpm.text = bpmInfoBPM.info[bpmInfoBPM.pos]
        bpmSection.text = bpmInfoType.info[bpmInfoType.pos]

        infoLocation1 = InfoLocation(bpmTimeRecyclerView, bpmInfoTime, this)
        infoLocation2 = InfoLocation(bpmInfoRecyclerView, bpmInfoBPM, this)
        infoLocation3 = InfoLocation(bpmSectionRecyclerView, bpmInfoType, this)

        infoLocation1?.init()
        infoLocation2?.init()
        infoLocation3?.init()

        infoLocation1?.setVisibility(View.VISIBLE)

        bpmTimeSignature.setOnClickListener {
            if (currentType == CurrentType.TIME_SIGNATURE)
                return@setOnClickListener
            updateType(CurrentType.TIME_SIGNATURE)
        }

        bpm.setOnClickListener {
            if (currentType == CurrentType.BPM)
                return@setOnClickListener
            updateType(CurrentType.BPM)
        }

        bpmSection.setOnClickListener {
            if (currentType == CurrentType.TYPE)
                return@setOnClickListener
            updateType(CurrentType.TYPE)
        }


        infoLocation1?.listener = object : InfoLocation.OnChangePosListener {
            override fun onChangePos(pos: Int) {
                if (pos != bpmInfoTime.pos) {
                    bpmTimeSignature.text = bpmInfoTime.info[pos]
                    bpmInfoTime.pos = pos
                    //变更节拍器播放
                    updateMetronome()
                }
            }
        }

        infoLocation2?.listener = object : InfoLocation.OnChangePosListener {
            override fun onChangePos(pos: Int) {
                if (pos != bpmInfoBPM.pos) {
                    bpm.text = bpmInfoBPM.info[pos]
                    bpmInfoBPM.pos = pos
                    //变更节拍器播放
                    updateMetronome()
                }
            }
        }
        infoLocation3?.listener = object : InfoLocation.OnChangePosListener {
            override fun onChangePos(pos: Int) {
                if (pos != bpmInfoType.pos) {
                    bpmSection.text = bpmInfoType.info[pos]
                    bpmInfoType.pos = pos
                    //变更节拍器播放
                    updateMetronome()
                }
            }
        }
    }

    private fun updateMetronome() {
        val bpmNumber = bpmInfoBPM.info[bpmInfoBPM.pos].toInt()
        val str = bpmInfoTime.info[bpmInfoTime.pos]

        val molecule = str.split("/")[0].toInt()
        val denominator = str.split("/")[1].toInt()

        metronome?.updateMetronome(
            bpmNumber,
            molecule,
            denominator,
            NoteType.values()[bpmInfoType.pos]
        )
    }


    private fun updateType(updateType: CurrentType) {
        when (currentType) {
            CurrentType.TIME_SIGNATURE -> {
                bpmTimeSignature.setTextColor(resources.getColor(R.color.white, null))
                bpmTimeSignature.setBackgroundResource(R.drawable.bg_212124_40)

                infoLocation1?.setVisibility(View.INVISIBLE)
            }
            CurrentType.BPM -> {
                bpm.setTextColor(resources.getColor(R.color.white, null))
                bpm.setBackgroundResource(R.drawable.bg_212124_40)

                infoLocation2?.setVisibility(View.INVISIBLE)
            }
            CurrentType.TYPE -> {
                bpmSection.setTextColor(resources.getColor(R.color.white, null))
                bpmSection.setBackgroundResource(R.drawable.bg_212124_40)

                infoLocation3?.setVisibility(View.INVISIBLE)
            }
        }
        currentType = updateType
        when (updateType) {
            CurrentType.TIME_SIGNATURE -> {
                bpmTimeSignature.setTextColor(resources.getColor(R.color.white, null))
                bpmTimeSignature.setBackgroundResource(R.drawable.bg_2094fa_40)
                infoLocation1?.setVisibility(View.VISIBLE)
            }
            CurrentType.BPM -> {
                bpm.setTextColor(resources.getColor(R.color.white, null))
                bpm.setBackgroundResource(R.drawable.bg_2094fa_40)
                infoLocation2?.setVisibility(View.VISIBLE)
            }
            CurrentType.TYPE -> {
                bpmSection.setTextColor(resources.getColor(R.color.white, null))
                bpmSection.setBackgroundResource(R.drawable.bg_2094fa_40)

                infoLocation3?.setVisibility(View.VISIBLE)
            }
        }
    }
}