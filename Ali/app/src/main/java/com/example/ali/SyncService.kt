package com.example.ali

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
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class SyncService : Service() {

    private val binder = LocalBinder()
    private val client = OkHttpClient() // Instanciar o cliente OkHttp

    inner class LocalBinder : Binder() {
        fun getService(): SyncService = this@SyncService
    }

    override fun onCreate() {
        super.onCreate()
        showToast("Sincronização iniciada")
        setupAlarm() // Configura o alarme quando o serviço é criado
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(2, notification)

        // Chama syncUsers ao iniciar o serviço
        syncUsers(applicationContext)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelAlarm() // Cancela o alarme quando o serviço é destruído
    }

    private fun createNotification(): Notification {
        val channelId = "SyncServiceChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Sync Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
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
            Toast.makeText(this@SyncService, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun syncUsers(context: Context) {
        val url = BuildConfig.USER_SYNC_API_ENDPOINT

        val jsonBody = """
        {
            "users": [
                { "email": "express.user@email.com", "status": "ACTIVE" }
            ]
        }
    """.trimIndent()

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonBody.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast("Erro na requisição: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    showToast("Resposta da requisição: $responseData")

                    // Salvar a resposta no arquivo
                    responseData?.let {
                        saveResponseToFile(context, it)
                    }
                } else {
                    showToast("Erro na resposta: ${response.message}")
                }
            }
        })
    }

    // Método para salvar a resposta em um arquivo
    private fun saveResponseToFile(context: Context, data: String) {
        try {
            val file = File(context.filesDir, "usuarios.json")
            FileOutputStream(file).use { fos ->
                fos.write(data.toByteArray())
                fos.flush()
            }
            showToast("Resposta salva em usuarios.json")
        } catch (e: IOException) {
            showToast("Erro ao salvar o arquivo: ${e.message}")
        }
    }

    private fun setupAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val intervalMillis: Long = 15 * 60 * 1000 // 15 minutos
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 1000, // Início após 1 segundo
            intervalMillis,
            pendingIntent
        )
    }

    private fun cancelAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        alarmManager.cancel(pendingIntent)
    }
}
