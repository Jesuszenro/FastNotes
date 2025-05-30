package com.jesus.fastnotes

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.nl.entityextraction.EntityExtraction
import com.google.mlkit.nl.entityextraction.EntityExtractorOptions
import com.jesus.fastnotes.databinding.ActivityMainBinding
import java.util.*
import android.view.animation.AnimationUtils
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Simulación de notas recientes
    private val notasRecientes = listOf(
        "Comprar leche",
        "Estudiar redes",
        "Revisar correo del profe"
    )
    private val sugerencias = listOf(
        "Graba una lista de compras",
        "Anota tus ideas del día",
        "Registra una cita importante",
        "Haz una nota para mañana",
        "Crea un recordatorio por voz"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        descargarModeloMLKit()
        verificarPermisosAudio()
        cargarNotasRecientes()
        iniciarSugerencias()

        // Animación bounce
        val bounce = AnimationUtils.loadAnimation(this, R.anim.bounce)

        // Botón de voz
        binding.btnStartVoice.setOnClickListener {
            it.startAnimation(bounce)

            // Abre el fragmento para dictar nota
            val noteDialog = NoteDialogFragment()
            noteDialog.show(supportFragmentManager, "note_dialog")
        }

        // Ir a la vista de notas
        binding.btnNotes.setOnClickListener {
            val intent = Intent(this, NotesActivity::class.java)
            startActivity(intent)
        }
    }
    private fun cargarNotasRecientes() {
        val db = FirebaseFirestore.getInstance()
        val container = binding.containerNotas
        container.removeAllViews()

        db.collection("notes")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(3)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val titulo = document.getString("title") ?: "Sin título"
                    val contenido = document.getString("content") ?: ""
                    val categoria = document.getString("category") ?: "General"

                    val card = TextView(this).apply {
                        text = titulo
                        setPadding(32, 24, 32, 24)
                        setBackgroundResource(R.drawable.nota_background)
                        setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.black))
                        textSize = 14f
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(16, 0, 16, 0)
                        }
                    }

                    container.addView(card)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar notas recientes", Toast.LENGTH_SHORT).show()
            }
    }
    private fun iniciarSugerencias() {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                val sugerenciaAleatoria = sugerencias.random()
                binding.tvSugerencia.text = "Sugerencia: $sugerenciaAleatoria"
                handler.postDelayed(this, 5000) // Cada 10 segundos
            }
        }
        handler.post(runnable)
    }

    private fun verificarPermisosAudio() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
    }

    private fun descargarModeloMLKit() {
        val options = EntityExtractorOptions.Builder(EntityExtractorOptions.SPANISH).build()
        val extractor = EntityExtraction.getClient(options)

        extractor.downloadModelIfNeeded()
            .addOnSuccessListener {
                Log.d("MLKit", "Modelo descargado correctamente")
            }
            .addOnFailureListener { e ->
                Log.e("MLKit", "Error al descargar modelo: ${e.localizedMessage}")
            }
    }
}





