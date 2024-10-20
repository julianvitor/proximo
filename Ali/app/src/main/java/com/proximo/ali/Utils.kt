package com.proximo.ali

import android.content.Context
import java.util.Properties
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Utils {
    fun getApiUrl(context: Context): String {
        val properties = Properties()
        context.assets.open("config.properties").use { inputStream ->
            properties.load(inputStream)
        }
        return properties.getProperty("api_url")
    }

    fun getLoanEndpoint(context: Context): String {
        val properties = Properties()
        context.assets.open("config.properties").use { inputStream ->
            properties.load(inputStream)
        }
        return properties.getProperty("loan_api_url");
    }

    fun getDateNow(): String {
        val currentDate = Date()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        return dateFormat.format(currentDate);
    }
}