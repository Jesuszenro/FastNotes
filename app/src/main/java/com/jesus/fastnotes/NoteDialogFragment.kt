package com.jesus.fastnotes

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class NoteDialogFragment : DialogFragment() {

    private lateinit var tvResult: TextView
    private lateinit var tvListening: TextView
    private lateinit var etInputNote: EditText
    private lateinit var btnUseTypedNote: Button
    private lateinit var btnSaveNote: Button
    private var actualNote: String = ""

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.popup_voice, null)

        // Inicialización de vistas
        tvResult = view.findViewById(R.id.tvResult)
        tvListening = view.findViewById(R.id.tvTitle)
        etInputNote = view.findViewById(R.id.etInputNote)
        btnUseTypedNote = view.findViewById(R.id.btnUseTypedNote)
        btnSaveNote = view.findViewById(R.id.btnSaveNote)

        tvListening.text = "Escuchando..."
        btnSaveNote.visibility = View.GONE

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
                guardarNotaEnFirestore(actualNote)
            } else {
                Toast.makeText(context, "No hay nota para guardar", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setView(view)

        return builder.create()
    }

    private fun guardarNotaEnFirestore(nota: String) {
        val db = FirebaseFirestore.getInstance()

        val nuevaNota = hashMapOf(
            "contenido" to nota,
            "timestamp" to FieldValue.serverTimestamp()
        )

        // Mostrar mensaje antes y después de guardar
        Toast.makeText(requireContext(), "Guardando nota...", Toast.LENGTH_SHORT).show()

        db.collection("notes")
            .add(nuevaNota)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Nota guardada correctamente", Toast.LENGTH_SHORT).show()
                dismiss() // 👈 cerrar el diálogo solo si se guardó exitosamente
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al guardar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }


    fun mostrarResultadoReconocido(texto: String) {
        actualNote = texto
        tvResult.text = texto
        btnSaveNote.visibility = View.VISIBLE
        tvListening.text = "¿Deseas guardar esta nota?"
    }
}
