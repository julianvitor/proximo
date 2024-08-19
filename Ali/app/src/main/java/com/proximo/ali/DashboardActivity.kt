package com.proximo.ali

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class DashboardActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var apelido: String? = null
    private var countdownBotao: Int = 30
    private var countdownGeral: Int = 120
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

        requisitarMaquinas() // Requisita as máquinas aos filhos e salva em maquinasPresentes.json
        // Adiciona um delay antes de chamar outras funções
        Handler(Looper.getMainLooper()).postDelayed({
            createAvailableMachinesJson(this) // Mescla e carrega as máquinas
            setupUI()
            connectToWebSocketService()
            startGeneralCountdown(countdownGeral)
        }, 10000)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(
            broadcastReceiver,
            IntentFilter("com.example.com.proximo.ali.ACTION_SUCCESS_REMOVIDO")
        )
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
        val buttonBack: MaterialButton = findViewById(R.id.buttonBack)
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
            showToast("Erro: Serviço WebSocket não vinculado")
        }
    }

    private fun requisitarMaquinas() {
        val accioMachine = JSONObject().apply {
            put("accio_machine", JSONObject()) // Objeto vazio
            put("requestId", 12345678) // ID para confirmação
        }
        showToast("Requisitando máquinas...")
        sendMessage(accioMachine.toString())
    }

    fun createAvailableMachinesJson(context: Context) {
        try {
            // Ler arquivos JSON
            val catalogFile = File(context.filesDir, "maquinasCatalog.json")
            val presentesFile = File(context.filesDir, "maquinasPresentes.json")

            val catalogData = catalogFile.readText()
            val presentesData = presentesFile.readText()

            // Converter strings JSON em objetos JSONArray
            val catalogArray = JSONArray(catalogData)
            val presentesArray = JSONArray(presentesData)

            Log.d("DashboardActivity", "Catálogo JSON: $catalogData")
            Log.d("DashboardActivity", "Presentes JSON: $presentesData")

            // Criar um mapa para armazenar RFID para modelo
            val rfidToModelMap = mutableMapOf<String, String>()

            // Adicionar máquinas do catálogo ao mapa, verificando RFIDs duplicados
            for (i in 0 until catalogArray.length()) {
                val item = catalogArray.getJSONObject(i)
                val rfid = item.optString("rfid", null)
                val name = item.optString("name", null)

                if (rfid != null && name != null) {
                    if (rfidToModelMap.containsKey(rfid)) {
                        // Log e Toast informando sobre o RFID duplicado
                        val errorMessage = "Erro: RFID duplicado encontrado no catálogo: $rfid"
                        Log.e("DashboardActivity", errorMessage)
                        showToast("Erro nos dados da API: RFID duplicado encontrado")
                    } else {
                        // Adiciona ao mapa apenas se o RFID não for duplicado
                        rfidToModelMap[rfid] = name
                    }
                }
            }

            // Criar um JSONArray para armazenar máquinas disponíveis
            val availableMachinesArray = JSONArray()

            // Adicionar máquinas presentes ao JSONArray disponível
            for (i in 0 until presentesArray.length()) {
                val item = presentesArray.getJSONObject(i)
                val accioMachineResponse = item.optJSONObject("accio_machine_response")
                val rfid = accioMachineResponse?.optString("rfid", null)
                if (rfid != null) {
                    val model = rfidToModelMap[rfid] // Obter o modelo usando o RFID
                    if (model != null) {
                        val availableItem = JSONObject().apply {
                            put("rfid", rfid)
                            put("modelo", model)
                        }
                        availableMachinesArray.put(availableItem)
                    }
                }
            }

            Log.d("DashboardActivity", "Máquinas combinadas: $availableMachinesArray")
            // Salvar o JSON disponível em um arquivo
            saveResponseToFile(context, availableMachinesArray.toString(), "MaquinasDisponiveis.json")

            // Exibir um Toast indicando sucesso
            showToast( "Máquinas disponíveis salvas com sucesso.")

        } catch (e: JSONException) {
            showToast("Erro ao processar JSON: ${e.message}")
        } catch (e: IOException) {
            showToast("Erro ao ler arquivos: ${e.message}")
        }
    }


    private fun saveResponseToFile(context: Context, data: String, fileName: String) {
        try {
            val file = File(context.filesDir, fileName)
            FileOutputStream(file).use { fos ->
                fos.write(data.toByteArray())
                fos.flush()
            }
            Log.d("DashboardActivity", "Resposta salva em $fileName")
            showToast("Resposta salva em $fileName")
        } catch (e: IOException) {
            Log.e("DashboardActivity", "Erro ao salvar o arquivo", e)
            showToast("Erro ao salvar o arquivo: ${e.message}")
        }
    }

    // Exibir mensagem enviada
    private fun showMessageSent(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, "Mensagem enviada: $message", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}
