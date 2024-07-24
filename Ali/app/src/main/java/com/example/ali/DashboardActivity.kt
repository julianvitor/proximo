package com.example.ali

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import okhttp3.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var webSocket: WebSocket
    private lateinit var dbHelper: DatabaseHelper
    private val handler = Handler(Looper.getMainLooper())
    private var mensagemRecebida: String? = null
    private var uid: String? = null
    private var doca: String? = null
    private var apelido: String? = null
    private var countdownBotao: Int = 25
    private var countdownGeral: Int = 60
    private lateinit var countdownTextView: TextView
    private var countdownHandler: Handler = Handler(Looper.getMainLooper())
    private var countdownGeralHandler: Handler = Handler(Looper.getMainLooper())
    private var webSocketService: WebSocketService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as WebSocketService.LocalBinder
            webSocketService = binder.getService()
            isBound = true
            webSocketService?.setCurrentUser(apelido ?: "") // Passa o apelido para o serviço
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        apelido = intent.getStringExtra("apelidoUsuario")
        conectarWebSocket()
        contadorGeral(countdownGeral)
        dbHelper = DatabaseHelper(this)

        val bay1Button: Button = findViewById(R.id.bay1)
        val bay2Button: Button = findViewById(R.id.bay2)
        countdownTextView = findViewById(R.id.countdownTextView)

        bay1Button.setOnClickListener {
            countdownTextView.visibility = View.VISIBLE
            contadorBotao(countdownBotao)
            enviarMensagem("ativar 1")
            doca = "1"
        }
        bay2Button.setOnClickListener {
            countdownTextView.visibility = View.VISIBLE
            contadorBotao(countdownBotao)
            enviarMensagem("ativar 2")
            doca = "2"
        }
        //botão voltar
        val buttonBack: MaterialButton = findViewById(R.id.buttonBack)
        buttonBack.setOnClickListener{
            onBackPressedDispatcher.onBackPressed()
        }
    }
    private fun contadorGeral(countdownGeral: Int){
        var currentCountdown = countdownGeral
        exibirToast("tempo limite: $currentCountdown segundos")
        countdownGeralHandler.removeCallbacksAndMessages(null)
        countdownGeralHandler.postDelayed(object : Runnable {
            override fun run() {
                currentCountdown--
                if (currentCountdown == 0) {
                    exibirToast("tempo limite atingido")
                    finish()
                    return
                }
                countdownGeralHandler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    private fun contadorBotao(countdown: Int) {
        var currentCountdown = countdown
        countdownHandler.removeCallbacksAndMessages(null)
        countdownHandler.postDelayed(object : Runnable {
            override fun run() {
                currentCountdown--
                countdownTextView.text = "Tempo restante: $currentCountdown segundos"
                if (currentCountdown == 0) {
                    finish()
                    return
                }
                countdownHandler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    private fun conectarWebSocket() {
        val request = Request.Builder()
            .url("ws://192.168.1.150:8080")
            .build()

        val client = OkHttpClient()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)
                mensagemRecebida = text
                exibirMensagemRecebida(text)

                if (mensagemRecebida!!.startsWith("removido:")) {
                    extrairUid(mensagemRecebida!!)
                    dbHelper?.registrarUso(apelido ?: "", uid ?: "", doca ?: "")
                    exibirToast("Sucesso: registrado")
                    finish()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                super.onFailure(webSocket, t, response)
                reconectarWebSocket()
            }
        })
    }

    private fun extrairUid(mensagem: String) {
        if (mensagem.startsWith("removido:")) {
            uid = mensagem.substringAfter(":")
        }
    }

    private fun reconectarWebSocket() {
        handler.postDelayed({
            conectarWebSocket()
        }, 5000)
    }

    private fun enviarMensagem(mensagem: String) {
        webSocket.send(mensagem)
        exibirMensagemEnviada(mensagem)
    }

    private fun exibirMensagemEnviada(mensagem: String) {
        handler.post {
            Toast.makeText(this@DashboardActivity, "Mensagem enviada: $mensagem", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exibirMensagemRecebida(mensagem: String) {
        handler.post {
            Toast.makeText(this@DashboardActivity, "Mensagem recebida: $mensagem", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exibirToast(mensagemToast: String) {
        handler.post {
            Toast.makeText(this@DashboardActivity, "$mensagemToast", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, WebSocketService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
            startService(intent)
        }
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket.close(1000, "Activity fechada")
        countdownHandler.removeCallbacksAndMessages(null)
    }
}
