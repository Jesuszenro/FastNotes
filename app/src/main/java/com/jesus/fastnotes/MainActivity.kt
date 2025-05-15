package com.jesus.fastnotes

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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
        // Solicitar permiso de audio
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

        // Mostrar el diálogo de notas
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
    private fun descargarModeloMLKit() {
        val options = EntityExtractorOptions.Builder(EntityExtractorOptions.SPANISH).build()
        val extractor = EntityExtraction.getClient(options)

        extractor.downloadModelIfNeeded()
            .addOnSuccessListener {
                Log.d("MLKit", "Modelo de entidades descargado.")
            }
            .addOnFailureListener { e ->
                Log.e("MLKit", "Error al descargar modelo: ${e.localizedMessage}")
            }
    }
}


