package com.proximo.ali

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import org.java_websocket.server.WebSocketServer
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.json.JSONException
import org.json.JSONObject
import java.net.InetSocketAddress

private const val WEBSOCKET_PORT = 8080

class WebSocketService() : Service(), Parcelable {

    private val binder = LocalBinder()
    private val handler = Handler(Looper.getMainLooper())
    private var dbHelper: DatabaseHelper? = null
    private var currentUserEmail: String? = null
    private lateinit var webSocketServer: MyWebSocketServer



    //passar o usuario atual
    constructor(parcel: Parcel) : this() {
        currentUserEmail = parcel.readString()
    }

    // Classe interna para a ligação do serviço
    inner class LocalBinder : Binder() {
        fun getService(): WebSocketService = this@WebSocketService
    }

    // Ciclo de vida do serviço
    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        dbHelper = DatabaseHelper(this)
        startWebSocketServer()
        val notification = createNotification()
        startForeground(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        stopWebSocketServer()
    }

    // Métodos para controle do usuário atual
    fun setCurrentEmail(user: String) {
        currentUserEmail = user
    }

    fun setCurrentEmailIndefinido() {
        currentUserEmail = "indefinido"
    }

    private fun startWebSocketServer() {
        webSocketServer = MyWebSocketServer(InetSocketAddress(WEBSOCKET_PORT), this) // Passe o serviço
        webSocketServer.start()
    }

    private fun stopWebSocketServer() {
        webSocketServer.stop(1000)
        showToast("WebsocketServer parado")
    }

    // Manipulação de mensagens WebSocket
    private fun handleWebSocketMessage(message: String, conn: WebSocket) {
        when {
            // JSON RECEBIDO?
            message.startsWith("{") -> {
                receiveJsonHandler(message, conn)
            }

            // Remoção
            message.startsWith("removido:") -> {
                val uid = message.substringAfter(":").trim()
                when{
                    currentUserEmail == null -> {
                        showToast("Retirada inválida: Usuário não autenticado")
                        conn.send("Retirada inválida: Usuário não autenticado")
                    }
                    currentUserEmail == "indefinido" -> {
                        showToast("Retirada inválida: Usuário indefinido")
                        conn.send("Retirada inválida: Usuário indefinido")
                    }
                    else -> {
                        //dbHelper?.registrarUso(currentUserEmail!!, uid, "doca")
                        showToast("Sucesso: removido")
                        currentUserEmail = null
                        sendBroadcast(Intent("com.example.com.proximo.ali.ACTION_SUCCESS_REMOVIDO"))
                    }
                }
            }


            else -> {
                showToast("WebSocketMessage inesperada: $message")
            }
        }
    }

    // Função para lidar com JSON
    private fun receiveJsonHandler(message: String, conn: WebSocket) {
        try {
            val jsonObject = JSONObject(message)
            when {
                //requisitar maquinas presentes
                jsonObject.has("accio_machine_response") -> {
                    dbHelper?.addToMaquinasPresentes(jsonObject)
                }

                jsonObject.has("response_log") -> {
                    dbHelper?.addLogToFile(jsonObject)
                    showToast("Log.json salvo com sucesso")
                }

                jsonObject.has("inserted") -> {
                    try {
                        dbHelper?.addToReturns(jsonObject)
                        //dbHelper?.createReturnLoans() PAREI AQUI MDS DO CEU
                        showToast("Sucesso: devolvido")

                    }
                    catch (e: Exception) {
                        // Exibe a mensagem da exceção no Toast
                        showToast("Erro ao registrar devolução: ${e.message}")
                    }

                }


                jsonObject.has("removed")->{
                    try {
                        when{
                            currentUserEmail == null -> {
                                showToast("Retirada inválida: Usuário não autenticado")
                                conn.send("Retirada inválida: Usuário não autenticado")
                            }
                            currentUserEmail == "indefinido" -> {
                                showToast("Retirada inválida: Usuário indefinido")
                                conn.send("Retirada inválida: Usuário indefinido")
                            }
                            else -> {
                                //dbHelper?.registrarUso(currentUserEmail!!, uid, "doca")
                                showToast("Sucesso: removido")
                                currentUserEmail = null
                                sendBroadcast(Intent("com.example.com.proximo.ali.ACTION_SUCCESS_REMOVIDO"))
                            }
                        }

                    }
                    catch (e: Exception) {

                    }
                }

                else -> {
                    // Outro JSON
                    showToast("Json inesperado: $jsonObject")
                    Log.i("websocketService", "Json inesperado recebido: $jsonObject")
                }
            }


        }
        catch (e: JSONException) {
            showToast("Erro ao processar o JSON: ${e.message}")
            Log.e("websocketService", "Erro ao processar o JSON recebido: ${e.message}")
        }
        catch (e: Exception) {
            showToast("Erro inesperado: ${e.message}")
            Log.e("websocketService", "Erro inesperado: ${e.message}")
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
            conn.send("Cliente conectado")
        }

        override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
            service.showToast("Cliente desconectado")
        }

        override fun onMessage(conn: WebSocket, message: String) {
            service.handleWebSocketMessage(message, conn)
        }

        override fun onError(conn: WebSocket?, ex: Exception) {
            Log.e("WebSocketService", "Erro no WebSocketServer")
            service.showToast("Erro no WebsocketServer")
            ex.printStackTrace()
        }

        override fun onStart() {
            service.showToast("Servidor WebSocket iniciado na porta: $WEBSOCKET_PORT")
        }
    }
    // Implementação da interface Parcelable
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(currentUserEmail)
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
