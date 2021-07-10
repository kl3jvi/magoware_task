package com.egeniq.programguide.api

import com.google.gson.annotations.SerializedName

data class Tv(

    @SerializedName("-source-data-url") var sourceDataUrl: String,
    @SerializedName("-source-info-name") var sourceInfoName: String,
    @SerializedName("-source-info-url") var sourceInfoUrl: String,
    @SerializedName("channel") var channel: Channel,
    @SerializedName("programme") var programme: List<Programme>

)