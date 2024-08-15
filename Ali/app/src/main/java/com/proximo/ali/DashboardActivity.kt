package com.proximo.ali

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.*
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
    private var countdownBotao: Int = 30
    private var countdownGeral: Int = 60
    private lateinit var countdownTextView: TextView
    private var countdownHandler: Handler = Handler(Looper.getMainLooper())
    private var countdownGeralHandler: Handler = Handler(Looper.getMainLooper())
    private var webSocketService: WebSocketService? = null
    private var isBound = false

    // Conexão ao serviço WebSocket
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as WebSocketService.LocalBinder
            webSocketService = binder.getService()
            isBound = true
            webSocketService?.setCurrentUser(apelido ?: "")
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    // BroadcastReceiver para receber ações do serviço
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.example.com.proximo.ali.ACTION_SUCCESS_REMOVIDO") {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        apelido = intent.getStringExtra("apelidoUsuario")
        dbHelper = DatabaseHelper(this)
        countdownTextView = findViewById(R.id.countdownTextView)

        setupUI()
        connectToWebSocketService()
        startGeneralCountdown(countdownGeral)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(broadcastReceiver, IntentFilter("com.example.com.proximo.ali.ACTION_SUCCESS_REMOVIDO"))
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(broadcastReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
        countdownHandler.removeCallbacksAndMessages(null)
        countdownGeralHandler.removeCallbacksAndMessages(null)
    }

    // Configuração da interface do usuário
    private fun setupUI() {
        val bay1Button: Button = findViewById(R.id.bay1)
        val bay2Button: Button = findViewById(R.id.bay2)
        val buttonBack: MaterialButton = findViewById(R.id.buttonBack)

        bay1Button.setOnClickListener {
            startButtonCountdown(countdownBotao)
            sendMessage("ativar 1")
            doca = "1"
        }

        bay2Button.setOnClickListener {
            startButtonCountdown(countdownBotao)
            sendMessage("ativar 2")
            doca = "2"
        }

        buttonBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    // Conectar ao serviço WebSocket
    private fun connectToWebSocketService() {
        Intent(this, WebSocketService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
            startService(intent)
        }
    }

    // Iniciar contagem regressiva geral
    private fun startGeneralCountdown(countdownTime: Int) {
        var currentCountdown = countdownTime
        showToast("Tempo limite: $currentCountdown segundos")
        countdownGeralHandler.removeCallbacksAndMessages(null)
        countdownGeralHandler.postDelayed(object : Runnable {
            override fun run() {
                currentCountdown--
                if (currentCountdown == 0) {
                    showToast("Tempo limite atingido")
                    finish()
                    return
                }
                countdownGeralHandler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    // Iniciar contagem regressiva do botão
    private fun startButtonCountdown(countdownTime: Int) {
        var currentCountdown = countdownTime
        countdownTextView.visibility = View.VISIBLE
        countdownHandler.removeCallbacksAndMessages(null)
        countdownHandler.postDelayed(object : Runnable {
            override fun run() {
                currentCountdown--
                countdownTextView.text = "Tempo restante: $currentCountdown segundos"
                if (currentCountdown == 0) {
                    if (isBound) {
                        webSocketService?.setCurrentUserIndefinido()
                    }
                    finish()
                    return
                }
                countdownHandler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    // Enviar mensagem via WebSocket
    private fun sendMessage(message: String) {
        if (isBound) {
            webSocketService?.broadcast(message)
            showMessageSent(message)
        } else {
            showToast("Serviço WebSocket não vinculado")
        }
    }

    // Exibir mensagem enviada
    private fun showMessageSent(message: String) {
        handler.post {
            Toast.makeText(this, "Mensagem enviada: $message", Toast.LENGTH_SHORT).show()
        }
    }

    // Exibir Toast
    private fun showToast(message: String) {
        handler.post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}
