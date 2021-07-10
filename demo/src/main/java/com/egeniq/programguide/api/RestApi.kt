package com.egeniq.programguide.api

import com.google.gson.annotations.SerializedName


data class RestApi(

    @SerializedName("tv") var tv: Tv,
    @SerializedName("#standalone") var standalone: String

)