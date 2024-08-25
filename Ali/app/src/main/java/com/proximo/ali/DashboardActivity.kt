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
import android.widget.GridLayout
import android.widget.ImageView
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

import kotlinx.coroutines.*
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
    private var maquinasDisponiveis = "maquinasDisponiveis.json"
    private var maquinasPresentes = "maquinasPresentes.json"

    // Conexão ao serviço WebSocket
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as WebSocketService.LocalBinder
            webSocketService = binder.getService()
            isBound = true
            Log.d("DashboardActivity", "Serviço WebSocket vinculado com sucesso")
            webSocketService?.setCurrentEmail(apelido ?: "")
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            Log.d("DashboardActivity", "Serviço WebSocket desconectado")
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
        connectToWebSocketService() // Certifique-se de que o serviço está vinculado
        setContentView(R.layout.activity_dashboard)


        apelido = intent.getStringExtra("apelidoUsuario")
        dbHelper = DatabaseHelper(this)
        countdownTextView = findViewById(R.id.countdownTextView)

        requisitarMaquinas()
        CoroutineScope(Dispatchers.Main).launch {
            delay(2000)
            requisitarMaquinas()
        }

        // Adiciona um delay antes de chamar outras funções
        CoroutineScope(Dispatchers.Main).launch {
            delay(8000)
            createAvailableMachinesJson(this@DashboardActivity) // Mescla e carrega as máquinas
            setupUI()
            startGeneralCountdown(countdownGeral)
            // Chama a função para exibir as máquinas como cards
            displayMachinesAsCards()
        }
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
        dbHelper.deleteFile(maquinasDisponiveis)
        dbHelper.deleteFile(maquinasPresentes)
    }

    // Configuração da interface do usuário
    private fun setupUI() {
        val buttonBack: MaterialButton = findViewById(R.id.buttonBack)
        buttonBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    interface WebSocketServiceCallback {
        fun onWebSocketServiceConnected()
    }


    private fun connectToWebSocketService() {
        Intent(this, WebSocketService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
            startService(intent)
            Log.d("DashboardActivity", "Tentando conectar ao serviço WebSocket")
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
                        webSocketService?.setCurrentEmailIndefinido()
                    }
                    finish()
                    return
                }
                countdownHandler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    private fun sendMessage(message: String) {
        if (isBound) {
            webSocketService?.broadcast(message)
            Log.d("DashboardActivity", "Mensagem enviada: $message")
            showToast("Mensagem enviada: $message")  // Usando template de string para interpolação
        } else {
            Log.e("DashboardActivity", "Serviço WebSocket não vinculado na DashBoard")
            showToast("Serviço WebSocket não vinculado na DashBoard")
        }
    }

    private fun requisitarMaquinas() {
        val accioMachine = JSONObject().apply {
            put("accio_machine", JSONObject()) // Objeto vazio
            put("requestId", 12345678) // ID para confirmação
        }
        Log.d("DashboardActivity", "Requisitando máquinas...")
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

            // Usar um Set para rastrear RFIDs já adicionados
            val addedRfids = mutableSetOf<String>()

            // Adicionar máquinas presentes ao JSONArray disponível
            for (i in 0 until presentesArray.length()) {
                val item = presentesArray.getJSONObject(i)
                val accioMachineResponse = item.optJSONObject("accio_machine_response")
                val rfid = accioMachineResponse?.optString("rfid", null)

                if (rfid != null && !addedRfids.contains(rfid)) {
                    val model = rfidToModelMap[rfid] // Obter o modelo usando o RFID
                    if (model != null) {
                        val availableItem = JSONObject().apply {
                            put("rfid", rfid)
                            put("modelo", model)
                        }
                        availableMachinesArray.put(availableItem)
                        addedRfids.add(rfid) // Marcar o RFID como adicionado
                    }
                }
            }

            Log.d("DashboardActivity", "Máquinas combinadas: $availableMachinesArray")
            // Salvar o JSON disponível em um arquivo
            saveResponseToFile(context, availableMachinesArray.toString(), maquinasDisponiveis)

            // Exibir um Toast indicando sucesso
            showToast("Máquinas disponíveis salvas com sucesso.")

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


    private fun displayMachinesAsCards() {
        val gridLayout: GridLayout = findViewById(R.id.gridLayoutMachines)

        try {
            // Ler o arquivo JSON maquinasDisponiveis.json
            val file = File(filesDir, maquinasDisponiveis)
            val availableMachinesData = file.readText()
            val availableMachinesArray = JSONArray(availableMachinesData)

            // Iterar sobre os dados das máquinas disponíveis
            for (i in 0 until availableMachinesArray.length()) {
                val machine = availableMachinesArray.getJSONObject(i)
                val modelName = machine.getString("modelo")
                val rfid = machine.getString("rfid") // Obtenha o RFID da máquina

                // Criar um novo card dinamicamente
                val cardView = layoutInflater.inflate(R.layout.item_machine_card, gridLayout, false) as androidx.cardview.widget.CardView

                // Configurar o nome da máquina no TextView
                val textViewMachineName: TextView = cardView.findViewById(R.id.textViewMachineName)
                textViewMachineName.text = modelName

                // Configurar a imagem da máquina no ImageView
                val imageViewMachine: ImageView = cardView.findViewById(R.id.imageViewMachine)
                val drawableName = modelName.replace(" ", "").lowercase()
                val drawableResId = ImageMap.getDrawableResId(drawableName)
                imageViewMachine.setImageResource(drawableResId)

                // Adicionar o listener de clique no card
                cardView.setOnClickListener {
                    // Iniciar contagem regressiva do botão
                    startButtonCountdown(countdownBotao)

                    // Criar a mensagem JSON
                    val jsonMessage = JSONObject().apply {
                        put("command", "activate")
                        put("rfid", rfid) // Usar o RFID da máquina
                        put("message_id", 12345678) // ID fixo ou gerado dinamicamente
                    }

                    // Enviar a mensagem pelo WebSocket
                    sendMessage(jsonMessage.toString())
                }

                // Adicionar o card ao GridLayout
                gridLayout.addView(cardView)
            }

        } catch (e: JSONException) {
            showToast("Erro ao processar JSON: ${e.message}")
        } catch (e: IOException) {
            showToast("Erro ao ler o arquivo: ${e.message}")
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
