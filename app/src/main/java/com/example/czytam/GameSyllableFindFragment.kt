package com.example.czytam

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.czytam.models.Story
import com.google.android.flexbox.FlexboxLayout
import java.text.Normalizer
import java.util.Locale

class GameSyllableFindFragment() : Fragment() {


    private val allSyllables: MutableList<String> = mutableListOf()
    private var currentSyllable: String? = null

    private lateinit var syllableContainer: FlexboxLayout
    private lateinit var buttonSpeak: Button
    private lateinit var story: Story


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_game_syllable_find, container, false)

        syllableContainer = view.findViewById(R.id.syllableContainer)
        buttonSpeak = view.findViewById(R.id.buttonSpeak)


        story = (activity as? GameActivity)?.story!!
        story.sentences.forEach { s ->
            s.syllables.forEach { sylList ->
                allSyllables.addAll(sylList)
            }
        }

        if (allSyllables.isEmpty()) {
            Toast.makeText(requireContext(), "❌ Nie ma syllabów do układania!", Toast.LENGTH_SHORT).show()
            (activity as? GameActivity)?.nextGame()
            return view
        }
        updateCoinView()
        // Init TTS

        showAllSyllables()
        pickRandomSyllable()
        speak(currentSyllable!!)

        // Button to repeat current syllable
        buttonSpeak.setOnClickListener {
            currentSyllable?.let { syl ->

                speak(syl)

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

    private fun showAllSyllables() {
        syllableContainer.removeAllViews()
        allSyllables.forEach { syl ->
            val btn = Button(requireContext()).apply {
                setBackgroundResource(R.drawable.story_card_bg)
                text = syl
                textSize = 18f
                setPadding(16, 16, 16, 16)
                setOnClickListener {
                    // 🔊 Always speak tapped syllable
                    //tts.speak(syl, TextToSpeech.QUEUE_FLUSH, null, null)
                    // ✅ Then check correctness
                    checkSyllable(syl, this)
                }
            }
            syllableContainer.addView(btn)
        }
    }

    private fun pickRandomSyllable() {
        if (allSyllables.isEmpty()) {
            syllableContainer.removeAllViews()
            val toast = Toast.makeText(context, "Brawo! Wszystkie zadania ukończone! 🎉😊", Toast.LENGTH_SHORT)

// Inflate custom layout
            val toastLayout = layoutInflater.inflate(R.layout.toast_custom, null)
            val toastText: TextView = toastLayout.findViewById(R.id.toastText)
            toastText.text = "Brawo! Wszystkie zadania ukończone! 🎉😊"
            speak("Brawo! Wszystkie zadania ukończone!" )
            sillyAnimalShow(syllableContainer) {
                (activity as? GameActivity)?.nextGame()
            }

            return
        }
        currentSyllable = allSyllables.random()
        speak(currentSyllable!!)



    }

    private fun checkSyllable(syl: String, btn: Button) {
        val cleanSyl = normalizeWord(syl)
        val cleanSyllable = normalizeWord(currentSyllable!!)
        if (cleanSyl == cleanSyllable) {
            (activity as? GameActivity)?.playCorrectSound()

            animateCorrectButtonAndRemove(btn) {
                // This runs after animation ends
                allSyllables.remove(syl)
                updateCoinCount(1)
                updateCoinView()
                Handler(Looper.getMainLooper()).postDelayed({
                    pickRandomSyllable()
                }, 500)
            }

            val toast = Toast.makeText(context, "✅ Świetnie!!", Toast.LENGTH_SHORT)
            val toastLayout = layoutInflater.inflate(R.layout.toast_custom, null)
            val toastText: TextView = toastLayout.findViewById(R.id.toastText)
            toastText.text = "✅ Świetnie!!"
            toast.view = toastLayout
            toast.show()
            Handler(Looper.getMainLooper()).postDelayed({ toast.cancel() }, 400)
        } else {
            (activity as? GameActivity)?.playWrongSound()
            shakeButton(btn)
            val toast = Toast.makeText(context, "❌ Spróbuj ponownie!", Toast.LENGTH_SHORT)

// Inflate custom layout
            val toastLayout = layoutInflater.inflate(R.layout.toast_custom, null)
            val toastText: TextView = toastLayout.findViewById(R.id.toastText)
            toastText.text = "❌ Spróbuj ponownie!"
            toast.view = toastLayout
            toast.show()
            Handler(Looper.getMainLooper()).postDelayed({ toast.cancel() }, 400)
        }
    }
    private fun updateCoinCount(coin:Int){


        val prefs = context?.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val currentStory = prefs?.getInt("story_counter", 1)
        if(story.id == currentStory){

            CoinManager.addCoins(requireContext(),coin)}
    }
    private fun updateCoinView() {
        val coinText: TextView? = activity?.findViewById(R.id.coin_text)
        coinText?.text = CoinManager.getCoins().toString()
    }
    override fun onResume() {
        super.onResume()

        // Refresh every time activity comes to front
        updateCoinView()
    }
    private fun shakeButton(btn: Button) {
        val shakeDistance = 20f
        btn.animate().translationX(shakeDistance).setDuration(50).withEndAction {
            btn.animate().translationX(-shakeDistance).setDuration(50).withEndAction {
                btn.animate().translationX(0f).duration = 50
            }.start()
        }.start()
    }
    private fun animateCorrectButtonAndRemove(button: Button, onEnd: (() -> Unit)? = null) {
        val originalBg = button.background

        // Set green rounded background
        button.setBackgroundResource(R.drawable.rounded_green)
        val scaleX = ObjectAnimator.ofFloat(button, View.SCALE_X, 1f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(button, View.SCALE_Y, 1f, 1.2f, 1f)


        val set = AnimatorSet()
        set.playTogether(scaleX, scaleY)
        set.duration = 400

        set.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                (button.parent as? ViewGroup)?.removeView(button)
                onEnd?.invoke()
            }
        })

        set.start()
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


    fun normalizeWord(word: String): String {
        // Normalize to NFC form and convert to lowercase
        return Normalizer.normalize(word, Normalizer.Form.NFC).lowercase()
    }


}
