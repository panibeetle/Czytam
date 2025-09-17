package com.example.czytam

import android.content.Context
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class StoryOrderAdapter(
    private val shuffledSentences: List<String>,
    private val correctOrder: List<String>,
    private val onCorrect: () -> Unit
) : RecyclerView.Adapter<StoryOrderAdapter.SentenceViewHolder>() {

    private val arranged = mutableListOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SentenceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_syllable, parent, false)
        return SentenceViewHolder(view, parent.context)
    }

    override fun getItemCount() = shuffledSentences.size

    override fun onBindViewHolder(holder: SentenceViewHolder, position: Int) {
        holder.bind(shuffledSentences[position])
        holder.itemView.setOnClickListener {
            arranged.add(shuffledSentences[position])
            holder.itemView.visibility = View.INVISIBLE

            if (arranged.size == shuffledSentences.size) {
                if (arranged == correctOrder) {
                    onCorrect()
                }
            }
        }
    }

    class SentenceViewHolder(view: View, context: Context) : RecyclerView.ViewHolder(view) {
        private val tvSentence: TextView = view.findViewById(R.id.tvSyllable)
        private val tts = TextToSpeech(context) { }

        fun bind(sentence: String) {
            tvSentence.text = sentence
            tvSentence.setOnClickListener {
                tts.language = Locale("pl", "PL")
                tts.speak(sentence, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }
}