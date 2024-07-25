package com.example.ali

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var editTextUsername: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var buttonRegister: Button
    private lateinit var dbHelper: DatabaseHelper
    private var webSocketService: WebSocketService? = null
    private var isBound = false

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar as views
        editTextUsername = findViewById(R.id.editTextUsername)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonLogin = findViewById(R.id.buttonLogin)
        buttonRegister = findViewById(R.id.buttonRegister)
        dbHelper = DatabaseHelper(this)

        // Configurar OnClickListener para o botão Login
        buttonLogin.setOnClickListener {
            val apelido = editTextUsername.text.toString()
            val senha = editTextPassword.text.toString()

            // Limpar os campos de usuário e senha
            editTextUsername.text.clear()
            editTextPassword.text.clear()

            // Verificar se o usuário e a senha estão vazios
            if (apelido.isNotEmpty() && senha.isNotEmpty()) {
                // Verificar se as credenciais são de administrador
                if (apelido == "admin" && senha == "admin") {
                    val intent = Intent(this, AdminActivity::class.java)
                    startActivity(intent)
                } else {
                    // Verificar as credenciais no banco de dados
                    val isValidCredentials = dbHelper.verificarCredenciais(apelido, senha)

                    if (isValidCredentials) {
                        // Passar o nome de usuário para a próxima atividade
                        val intent = Intent(this, DashboardActivity::class.java)
                        intent.putExtra("apelidoUsuario", apelido) // Passando o nome de usuário como extra
                        startActivity(intent)
                    } else {
                        // Se as credenciais forem inválidas, exibir uma mensagem de erro
                        Toast.makeText(this, "Usuário ou senha inválidos", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Se o usuário ou a senha estiverem vazios, exibir uma mensagem de erro
                Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show()
            }
        }

        // Configurar OnClickListener para o botão Registro
        buttonRegister.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)

            // Limpar os campos de usuário e senha
            editTextUsername.text.clear()
            editTextPassword.text.clear()
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
        // Remover chamada para unbindService para garantir que o serviço continue rodando
    }
}
