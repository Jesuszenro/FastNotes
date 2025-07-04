package com.jesus.fastnotes

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import java.util.Locale
import java.util.TimeZone
import android.content.ContentValues
import android.util.Log
import com.google.mlkit.nl.entityextraction.DateTimeEntity
import com.google.mlkit.nl.entityextraction.Entity
import com.google.mlkit.nl.entityextraction.EntityExtraction
import com.google.mlkit.nl.entityextraction.EntityExtractionParams
import com.google.mlkit.nl.entityextraction.EntityExtractorOptions
import java.text.SimpleDateFormat
import com.google.mlkit.nl.entityextraction.*
import java.util.Calendar
import java.util.Date


class CalendarHelper(private val context: Context) {

    fun insertarEventoAutomaticamente(
        titulo: String,
        descripcion: String,
        fechaInicioMillis: Long,
        duracionMinutos: Int = 60
    ) {
        val calendarId = obtenerPrimerCalendarioId() ?: run {
            Toast.makeText(context, "No se encontró un calendario válido", Toast.LENGTH_SHORT).show()
            return
        }

        val zonaLocal = TimeZone.getDefault()
        val cal = Calendar.getInstance(zonaLocal).apply {
            timeInMillis = fechaInicioMillis
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val startMillis = cal.timeInMillis
        val endMillis = cal.timeInMillis + duracionMinutos * 60 * 1000

        Log.d("EventoInsertado", "📅 Fecha (local): ${Date(startMillis)}")

        val valores = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, titulo)
            put(CalendarContract.Events.DESCRIPTION, descripcion)
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, zonaLocal.id) // ⚠️ Este campo es crítico
        }

        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, valores)

        if (uri != null) {
            Log.d("EventoInsertado", "✅ Evento creado: $uri")
            Toast.makeText(context, "Evento agregado al calendario", Toast.LENGTH_SHORT).show()
        } else {
            Log.e("EventoInsertado", "❌ Error al insertar evento")
            Toast.makeText(context, "Error al insertar evento", Toast.LENGTH_SHORT).show()
        }
    }


    private fun obtenerPrimerCalendarioId(): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.OWNER_ACCOUNT,
            CalendarContract.Calendars.VISIBLE
        )

        val selection = "${CalendarContract.Calendars.VISIBLE} = 1"
        val cursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI, projection, selection, null, null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val nombre = it.getString(1)
                val cuenta = it.getString(2)
                val dueño = it.getString(3)
                val visible = it.getInt(4)

                Log.d(
                    "Calendario",
                    "ID: $id - Nombre: $nombre - Cuenta: $cuenta - Dueño: $dueño - Visible: $visible"
                )
                return id
            }
        }

        return null
    }

    fun extraerFechaDesdeTexto(texto: String, onFechaDetectada: (Long?) -> Unit) {
        Log.d("FechaDetectada", "📌 Analizando texto: $texto")

        val extractor = EntityExtraction.getClient(
            EntityExtractorOptions.Builder(EntityExtractorOptions.SPANISH).build()
        )

        val params = EntityExtractionParams.Builder(texto).build()

        extractor.annotate(params)
            .addOnSuccessListener { annotations ->
                var seDetecto = false

                for (annotation in annotations) {
                    for (entity in annotation.entities) {
                        if (entity.type == Entity.TYPE_DATE_TIME) {
                            val textoFecha = annotation.annotatedText
                            Log.d("FechaDetectada", " Entidad DATE_TIME detectada: $textoFecha")

                            val fechaParseada = intentarParsearFechaInformal(textoFecha)
                            Log.d("FechaDetectada", " Fecha parseada: $fechaParseada")
                            onFechaDetectada(fechaParseada)
                            seDetecto = true
                            return@addOnSuccessListener
                        } else {
                            Log.d("FechaDetectada", "Entidad no es DATE_TIME → tipo: ${entity.type}")
                        }
                    }
                }

                if (!seDetecto) {
                    Log.w("FechaDetectada", " No se detectó ninguna entidad DATE_TIME")
                    onFechaDetectada(null)
                }
            }
            .addOnFailureListener {
                Log.e("FechaDetectada", " Error al analizar texto: ${it.localizedMessage}")
                onFechaDetectada(null)
            }
    }


    fun intentarParsearFechaInformal(texto: String): Long? {
        val textoNormalizado = texto.lowercase().trim()
        val calendario = Calendar.getInstance()

        Log.d("FechaParseo", "📝 Texto original: $texto")
        Log.d("FechaParseo", "🔍 Texto normalizado: $textoNormalizado")

        var horaDetectada: Int? = null
        var minutosDetectados: Int = 0

        // Regex para detectar hora
        val regexHora = Regex("""(?:a\s+las\s+)?(\d{1,2})(?::(\d{2}))?\s*(a\.?m\.?|p\.?m\.?|pm|am|de la mañana|de la tarde|de la noche)?""")
        val match = regexHora.find(textoNormalizado)

        if (match != null) {
            horaDetectada = match.groupValues[1].toIntOrNull()
            minutosDetectados = match.groupValues[2].toIntOrNull() ?: 0
            val ampmRaw = match.groupValues[3].lowercase()

            Log.d("FechaParseo", "⏰ Hora cruda detectada: $horaDetectada:$minutosDetectados → $ampmRaw")

            if (horaDetectada != null && ampmRaw.isNotBlank()) {
                when {
                    ampmRaw.contains("p") || ampmRaw.contains("tarde") || ampmRaw.contains("noche") -> {
                        if (horaDetectada < 12) horaDetectada += 12
                    }
                    ampmRaw.contains("a") || ampmRaw.contains("mañana") -> {
                        if (horaDetectada == 12) horaDetectada = 0
                    }
                }
                Log.d("FechaParseo", "🕓 Hora ajustada a 24h: $horaDetectada:$minutosDetectados")
            }
        } else {
            Log.d("FechaParseo", "❗ No se detectó una hora en el texto")
        }

        // Fechas informales
        when {
            "hoy" in textoNormalizado -> {
                calendario.setHoraSegura(horaDetectada, minutosDetectados)
                Log.d("FechaParseo", "📍 Hoy: ${calendario.time} → ${calendario.timeInMillis}")
                return calendario.timeInMillis
            }

            "mañana" in textoNormalizado -> {
                calendario.add(Calendar.DAY_OF_YEAR, 1)
                calendario.setHoraSegura(horaDetectada, minutosDetectados)
                Log.d("FechaParseo", "📍 Mañana: ${calendario.time} → ${calendario.timeInMillis}")
                return calendario.timeInMillis
            }

            "pasado mañana" in textoNormalizado -> {
                calendario.add(Calendar.DAY_OF_YEAR, 2)
                calendario.setHoraSegura(horaDetectada, minutosDetectados)
                Log.d("FechaParseo", "📍 Pasado mañana: ${calendario.time} → ${calendario.timeInMillis}")
                return calendario.timeInMillis
            }
        }

        val formatos = listOf(
            "d 'de' MMMM 'de' yyyy",
            "d 'de' MMMM",
            "d/M/yyyy",
            "dd/MM/yyyy",
            "d MMM yyyy",
            "EEEE d 'de' MMMM"
        )

        for (formato in formatos) {
            try {
                val sdf = SimpleDateFormat(formato, Locale("es", "ES"))
                sdf.timeZone = TimeZone.getDefault()
                val fecha = sdf.parse(textoNormalizado)
                if (fecha != null) {
                    val cal = Calendar.getInstance()
                    cal.time = fecha
                    if (!formato.contains("yyyy")) {
                        cal.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR))
                    }

                    cal.setHoraSegura(horaDetectada, minutosDetectados)
                    Log.d("FechaParseo", "📆 Fecha parseada con formato '$formato': ${cal.time} → ${cal.timeInMillis}")
                    return cal.timeInMillis
                }
            } catch (e: Exception) {
                Log.w("FechaParseo", "❌ Falló con formato: $formato → ${e.localizedMessage}")
            }
        }

        Log.w("FechaParseo", "⚠️ No se pudo interpretar la fecha.")
        return null
    }


    // Función auxiliar para configurar hora segura
    private fun Calendar.setHoraSegura(hora: Int?, minutos: Int) {
        this.set(Calendar.HOUR_OF_DAY, hora ?: 10) // si no se detectó, por defecto 10 AM
        this.set(Calendar.MINUTE, minutos)
        this.set(Calendar.SECOND, 0)
        this.set(Calendar.MILLISECOND, 0)
    }


    fun imprimirCalendariosDisponibles() {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.OWNER_ACCOUNT,
            CalendarContract.Calendars.VISIBLE
        )
        val cursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI, projection, null, null, null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val nombre = it.getString(1)
                val cuenta = it.getString(2)
                val dueño = it.getString(3)
                val visible = it.getInt(4)

                Log.d(
                    "Calendario",
                    "ID: $id - Nombre: $nombre - Cuenta: $cuenta - Dueño: $dueño - Visible: $visible"
                )
            }
        }
    }
}
