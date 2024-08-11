package com.example.ali
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.*
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class TecActivity : AppCompatActivity() {

    private var webSocketService: WebSocketService? = null
    private var isBound = false

    private lateinit var logRecyclerView: RecyclerView
    private lateinit var logAdapter: LogAdapter
    private val logList = mutableListOf<JSONObject>()

    // Conexão ao serviço WebSocket
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as WebSocketService.LocalBinder
            webSocketService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    // BroadcastReceiver para receber ações do serviço
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.example.ali.ACTION_SUCCESS_REMOVIDO") {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tec)
        setupUI()
        connectToWebSocketService()
        loadLogData()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(broadcastReceiver, IntentFilter("com.example.ali.ACTION_SUCCESS_REMOVIDO"))
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
    }

    // Configuração da interface do usuário
    private fun setupUI() {
        val logButton: Button = findViewById(R.id.buttonGenerateLog)
        logButton.setOnClickListener {
            // Aqui você pode adicionar lógica para gerar um novo log
            // e atualizar a RecyclerView
            loadLogData()
        }

        val buttonBack: Button = findViewById(R.id.buttonBack)
        buttonBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        logRecyclerView = findViewById(R.id.log_recycler_view)
        logRecyclerView.layoutManager = LinearLayoutManager(this)
        logAdapter = LogAdapter(logList)
        logRecyclerView.adapter = logAdapter
    }

    // Conectar ao serviço WebSocket
    private fun connectToWebSocketService() {
        Intent(this, WebSocketService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
            startService(intent)
        }
    }

    // Carregar dados do log
    private fun loadLogData() {
        val logFile = File(filesDir, "log.json")
        if (logFile.exists()) {
            try {
                val jsonString = logFile.readText()
                val jsonArray = JSONArray(jsonString)
                logList.clear()
                for (i in 0 until jsonArray.length()) {
                    logList.add(jsonArray.getJSONObject(i))
                }
                logAdapter.notifyDataSetChanged()
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Erro ao carregar logs")
            }
        } else {
            showToast("Arquivo de log não encontrado")
        }
    }

    // Exibir Toast
    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}
