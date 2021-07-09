package com.egeniq.programguide.api

data class Programme(
    val `-channel`: String,
    val `-start`: String,
    val `-stop`: String,
    val category: String,
    val desc: Desc,
    val `previously-shown`: PreviouslyShown,
    val title: Title
)