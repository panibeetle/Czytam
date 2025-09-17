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
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.czytam.models.Story
import com.google.android.flexbox.FlexboxLayout


class GameStoryOrderFragment(
    private val story: Story
) : Fragment() {

    private lateinit var targetContainer: FlexboxLayout
    private lateinit var sourceContainer: FlexboxLayout
    private lateinit var readStory: Button
    private lateinit var checkStory: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_game_story_order, container, false)


        targetContainer = view.findViewById(R.id.targetContainer)
        sourceContainer = view.findViewById(R.id.sourceContainer)
        readStory = view.findViewById(R.id.buttonSpeaker)
        checkStory = view.findViewById(R.id.buttonCheck)


        readStory.setOnClickListener { readStoryAloud()}
        checkStory.setOnClickListener { checkOrder() }
        updateCoinView()
        loadSentences()
        readStoryAloud()

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


    private fun loadSentences() {
        targetContainer.removeAllViews()
        sourceContainer.removeAllViews()

        val shuffled = story.sentences.shuffled()

        shuffled.forEach { sentence ->
            val tv = createSentenceTextView(sentence.text)
            sourceContainer.addView(tv)
        }

        // allow drop both ways
        setupDragAndDrop(targetContainer)
        setupDragAndDrop(sourceContainer)

    }

    private fun createSentenceTextView(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text

            textSize = 20f
            setPadding(16, 16, 16, 16)
            // ✅ Tap-to-move + TTS
            setOnClickListener {
                val parent = this.parent as? ViewGroup
                parent?.removeView(this)

                if (parent == sourceContainer) {
                    targetContainer.addView(this)
                } else {
                    sourceContainer.addView(this)
                }



                // check order when all placed
//                if (targetContainer.childCount == story.sentences.size) {
//                    checkOrder()
//                }
            }
           
            setBackgroundResource(R.drawable.story_card_bg)


            setOnLongClickListener {
                val clip = ClipData.newPlainText("sentence", text)
                val shadow = View.DragShadowBuilder(it)
                it.startDragAndDrop(clip, shadow, it, 0)
                true
            }
        }
    }

    private fun setupDragAndDrop(container: FlexboxLayout) {
        container.setOnDragListener { v, event ->
            when (event.action) {
                DragEvent.ACTION_DROP -> {
                    val draggedView = event.localState as View
                    (draggedView.parent as ViewGroup).removeView(draggedView)
                    (v as FlexboxLayout).addView(draggedView)

                    // check when all placed in target
                    if (targetContainer.childCount == story.sentences.size) {
                        checkOrder()
                    }
                }
            }
            true
        }
    }
    private fun updateCoinView() {
        val coinText: TextView? = activity?.findViewById(R.id.coin_text)
        coinText?.text = CoinManager.getCoins().toString()
    }

    private fun checkOrder() {
        val builtOrder = mutableListOf<String>()
        for (i in 0 until targetContainer.childCount) {
            val tv = targetContainer.getChildAt(i) as TextView
            builtOrder.add(tv.text.toString())
        }

        val correctOrder = story.sentences.map { it.text }

        if (builtOrder == correctOrder) {


            speak("Brawo! Historia ułożona! Nowa historia odblokowana!")
            updateCoinCount(5)

            updateCoinView()
            targetContainer.removeAllViews()
            sillyAnimalShow(targetContainer) {(activity as? GameActivity)?.nextGame()}




        } else {
            (activity as? GameActivity)?.playWrongSound()
            val toast = Toast.makeText(context, "❌ Spróbuj ponownie!", Toast.LENGTH_SHORT)

// Inflate custom layout
            val toastLayout = layoutInflater.inflate(R.layout.toast_custom, null)
            val toastText: TextView = toastLayout.findViewById(R.id.toastText)
            toastText.text = "❌ Spróbuj ponownie!"

            toast.view = toastLayout
            toast.show()
            resetSentences() // reset with shuffled order
        }
    }
    private fun readStoryAloud() {
        // Read all sentences in the right order
        val storyText = story.sentences.joinToString(" ") { it.text }
        speak(storyText)

    }
    private fun updateCoinCount(coin:Int){
        val story = (activity as? GameActivity)?.story!!
        val prefs = context?.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val currentStory = prefs?.getInt("story_counter", 1)
        if(story.id == currentStory){
            CoinManager.addCoins(requireContext(),coin)}
    }


    private fun resetSentences() {
        val viewsToMove = mutableListOf<View>()
        for (i in 0 until targetContainer.childCount) {
            viewsToMove.add(targetContainer.getChildAt(i))
        }
        targetContainer.removeAllViews()

        for (view in viewsToMove) {
            val tv = view as TextView
            sourceContainer.addView(tv)

            tv.alpha = 0f
            tv.translationY = -100f
            tv.scaleX = 0.5f
            tv.scaleY = 0.5f

            tv.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setInterpolator(android.view.animation.OvershootInterpolator())
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
    override fun onResume() {
        super.onResume()

        // Refresh every time activity comes to front
        updateCoinView()
    }


}
