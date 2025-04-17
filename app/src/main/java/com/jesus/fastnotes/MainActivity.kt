package com.jesus.fastnotes

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.jesus.fastnotes.databinding.ActivityMainBinding
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val REQUEST_CODE_SPEECH_INPUT = 100 // Código de solicitud para reconocimiento de voz

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflar la vista
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        binding.btnStartVoice.setOnClickListener() {
            // Iniciar la actividad de reconocimiento de voz
            Log.d("MainActivity", "Botón de reconocimiento de voz presionado")
            startVoiceRecognition()
        }
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 1)
        }

    }
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permiso concedido
        } else {
            // El usuario negó el permiso
            Toast.makeText(this, "Se necesita el permiso de micrófono para dictar notas", Toast.LENGTH_LONG).show()
        }
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            // Configuraciones adicionales para el reconocimiento de voz
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            // Configura el idioma del reconocimiento de voz
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault()
            )  // Idioma predeterminado por la region
            // Configura el prompt para la actividad de reconocimiento de voz
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Di algo")
        }
        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
        } catch (e: Exception) {
            binding.tvNoteContent.text = "Tu dispositivo no soporta esta función"
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == Activity.RESULT_OK && data != null) {
                //Manejar el resultado del reconocimiento de voz
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            binding.tvNoteContent.text = result?.get(0) ?: "No se reconoció ningún texto"
        }
    }
}