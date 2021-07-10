package com.egeniq.programguide.api

import com.google.gson.annotations.SerializedName


data class Desc(

    @SerializedName("-lang") var lang: String,
    @SerializedName("#text") var text: String

)