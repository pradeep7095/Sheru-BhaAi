package com.sherubhai.assistant

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Client contains no provider API keys. Configure a trusted HTTPS server in app settings. */
object AiGateway {
    fun ask(endpoint: String, token: String, message: String, context: String): String {
        val url = URL(endpoint)
        require(url.protocol == "https" && !url.host.isNullOrBlank()) { "HTTPS endpoint required" }
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 12000
            connection.readTimeout = 30000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Authorization", "Bearer $token")
            val payload = JSONObject().put("message", message).put("context", context.take(6000))
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            if (connection.responseCode !in 200..299) throw IllegalStateException("Server HTTP ${connection.responseCode}")
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            return JSONObject(body).optString("reply").ifBlank { "The AI server returned no reply." }
        } finally { connection.disconnect() }
    }
}
