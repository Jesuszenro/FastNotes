package com.jesus.fastnotes

import android.app.DownloadManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jesus.fastnotes.databinding.ActivityNotesBinding

class NotesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotesBinding
    private lateinit var adapter: NotesAdapter
    private val notesList = mutableListOf<Note>()

    private val categoriaColorMap = mapOf(
        "Tarea" to "#FFCDD2",
        "Evento" to "#C5CAE9",
        "Compras" to "#C8E6C9",
        "Ideas" to "#FFF9C4",
        "Finanzas" to "#FFE0B2",
        "Contacto" to "#B3E5FC",
        "Correo" to "#E1BEE7",
        "Lugar" to "#D7CCC8",
        "General" to "#F5F5F5"
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        adapter = NotesAdapter(notesList.toMutableList())
        binding.recyclerViewNotes.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewNotes.adapter = adapter
        observeNotes()
    }
    private fun observeNotes() {
        val db = FirebaseFirestore.getInstance()

        db.collection("notes")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(this, "Error al obtener las notas", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                notesList.clear()
                for (document in snapshot!!) {
                    val contenido = document.getString("content") ?: ""
                    val titulo = document.getString("title") ?: "Sin título"
                    val categoria = document.getString("category") ?: "General"

                    notesList.add(Note(contenido, titulo, categoria))
                }
                adapter.updateNotas(notesList)

                // Extraer categorías únicas y generar chips
                val categoriasUnicas = notesList.map { it.category }.toSet()
                mostrarChipsDeCategorias(categoriasUnicas)
            }
    }

    private fun mostrarChipsDeCategorias(categorias: Set<String>) {
        binding.chipGroupCategorias.removeAllViews()
        val categoriasConTodas = listOf("Todas") + categorias

        for (categoria in categoriasConTodas) {
            val chip = Chip(this).apply {
                text = categoria
                isCheckable = true

                if (categoria != "Todas") {
                    val colorHex = categoriaColorMap[categoria] ?: "#DDDDDD" // color por defecto si no está mapeado
                    chipBackgroundColor = ColorStateList.valueOf(Color.parseColor(colorHex))
                    setTextColor(Color.BLACK)
                }
            }
            binding.chipGroupCategorias.addView(chip)
        }


        binding.chipGroupCategorias.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chip = group.findViewById<Chip>(checkedIds[0])
                val categoriaSeleccionada = chip.text.toString()
                filtrarNotasPorCategoria(categoriaSeleccionada)
            } else {
                adapter.updateNotas(notesList)
            }
        }
    }
    private fun filtrarNotasPorCategoria(categoria: String) {
        if (categoria == "Todas") {
            adapter.updateNotas(notesList)
        } else {
            val filtradas = notesList.filter { it.category.equals(categoria, ignoreCase = true) }
            adapter.updateNotas(filtradas)
        }
    }



}