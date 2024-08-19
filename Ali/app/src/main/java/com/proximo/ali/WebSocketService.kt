package com.proximo.ali

import android.annotation.SuppressLint
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
import org.json.JSONException
import org.json.JSONObject
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

    @SuppressLint("ForegroundServiceType")
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

    private fun startWebSocketServer() {
        val port = 8080
        webSocketServer = MyWebSocketServer(InetSocketAddress(port), this) // Passe o serviço
        webSocketServer.start()
        showToast("Servidor WebSocket iniciado na porta $port")
    }


    private fun stopWebSocketServer() {
        webSocketServer.stop(1000)
        showToast("Servidor WebSocket parado")
    }

    // Manipulação de mensagens WebSocket
    // Manipulação de mensagens WebSocket
    private fun handleWebSocketMessage(message: String, conn: WebSocket) {
        when {
            // Inserção
            message.startsWith("inserido:") -> {
                val uid = message.substringAfter(":").trim()
                dbHelper?.registrarDevolucao(uid)
                showToast("Sucesso: devolvido")
                conn.send("Sucesso: devolvido")
            }

            // Remoção
            message.startsWith("removido:") -> {
                val uid = message.substringAfter(":").trim()
                when {
                    currentUser == null -> {
                        showToast("Retirada inválida: Usuário não autenticado")
                        conn.send("Retirada inválida: Usuário não autenticado")
                    }
                    currentUser == "indefinido" -> {
                        showToast("Retirada inválida: Usuário indefinido")
                        conn.send("Retirada inválida: Usuário indefinido")
                    }
                    else -> {
                        dbHelper?.registrarUso(currentUser!!, uid, "doca")
                        showToast("Sucesso: removido")
                        conn.send("Sucesso: removido")
                        currentUser = null
                        sendBroadcast(Intent("com.example.com.proximo.ali.ACTION_SUCCESS_REMOVIDO"))
                    }
                }
            }

            // Resposta de máquina (JSON accio_machine_response)
            message.startsWith("{") -> {
                try {
                    val jsonObject = JSONObject(message)

                    if (jsonObject.has("accio_machine_response")) {
                        // Se contém "accio_machine_response", processar como resposta de máquina
                        dbHelper?.addToMaquinasPresentes(jsonObject)
                        showToast("Informações de máquina salvas com sucesso")
                    } else if (jsonObject.has("log")) {
                        // Se contém "log", processar como log
                        dbHelper?.addLogToFile(jsonObject)
                        showToast("Log salvo com sucesso")
                    } else {
                        // Outro JSON
                        showToast("Outro JSON recebido: $jsonObject")
                        conn.send("Outro JSON processado com sucesso")
                    }
                } catch (e: JSONException) {
                    showToast("Erro ao processar o JSON: ${e.message}")
                    conn.send("Erro ao processar o JSON")
                } catch (e: Exception) {
                    showToast("Erro inesperado: ${e.message}")
                    conn.send("Erro inesperado ao processar o JSON")
                }
            }

            else -> {
                showToast("Mensagem recebida: $message")
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
            .setSmallIcon(R.drawable.ic_websocket_notification) // Certifique-se de que o ícone está correto
            .build()
    }

    // Exibir mensagens Toast
    private fun showToast(message: String) {
        handler.post {
            Toast.makeText(this@WebSocketService, message, Toast.LENGTH_SHORT).show()
        }
    }

    private class MyWebSocketServer(
        address: InetSocketAddress,
        private val service: WebSocketService // Adicione o serviço como parâmetro
    ) : WebSocketServer(address) {

        override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
            conn.send("Conexão WebSocket estabelecida")
        }

        override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
            service.showToast("Cliente desconectado")
        }

        override fun onMessage(conn: WebSocket, message: String) {
            // Chame diretamente o método handleWebSocketMessage do serviço
            service.handleWebSocketMessage(message, conn)
        }

        override fun onError(conn: WebSocket?, ex: Exception) {
            ex.printStackTrace()
        }

        override fun onStart() {
            service.showToast("Servidor WebSocket iniciado ")
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
