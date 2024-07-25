package com.example.ali

import android.content.BroadcastReceiver
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

class DashboardActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private val handler = Handler(Looper.getMainLooper())
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
            // Passar o nome de usuário para o serviço
            webSocketService?.setCurrentUser(apelido ?: "")
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.example.ali.ACTION_SUCCESS_REMOVIDO") {
                finish()  // Encerra a atividade
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        apelido = intent.getStringExtra("apelidoUsuario")

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

        // Botão voltar
        val buttonBack: MaterialButton = findViewById(R.id.buttonBack)
        buttonBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Conectar ao serviço
        Intent(this, WebSocketService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
            startService(intent)
        }

        contadorGeral(countdownGeral)
    }

    private fun contadorGeral(countdownGeral: Int) {
        var currentCountdown = countdownGeral
        exibirToast("Tempo limite: $currentCountdown segundos")
        countdownGeralHandler.removeCallbacksAndMessages(null)
        countdownGeralHandler.postDelayed(object : Runnable {
            override fun run() {
                currentCountdown--
                if (currentCountdown == 0) {
                    exibirToast("Tempo limite atingido")
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

    private fun enviarMensagem(mensagem: String) {
        if (isBound) {
            webSocketService?.webSocket?.send(mensagem)
            exibirMensagemEnviada(mensagem)
        } else {
            exibirToast("Serviço WebSocket não disponível")
        }
    }

    private fun exibirMensagemEnviada(mensagem: String) {
        handler.post {
            Toast.makeText(this@DashboardActivity, "Mensagem enviada: $mensagem", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exibirToast(mensagemToast: String) {
        handler.post {
            Toast.makeText(this@DashboardActivity, mensagemToast, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Desconectar do serviço
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
        countdownHandler.removeCallbacksAndMessages(null)
    }
}
