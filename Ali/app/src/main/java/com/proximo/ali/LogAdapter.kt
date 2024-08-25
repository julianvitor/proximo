package com.proximo.ali

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

class LogAdapter(private val logList: MutableList<JSONObject>) : RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

    class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val macAddressTextView: TextView = itemView.findViewById(R.id.macAddressTextView)
        val ipAddressTextView: TextView = itemView.findViewById(R.id.ipAddressTextView)
        val coreTemperatureTextView: TextView = itemView.findViewById(R.id.coreTemperatureTextView)
        val uptimeTextView: TextView = itemView.findViewById(R.id.uptimeTextView)
        val firmwareVersionTextView: TextView = itemView.findViewById(R.id.firmwareVersionTextView)
        val rfidTextView: TextView = itemView.findViewById(R.id.rfidTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        // Obtém o objeto JSON da lista
        val log = logList[position].optJSONObject("response_log") ?: return

        try {
            // Verifica e preenche informações do dispositivo
            if (log.has("device_info")) {
                val deviceInfo = log.getJSONObject("device_info")
                holder.macAddressTextView.text = "MAC Address: ${deviceInfo.optString("station_mac", "N/A")}"
                holder.ipAddressTextView.text = "IP Address: ${deviceInfo.optString("ip_address", "N/A")}"
            } else {
                holder.macAddressTextView.text = "MAC Address: N/A"
                holder.ipAddressTextView.text = "IP Address: N/A"
            }

            // Verifica e preenche o status do sistema
            if (log.has("system_status")) {
                val systemStatus = log.getJSONObject("system_status")
                holder.coreTemperatureTextView.text = "Core Temperature: ${systemStatus.optDouble("core_temperature", 0.0)} °C"
                holder.uptimeTextView.text = "Uptime: ${systemStatus.optString("uptime", "N/A")}"
            } else {
                holder.coreTemperatureTextView.text = "Core Temperature: N/A"
                holder.uptimeTextView.text = "Uptime: N/A"
            }

            // Verifica e preenche a versão do firmware PN532 e RFID
            if (log.has("pn532")) {
                val pn532 = log.getJSONObject("pn532")
                holder.firmwareVersionTextView.text = "Pn532 Firmware Version: ${pn532.optString("version", "N/A")}"
                holder.rfidTextView.text = "RFID: ${pn532.optString("rfid", "N/A")}"
            } else {
                holder.firmwareVersionTextView.text = "Pn532 Firmware Version: N/A"
                holder.rfidTextView.text = "RFID: N/A"
            }
        } catch (e: Exception) {
            // Tratar exceções que possam ocorrer ao acessar dados do JSON
            e.printStackTrace()
            // Exibir uma mensagem de erro genérica ou definir valores padrão
            holder.macAddressTextView.text = "MAC Address: Não informado"
            holder.ipAddressTextView.text = "IP Address: Não informado"
            holder.coreTemperatureTextView.text = "Core Temperature: Não informado"
            holder.uptimeTextView.text = "Uptime: Não informado"
            holder.firmwareVersionTextView.text = "Pn532 Firmware Version: Não informado"
            holder.rfidTextView.text = "RFID: Não informado"
        }
    }

    override fun getItemCount(): Int {
        return logList.size
    }
}
