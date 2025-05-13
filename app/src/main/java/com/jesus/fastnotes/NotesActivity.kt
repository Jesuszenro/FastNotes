package com.jesus.fastnotes

import android.app.DownloadManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jesus.fastnotes.databinding.ActivityNotesBinding

class NotesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotesBinding
    private lateinit var adapter: NotesAdapter
    private val notesList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        adapter = NotesAdapter(notesList)
        binding.recyclerViewNotes.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewNotes.adapter = adapter
        observeNotes()
    }
    private fun observeNotes (){
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
                    val note = document.getString("content")
                    val content = document.getString("contenido") ?: ""
                    notesList.add(content)
                }
                adapter.notifyDataSetChanged()
                }
    }

}