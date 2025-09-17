package com.example.czytam

import android.content.ClipData
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.czytam.models.Sentence
import com.google.android.flexbox.FlexboxLayout
import java.text.Normalizer
import java.util.Locale

class GameWordFragment(
    private val sentence: Sentence
) : Fragment() {


    private var allKeywords: MutableList<Pair<String, List<String>>> = mutableListOf()
    private var currentIndex = 0

    private lateinit var correctWord: String
    private lateinit var syllables: List<String>

    private lateinit var syllableContainer: FlexboxLayout
    private lateinit var targetContainer: FlexboxLayout
    private lateinit var buttonReadWord: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_game_word, container, false)

        syllableContainer = view.findViewById(R.id.syllableContainer)
        targetContainer = view.findViewById(R.id.targetContainer)
        buttonReadWord = view.findViewById(R.id.buttonSpeakWord)
        val buttonCheck = view.findViewById<Button>(R.id.buttonCheck)
        updateCoinView()

        // ✅ Collect ALL keywords + syllables from story
        val story = (activity as? GameActivity)?.story
        story?.sentences?.forEach { s ->
            s.keywords.forEachIndexed { index, word ->
                if (index < s.syllables.size) {
                    allKeywords.add(Pair(word, s.syllables[index]))
                }
            }
        }

        if (allKeywords.isEmpty()) {
            Toast.makeText(requireContext(), "❌ Nie ma słów do układania!", Toast.LENGTH_SHORT).show()
            (activity as? GameActivity)?.nextGame()
            return view
        }else{loadNextWord()}
        updateCoinView()
        // ✅ Init TTS


        // ✅ Check correctness
        buttonCheck.setOnClickListener {
            val builtWord = StringBuilder()
            for (i in 0 until targetContainer.childCount) {
                val btn = targetContainer.getChildAt(i) as Button
                builtWord.append(btn.text.toString().replace("-", "")) // ignore "-"
            }
            val cleanWord = normalizeWord(builtWord.toString())
            val cleanCorrect = normalizeWord(correctWord)

            if (cleanWord.equals(cleanCorrect, ignoreCase = true)) {
                (activity as? GameActivity)?.playCorrectSound()
                celebrateWord {
                    currentIndex++
                    if (currentIndex < allKeywords.size) {
                        loadNextWord()
                    } else {
                        targetContainer.removeAllViews()
                        (activity as? GameActivity)?.speakEncouragement()
                        sillyAnimalShow(targetContainer){(activity as? GameActivity)?.nextGame()}

                    }
                }
                updateCoinCount(2)
                updateCoinView()
            } else {
                (activity as? GameActivity)?.playWrongSound()
                resetSyllablesAnimated()
                val toast = Toast.makeText(context, "❌ Spróbuj ponownie!", Toast.LENGTH_SHORT)

// Inflate custom layout
                val toastLayout = layoutInflater.inflate(R.layout.toast_custom, null)
                val toastText: TextView = toastLayout.findViewById(R.id.toastText)
                toastText.text = "❌ Spróbuj ponownie!"

                toast.view = toastLayout
                toast.show()
            }
        }

        return view
    }

    private fun speak(text: String) {
        (activity as? GameActivity)?.tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "SYLLABLE"
        )
    }

    private fun loadNextWord() {
        syllableContainer.removeAllViews()
        targetContainer.removeAllViews()

        val (word, sylList) = allKeywords[currentIndex]
        correctWord = word
        syllables = sylList

        speak(word)

        // 🔊 Speak full word on button click
        buttonReadWord.setOnClickListener { speak(word) }



        // ✅ Add shuffled syllables
        syllables.shuffled().forEach { syl ->
            val btn = createSyllableButton(syl)
            syllableContainer.addView(btn)
        }


    }

    private fun createSyllableButton(syl: String): Button {
        return Button(requireContext()).apply {
            text = syl
            textSize = 20f
            setPadding(16, 16, 16, 16)
            // 🔊 Speak syllable after tap

            setOnClickListener {
                val parent = this.parent as? ViewGroup
                parent?.removeView(this)

                if (parent == syllableContainer) {
                    targetContainer.addView(this)
                } else {
                    syllableContainer.addView(this)
                }
                val normalized = normalizeForTts(syl)
                speak(normalized)
                // Speak syllable after tap

            }
            setBackgroundResource(R.drawable.story_card_bg)
            setOnLongClickListener {
                val clip = ClipData.newPlainText("syllable", syl)
                val dragShadow = View.DragShadowBuilder(it)
                it.startDragAndDrop(clip, dragShadow, it, 0)
                true
            }
        }
    }
    private fun updateCoinView() {
        val coinText: TextView? = activity?.findViewById(R.id.coin_text)
        coinText?.text = CoinManager.getCoins().toString()
    }


    // 🔄 Reset syllables to start
    private fun resetSyllables() {
        targetContainer.removeAllViews()
        syllableContainer.removeAllViews()

        // recreate fresh shuffled syllables
        syllables.shuffled().forEach { syl ->
            syllableContainer.addView(createSyllableButton(syl))
        }


    }
    private fun updateCoinCount(coin:Int){
        val story = (activity as? GameActivity)?.story!!
        val prefs = context?.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val currentStory = prefs?.getInt("story_counter", 1)
        if(story.id == currentStory){
            CoinManager.addCoins(requireContext(),coin)}
    }
    override fun onResume() {
        super.onResume()

        // Refresh every time activity comes to front
        updateCoinView()
    }


    fun normalizeForTts(syllable: String): String {
        val singleLetterWords = listOf("w", "i", "u", "a", "z")
        val clean = syllable.lowercase()

        return when {
            // Single-letter Polish words → force as word
            singleLetterWords.contains(clean) -> "$syllable\u202F"

            // 1–2 letter syllables → split by hyphen to force TTS pronounce letters
            syllable.length in 1..2 -> syllable+"-"

            // Longer syllables → leave as is
            else -> syllable
        }
    }
    fun normalizeWord(word: String): String {
        // Normalize to NFC form and convert to lowercase
        return Normalizer.normalize(word, Normalizer.Form.NFC)
            .replace(Regex("\\p{C}"), "") // usuwa znaki kontrolne i niewidoczne
            .lowercase()
            .trim()
    }
    // ✅ Animate correct syllables
    private fun celebrateWord(onEnd: () -> Unit) {
        val count = targetContainer.childCount
        if (count == 0) {
            onEnd()
            return
        }

        var finished = 0
        for (i in 0 until count) {
            val btn = targetContainer.getChildAt(i)
            btn.animate()
                .scaleX(1.3f)
                .scaleY(1.3f)
                .translationY(-40f) // swim upward
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

    // ❌ Animate wrong → send syllables back up
    private fun resetSyllablesAnimated() {
        val toReset = mutableListOf<Button>()

        for (i in 0 until targetContainer.childCount) {
            val btn = targetContainer.getChildAt(i) as Button
            toReset.add(btn)
        }

        if (toReset.isEmpty()) {
            resetSyllables()
            return
        }

        var finished = 0
        toReset.forEach { btn ->
            btn.animate()
                .translationYBy(-80f) // jump up
                .alpha(0f)           // fade out
                .setDuration(400)
                .withEndAction {
                    // move back to syllableContainer
                    targetContainer.removeView(btn)
                    btn.translationY = 0f
                    btn.alpha = 1f
                    syllableContainer.addView(btn)

                    finished++
                    if (finished == toReset.size) {
                        // After all moved, reshuffle
                        reshuffleSyllables()
                    }
                }
                .start()
        }
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
    private fun reshuffleSyllables() {
        val current = mutableListOf<String>()
        for (i in 0 until syllableContainer.childCount) {
            current.add((syllableContainer.getChildAt(i) as Button).text.toString())
        }
        syllableContainer.removeAllViews()
        current.shuffled().forEach { syl ->
            syllableContainer.addView(createSyllableButton(syl))
        }
    }

}
