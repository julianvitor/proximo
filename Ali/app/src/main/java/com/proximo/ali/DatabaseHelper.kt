package com.proximo.ali

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast

import org.json.JSONArray
import org.json.JSONObject

import java.io.File
import java.io.IOException
import java.util.*

data class UsuarioUsoInfo(
    val email: String,
    val retirada: String,
    val devolucao: String,

)

class DatabaseHelper(context: Context) {
    private val expressSyncFileName = "syncGeral.json"
    private val usersFileName = "syncGeral.json"
    private val removalsFileName = "removals.json"
    private val returnsFileName = "returns.json"
    private val handler = Handler(Looper.getMainLooper())
    private val context: Context = context.applicationContext

    fun registrarDevolucao(rfid: String) {
        Log.d("DatabaseHelper", "Registrando devolução para UID: $rfid")

        val usosJson = loadJsonFromFile(removalsFileName) ?: JSONObject()
        val usosArray = usosJson.optJSONArray("usos") ?: JSONArray()

        var devolucaoRegistrada = false

        for (i in 0 until usosArray.length()) {
            val uso = usosArray.optJSONObject(i)
            if (uso.optString("rfid") == rfid && !uso.has("devolucao")) {
                uso.put("devolucao", getDataHoraAtual())
                devolucaoRegistrada = true
                Log.d("DatabaseHelper", "Devolução registrada para UID: $rfid")
            }
        }

        writeJsonToFile(usosJson.toString(), removalsFileName)
        if (!devolucaoRegistrada) {
            Log.w("DatabaseHelper", "Nenhuma devolução registrada para UID: $rfid")
        }
    }

    fun registrarUso(email: String, rfid: String, doca: String) {
        Log.d("DatabaseHelper", "Registrando uso para o email: $email e UID: $rfid")

        val usosJson = loadJsonFromFile(removalsFileName) ?: JSONObject()
        val usosArray = usosJson.optJSONArray("usos") ?: JSONArray()

        val novoUso = JSONObject()
        novoUso.put("email", email)
        novoUso.put("retirada", getDataHoraAtual())
        novoUso.put("rfid", rfid)
        novoUso.put("doca", doca)
        usosArray.put(novoUso)

        usosJson.put("usos", usosArray)
        writeJsonToFile(usosJson.toString(), removalsFileName)

        Log.d("DatabaseHelper", "Uso registrado para o email: $email e UID: $rfid")
    }

    fun verificarCredenciais(email: String, pin: String): Boolean {
        Log.d("DatabaseHelper", "Verificando credenciais para o email: $email")

        // Carregar JSON do arquivo
        val usuariosJson = loadJsonFromFile(usersFileName)
        if (usuariosJson == null) {
            Log.w("DatabaseHelper", "Arquivo JSON não encontrado ou erro ao carregar.")
            return false
        }

        // Obter o JSONArray de usuários
        val attributesJson = usuariosJson.optJSONObject("attributes")
        val usuariosArray = attributesJson?.optJSONArray("users") ?: JSONArray()

        // Procurar as credenciais do usuário
        for (i in 0 until usuariosArray.length()) {
            val usuario = usuariosArray.optJSONObject(i)
            if (usuario != null) {
                val userEmail = usuario.optString("email", "")
                val userPin = usuario.optString("pin", "")

                // Verificar se o email e o pin correspondem
                if (userEmail == email && userPin == pin) {
                    Log.d("DatabaseHelper", "Credenciais válidas para o email: $email")
                    return true
                }
            }
        }

        Log.w("DatabaseHelper", "Credenciais inválidas para o email: $email")
        return false
    }

    fun getUsuariosERetiradasDevolucao(): List<UsuarioUsoInfo> {
        Log.d("DatabaseHelper", "Obtendo usuários e retiradas/devoluções")

        val usosJson = loadJsonFromFile(removalsFileName) ?: JSONObject()
        val usosArray = usosJson.optJSONArray("usos") ?: JSONArray()

        val usuariosERetiradasDevolucao = mutableListOf<UsuarioUsoInfo>()

        for (i in 0 until usosArray.length()) {
            val uso = usosArray.optJSONObject(i)
            val email = uso.optString("email")
            val retirada = uso.optString("retirada")
            val devolucao = uso.optString("devolucao", "")
            val rfid = uso.optString("rfid")

            Log.d("DatabaseHelper", "Obtendo informações para o email: $email")

            usuariosERetiradasDevolucao.add(
                UsuarioUsoInfo(email, retirada, devolucao)
            )
        }

        Log.d("DatabaseHelper", "Total de usuários e retiradas/devoluções obtidos: ${usuariosERetiradasDevolucao.size}")

        return usuariosERetiradasDevolucao
    }

    fun addToMaquinasPresentes(machineJson: JSONObject) {
        Log.d("DatabaseHelper", "Adicionando maquina ao arquivo maquinasPresentes.json")

        val fileName = "maquinasPresentes.json"
        val file = File(context.filesDir, fileName)
        try {
            if (file.exists()) {
                val existingContent = file.readText()
                val jsonArray = if (existingContent.isNotEmpty()) { JSONArray(existingContent) } else { JSONArray() }
                jsonArray.put(machineJson)
                writeJsonToFile(jsonArray.toString(4), fileName)
                Log.i("DatabaseHelper", "Salvo em maquinasPresentes.json")
            }
            else {
                Log.i("DatabaseHelper","maquinasPresentes.json não existe")
                val jsonArray = JSONArray()
                jsonArray.put(machineJson)
                writeJsonToFile(jsonArray.toString(4), fileName)
                Log.i("DatabaseHelper","Criado e salvo em maquinasPresentes.json")
            }
        } catch (e: IOException) {
            Log.e("DatabaseHelper", "Erro ao adicionar máquina ao arquivo maquinasPresentes.json", e)
        }
    }

    fun addLogToFile(logJson: JSONObject) {
        Log.d("DatabaseHelper", "Adicionando log ao arquivo log.json")

        val logFileName = "log.json"
        val file = File(context.filesDir, logFileName)
        try {
            if (file.exists()) {
                val existingContent = file.readText()
                val jsonArray = if (existingContent.isNotEmpty()) { JSONArray(existingContent) } else { JSONArray() }

                jsonArray.put(logJson)

                writeJsonToFile(jsonArray.toString(4), logFileName)
            } else {
                val jsonArray = JSONArray()
                jsonArray.put(logJson)

                writeJsonToFile(jsonArray.toString(4), logFileName)
            }
        } catch (e: IOException) {
            Log.e("DatabaseHelper", "Erro ao adicionar log ao arquivo log.json", e)
        }
    }

    fun deleteFile(fileName: String) {
        Log.d("DatabaseHelper", "Deletando arquivo: $fileName")

        val file = File(context.filesDir, fileName)

        if (file.exists()) {
            val deleted = file.delete()

            if (deleted) {
                Log.d("DatabaseHelper", "Arquivo '$fileName' deletado com sucesso.")
            } else {
                Log.w("DatabaseHelper", "Falha ao deletar o arquivo '$fileName'.")
            }
        } else {
            Log.w("DatabaseHelper", "Arquivo '$fileName' não encontrado.")
        }
    }

    fun usuarioExiste(email: String): Boolean {
        val usuariosJson = loadJsonFromFile(usersFileName)
        usuariosJson?.let {
            val usuariosArray = it.optJSONObject("attributes")?.optJSONArray("users")
            usuariosArray?.let { array ->
                for (i in 0 until array.length()) {
                    val usuario = array.optJSONObject(i)
                    if (usuario.optString("email") == email) {
                        return true
                    }
                }
            }
        }
        return false
    }

    fun cadastrarUsuario(usuario: JSONObject) {
        val usuariosJson = loadJsonFromFile(usersFileName) ?: JSONObject().apply {
            put("attributes", JSONObject().apply {
                put("users", JSONArray())
            })
        }
        val attributesJson = usuariosJson.optJSONObject("attributes") ?: JSONObject().apply {
            put("users", JSONArray())
        }
        val usuariosArray = attributesJson.optJSONArray("users") ?: JSONArray()
        usuariosArray.put(usuario)
        attributesJson.put("users", usuariosArray)
        usuariosJson.put("attributes", attributesJson)
        writeJsonToFile(usuariosJson.toString(), usersFileName)
    }

    private fun loadJsonFromFile(fileName: String): JSONObject? {
        Log.d("DatabaseHelper", "Carregando JSON do arquivo: $fileName")

        val file = File(context.filesDir, fileName)
        return if (file.exists()) {
            try {
                val bufferedReader = file.bufferedReader()
                val jsonString = bufferedReader.use { it.readText() }
                JSONObject(jsonString)
            } catch (e: IOException) {
                Log.e("DatabaseHelper", "Erro ao ler o arquivo JSON: $fileName", e)
                null
            }
        } else {
            Log.w("DatabaseHelper", "Arquivo JSON não encontrado: $fileName")
            null
        }
    }

    private fun writeJsonToFile(jsonData: String, fileName: String) {
        Log.d("DatabaseHelper", "Escrevendo JSON no arquivo: $fileName")

        try {
            val fileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE)
            fileOutputStream.write(jsonData.toByteArray())
            fileOutputStream.close()
        } catch (e: IOException) {
            Log.e("DatabaseHelper", "Erro ao escrever JSON no arquivo: $fileName", e)
        }
    }

    private fun getDataHoraAtual(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // Mês é zero baseado
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)

        return String.format("%04d-%02d-%02d %02d:%02d:%02d", year, month, day, hour, minute, second)
    }
}
