package com.example.czytam


import android.content.Context
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class WordSentenceAdapter(
    private val words: List<String>,
    private val correctSentence: String,
    private val onCorrect: () -> Unit
) : RecyclerView.Adapter<WordSentenceAdapter.WordViewHolder>() {

    private val arranged = mutableListOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_syllable, parent, false)
        return WordViewHolder(view, parent.context)
    }

    override fun getItemCount() = words.size

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        holder.bind(words[position])
        holder.itemView.setOnClickListener {
            arranged.add(words[position])
            holder.itemView.visibility = View.INVISIBLE

            if (arranged.size == words.size) {
                if (arranged.joinToString(" ") == correctSentence) {
                    onCorrect()
                }
            }
        }
    }

    class WordViewHolder(view: View, context: Context) : RecyclerView.ViewHolder(view) {
        private val tvWord: TextView = view.findViewById(R.id.tvSyllable)
        private val tts = TextToSpeech(context) { }

        fun bind(word: String) {
            tvWord.text = word
            tvWord.setOnClickListener {
                tts.language = Locale("pl", "PL")
                tts.speak(word, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }
}
