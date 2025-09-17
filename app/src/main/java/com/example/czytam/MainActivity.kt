package com.example.czytam

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.czytam.models.Story
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var coinText: TextView
    private lateinit var coinIcon: ImageView
    private lateinit var coinManager: CoinManager
    private lateinit var adapter: StoryAdapter
    private var stories: List<Story> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        CoinManager.init(this)

        recyclerView = findViewById(R.id.recyclerStories)
        coinText = findViewById(R.id.coin_text)
        coinIcon = findViewById(R.id.iconCoins)
        coinIcon.setOnClickListener { val intent = Intent(this, CoinManagerActivity::class.java)
            startActivity(intent)
        }
        updateCoinView()




        recyclerView.layoutManager = LinearLayoutManager(this)
        stories = loadStories()
        adapter = StoryAdapter(stories) { story ->
            val intent = Intent(this, StoryPageActivity::class.java)
            intent.putExtra("story", story)
            startActivity(intent)
        }
        recyclerView.adapter = adapter
        initStoryCounter(this)

    }

    private fun loadStories(): List<Story> {
        val inputStream = assets.open("stories.json")
        val reader = InputStreamReader(inputStream)
        val storyType = object : TypeToken<List<Story>>() {}.type
        return Gson().fromJson(reader, storyType)
    }

    private fun updateCoinView() {

        coinText.text = CoinManager.getCoins().toString()
    }
    override fun onResume() {
        super.onResume()

        // Refresh every time activity comes to front
        updateCoinView()
    }
    private fun initStoryCounter(context: Context) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        if (!prefs.contains("story_counter")) {
            prefs.edit().putInt("story_counter", 1).apply()  // first story is unlocked
        }
    }

}

