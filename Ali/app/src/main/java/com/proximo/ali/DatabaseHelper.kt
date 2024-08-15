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
    val usuariosFileName = "usuarios.json"
    val usosFileName = "usos.json"

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
        val usuariosArray = usuariosJson.optJSONArray("attributes") ?: JSONArray()

        for (i in 0 until usuariosArray.length()) {
            val usuario = usuariosArray.optJSONObject(i)
            if (usuario.optString("email") == email) {
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

        val usuariosJson = loadJsonFromFile(usuariosFileName) ?: JSONObject()
        val usuariosArray = usuariosJson.optJSONArray("attributes") ?: JSONArray()

        for (i in 0 until usuariosArray.length()) {
            val usuario = usuariosArray.optJSONObject(i)
            if (usuario.optString("email") == email) {
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

        val usuariosJson = loadJsonFromFile(usuariosFileName) ?: JSONObject()
        val usuariosArray = usuariosJson.optJSONArray("attributes") ?: JSONArray()

        for (i in 0 until usuariosArray.length()) {
            val usuario = usuariosArray.optJSONObject(i)
            if (usuario.optString("email") == email && usuario.optString("pin") == pin) {
                Log.d("DatabaseHelper", "Credenciais válidas para o email: $email")
                return true
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

    fun addLogToFile(logJson: JSONObject) {
        Log.d("DatabaseHelper", "Adicionando log ao arquivo log.json")

        val logFileName = "log.json"
        val file = File(context.filesDir, logFileName)
        try {
            if (file.exists()) {
                // Ler o conteúdo existente
                val existingContent = file.readText()
                val jsonArray = if (existingContent.isNotEmpty()) {
                    JSONArray(existingContent)
                } else {
                    JSONArray()
                }

                // Adicionar novo log
                jsonArray.put(logJson)

                // Escrever o conteúdo atualizado
                writeJsonToFile(jsonArray.toString(4), logFileName)
            } else {
                // Criar um novo arquivo com o primeiro log
                val jsonArray = JSONArray()
                jsonArray.put(logJson)

                // Escrever o novo arquivo
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
            // Tenta deletar o arquivo
            val deleted = file.delete()

            // Verifica se a exclusão foi bem-sucedida
            if (deleted) {
                Log.d("DatabaseHelper", "Arquivo '$fileName' deletado com sucesso.")
            } else {
                Log.w("DatabaseHelper", "Falha ao deletar o arquivo '$fileName'.")
            }
        } else {
            Log.w("DatabaseHelper", "Arquivo '$fileName' não encontrado.")
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
        return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)} " +
                "${calendar.get(Calendar.HOUR_OF_DAY)}:${calendar.get(Calendar.MINUTE)}:${calendar.get(Calendar.SECOND)}"
    }
}
