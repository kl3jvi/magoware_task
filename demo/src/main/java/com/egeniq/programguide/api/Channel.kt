package com.egeniq.programguide.api

import com.google.gson.annotations.SerializedName


data class Channel (

    @SerializedName("-id") var id : String,
    @SerializedName("display-name") var displayName : String,
    @SerializedName("url") var url : String,
    @SerializedName("icon") var icon : Icon

)