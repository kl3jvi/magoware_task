package com.egeniq.programguide.api

import com.google.gson.annotations.SerializedName

data class Programme(

    @SerializedName("-channel") var channel : String,
    @SerializedName("-start") var start : String,
    @SerializedName("-stop") var stop : String,
    @SerializedName("title") var title : Title,
    @SerializedName("category") var category : String,
    @SerializedName("desc") var desc : Desc,
    @SerializedName("previously-shown")val previouslyShown: PreviouslyShown,

)