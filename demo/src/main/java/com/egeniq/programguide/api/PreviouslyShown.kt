package com.egeniq.programguide.api

import com.google.gson.annotations.SerializedName

data class PreviouslyShown(
    @SerializedName("-self-closing") var selClosing: String
)