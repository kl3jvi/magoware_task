package com.egeniq.programguide.utils

import com.egeniq.programguide.api.RestApi
import com.google.gson.GsonBuilder
import okhttp3.*
import java.io.IOException

class ApiClient : Callback {

    val API_URL = "https://admin.magoware.tv/epg.json"
    private var magowareApi: RestApi? = null
    private var onRequestCompleteListener: OnRequestCompleteListener? = null


    fun fetchJson(callback: OnRequestCompleteListener) {

        this.onRequestCompleteListener = callback
        val request = Request.Builder().url(API_URL).build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(this)
    }


    override fun onFailure(call: Call, e: IOException) {
        onRequestCompleteListener?.onError()
        println("failure ne callback")
    }

    override fun onResponse(call: Call, response: Response) {

        if (response.isSuccessful) {
            val body = response.body()?.string()

            val gson = GsonBuilder().create()
            val mainEntryPerApi = gson.fromJson(body, RestApi::class.java)

            parse(mainEntryPerApi)
        }
        magowareApi?.let { onRequestCompleteListener?.onSuccess(it) }
    }

    private fun parse(response: RestApi) {
        this.magowareApi = response
    }

}


interface OnRequestCompleteListener {
    fun onSuccess(forcast: RestApi)
    fun onError()
}