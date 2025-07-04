package com.jesus.fastnotes

import android.app.DownloadManager
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentTransaction
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

        adapter = NotesAdapter(notesList.toMutableList()) { nota ->
            val fragment = EditNoteFragment.newInstance(
                noteId = nota.id,
                title = nota.title,
                content = nota.content
            )
            binding.fragmentContainer.visibility = View.VISIBLE

            supportFragmentManager.beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
        binding.recyclerViewNotes.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewNotes.adapter = adapter


        // Pedir permisos de calendario si no se han concedido
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_CALENDAR)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.WRITE_CALENDAR, android.Manifest.permission.READ_CALENDAR),
                100
            )
        }

        // Imprimir los calendarios visibles (debug)
        CalendarHelper(this).imprimirCalendariosDisponibles()

        // Observar notas en Firestore
        observeNotes()
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                binding.fragmentContainer.visibility = View.GONE
            }
        }

    }


    private fun observeNotes() {
        val db = FirebaseFirestore.getInstance()
        val calendarHelper = CalendarHelper(this)

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
                    val yaInsertado = document.getBoolean("calendarInserted") ?: false

                    val nota = Note(
                        content = contenido,
                        title = titulo,
                        category = categoria,
                        id = document.id
                    )
                    notesList.add(nota)

                    // Solo insertar al calendario si aún no se ha insertado
                   /* if (!yaInsertado && (categoria == "Tarea" || categoria == "Evento")) {
                        calendarHelper.extraerFechaDesdeTexto(contenido) { fecha ->
                            if (fecha != null) {
                                calendarHelper.insertarEventoAutomaticamente(titulo, contenido, fecha)
                                document.reference.update("calendarInserted", true)
                            }
                        }
                    }*/
                }

                adapter.updateNotas(notesList)
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
                    val colorHex = categoriaColorMap[categoria] ?: "#DDDDDD"
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
