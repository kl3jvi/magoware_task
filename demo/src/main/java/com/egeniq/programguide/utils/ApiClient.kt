package com.egeniq.programguide.utils

import com.egeniq.programguide.api.RestApi
import com.google.gson.GsonBuilder
import okhttp3.*
import java.io.IOException


class ApiClient : Callback {

    val MAIN_URL = "https://admin.magoware.tv"
    val JSON_PATH = "/epg.json"
    val API_URL = MAIN_URL + JSON_PATH

    private var reusable: String? = null
    private var magowareApi: RestApi? = null


    private var onRequestCompleteListener: OnRequestCompleteListener? = null


    fun fetchJson(callback: OnRequestCompleteListener) {

        this.onRequestCompleteListener = callback
        val request = Request.Builder().url(API_URL).build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(this)
//        client.newCall(request).execute()
    }


    override fun onFailure(call: Call, e: IOException) {
        onRequestCompleteListener?.onError()
        println("failure ne callback")
    }

    override fun onResponse(call: Call, response: Response) {

        if (response.isSuccessful) {
            val body = response.peekBody(Long.MAX_VALUE)
            val gson = GsonBuilder().create()
            reusable = body.string()
            val mainEntryPerApi = gson.fromJson(reusable, RestApi::class.java)
            parse(mainEntryPerApi)
        }
        magowareApi?.let { onRequestCompleteListener?.onSuccess(it) }
    }

    private fun parse(response: RestApi) {
        this.magowareApi = response
    }
}


interface OnRequestCompleteListener {
    fun onSuccess(mainEntryPerApi: RestApi)
    fun onError()
}