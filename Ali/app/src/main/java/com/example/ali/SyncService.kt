package com.example.ali

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.*
import android.widget.Toast
import androidx.core.app.NotificationCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException


class SyncService : Service() {

    private val binder = LocalBinder()
    private val handler = Handler(Looper.getMainLooper())
    private var dbHelper: DatabaseHelper? = null
    private val client = OkHttpClient() // Instanciar o cliente OkHttp

    // Classe interna para a ligação do serviço
    inner class LocalBinder : Binder() {
        fun getService(): SyncService = this@SyncService
    }

    // Ciclo de vida do serviço
    override fun onCreate() {
        super.onCreate()
        dbHelper = DatabaseHelper(this)
        showToast("Sincronização iniciada")
        syncUsers() // Chamar o método para sincronizar usuários
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(2, notification)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    // Criação e gerenciamento de notificações
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

    // Exibir mensagens Toast
    private fun showToast(message: String) {
        handler.post {
            Toast.makeText(this@SyncService, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Método para sincronizar usuários
    private fun syncUsers() {
        val url = BuildConfig.USER_SYNC_API_ENDPOINT

        val jsonBody = """
            {
                "users": [
                    { "email": "express.user@email.com", "status": "ACTIVE" }
                ]
            }
        """.trimIndent()

        // Usar a nova forma de criar o MediaType
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
                } else {
                    showToast("Erro na resposta: ${response.message}")
                }
            }
        })
    }

}
