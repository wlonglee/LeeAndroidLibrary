package com.lee.android.demo.metronome

import android.view.View
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView

/**
 * 解决LinearSnapHelper 首尾无法居中的问题
 *@author lee
 *@date 2021/4/30
 */
class PagerSelectSnapHelper: LinearSnapHelper() {

    override fun findSnapView(layoutManager: RecyclerView.LayoutManager): View? {
        // Use existing LinearSnapHelper but override when the itemDecoration calculations are off
        val snapView = super.findSnapView(layoutManager)
        return if (!snapView.isViewInCenterOfParent(layoutManager.width)) {
            val endView = layoutManager.findViewByPosition(layoutManager.itemCount - 1)
            val startView = layoutManager.findViewByPosition(0)

            when {
                endView.isViewInCenterOfParent(layoutManager.width) -> endView
                startView.isViewInCenterOfParent(layoutManager.width) -> startView
                else -> snapView
            }
        } else {
            snapView
        }
    }

    private fun View?.isViewInCenterOfParent(parentWidth: Int): Boolean {
        if (this == null || width == 0) {
            return false
        }
        val parentCenter = parentWidth / 2
        return parentCenter in (left + 1) until right
    }
}