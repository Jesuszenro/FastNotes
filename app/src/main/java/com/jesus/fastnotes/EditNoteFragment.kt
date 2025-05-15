package com.jesus.fastnotes

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.jesus.fastnotes.databinding.FragmentEditNoteBinding

/**
 * A simple [Fragment] subclass.
 * Use the [EditNoteFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class EditNoteFragment : Fragment() {

    private lateinit var binding: FragmentEditNoteBinding
    private var noteId: String? = null
    private var originalTitle: String? = null
    private var originalContent: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditNoteBinding.inflate(inflater, container, false)

        arguments?.let {
            noteId = it.getString("noteId")
            originalTitle = it.getString("title")
            originalContent = it.getString("content")
        }

        binding.etTitle.setText(originalTitle)
        binding.etContent.setText(originalContent)

        binding.btnSaveChanges.setOnClickListener {
            val newTitle = binding.etTitle.text.toString().trim()
            val newContent = binding.etContent.text.toString().trim()

            if (noteId != null && newTitle.isNotEmpty() && newContent.isNotEmpty()) {
                actualizarNota(noteId!!, newTitle, newContent)
            } else {
                Toast.makeText(context, "No puede estar vacío", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }

    private fun actualizarNota(id: String, title: String, content: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("notes").document(id)
            .update(
                mapOf(
                    "title" to title,
                    "content" to content,
                    "timestamp" to FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener {
                Toast.makeText(context, "Nota actualizada", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    companion object {
        fun newInstance(noteId: String, title: String, content: String): EditNoteFragment {
            val fragment = EditNoteFragment()
            val args = Bundle()
            args.putString("noteId", noteId)
            args.putString("title", title)
            args.putString("content", content)
            fragment.arguments = args
            return fragment
        }
    }
}
