package com.example.ali

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

class LogAdapter(private val logList: MutableList<JSONObject>) : RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

    class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
        val macAddressTextView: TextView = itemView.findViewById(R.id.macAddressTextView)
        val ipAddressTextView: TextView = itemView.findViewById(R.id.ipAddressTextView)
        val coreTemperatureTextView: TextView = itemView.findViewById(R.id.coreTemperatureTextView)
        val uptimeTextView: TextView = itemView.findViewById(R.id.uptimeTextView)
        val firmwareVersionTextView: TextView = itemView.findViewById(R.id.firmwareVersionTextView)
        val firmwareStatusTextView: TextView = itemView.findViewById(R.id.firmwareStatusTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = logList[position].getJSONObject("log")

        // Preenche as TextViews com os valores do JSON
        holder.timestampTextView.text = log.getString("timestamp")
        val deviceInfo = log.getJSONObject("deviceInfo")
        holder.macAddressTextView.text = "MAC Address: ${deviceInfo.getString("macAddress")}"
        holder.ipAddressTextView.text = "IP Address: ${deviceInfo.getString("ipAddress")}"

        val systemStatus = log.getJSONObject("systemStatus")
        holder.coreTemperatureTextView.text = "Core Temperature: ${systemStatus.getDouble("coreTemperature")} °C"
        holder.uptimeTextView.text = "Uptime: ${systemStatus.getString("uptime")}"

        val pn532Firmware = log.getJSONObject("pn532Firmware")
        holder.firmwareVersionTextView.text = "Firmware Version: ${pn532Firmware.getString("version")}"
        holder.firmwareStatusTextView.text = "Firmware Status: ${pn532Firmware.getString("status")}"
    }

    override fun getItemCount(): Int {
        return logList.size
    }
}
