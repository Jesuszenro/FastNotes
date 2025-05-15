package com.jesus.fastnotes

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.mlkit.nl.entityextraction.EntityExtraction
import com.google.mlkit.nl.entityextraction.EntityExtractorOptions
import com.jesus.fastnotes.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        descargarModeloMLKit()
        verificarPermisosAudio()

        // Botón de voz
        binding.btnStartVoice.setOnClickListener {
            val noteDialog = NoteDialogFragment()
            noteDialog.show(supportFragmentManager, "note_dialog")
        }

        // Ir a la vista de notas
        binding.btnNotes.setOnClickListener {
            val intent = Intent(this, NotesActivity::class.java)
            startActivity(intent)
        }
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





