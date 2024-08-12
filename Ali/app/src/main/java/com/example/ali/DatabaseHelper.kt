package com.example.ali

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.util.*

data class Quadra<T, U, V, W>(val first: T, val second: U, val third: V, val fourth: W)

class DatabaseHelper(context: Context) {
    val usuariosFileName = "usuarios.json"
    val usosFileName = "usos.json"

    val context: Context = context.applicationContext

    fun registrarDevolucao(uid: String) {
        val usosJson = loadJsonFromFile(usosFileName) ?: JSONObject()
        val usosArray = usosJson.optJSONArray("usos") ?: JSONArray()

        var devolucaoRegistrada = false

        // Iterar sobre todos os registros para encontrar aqueles com o UID correspondente
        for (i in 0 until usosArray.length()) {
            val uso = usosArray.optJSONObject(i)
            if (uso.optString("uid") == uid && !uso.has("devolucao")) {
                // Registrar a devolução para este registro apenas se a devolução ainda não foi registrada
                uso.put("devolucao", getDataHoraAtual())
                devolucaoRegistrada = true
            }
        }

        // Salvar as alterações no arquivo JSON
        writeJsonToFile(usosJson.toString(), usosFileName)

        // Verificar se a devolução foi registrada
        if (!devolucaoRegistrada) {
            // Lidar com o caso em que nenhum registro com o UID correspondente ou já tem devolução registrada
            // Aqui você pode lançar uma exceção, enviar uma mensagem de erro ou tomar outra ação adequada ao seu aplicativo
            return
        }
    }

    fun getCnpjByEmail(email: String): String {
        val usuariosJson = loadJsonFromFile(usuariosFileName) ?: JSONObject()
        val usuariosArray = usuariosJson.optJSONArray("attributes") ?: JSONArray()

        for (i in 0 until usuariosArray.length()) {
            val usuario = usuariosArray.optJSONObject(i)
            if (usuario.optString("email") == email) {
                return usuario.optString("cnpj", "CNPJ não disponível")
            }
        }
        return "CNPJ não encontrado"
    }

    fun getNomeEmpresaByEmail(email: String): String {
        val usuariosJson = loadJsonFromFile(usuariosFileName) ?: JSONObject()
        val usuariosArray = usuariosJson.optJSONArray("attributes") ?: JSONArray()

        for (i in 0 until usuariosArray.length()) {
            val usuario = usuariosArray.optJSONObject(i)
            if (usuario.optString("email") == email) {
                return usuario.optString("commercial_name", "Nome da Empresa não disponível")
            }
        }
        return "Nome da Empresa não encontrado"
    }

    fun registrarUso(email: String, uid: String, doca: String) {
        val usosJson = loadJsonFromFile(usosFileName) ?: JSONObject()
        val usosArray = usosJson.optJSONArray("usos") ?: JSONArray()

        val novoUso = JSONObject()
        novoUso.put("email", email)
        novoUso.put("retirada", getDataHoraAtual())
        novoUso.put("uid", uid)
        novoUso.put("doca", doca)
        usosArray.put(novoUso)

        // Salvar as alterações no arquivo JSON
        usosJson.put("usos", usosArray)
        writeJsonToFile(usosJson.toString(), usosFileName)
    }

    fun verificarCredenciais(email: String, pin: String): Boolean {
        val usuariosJson = loadJsonFromFile(usuariosFileName) ?: JSONObject()
        val usuariosArray = usuariosJson.optJSONArray("attributes") ?: JSONArray()

        // Verificar se há um usuário com as credenciais fornecidas
        for (i in 0 until usuariosArray.length()) {
            val usuario = usuariosArray.optJSONObject(i)
            if (usuario.optString("email") == email && usuario.optString("pin") == pin) {
                return true
            }
        }
        return false
    }

    fun getUsuariosERetiradas(): List<Pair<String, String>> {
        val usosJson = loadJsonFromFile(usosFileName) ?: JSONObject()
        val usosArray = usosJson.optJSONArray("usos") ?: JSONArray()

        val usuariosERetiradas = mutableListOf<Pair<String, String>>()

        for (i in 0 until usosArray.length()) {
            val uso = usosArray.optJSONObject(i)
            val email = uso.optString("email")
            val retirada = uso.optString("retirada")
            usuariosERetiradas.add(Pair(email, retirada))
        }

        return usuariosERetiradas
    }

    fun getUsuariosERetiradasDevolucao(): List<Quadra<String, String, String, String>> {
        val usosJson = loadJsonFromFile(usosFileName) ?: JSONObject()
        val usosArray = usosJson.optJSONArray("usos") ?: JSONArray()

        val usuariosERetiradasDevolucao = mutableListOf<Quadra<String, String, String, String>>()

        for (i in 0 until usosArray.length()) {
            val uso = usosArray.optJSONObject(i)
            val email = uso.optString("email")
            val retirada = uso.optString("retirada")
            val devolucao = uso.optString("devolucao", "") // Se não houver devolução, retorna uma string vazia
            val uid = uso.optString("uid")

            // Agora, em vez de CNPJ e nome da empresa separadamente, podemos concatenar essas informações
            val cnpjENomeEmpresa = "${getCnpjByEmail(email)} - ${getNomeEmpresaByEmail(email)}"

            usuariosERetiradasDevolucao.add(Quadra(email, retirada, devolucao, cnpjENomeEmpresa))
        }

        return usuariosERetiradasDevolucao
    }

    private fun loadJsonFromAsset(fileName: String): String? {
        return try {
            val inputStream = context.assets.open(fileName)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charset.defaultCharset())
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun addLogToFile(logJson: JSONObject) {
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
            e.printStackTrace()
        }
    }

    fun deleteFile(fileName: String) {
        val file = File(context.filesDir, fileName)

        if (file.exists()) {
            // Tenta deletar o arquivo
            val deleted = file.delete()

            // Verifica se a exclusão foi bem-sucedida
            if (deleted) {
                println("Arquivo '$fileName' deletado com sucesso.")
            } else {
                println("Falha ao deletar o arquivo '$fileName'.")
            }
        } else {
            println("Arquivo '$fileName' não encontrado.")
        }
    }

    private fun writeJsonToFile(jsonData: String, fileName: String) {
        try {
            val fileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE)
            fileOutputStream.write(jsonData.toByteArray())
            fileOutputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun loadJsonFromFile(fileName: String): JSONObject? {
        val file = File(context.filesDir, fileName)
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

    private fun getDataHoraAtual(): String {
        val calendar = Calendar.getInstance()
        return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)} " +
                "${calendar.get(Calendar.HOUR_OF_DAY)}:${calendar.get(Calendar.MINUTE)}:${calendar.get(Calendar.SECOND)}"
    }
}
