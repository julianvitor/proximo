package com.example.ali

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.*
import android.widget.Toast
import androidx.core.app.NotificationCompat
import org.java_websocket.server.WebSocketServer
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import java.net.InetSocketAddress

class WebSocketService() : Service(), Parcelable {

    private val binder = LocalBinder()
    private val handler = Handler(Looper.getMainLooper())
    private var dbHelper: DatabaseHelper? = null
    private var currentUser: String? = null
    private lateinit var webSocketServer: MyWebSocketServer

    constructor(parcel: Parcel) : this() {
        currentUser = parcel.readString()
    }

    // Classe interna para a ligação do serviço
    inner class LocalBinder : Binder() {
        fun getService(): WebSocketService = this@WebSocketService
    }

    // Ciclo de vida do serviço
    override fun onCreate() {
        super.onCreate()
        dbHelper = DatabaseHelper(this)
        startWebSocketServer()
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
        stopWebSocketServer()
    }

    // Métodos para controle do usuário atual
    fun setCurrentUser(user: String) {
        currentUser = user
    }

    fun setCurrentUserIndefinido() {
        currentUser = "indefinido"
    }

    // Métodos para controle do servidor WebSocket
    private fun startWebSocketServer() {
        val port = 8080
        webSocketServer = MyWebSocketServer(InetSocketAddress(port))
        webSocketServer.start()
        showToast("Servidor WebSocket iniciado na porta $port")
    }

    private fun stopWebSocketServer() {
        webSocketServer.stop(1000)
        showToast("Servidor WebSocket parado")
    }

    // Manipulação de mensagens WebSocket
    private fun handleWebSocketMessage(message: String, conn: WebSocket) {
        when {
            message.startsWith("inserido:") -> {
                val uid = message.substringAfter(":")
                dbHelper?.registrarDevolucao(uid)
                showToast("Sucesso: devolvido")
                conn.send("Sucesso: devolvido")
            }
            message.startsWith("removido:") -> {
                val uid = message.substringAfter(":")
                if (currentUser == null) {
                    showToast("Retirada inválida: Usuário não autenticado")
                    conn.send("Retirada inválida: Usuário não autenticado")
                } else if (currentUser == "indefinido") {
                    showToast("Retirada inválida: Usuário indefinido")
                    conn.send("Retirada inválida: Usuário indefinido")
                } else {
                    dbHelper?.registrarUso(currentUser!!, uid, "doca")
                    showToast("Sucesso: removido")
                    conn.send("Sucesso: removido")
                    currentUser = null
                    sendBroadcast(Intent("com.example.ali.ACTION_SUCCESS_REMOVIDO"))
                }
            }
            else -> {
                showToast("Mensagem recebida: $message")
                conn.send("Mensagem recebida: $message")
            }
        }
    }

    // Método para enviar uma mensagem para todos os clientes conectados
    fun broadcast(mensagem: String) {
        if (::webSocketServer.isInitialized) {
            for (client in webSocketServer.connections) {
                client.send(mensagem)
            }
        } else {
            showToast("Servidor WebSocket não iniciado")
        }
    }


    // Criação e gerenciamento de notificações
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
            .setContentTitle("WebSocket Server")
            .setContentText("O Servidor WebSocket está rodando")
            .setSmallIcon(R.drawable.ic_notification) // Certifique-se de que o ícone está correto
            .build()
    }

    // Exibir mensagens Toast
    private fun showToast(message: String) {
        handler.post {
            Toast.makeText(this@WebSocketService, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Implementação da classe WebSocketServer
    private class MyWebSocketServer(address: InetSocketAddress) : WebSocketServer(address) {
        override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
            conn.send("Conexão WebSocket estabelecida")
        }

        override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
            // Ação quando um cliente desconecta
        }

        override fun onMessage(conn: WebSocket, message: String) {
            // Aqui, você chama handleWebSocketMessage no serviço
            (conn.remoteSocketAddress?.address as? WebSocketService)?.handleWebSocketMessage(message, conn)
        }

        override fun onError(conn: WebSocket?, ex: Exception) {
            ex.printStackTrace()
        }

        override fun onStart() {
            // Ação quando o servidor começa a rodar
        }
    }

    // Implementação da interface Parcelable
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(currentUser)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<WebSocketService> {
        override fun createFromParcel(parcel: Parcel): WebSocketService {
            return WebSocketService(parcel)
        }

        override fun newArray(size: Int): Array<WebSocketService?> {
            return arrayOfNulls(size)
        }
    }
}
