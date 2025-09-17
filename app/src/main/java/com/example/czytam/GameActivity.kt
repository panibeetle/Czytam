package com.example.czytam

import android.content.Context
import com.example.czytam.models.Story
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import java.util.Locale

class GameActivity : AppCompatActivity(),TextToSpeech.OnInitListener {


    lateinit var story: Story
    private var gameIndex = 0
    private val encouragementPhrases = listOf(
        "Super! Teraz czas na kolejną przygodę!",
        "Brawo! Ukończyłeś tę rundę!",
        "Fantastycznie! Nowe wyzwania czekają!",
        "Świetna robota! Twoja kolejna przygoda!!",
        "Ekstra! Udało się! Kolejna zabawa z literkami i słowami!",
        "Wspaniale! Kolejna gra czeka!"
    )
    lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        tts = TextToSpeech(this, this)


        // ✅ Get Story object directly from Intent
        story = (intent.getSerializableExtra("story")
            ?: throw IllegalArgumentException("Story not found in Intent")) as Story

        startNextGame()
    }
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("pl", "PL")
        }
    }

    // 🔊 Dźwięk poprawnej odpowiedzi
    fun playCorrectSound() {
        playSound(R.raw.coin) // albo R.raw.correct
    }
    fun playWrongSound() {
        playSound(R.raw.wrong)
    }

    // 🔊 Fraza motywacyjna po ukończeniu gry
    fun speakEncouragement() {
        val phrase = encouragementPhrases.random()
        tts.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "ENCOURAGE")
    }

    private fun playSound(@RawRes soundResId: Int) {
        val mp = MediaPlayer.create(this, soundResId)
        mp.setOnCompletionListener { player -> player.release() }
        mp.start()
    }

    private fun startNextGame() {
        val fragment: Fragment = when (gameIndex) {
            0 -> GameSyllableFindFragment() // NEW syllable find game
            1 -> GameWordFragment(story.sentences[0])
            2 -> GameSentenceFragment(story.sentences)
            3 -> GameStoryOrderFragment(story)
            else -> {
                endGames()
                return
            }
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.gameContainer, fragment)
            .commit()
    }

    fun nextGame() {
        gameIndex++
        startNextGame()
    }
    private fun increaseStoryCounter(context: Context) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val current = prefs.getInt("story_counter", 1)
        if(current == story.id){
        prefs.edit().putInt("story_counter", current + 1).apply()}
    }

    private fun endGames() {
        increaseStoryCounter(this)
        // ✅ Go to RewardActivity
        val intent = Intent(this, MainActivity::class.java)

        startActivity(intent)
        finish()
    }
    override fun onDestroy() {
        super.onDestroy()
        tts.stop()
        tts.shutdown()
    }
}
