package com.example.czytam

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.example.czytam.models.Sentence
import com.google.android.flexbox.FlexboxLayout
import java.util.*

class GameSentenceFragment(
    private val sentences: List<Sentence> // pass full story sentences
) : Fragment() {



    private lateinit var wordPool: FlexboxLayout
    private lateinit var sentenceContainer: FlexboxLayout
    private lateinit var btnCheck: Button
    private lateinit var buttonPlaySentence: Button

    private var sentenceIndex = 0 // current sentence

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_game_sentence, container, false)


        wordPool = view.findViewById(R.id.wordPool)
        sentenceContainer = view.findViewById(R.id.sentenceContainer)
        btnCheck = view.findViewById(R.id.btnCheck)
        updateCoinView()



        buttonPlaySentence = view.findViewById(R.id.buttonSpeakSent)
        buttonPlaySentence.setOnClickListener {
            val sentence = sentences[sentenceIndex].text
            speak(sentence, "SENTENCES")

        }




        btnCheck.setOnClickListener { checkSentence() }
        setupSentence()

        return view
    }
    private fun speak(text: String, type: String = "WORD") {
        (activity as? GameActivity)?.tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            type
        )
    }

    private fun setupSentence() {
        if (sentenceIndex >= sentences.size) {
            sentenceContainer.removeAllViews()
            // ✅ Move to next game after finishing all sentences
            (activity as? GameActivity)?.speakEncouragement()
            sillyAnimalShow(sentenceContainer){ (activity as? GameActivity)?.nextGame()}


            return
        }

        val sentence = sentences[sentenceIndex]
        val sentenceRead = sentences[sentenceIndex].text
        speak(sentenceRead, "SENTENCES")

        val words = sentence.text.replace(".", "").split(" ").shuffled()

        wordPool.removeAllViews()
        sentenceContainer.removeAllViews()

        words.forEach { word ->
            val btn = createWordButton(word)
            wordPool.addView(btn)
        }


    }

    private fun createWordButton(word: String): Button {
        return Button(requireContext()).apply {
            text = word
            textSize = 18f
            setPadding(16, 16, 16, 16)
            setOnClickListener {
                val parent = this.parent as? ViewGroup
                parent?.removeView(this)

                if (parent == wordPool) {
                    sentenceContainer.addView(this)
                } else {
                    wordPool.addView(this)
                }

                // Speak word after tap
                //tts.speak(word, TextToSpeech.QUEUE_FLUSH, null, null)
            }
            setOnLongClickListener {
                val clipData = android.content.ClipData.newPlainText("", word)
                val shadow = View.DragShadowBuilder(it)
                it.startDragAndDrop(clipData, shadow, it, 0)
                true
            }
            setBackgroundResource(R.drawable.story_card_bg)
        }
    }



    private fun checkSentence() {
        val target = sentences[sentenceIndex].text
        val userSentence = sentenceContainer.children
            .filterIsInstance<Button>()
            .joinToString(" ") { it.text.toString() }

        if (userSentence.lowercase() == target.removeSuffix(".").lowercase()) {

            celebrateCorrectAnswer {
                // After animation ends, move to next game
                Handler(Looper.getMainLooper()).postDelayed({
                    (activity as? GameActivity)?.playCorrectSound()
                    sentenceIndex++

                    updateCoinCount(3)  // example for syllable→word
                    updateCoinView()
                    setupSentence()
                }, 300)
            }

        } else {

            val toast = Toast.makeText(context, "❌ Spróbuj ponownie!", Toast.LENGTH_SHORT)

// Inflate custom layout
            val toastLayout = layoutInflater.inflate(R.layout.toast_custom, null)
            val toastText: TextView = toastLayout.findViewById(R.id.toastText)
            toastText.text = "❌ Spróbuj ponownie!"

            toast.view = toastLayout
            toast.show()
            Handler(Looper.getMainLooper()).postDelayed({ toast.cancel() }, 800)
            (activity as? GameActivity)?.playWrongSound()
            resetSentence()
        }
    }
    private fun normalizeWord(word: String): String {
        val letters = listOf("w", "i", "u", "a", "z")

        // Separate word from punctuation
        val regex = "([a-zA-ZąćęłńóśżźĄĆĘŁŃÓŚŻŹ]+)([.,!?;:]*)".toRegex()
        val match = regex.matchEntire(word)
        return if (match != null) {
            val base = match.groupValues[1]
            val punct = match.groupValues[2]
            if (letters.contains(base.lowercase())) "$base\u202F$punct" else word
        } else {
            word
        }
    }
    private fun celebrateCorrectAnswer(onEnd: () -> Unit) {
        val count = sentenceContainer.childCount
        if (count == 0) {
            onEnd()
            return
        }

        var finished = 0
        for (i in 0 until count) {
            val btn = sentenceContainer.getChildAt(i)
            btn.animate()
                .scaleX(1.3f)
                .scaleY(1.3f)
                .translationY(-50f) // swim upward
                .setDuration(500)
                .withEndAction {
                    // return to normal
                    btn.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .translationY(0f)
                        .setDuration(300)
                        .withEndAction {
                            finished++
                            if (finished == count) onEnd()
                        }
                        .start()
                }
                .start()
        }
    }
    private fun resetSentence() {
        val wordsToMove = mutableListOf<View>()

        // collect words from sentenceContainer
        for (i in 0 until sentenceContainer.childCount) {
            wordsToMove.add(sentenceContainer.getChildAt(i))
        }

        // clear container for new round (but keep words alive during animation)
        sentenceContainer.removeAllViews()

        for (view in wordsToMove) {
            val button = view as Button

            // find target position: just add to wordPool at the end
            wordPool.addView(button)

            // animate "jump" effect
            button.alpha = 0f
            button.translationY = -100f   // start a bit higher
            button.scaleX = 0.5f
            button.scaleY = 0.5f

            button.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setInterpolator(android.view.animation.OvershootInterpolator())
                .start()
        }
    }

    private fun updateCoinCount(coin:Int){
        val story = (activity as? GameActivity)?.story!!
        val prefs = context?.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val currentStory = prefs?.getInt("story_counter", 1)
        if(story.id == currentStory){
            CoinManager.addCoins(requireContext(),coin)}
    }
    private fun updateCoinView() {
        val coinText: TextView? = activity?.findViewById(R.id.coin_text)
        coinText?.text = CoinManager.getCoins().toString()
    }
    private fun sillyAnimalShow(flexbox: FlexboxLayout, onEnd: () -> Unit) {
        val context = flexbox.context
        val animals = listOf("🐱", "🐶", "🐸", "🐵", "🐰", "🐷")
        val styles = listOf("bounce", "wiggle", "swim", "jump", "spin")

        val animalEmoji = animals.random()
        val style = styles.random()
        val size = (50..80).random().toFloat()  // bigger smile

        val animal = TextView(context).apply {
            text = animalEmoji
            textSize = size
            // optional: add some margin
            val params = FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.WRAP_CONTENT,
                FlexboxLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(8, 8, 8, 8)
            layoutParams = params
        }

        flexbox.addView(animal)

        // Run animation only after layout is ready
        animal.post {
            // Start roughly at center with small random offset
            val offsetX = (-50..50).random().toFloat()
            val offsetY = (-50..50).random().toFloat()
            val centerX = flexbox.width / 2f - animal.width / 2f + offsetX
            val centerY = flexbox.height / 2f - animal.height / 2f + offsetY
            animal.translationX = centerX
            animal.translationY = centerY
            animal.alpha = 0f

            when (style) {
                "bounce" -> {
                    animal.animate()
                        .alpha(1f)
                        .translationYBy(-200f)
                        .setDuration(1200)
                        .withEndAction {
                            animal.animate()
                                .translationYBy(150f)
                                .translationXBy((-100..100).random().toFloat())
                                .setDuration(1200)
                                .withEndAction {
                                    animal.animate()
                                        .alpha(0f)
                                        .setDuration(1200)
                                        .withEndAction { flexbox.removeView(animal); onEnd() }
                                        .start()
                                }
                                .start()
                        }
                        .start()
                }
                "wiggle" -> {
                    animal.animate()
                        .alpha(1f)
                        .rotationBy(30f)
                        .setDuration(800)
                        .withEndAction {
                            animal.animate()
                                .rotationBy(-60f)
                                .setDuration(800)
                                .withEndAction {
                                    animal.animate()
                                        .rotationBy(30f)
                                        .alpha(0f)
                                        .setDuration(800)
                                        .withEndAction { flexbox.removeView(animal); onEnd() }
                                        .start()
                                }
                                .start()
                        }
                        .start()
                }
                "swim" -> {
                    val dx = (-250..250).random().toFloat()
                    val dy = (-150..150).random().toFloat()
                    animal.animate()
                        .alpha(1f)
                        .translationXBy(dx)
                        .translationYBy(dy)
                        .setDuration(2500)
                        .withEndAction {
                            animal.animate()
                                .alpha(0f)
                                .setDuration(1000)
                                .withEndAction { flexbox.removeView(animal); onEnd() }
                                .start()
                        }
                        .start()
                }
                "jump" -> {
                    animal.animate()
                        .alpha(1f)
                        .translationYBy(-300f)
                        .setDuration(1500)
                        .withEndAction {
                            animal.animate()
                                .translationYBy(300f)
                                .setDuration(1500)
                                .withEndAction {
                                    animal.animate()
                                        .alpha(0f)
                                        .setDuration(1000)
                                        .withEndAction { flexbox.removeView(animal); onEnd() }
                                        .start()
                                }
                                .start()
                        }
                        .start()
                }
                "spin" -> {
                    animal.animate()
                        .alpha(1f)
                        .rotationBy(360f)
                        .setDuration(2500)
                        .withEndAction {
                            animal.animate()
                                .alpha(0f)
                                .setDuration(1000)
                                .withEndAction { flexbox.removeView(animal); onEnd() }
                                .start()
                        }
                        .start()
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()

        // Refresh every time activity comes to front
        updateCoinView()
    }

}
