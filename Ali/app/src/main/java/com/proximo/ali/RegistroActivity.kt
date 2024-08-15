@file:Suppress("DEPRECATION")

package com.proximo.ali

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager.LayoutParams
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.Calendar


class RegistroActivity : AppCompatActivity() {

    private lateinit var editTextNome: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPin: EditText
    private lateinit var editTextCnpj: EditText
    private lateinit var editTextNomeEmpresa: EditText
    private lateinit var buttonRegistrar: Button
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        // Impedir que o teclado virtual sobreponha o layout
        window.setSoftInputMode(LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        // Inicializar as views
        editTextNome = findViewById(R.id.editTextNome)
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPin = findViewById(R.id.editTextPin)
        editTextCnpj = findViewById(R.id.editTextCnpj)
        editTextNomeEmpresa = findViewById(R.id.editTextNomeEmpresa)
        buttonRegistrar = findViewById(R.id.buttonRegistrar)

        // Inicializar o DBHelper
        dbHelper = DatabaseHelper(this)

        // Configurar OnClickListener para o botão Registrar
        buttonRegistrar.setOnClickListener {
            registrar()
        }

        // Botão voltar
        val buttonBack: MaterialButton = findViewById(R.id.buttonBack)
        buttonBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun registrar() {
        val nome = editTextNome.text.toString().trim()
        val email = editTextEmail.text.toString().trim()
        val pin = editTextPin.text.toString().trim()
        val cnpj = editTextCnpj.text.toString().trim()
        val nomeEmpresa = editTextNomeEmpresa.text.toString().trim()

        // Validar os campos
        if (nome.isEmpty() || email.isEmpty() || pin.isEmpty() || cnpj.isEmpty() || nomeEmpresa.isEmpty()) {
            Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Verificar se o usuário já existe no arquivo JSON
            if (usuarioExiste(email)) {
                Toast.makeText(this, "E-mail já cadastrado. Por favor, escolha outro E-mail.", Toast.LENGTH_SHORT).show()
                return
            }

            // Criar um novo objeto de usuário
            val novoUsuario = JSONObject().apply {
                put("id", UUID.randomUUID().toString())  // Adiciona um ID único
                put("created_at", getDataHoraAtual())    // Adiciona a data e hora atual
                put("updated_at", getDataHoraAtual())    // Adiciona a data e hora atual
                put("cpf", null)
                put("cnpj", cnpj)
                put("commercial_name", null)
                put("email", email)
                put("isAdmin", false)
                put("doTraining", false)
                put("isQualified", false)
                put("isLiabilityTermAccepted", false)
                put("environments", JSONArray().put("EXPRESS"))
                put("pin", pin)
                put("status", "ACTIVE")
                put("passwordChangeRequested", false)
                put("companyId", UUID.randomUUID().toString())  // Adiciona um ID de empresa fictício
                put("category", null)
            }

            // Adicionar o novo usuário ao arquivo JSON de usuários
            adicionarUsuarioAoJSON(novoUsuario)

            Toast.makeText(this, "Registro bem-sucedido!", Toast.LENGTH_SHORT).show()
            // Limpar os campos após o registro bem-sucedido
            editTextNome.text.clear()
            editTextEmail.text.clear()
            editTextPin.text.clear()
            editTextCnpj.text.clear()
            editTextNomeEmpresa.text.clear()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

        } catch (e: Exception) {
            // Exibir mensagem de erro caso ocorra uma exceção
            Toast.makeText(this, "Erro ao registrar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun usuarioExiste(email: String): Boolean {
        val usuariosJson = loadJsonFromFile(dbHelper.usuariosFileName)
        usuariosJson?.let {
            val usuariosArray = it.optJSONArray("attributes")
            usuariosArray?.let {
                for (i in 0 until usuariosArray.length()) {
                    val usuario = usuariosArray.optJSONObject(i)
                    if (usuario.optString("email") == email) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun adicionarUsuarioAoJSON(usuario: JSONObject) {
        val usuariosJson = loadJsonFromFile(dbHelper.usuariosFileName) ?: JSONObject().apply {
            put("attributes", JSONArray())
        }
        val usuariosArray = usuariosJson.optJSONArray("attributes") ?: JSONArray()
        usuariosArray.put(usuario)
        usuariosJson.put("attributes", usuariosArray)
        writeJsonToFile(usuariosJson.toString(), dbHelper.usuariosFileName)
    }

    private fun loadJsonFromFile(fileName: String): JSONObject? {
        val file = File(filesDir, fileName)
        return if (file.exists()) {
            try {
                val bufferedReader = file.bufferedReader()
                val jsonString = bufferedReader.use { it.readText() }
                JSONObject(jsonString)
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }

    private fun writeJsonToFile(jsonData: String, fileName: String) {
        try {
            val fileOutputStream = openFileOutput(fileName, MODE_PRIVATE)
            fileOutputStream.write(jsonData.toByteArray())
            fileOutputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun getDataHoraAtual(): String {
        val calendar = Calendar.getInstance()
        return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)} " +
                "${calendar.get(Calendar.HOUR_OF_DAY)}:${calendar.get(Calendar.MINUTE)}:${calendar.get(Calendar.SECOND)}"
    }
}
