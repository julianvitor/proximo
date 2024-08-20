package com.proximo.ali

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.io.File
import java.io.IOException

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone


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



    // Função para enriquecer o JSON
    fun createReturnLoans(
        returnsJson: JSONObject,
        openLoansJson: JSONObject,
        maquinascatalogJson: JSONArray,
        stationsJson: JSONArray
    ): JSONObject {
        val createPatches = JSONArray()

        val returns = returnsJson.getJSONArray("returns")
        val openLoans = openLoansJson.getJSONArray("openLoans").toList()
        val maquinascatalog = maquinascatalogJson.toList()
        val stations = stationsJson.toList()

        // Itera sobre cada item do returns.json
        for (i in 0 until returns.length()) {
            val returnItem = returns.getJSONObject(i)
            val rfid = returnItem.getString("rfid")
            val endTime = returnItem.getString("end_time")
            val stationMac = returnItem.getString("station_mac")

            // Encontra a machineId correspondente no maquinascatalog.json
            val machineId = maquinascatalog.firstOrNull { it.getString("rfid") == rfid }?.getString("id")

            // Encontra o loan com a data mais recente para a machineId
            val loan = openLoans
                .filter { it.getJSONObject("attributes").getString("machineId") == machineId }
                .maxByOrNull { it.getJSONObject("attributes").getString("start_time") }

            // Encontra o stationId correspondente no stations.json
            val stationId = stations.firstOrNull { it.getString("station_mac") == stationMac }?.getString("stationId")

            if (loan != null && stationId != null) {
                // Cria o novo objeto enriquecido
                val patchItem = JSONObject()
                patchItem.put("end_time", endTime)
                patchItem.put("endStationId", stationId)
                patchItem.put("id", loan.getJSONObject("attributes").getString("id"))

                createPatches.put(patchItem)
            }
        }

        // Retorna o JSON final
        return JSONObject().put("createPatches", createPatches)
    }

    // Extensão para converter JSONArray em List<JSONObject>
    fun JSONArray.toList(): List<JSONObject> =
        (0 until length()).map { getJSONObject(it) }


    fun addToReturns(returnJson: JSONObject) {
        Log.i("DatabaseHelper", "Adicionando retorno ao returns.json")
        val returnsFileName = "returns.json"

        try {
            // Lê o conteúdo atual do arquivo returns.json
            val currentJson = loadJsonFromFile(returnsFileName)
            val currentReturnsArray = currentJson?.optJSONArray("returns") ?: JSONArray()

            // Adiciona a data e hora atual ao JSON de retorno
            val currentTime = getDataHoraAtual()
            val insertedObject = returnJson.optJSONObject("inserted") ?: JSONObject()
            insertedObject.put("end_time", currentTime)

            // Cria um novo objeto JSON para adicionar ao array de retornos
            val newReturnObject = JSONObject()
            newReturnObject.put("rfid", insertedObject.optString("rfid"))
            newReturnObject.put("station_mac", insertedObject.optString("station_mac"))
            newReturnObject.put("end_time", currentTime)

            // Adiciona o novo objeto ao array existente
            currentReturnsArray.put(newReturnObject)

            // Atualiza o JSON com o array de retornos
            val updatedJson = JSONObject()
            updatedJson.put("returns", currentReturnsArray)

            // Escreve o JSON atualizado de volta para o arquivo
            writeJsonToFile(updatedJson.toString(4), returnsFileName)

            Log.i("DatabaseHelper", "Retorno adicionado com sucesso")

        } catch (e: JSONException) {
            Log.e("DatabaseHelper", "Erro ao processar JSON", e)
        } catch (e: IOException) {
            Log.e("DatabaseHelper", "Erro ao ler ou escrever no arquivo", e)
        }
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

        Log.d(
            "DatabaseHelper",
            "Total de usuários e retiradas/devoluções obtidos: ${usuariosERetiradasDevolucao.size}"
        )

        return usuariosERetiradasDevolucao
    }

    fun addToMaquinasPresentes(machineJson: JSONObject) {
        Log.d("DatabaseHelper", "Adicionando maquina ao arquivo maquinasPresentes.json")

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
                Log.i("DatabaseHelper", "Salvo em maquinasPresentes.json")
            } else {
                Log.i("DatabaseHelper", "maquinasPresentes.json não existe")
                val jsonArray = JSONArray()
                jsonArray.put(machineJson)
                writeJsonToFile(jsonArray.toString(4), fileName)
                Log.i("DatabaseHelper", "Criado e salvo em maquinasPresentes.json")
            }
        } catch (e: IOException) {
            Log.e(
                "DatabaseHelper",
                "Erro ao adicionar máquina ao arquivo maquinasPresentes.json",
                e
            )
        }
    }

    fun addLogToFile(logJson: JSONObject) {
        Log.i("DatabaseHelper", "Adicionando log ao arquivo log.json")

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
        // Cria uma instância de Calendar
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

        // Define o formato desejado
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")

        // Retorna a data formatada
        return dateFormat.format(calendar.time)
    }
}