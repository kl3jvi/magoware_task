package com.egeniq.programguide.api

import com.google.gson.annotations.SerializedName


data class Icon (

    @SerializedName("-src") var src : String,
    @SerializedName("-self-closing") var selfClosing : String

)