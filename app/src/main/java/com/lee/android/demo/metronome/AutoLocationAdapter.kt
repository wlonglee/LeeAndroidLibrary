package com.lee.android.demo.metronome

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lee.android.R
import kotlinx.android.synthetic.main.bpm_item.view.*

/**
 *@author lee
 *@date 2021/4/30
 */
class AutoLocationAdapter(var context: Context?) : RecyclerView.Adapter<AutoLocationAdapter.ViewHolder>() {

    private var data: ArrayList<String> = arrayListOf()


    fun updateData(data: ArrayList<String>) {
        this.data = data
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.bpm_item, parent, false))
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val info = data[position]
        holder.itemView.name.text = info
        holder.itemView.setOnClickListener {
        }
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}