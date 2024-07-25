@file:Suppress("DEPRECATION")

package com.example.ali

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager.LayoutParams
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import com.google.android.material.button.MaterialButton

class RegistroActivity : AppCompatActivity() {

    private lateinit var editTextNome: EditText
    private lateinit var editTextUsuario: EditText
    private lateinit var editTextSenha: EditText
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
        editTextUsuario = findViewById(R.id.editTextUsuario)
        editTextSenha = findViewById(R.id.editTextSenha)
        editTextCnpj = findViewById(R.id.editTextCnpj)
        editTextNomeEmpresa = findViewById(R.id.editTextNomeEmpresa)
        buttonRegistrar = findViewById(R.id.buttonRegistrar)

        // Inicializar o DBHelper
        dbHelper = DatabaseHelper(this)

        // Configurar OnClickListener para o botão Registrar
        buttonRegistrar.setOnClickListener {
            registrar()
        }

        //botão voltar
        val buttonBack: MaterialButton = findViewById(R.id.buttonBack)
        buttonBack.setOnClickListener{
            onBackPressedDispatcher.onBackPressed()
        }

    }

    private fun registrar() {
        val nome = editTextNome.text.toString().trim()
        val apelido = editTextUsuario.text.toString().trim()
        val senha = editTextSenha.text.toString().trim()
        val cnpj = editTextCnpj.text.toString().trim()
        val nomeEmpresa = editTextNomeEmpresa.text.toString().trim()

        // Validar os campos
        if (nome.isEmpty() || apelido.isEmpty() || senha.isEmpty() || cnpj.isEmpty() || nomeEmpresa.isEmpty()) {
            Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Verificar se o usuário já existe no arquivo JSON
            if (usuarioExiste(apelido)) {
                Toast.makeText(this, "Usuário já cadastrado. Por favor, escolha outro nome de usuário.", Toast.LENGTH_SHORT).show()
                return
            }

            // Criar um novo objeto de usuário
            val novoUsuario = JSONObject()
            novoUsuario.put("nome", nome)
            novoUsuario.put("apelido", apelido)
            novoUsuario.put("senha", senha)
            novoUsuario.put("cnpj", cnpj)
            novoUsuario.put("nome_empresa", nomeEmpresa)

            // Adicionar o novo usuário ao arquivo JSON de usuários
            adicionarUsuarioAoJSON(novoUsuario)

            Toast.makeText(this, "Registro bem-sucedido!", Toast.LENGTH_SHORT).show()
            // Limpar os campos após o registro bem-sucedido
            editTextNome.text.clear()
            editTextUsuario.text.clear()
            editTextSenha.text.clear()
            editTextCnpj.text.clear()
            editTextNomeEmpresa.text.clear()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

        } catch (e: Exception) {
            // Exibir mensagem de erro caso ocorra uma exceção
            Toast.makeText(this, "Erro ao registrar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun usuarioExiste(apelido: String): Boolean {
        val usuariosJson = loadJsonFromFile(dbHelper.usuariosFileName)
        usuariosJson?.let {
            val usuariosArray = it.optJSONArray("usuarios")
            usuariosArray?.let {
                for (i in 0 until usuariosArray.length()) {
                    val usuario = usuariosArray.optJSONObject(i)
                    if (usuario.optString("apelido") == apelido) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun adicionarUsuarioAoJSON(usuario: JSONObject) {
        val usuariosJson = loadJsonFromFile(dbHelper.usuariosFileName) ?: JSONObject()
        val usuariosArray = usuariosJson.optJSONArray("usuarios") ?: JSONArray()
        usuariosArray.put(usuario)
        usuariosJson.put("usuarios", usuariosArray)
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
}
