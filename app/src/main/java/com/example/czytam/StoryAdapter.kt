package com.example.czytam

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.czytam.models.Story

class StoryAdapter(
    private val stories: List<Story>,
    private val onClick: (Story) -> Unit
) : RecyclerView.Adapter<StoryAdapter.StoryViewHolder>() {

    inner class StoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val title: TextView = view.findViewById(R.id.textTitle)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_story, parent, false)
        return StoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        val story = stories[position]
        val resId = holder.itemView.context.resources.getIdentifier(
            story.cover, "drawable", holder.itemView.context.packageName
        )
        val storyId = story.id
        val storyCounter = getStoryCounter(holder.itemView.context)
        if (storyId == storyCounter) {
            // storyId is “magic” or not unlocked yet
            holder.itemView.setBackgroundResource(R.drawable.magic_background_small)
            holder.title.textSize = 24f
        } else {
            // normal background
            holder.itemView.setBackgroundResource(R.drawable.story_card_bg)
        }

        if (storyId <= storyCounter) {
            holder.itemView.isEnabled = true
            holder.itemView.alpha = 1.0f
        }
        else {
            holder.itemView.isEnabled = false
            holder.itemView.alpha = 0.5f
        }
        holder.title.text = story.title
        holder.itemView.setOnClickListener { onClick(story) }
    }
    private fun getStoryCounter(context: Context): Int {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("story_counter", 1)
    }

    override fun getItemCount(): Int = stories.size
}
