package com.proximo.ali

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
import android.util.Log



class MainActivity : AppCompatActivity() {

    private lateinit var editTextEmail: EditText
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
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonLogin = findViewById(R.id.buttonLogin)
        dbHelper = DatabaseHelper(this)

        // Iniciar o WebSocketService
        Intent(this, WebSocketService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
            startService(intent)
        }

        // Iniciar o SyncService
        Intent(this, SyncService::class.java).also { intent ->
            startService(intent)
        }

        // Configurar OnClickListener para o botão Login
        buttonLogin.setOnClickListener {
            val email = editTextEmail.text.toString()
            val pin = editTextPassword.text.toString()

            // Limpar os campos de email e pin
            editTextEmail.text.clear()
            editTextPassword.text.clear()

            // Verificar se o email e a pin estão vazios
            if (email.isNotEmpty() && pin.isNotEmpty()) {
                // Verificar se as credenciais são de administrador
                if (email == "0001" && pin == "8659") {
                    val intentAdmin = Intent(this, AdminActivity::class.java)
                    startActivity(intentAdmin)
                }
                else if(email == "0000" && pin == "8659"){
                    val intentTec = Intent(this, TecActivity::class.java)
                    startActivity(intentTec)
                }

                else {
                    // Verificar as credenciais no banco de dados
                    val isValidCredentials = dbHelper.verificarCredenciais(email, pin)
                    // Adicionar o log com as credenciais
                    Log.d("LoginCredentials", "Email: $email, Senha: $pin")

                    if (isValidCredentials) {
                        // Passar o nome de email para a próxima atividade
                        val intent = Intent(this, DashboardActivity::class.java)
                        intent.putExtra("emailUsuario", email) // Passando o nome de email como extra
                        startActivity(intent)

                    } else {
                        // Se as credenciais forem inválidas, exibir uma mensagem de erro
                        Toast.makeText(this, "E-mail ou Senha inválidos", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Se o email ou a pin estiverem vazios, exibir uma mensagem de erro
                Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show()
            }
        }


    }

    override fun onStart() {
        super.onStart()

    }

    override fun onStop() {
        super.onStop()
    }
}
