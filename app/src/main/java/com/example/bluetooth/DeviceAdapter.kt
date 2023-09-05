package com.example.bluetooth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeviceAdapter(private var devicesList: List<String>) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var device: TextView = view.findViewById(R.id.device)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val device = LayoutInflater.from(parent.context).inflate(R.layout.row_device, parent, false)
        return ViewHolder(device)
    }

    override fun getItemCount(): Int {
        return devicesList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = devicesList[position]
        holder.device.text = item
    }
}

