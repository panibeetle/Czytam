package com.example.czytam.models

import java.io.Serializable

data class Sentence(
    val text: String,
    val image: String,
    val keywords: List<String>,
    val syllables: List<List<String>>
) : Serializable