package com.jesus.fastnotes

import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import com.google.mlkit.nl.entityextraction.*
import com.google.mlkit.nl.entityextraction.EntityExtractor
import com.google.mlkit.nl.entityextraction.EntityExtractorOptions
import com.google.mlkit.nl.entityextraction.EntityExtractionParams
import com.google.mlkit.nl.entityextraction.Entity


class NoteDialogFragment : DialogFragment() {

    private lateinit var tvResult: TextView
    private lateinit var tvListening: TextView
    private lateinit var etInputNote: EditText
    private lateinit var btnUseTypedNote: Button
    private lateinit var btnSaveNote: Button
    private lateinit var speechRecognizer: SpeechRecognizer
    private var actualNote: String = ""
    private lateinit var progressBar: ProgressBar


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.popup_voice, null)

        progressBar = view.findViewById(R.id.progressBarLoading)
        tvResult = view.findViewById(R.id.tvResult)
        tvListening = view.findViewById(R.id.tvTitle)
        etInputNote = view.findViewById(R.id.etInputNote)
        btnUseTypedNote = view.findViewById(R.id.btnUseTypedNote)
        btnSaveNote = view.findViewById(R.id.btnSaveNote)

        tvListening.text = "Escuchando..."
        btnSaveNote.visibility = View.GONE

        // Iniciar reconocimiento de voz
        iniciarReconocimientoVoz()

        btnUseTypedNote.setOnClickListener {
            val typed = etInputNote.text.toString().trim()
            if (typed.isNotEmpty()) {
                actualNote = typed
                tvResult.text = typed
                tvListening.text = "¿Deseas guardar esta nota escrita?"
                btnSaveNote.visibility = View.VISIBLE
            } else {
                Toast.makeText(context, "Escribe algo primero", Toast.LENGTH_SHORT).show()
            }
        }

        btnSaveNote.setOnClickListener {
            if (actualNote.isNotEmpty()) {
                mostrarLoadingUI()
                analizarContenidoConMLKit(actualNote) { categoria, titulo ->
                    guardarNotaEnFirestore(actualNote, titulo, categoria)
                }
            } else {
                Toast.makeText(context, "No hay nota para guardar", Toast.LENGTH_SHORT).show()
            }
        }


        builder.setView(view)
        return builder.create()
    }

    private fun iniciarReconocimientoVoz() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                mostrarResultadoReconocido("Error al reconocer la voz")
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.get(0) ?: "No se reconoció nada"
                mostrarResultadoReconocido(text)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial =
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                mostrarResultadoReconocido(partial?.get(0) ?: "")
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer.startListening(intent)
    }

    private fun guardarNotaEnFirestore(nota: String, titulo: String, categoria: String) {
        val db = FirebaseFirestore.getInstance()
        val calendarHelper = CalendarHelper(requireContext())

        val nuevaNota = hashMapOf(
            "content" to nota,
            "title" to titulo,
            "category" to categoria,
            "timestamp" to FieldValue.serverTimestamp(),
            "calendarInserted" to false // ✅ Campo de control
        )

        Toast.makeText(requireContext(), "Guardando nota...", Toast.LENGTH_SHORT).show()

        db.collection("notes")
            .add(nuevaNota)
            .addOnSuccessListener { documentRef ->
                Toast.makeText(requireContext(), "Nota guardada correctamente", Toast.LENGTH_SHORT).show()

                calendarHelper.extraerFechaDesdeTexto(nota) { fecha ->
                    if (fecha != null) {
                        calendarHelper.insertarEventoAutomaticamente(titulo, nota, fecha)
                        // ✅ Marcar como insertado en Firestore
                        documentRef.update("calendarInserted", true)
                    }
                    // ✅ Ocultar loading y cerrar diálogo al final
                    ocultarLoadingUI()
                    dismiss()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al guardar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    fun mostrarResultadoReconocido(texto: String) {
        actualNote = texto.trim()
        tvResult.text = actualNote
        btnSaveNote.visibility = View.VISIBLE
        tvListening.text = "¿Deseas guardar esta nota?"
    }


    // Analizar el contenido con ML Kit
    fun analizarContenidoConMLKit(texto: String, callback: (String, String) -> Unit) {
        val options = EntityExtractorOptions.Builder(EntityExtractorOptions.SPANISH).build()
        val extractor = EntityExtraction.getClient(options)
        val params = EntityExtractionParams.Builder(texto).build()

        extractor.downloadModelIfNeeded()
            .addOnSuccessListener {
                extractor.annotate(params)
                    .addOnSuccessListener { annotations ->
                        var categoria = "General"
                        var titulo: String? = null

                        for (annotation in annotations) {
                            for (entity in annotation.entities) {
                                when (entity.type) {
                                    Entity.TYPE_DATE_TIME -> categoria = "Evento"
                                    Entity.TYPE_ADDRESS -> categoria = "Lugar"
                                    Entity.TYPE_MONEY -> categoria = "Finanzas"
                                    Entity.TYPE_PHONE -> categoria = "Contacto"
                                    Entity.TYPE_EMAIL -> categoria = "Correo"
                                }

                                // Guardar como título solo si no es una fecha
                                if (titulo == null && entity.type != Entity.TYPE_DATE_TIME) {
                                    titulo = annotation.annotatedText
                                }
                            }
                        }

                        // Si no se detectó título, usa lógica por reglas o resumen
                        if (titulo.isNullOrBlank()) {
                            // Reglas por palabras clave
                            val reglas = mapOf(
                                "Tarea" to listOf("tarea", "examen", "proyecto"),
                                "Evento" to listOf("cita", "reunión", "evento", "agendar"),
                                "Compras" to listOf("comprar", "super", "lista"),
                                "Ideas" to listOf("idea", "reflexión", "pensamiento")
                            )

                            for ((cat, palabras) in reglas) {
                                if (palabras.any { it in texto.lowercase() }) {
                                    categoria = cat
                                    break
                                }
                            }

                            // Si hay punto o salto de línea, toma la primera frase como respaldo
                            titulo = generarTituloResumenInteligente(texto)
                        }

                        callback(categoria, titulo.replaceFirstChar { it.uppercase() })
                    }
                    .addOnFailureListener {
                        callback("General", generarTituloResumenInteligente(texto))
                    }
            }
            .addOnFailureListener {
                callback("General", generarTituloResumenInteligente(texto))
            }
    }


    fun generarTituloResumenInteligente(texto: String): String {
        val palabrasClaveIgnoradas = listOf(
            "yo", "necesito", "quiero", "tengo", "que", "hacer", "cómo", "esta", "está", "en",
            "la", "el", "una", "un", "para", "mi", "lo", "se", "más", "menos", "muy", "también", "porque", "con"
        )

        val oraciones = texto.split(".", "\n").filter { it.isNotBlank() }
        val palabras = mutableListOf<String>()

        for (oracion in oraciones) {
            val tokens = oracion.lowercase().split(" ", ",", ";")
                .filter { it.isNotBlank() && it !in palabrasClaveIgnoradas && it.length > 3 }

            palabras.addAll(tokens)
        }

        // Contar frecuencia de palabras
        val frecuencia = palabras.groupingBy { it }.eachCount()
        val palabrasClave = frecuencia.entries.sortedByDescending { it.value }.map { it.key }.take(5)

        return palabrasClave.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }

    private fun mostrarLoadingUI() {
        progressBar.visibility = View.VISIBLE
        btnSaveNote.isEnabled = false
        btnUseTypedNote.isEnabled = false
        etInputNote.isEnabled = false
    }

    private fun ocultarLoadingUI() {
        progressBar.visibility = View.GONE
        btnSaveNote.isEnabled = true
        btnUseTypedNote.isEnabled = true
        etInputNote.isEnabled = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        speechRecognizer.destroy()
    }
}

