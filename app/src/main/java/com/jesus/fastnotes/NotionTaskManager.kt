package com.jesus.fastnotes

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class NotionTaskManager(
    private val token: String,
    private val databaseId: String
) {
    private val client = OkHttpClient()
    private val notionVersion = "2022-06-28"
    private val contentType = "application/json; charset=utf-8".toMediaType()

    /**
     * Envía una tarea a Notion con los campos adaptados a tu base de datos.
     */
    fun enviarTarea(
        titulo: String,
        descripcion: String,
        fecha: String,
        estado: String = "In progress" // asegúrate que coincida con tu select
    ) {
        val parent = JSONObject().put("database_id", databaseId)

// Título (campo tipo title)
        val titleObject = JSONObject()
            .put("text", JSONObject().put("content", titulo))

        val tarea = JSONObject().put("title", JSONArray().put(titleObject))

// Estado (campo tipo select)
        val estadoJson = JSONObject().put("status", JSONObject().put("name", estado))

// Fecha (campo tipo date)
        val fechaJson = JSONObject().put("date", JSONObject().put("start", fecha))

// Nota (campo tipo rich_text)
        val nota = JSONObject()
            .put("rich_text", JSONArray().put(
                JSONObject().put("text", JSONObject().put("content", descripcion))
            ))

// Propiedades del objeto final
        val properties = JSONObject()
            .put("Tarea", tarea)
            .put("Estado", estadoJson)
            .put("Fecha", fechaJson)
            .put("Nota", nota)

// JSON final para Notion
        val finalJson = JSONObject()
            .put("parent", parent)
            .put("properties", properties)

        val body = finalJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())


        val request = Request.Builder()
            .url("https://api.notion.com/v1/pages")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Notion-Version", notionVersion)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("NotionTaskManager", "❌ Error de red: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d("NotionTaskManager", "✅ Tarea enviada a Notion correctamente")
                } else {
                    Log.e("NotionTaskManager", "❗ Error en la respuesta: ${response.body?.string()}")
                }
            }
        })
    }
}
