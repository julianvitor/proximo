package com.proximo.ali

import android.content.Context
import android.util.Log

import org.json.JSONArray
import org.json.JSONObject

import java.io.File
import java.io.IOException
import java.util.*

data class UsuarioUsoInfo(
    val email: String,
    val retirada: String,
    val devolucao: String,
    val cnpj: String,
    val nomeEmpresa: String
)

class DatabaseHelper(context: Context) {
    private val usuariosFileName = "syncGeral.json"
    private val usosFileName = "usos.json"

    private val context: Context = context.applicationContext

    fun registrarDevolucao(uid: String) {
        Log.d("DatabaseHelper", "Registrando devolução para UID: $uid")

        val usosJson = loadJsonFromFile(usosFileName) ?: JSONObject()
        val usosArray = usosJson.optJSONArray("usos") ?: JSONArray()

        var devolucaoRegistrada = false

        for (i in 0 until usosArray.length()) {
            val uso = usosArray.optJSONObject(i)
            if (uso.optString("uid") == uid && !uso.has("devolucao")) {
                uso.put("devolucao", getDataHoraAtual())
                devolucaoRegistrada = true
                Log.d("DatabaseHelper", "Devolução registrada para UID: $uid")
            }
        }

        writeJsonToFile(usosJson.toString(), usosFileName)

        if (!devolucaoRegistrada) {
            Log.w("DatabaseHelper", "Nenhuma devolução registrada para UID: $uid")
        }
    }

    fun getCnpjByEmail(email: String): String {
        Log.d("DatabaseHelper", "Procurando CNPJ para o email: $email")

        val usuariosJson = loadJsonFromFile(usuariosFileName) ?: JSONObject()
        val attributesJson = usuariosJson.optJSONObject("attributes") ?: JSONObject()
        val usuariosArray = attributesJson.optJSONArray("users") ?: JSONArray()

        for (i in 0 until usuariosArray.length()) {
            val usuario = usuariosArray.optJSONObject(i)
            if (usuario?.optString("email") == email) {
                val cnpj = usuario.optString("cnpj", "CNPJ não disponível")
                Log.d("DatabaseHelper", "CNPJ encontrado: $cnpj")
                return cnpj
            }
        }

        Log.w("DatabaseHelper", "CNPJ não encontrado para o email: $email")
        return "CNPJ não encontrado"
    }


    fun getNomeEmpresaByEmail(email: String): String {
        Log.d("DatabaseHelper", "Procurando nome da empresa para o email: $email")

        // Carregar JSON do arquivo
        val usuariosJson = loadJsonFromFile(usuariosFileName)
        if (usuariosJson == null) {
            Log.w("DatabaseHelper", "Arquivo JSON não encontrado ou erro ao carregar.")
            return "Nome da Empresa não encontrado"
        }

        // Obter o JSONArray de usuários
        val attributesJson = usuariosJson.optJSONObject("attributes")
        val usuariosArray = attributesJson?.optJSONArray("users") ?: JSONArray()

        // Procurar o usuário com o email fornecido
        for (i in 0 until usuariosArray.length()) {
            val usuario = usuariosArray.optJSONObject(i)
            if (usuario?.optString("email") == email) {
                // Retornar o nome da empresa ou uma mensagem padrão
                val nomeEmpresa = usuario.optString("companyId", "Nome da Empresa não disponível")
                Log.d("DatabaseHelper", "Nome da empresa encontrado: $nomeEmpresa")
                return nomeEmpresa
            }
        }

        Log.w("DatabaseHelper", "Nome da empresa não encontrado para o email: $email")
        return "Nome da Empresa não encontrado"
    }


    fun registrarUso(email: String, uid: String, doca: String) {
        Log.d("DatabaseHelper", "Registrando uso para o email: $email e UID: $uid")

        val usosJson = loadJsonFromFile(usosFileName) ?: JSONObject()
        val usosArray = usosJson.optJSONArray("usos") ?: JSONArray()

        val novoUso = JSONObject()
        novoUso.put("email", email)
        novoUso.put("retirada", getDataHoraAtual())
        novoUso.put("uid", uid)
        novoUso.put("doca", doca)
        usosArray.put(novoUso)

        usosJson.put("usos", usosArray)
        writeJsonToFile(usosJson.toString(), usosFileName)

        Log.d("DatabaseHelper", "Uso registrado para o email: $email e UID: $uid")
    }

    fun verificarCredenciais(email: String, pin: String): Boolean {
        Log.d("DatabaseHelper", "Verificando credenciais para o email: $email")

        // Carregar JSON do arquivo
        val usuariosJson = loadJsonFromFile(usuariosFileName)
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

        val usosJson = loadJsonFromFile(usosFileName) ?: JSONObject()
        val usosArray = usosJson.optJSONArray("usos") ?: JSONArray()

        val usuariosERetiradasDevolucao = mutableListOf<UsuarioUsoInfo>()

        for (i in 0 until usosArray.length()) {
            val uso = usosArray.optJSONObject(i)
            val email = uso.optString("email")
            val retirada = uso.optString("retirada")
            val devolucao = uso.optString("devolucao", "")
            val uid = uso.optString("uid")

            Log.d("DatabaseHelper", "Obtendo informações para o email: $email")

            val cnpj = getCnpjByEmail(email)
            val nomeEmpresa = getNomeEmpresaByEmail(email)

            usuariosERetiradasDevolucao.add(
                UsuarioUsoInfo(email, retirada, devolucao, cnpj, nomeEmpresa)
            )
        }

        Log.d("DatabaseHelper", "Total de usuários e retiradas/devoluções obtidos: ${usuariosERetiradasDevolucao.size}")

        return usuariosERetiradasDevolucao
    }

    fun addToMaquinasPresentes(machineJson: JSONObject) {
        Log.d("DatabaseHelper", "Adicionando máquina ao arquivo maquinasPresentes.json")

        val fileName = "maquinasPresentes.json"
        val file = File(context.filesDir, fileName)
        try {
            if (file.exists()) {
                val existingContent = file.readText()
                val jsonArray = if (existingContent.isNotEmpty()) {
                    JSONArray(existingContent)
                } else {
                    JSONArray()
                }

                jsonArray.put(machineJson)

                writeJsonToFile(jsonArray.toString(4), fileName)
            } else {
                val jsonArray = JSONArray()
                jsonArray.put(machineJson)

                writeJsonToFile(jsonArray.toString(4), fileName)
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
                val jsonArray = if (existingContent.isNotEmpty()) {
                    JSONArray(existingContent)
                } else {
                    JSONArray()
                }

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
        val usuariosJson = loadJsonFromFile(usuariosFileName)
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
        val usuariosJson = loadJsonFromFile(usuariosFileName) ?: JSONObject().apply {
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
        writeJsonToFile(usuariosJson.toString(), usuariosFileName)
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
