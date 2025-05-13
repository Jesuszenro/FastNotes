package com.jesus.fastnotes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NotesAdapter(private val notas: List<String>) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            //Views of notes
        val content: TextView = itemView.findViewById(R.id.tvNoteContent)
    }
        //Creates the view
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
            //Inflates the layout for each item
        val view = LayoutInflater.from(parent.context).inflate(R.layout.note_item, parent, false)
        return NoteViewHolder(view)
    }

        //Binds the data to the views
    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.content.text = notas[position]
    }

    override fun getItemCount(): Int = notas.size
}