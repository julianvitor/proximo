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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        // Obtém o objeto JSON da lista
        val log = logList[position].optJSONObject("log") ?: return

        try {
            // Verifica e preenche informações do dispositivo
            if (log.has("deviceInfo")) {
                val deviceInfo = log.getJSONObject("deviceInfo")
                holder.macAddressTextView.text = "MAC Address: ${deviceInfo.optString("macAddress", "N/A")}"
                holder.ipAddressTextView.text = "IP Address: ${deviceInfo.optString("ipAddress", "N/A")}"
            } else {
                holder.macAddressTextView.text = "MAC Address: N/A"
                holder.ipAddressTextView.text = "IP Address: N/A"
            }

            // Verifica e preenche o status do sistema
            if (log.has("systemStatus")) {
                val systemStatus = log.getJSONObject("systemStatus")
                holder.coreTemperatureTextView.text = "Core Temperature: ${systemStatus.optDouble("coreTemperature", 0.0)} °C"
                holder.uptimeTextView.text = "Uptime: ${systemStatus.optString("uptime", "N/A")}"
            } else {
                holder.coreTemperatureTextView.text = "Core Temperature: N/A"
                holder.uptimeTextView.text = "Uptime: N/A"
            }

            // Verifica e preenche a versão do firmware PN532
            if (log.has("pn532Firmware")) {
                val pn532 = log.getJSONObject("pn532Firmware")
                holder.firmwareVersionTextView.text = "Pn532 Firmware Version: ${pn532.optString("version", "N/A")}"
            } else {
                holder.firmwareVersionTextView.text = "Pn532 Firmware Version: N/A"
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
        }
    }

    override fun getItemCount(): Int {
        return logList.size
    }
}
