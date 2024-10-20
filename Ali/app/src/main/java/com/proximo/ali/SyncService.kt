package com.proximo.ali

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.widget.Toast
import androidx.core.app.NotificationCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class SyncService : Service() {

    private val binder = LocalBinder()
    private val client = OkHttpClient()
    private val handler = Handler(Looper.getMainLooper())
    private val intervalMillis: Long = 15 * 60 * 1000 // 15 minutos

    private val runnable = object : Runnable {
        override fun run() {
            performPeriodicTask()
            handler.postDelayed(this, intervalMillis)
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): SyncService = this@SyncService
    }


    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(2, notification)

        showToast("Sync service iniciado")
        handler.postDelayed(runnable, intervalMillis)
        syncGeral(applicationContext)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }


    private fun performPeriodicTask() {
        showToast("Sincronizando com API")
        syncGeral(applicationContext)
    }

    private fun createNotification(): Notification {
        val channelId = "SyncServiceChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Sync Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Sync Service")
            .setContentText("O serviço de sincronização está ativo")
            .setSmallIcon(R.drawable.ic_sync_notification)
            .build()
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun syncGeral(context: Context) {
        val url = "https://uzmjrszlcg.execute-api.us-east-1.amazonaws.com/express-sync"

        val jsonBody = "{}".trimIndent()

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonBody.toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        showToast("Sincronização em andamento...")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast("Erro na requisição: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    showToast("Resposta da requisição recebida")

                    responseData?.let {
                        // Salvar o JSON completo em syncGeral.json
                        saveResponseToFile(context, it, "syncGeral.json")

                        // Separar a lista de máquinas e salvar em maquinasLista.json
                        extractAndSaveMachines(context, it)
                    }
                } else {
                    showToast("Erro na resposta: ${response.message}")
                }
            }
        })
    }

    // Método para salvar a resposta em um arquivo
    private fun saveResponseToFile(context: Context, data: String, fileName: String) {
        try {
            val file = File(context.filesDir, fileName)
            FileOutputStream(file).use { fos ->
                fos.write(data.toByteArray())
                fos.flush()
            }
            showToast("Resposta salva em $fileName")
        } catch (e: IOException) {
            showToast("Erro ao salvar o arquivo: ${e.message}")
        }
    }

    // Método para extrair a lista de máquinas e salvar em um arquivo separado
    private fun extractAndSaveMachines(context: Context, jsonData: String) {
        try {
            val jsonObject = JSONObject(jsonData)
            val attributes = jsonObject.getJSONObject("attributes")
            val machines = attributes.getJSONArray("machines")

            // Converter a lista de máquinas em uma string JSON e salvar
            saveResponseToFile(context, machines.toString(), "maquinasCatalog.json")
        } catch (e: JSONException) {
            showToast("Erro ao processar o JSON: ${e.message}")
        }

    }
}