package com.jesus.fastnotes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NotesAdapter(private val notas: MutableList<Note>) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            //Vista de las notas
            val content: TextView = itemView.findViewById(R.id.tvNoteContent)
            val title: TextView = itemView.findViewById(R.id.tvNoteTitle)
            val category: TextView = itemView.findViewById(R.id.tvNoteCategory)
    }
        //Crear la vista
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
            //Inflar el layout de la nota
        val view = LayoutInflater.from(parent.context).inflate(R.layout.note_item, parent, false)
        return NoteViewHolder(view)
    }

        //Llenas los datos en la vista
    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
            val nota = notas[position]
            holder.title.text = nota.title
            holder.category.text = "Categoría: ${nota.category}"
            holder.content.text = nota.content
    }
    fun updateNotas(nuevasNotas: List<Note>) {
        notas.clear()
        notas.addAll(nuevasNotas)
        notifyDataSetChanged()
    }
    override fun getItemCount(): Int = notas.size
}