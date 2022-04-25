package com.lee.android.demo.metronome

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 *@author lee
 *@date 2021/4/30
 */
class InfoLocation(
        var recyclerView: RecyclerView,
        var info: BpmInfo,
        var context: Context
) {

    var listener: OnChangePosListener? = null

    interface OnChangePosListener {
        fun onChangePos(pos: Int)
    }


    var adapter: AutoLocationAdapter? = null
    var snapHelper: PagerSelectSnapHelper? = null
    var currentMovePos = 0

    var layoutManager: LinearLayoutManager? = null

    fun init() {
        adapter = AutoLocationAdapter(context)
        adapter?.updateData(info.info)
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.layoutManager = layoutManager
        snapHelper = PagerSelectSnapHelper()
        snapHelper?.attachToRecyclerView(recyclerView)
        recyclerView.adapter = adapter

        recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                val pos = parent.getChildAdapterPosition(view)
                val count = parent.adapter!!.itemCount

                outRect.left = if (pos == 0) {
                    ((880 - info.itemW[pos]) / 2).toInt()
                } else 32
                outRect.right = if (pos == count - 1) {
                    ((880 - info.itemW[pos]) / 2).toInt()
                } else 32
            }
        })

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val lp = snapHelper!!.findSnapView(layoutManager!!)?.layoutParams
                    if (lp != null) {
                        currentMovePos = (lp as RecyclerView.LayoutParams).viewAdapterPosition
                        listener?.onChangePos(currentMovePos)
                    }
                }
            }
        })

        movePosition(info.pos)
    }

    fun setVisibility(visibility: Int) {
        recyclerView.visibility = visibility
    }

    private fun movePosition(pos: Int) {
        val scrollX = recyclerView.scrollX
        if (currentMovePos < pos) {
            var s = 0f
            for (i in (currentMovePos + 1) until pos) {
                s += info.itemW[i] + 64
            }
            s += info.itemW[currentMovePos] / 2 + 64
            s += info.itemW[pos] / 2
            recyclerView.smoothScrollBy((s - scrollX).toInt(), 0)
        } else if (currentMovePos > pos) {
            var s = 0f
            for (i in (pos + 1) until currentMovePos) {
                s += info.itemW[i] + 64
            }
            s += info.itemW[pos] / 2 + 64
            s += info.itemW[currentMovePos] / 2
            recyclerView.smoothScrollBy((scrollX - s).toInt(), 0)
        }
    }

}