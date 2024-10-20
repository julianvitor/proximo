package com.proximo.ali

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.proximo.ali.Utils.getDateNow
import com.proximo.ali.Utils.getLoanEndpoint
import org.java_websocket.server.WebSocketServer
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.json.JSONException
import org.json.JSONObject
import java.net.InetSocketAddress
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

private const val WEBSOCKET_PORT = 8080

class WebSocketService() : Service(), Parcelable {

    private val binder = LocalBinder()
    private val handler = Handler(Looper.getMainLooper())
    private var dbHelper: DatabaseHelper? = null
    private var currentUserCpf: String? = null
    private lateinit var webSocketServer: MyWebSocketServer



    //passar o usuario atual
    constructor(parcel: Parcel) : this() {
        currentUserCpf = parcel.readString()
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
    fun setCurrentCpf(cpf: String) {
        currentUserCpf = cpf
    }

    fun setCurrentCpfIndefinido() {
        currentUserCpf = "indefinido"
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
                val rfid = message.substringAfter(":").trim()
                when{
                    currentUserCpf == null -> {
                        showToast("Retirada inválida: Usuário não autenticado")
                        conn.send("Retirada inválida: Usuário não autenticado")
                    }
                    currentUserCpf == "indefinido" -> {
                        showToast("Retirada inválida: Usuário indefinido")
                        conn.send("Retirada inválida: Usuário indefinido")
                    }
                    else -> {
                        //dbHelper?.registrarUso(currentUserEmail!!, rfid, "doca")
                        showToast("Sucesso: removido")
                        currentUserCpf = null
                        sendBroadcast(Intent("com.example.com.proximo.ali.ACTION_SUCCESS_REMOVIDO"))
                    }
                }
            }


            else -> {
                showToast("WebSocketMessage inesperada: $message")
            }
        }
    }

    //TODO: Mover requisição para outro arquivo e pensar solução para stationId
    private fun postLoan(context: Context, rfid: String, userId: String) {
        val url = getLoanEndpoint(context)
        val jsonBody = JSONObject().apply {
            put("machineId", rfid)
            put("startStationId", "a2b7fab1-b8f0-4c85-b5a4-c629bbf12936")
            put("userId", userId)
            put("start_time", getDateNow())
        }.toString()

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonBody.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast("Erro na requisição: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    showToast("Sucesso: removido")
                    currentUserCpf = null
                    sendBroadcast(Intent("com.example.com.proximo.ali.ACTION_SUCCESS_REMOVIDO"))
                } else {
                    showToast("Erro na resposta: ${response.message}")
                }
            }
        })
    }

    // Função para lidar com JSON
    private fun receiveJsonHandler(message: String, conn: WebSocket) {
        try {
            val jsonObject = JSONObject(message)
            val rfid = jsonObject.optString("rfid", "RFID não encontrado")
            val userId = jsonObject.optString("userId", "Usuário não encontrado")
            // Log the entire JSON message
            Log.d("WebSocketService", "Received JSON: $message")
            when {
                //requisitar maquinas presentes
                jsonObject.has("accio_machine_response") -> {
                    dbHelper?.addToMaquinasPresentes(jsonObject)
                }

                jsonObject.has("response_log") -> {
                    dbHelper?.addLogToFile(jsonObject)
                    showToast("Log.json salvo com sucesso")
                }

                jsonObject.has("removed")->{
                    try {
                        when{
                            currentUserCpf == null -> {
                                showToast("Retirada inválida: Usuário não autenticado")
                                conn.send("Retirada inválida: Usuário não autenticado")
                            }
                            currentUserCpf == "indefinido" -> {
                                showToast("Retirada inválida: Usuário indefinido")
                                conn.send("Retirada inválida: Usuário indefinido")
                            }
                            else -> {
                                postLoan(this, rfid, userId)
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
        parcel.writeString(currentUserCpf)
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
