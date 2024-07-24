package com.example.ali

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import okhttp3.*

class WebSocketService : Service() {

    private val binder = LocalBinder()
    private lateinit var webSocket: WebSocket
    private val client = OkHttpClient()
    private val handler = Handler(Looper.getMainLooper())
    private var dbHelper: DatabaseHelper? = null
    private var currentUser: String? = null

    inner class LocalBinder : Binder() {
        fun getService(): WebSocketService = this@WebSocketService
    }

    override fun onCreate() {
        super.onCreate()
        dbHelper = DatabaseHelper(this)
        connectWebSocket()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(1, notification)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectWebSocket()
    }

    fun setCurrentUser(user: String) {
        currentUser = user
    }

    private fun createNotification(): Notification {
        val channelId = "WebSocketServiceChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "WebSocket Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("WebSocket Service")
            .setContentText("O servidor WebSocket está rodando.")
            .setSmallIcon(R.drawable.ic_notification) // Certifique-se de que o ícone está correto
            .build()
    }

    private fun connectWebSocket() {
        val request = Request.Builder()
            .url("ws://192.168.1.150:8080")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                showToast("Conexão WebSocket aberta")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleWebSocketMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                showToast("Erro na conexão WebSocket: ${t.message ?: "Erro desconhecido"}")
                reconnectWebSocket()
            }
        })
    }

    private fun disconnectWebSocket() {
        try {
            webSocket.close(1000, "Service stopped")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleWebSocketMessage(message: String) {
        when {
            message.startsWith("inserido:") -> {
                val uid = message.substringAfter(":")
                dbHelper?.registrarDevolucao(uid)
                showToast("Sucesso: devolvido")
            }
            message.startsWith("removido:") -> {
                val uid = message.substringAfter(":")
                if (currentUser != null) {
                    dbHelper?.registrarUso(currentUser!!, uid, "doca") // Assumindo que "doca" é um placeholder; ajuste conforme necessário
                    showToast("Sucesso: registrado")
                    currentUser = null // Limpar usuário atual após registrar
                } else {
                    showToast("Erro: Usuário não definido")
                }
            }
            else -> {
                showToast("Erro: Formato de mensagem inválido: $message")
            }
        }
    }

    private fun showToast(message: String) {
        handler.post {
            Toast.makeText(this@WebSocketService, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun reconnectWebSocket() {
        handler.postDelayed({
            connectWebSocket()
        }, 5000)
    }
}
