package com.egeniq.programguide.api

data class Tv(
    val `-source-data-url`: String,
    val `-source-info-name`: String,
    val `-source-info-url`: String,
    val channel: Channel,
    val programme: List<Programme>
)