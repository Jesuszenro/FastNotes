package com.jesus.fastnotes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NotesAdapter(
    private var notas: List<Note>,
    private val onItemClick: (Note) -> Unit
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val content: TextView = itemView.findViewById(R.id.tvNoteContent)
        val title: TextView = itemView.findViewById(R.id.tvNoteTitle)
        val category: TextView = itemView.findViewById(R.id.tvNoteCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.note_item, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notas[position]
        holder.title.text = note.title
        holder.content.text = note.content
        holder.category.text = note.category

        holder.itemView.setOnClickListener {
            onItemClick(note)
        }
    }

    override fun getItemCount() = notas.size

    fun updateNotas(nuevasNotas: List<Note>) {
        this.notas = nuevasNotas
        notifyDataSetChanged()
    }
}
