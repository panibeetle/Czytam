package com.example.czytam

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.czytam.models.Sentence
import com.example.czytam.models.Story
import java.util.*

class StoryPageActivity : AppCompatActivity() {

    private lateinit var textView: TextView
    private lateinit var imageView: ImageView

    private lateinit var tts: TextToSpeech
    private lateinit var story: Story
    private var sentenceIndex = 0
    private var words: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_story_page)

        textView = findViewById(R.id.storyText)

        imageView = findViewById(R.id.storyImage)
        // 🔹 Load your Story from Intent
        story = intent.getSerializableExtra("story") as Story

        // 🔹 Init TTS
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale("pl", "PL")
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        runOnUiThread {
                            utteranceId?.toIntOrNull()?.let { index ->
                                highlightWord(index)
                            }
                        }
                    }

                    override fun onDone(utteranceId: String?) {}
                    override fun onError(utteranceId: String?) {}
                })
                showSentence()
            }
        }

        findViewById<ImageButton>(R.id.btnNext).setOnClickListener {
            if (sentenceIndex < story.sentences.size - 1) {
                sentenceIndex++
                showSentence()
            } else {
                // 🔹 Last sentence finished -> go to games
                val intent = Intent(this, GameActivity::class.java)
                intent.putExtra("story", story)
                startActivity(intent)
                finish() // optional, close story page
            }
        }

        findViewById<ImageButton>(R.id.btnPrev).setOnClickListener {
            if (sentenceIndex > 0) {
                sentenceIndex--
                showSentence()
            }
        }

        findViewById<ImageButton>(R.id.btnPlay).setOnClickListener {
            speakSentence(story.sentences[sentenceIndex])
        }
    }

    private fun showSentence() {
        val sentence = story.sentences[sentenceIndex]
        textView.text = sentence.text

        // Load picture
        if (!sentence.image.isNullOrEmpty()) {
            val resId = resources.getIdentifier(sentence.image, "drawable", packageName)
            if (resId != 0) {
                imageView.setImageResource(resId)
                imageView.visibility = ImageView.VISIBLE
            } else {
                imageView.visibility = ImageView.GONE
            }
        } else {
            imageView.visibility = ImageView.GONE
        }


        speakSentence(sentence)
    }

    private fun speakSentence(sentence: Sentence) {
        words = splitSentenceMergeSingleLetter(sentence.text)

            
        tts.stop()


        words.forEachIndexed { i, word ->

            val params = Bundle()
            tts.speak(word, TextToSpeech.QUEUE_ADD, params, i.toString())
        }

    }

    fun splitSentenceMergeSingleLetter(sentence: String): List<String> {
        val words = sentence.split(" ")
        val result = mutableListOf<String>()
        var i = 0

        while (i < words.size) {
            val word = words[i]
            // Merge if word is single-letter
            if (word.length == 1 && i + 1 < words.size) {
                result.add("$word\u202F${words[i + 1]}")  // narrow no-break space
                i += 2  // skip next word, already merged
            } else {
                result.add(word)
                i += 1
            }
        }

        return result
    }



    private fun highlightWord(index: Int) {
        if (index < 0 || index >= words.size) return

        val text = words.joinToString(" ")
        val spannable = SpannableString(text)

        val start = words.take(index).joinToString(" ").length + if (index > 0) 1 else 0
        val end = start + words[index].length
        val colorO = resources.getColor(R.color.container_orange, null)
        // 🔹 highlight ONLY the currently read word
        spannable.setSpan(

            BackgroundColorSpan(colorO),
            start, end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        textView.text = spannable
    }


    override fun onDestroy() {
        super.onDestroy()
        tts.stop()
        tts.shutdown()
    }
}
