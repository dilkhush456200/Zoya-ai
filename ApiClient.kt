package com.zoya.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ApiClient {

    private val SYSTEM_PROMPT = """
        You are ZOYA, a voice-controlled AI assistant inspired by JARVIS.
        You are intelligent, precise, and slightly futuristic in personality.
        Keep responses concise (1-3 sentences) since they will be spoken aloud.
        Be helpful, direct, and occasionally show personality.
        Never use markdown formatting — plain text only since you are speaking.
    """.trimIndent()

    suspend fun ask(apiKey: String, history: List<Map<String, String>>): String {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://api.anthropic.com/v1/messages")
                val connection = url.openConnection() as HttpURLConnection

                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-api-key", apiKey)
                    setRequestProperty("anthropic-version", "2023-06-01")
                    doOutput = true
                    connectTimeout = 15000
                    readTimeout = 30000
                }

                val messagesArray = JSONArray()
                history.forEach { msg ->
                    messagesArray.put(
                        JSONObject().apply {
                            put("role", msg["role"])
                            put("content", msg["content"])
                        }
                    )
                }

                val body = JSONObject().apply {
                    put("model", "claude-sonnet-4-20250514")
                    put("max_tokens", 1024)
                    put("system", SYSTEM_PROMPT)
                    put("messages", messagesArray)
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(body.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)
                    val contentArray = json.getJSONArray("content")
                    val textBuilder = StringBuilder()
                    for (i in 0 until contentArray.length()) {
                        val block = contentArray.getJSONObject(i)
                        if (block.getString("type") == "text") {
                            textBuilder.append(block.getString("text"))
                        }
                    }
                    textBuilder.toString().trim()
                } else {
                    val error = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                    val errJson = runCatching { JSONObject(error) }.getOrNull()
                    val errMsg = errJson?.optJSONObject("error")?.optString("message") ?: "HTTP $responseCode"
                    "API error: $errMsg"
                }

            } catch (e: java.net.SocketTimeoutException) {
                "Request timed out. Please check your connection."
            } catch (e: Exception) {
                "Connection error: ${e.message}"
            }
        }
    }
}
