package com.example.czytam.models

import java.io.Serializable

data class Story(
    val id: Int,
    val title: String,
    val cover: String,
    val sentences: List<Sentence>
) : Serializable
