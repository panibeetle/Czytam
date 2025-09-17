package com.example.czytam


import android.content.Context
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class SyllableGameAdapter(
    private val syllables: List<String>,
    private val onCorrect: () -> Unit
) : RecyclerView.Adapter<SyllableGameAdapter.SyllableViewHolder>() {

    private val arranged = mutableListOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SyllableViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_syllable, parent, false)
        return SyllableViewHolder(view, parent.context)
    }

    override fun getItemCount() = syllables.size

    override fun onBindViewHolder(holder: SyllableViewHolder, position: Int) {
        holder.bind(syllables[position])
        holder.itemView.setOnClickListener {
            arranged.add(syllables[position])
            holder.itemView.visibility = View.INVISIBLE

            // 🔹 Check if all syllables are placed
            if (arranged.size == syllables.size) {
                if (arranged == syllables) { // correct order
                    onCorrect()
                }
            }
        }
    }

    class SyllableViewHolder(view: View, context: Context) : RecyclerView.ViewHolder(view) {
        private val tvSyllable: TextView = view.findViewById(R.id.tvSyllable)
        private val tts = TextToSpeech(context) {
            it
        }

        fun bind(syllable: String) {
            tvSyllable.text = syllable
            tvSyllable.setOnClickListener {
                tts.language = Locale("pl", "PL")
                tts.speak(syllable, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }
}