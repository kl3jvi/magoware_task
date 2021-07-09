package com.egeniq.programguide.utils

import android.text.Spanned
import android.text.SpannedString
import android.util.Log
import com.egeniq.programguide.EpgFragment
import com.egeniq.programguide.api.*
import com.google.gson.GsonBuilder
import okhttp3.*
import java.io.IOException

class ApiClient {
    val TAG = "API CLIENT INSTANCE"

    init {
        fetchJson()

    }

    fun fetchJson() {
        val API_URL = "https://admin.magoware.tv/epg.json"
        Log.i(TAG, "Fetching Json")

        val request = Request.Builder().url(API_URL).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body = response.body()?.string() //plain text i api
                val gson = GsonBuilder().create() // map ne gson object

                val mainEntryPerApi = gson.fromJson(body, RestApi::class.java)
                Log.i(TAG, "Json Fetched")
                simpChannel(mainEntryPerApi)

//                val channels = gson.fromJson(body, Channel::class.java)
//                val description = gson.fromJson(body, Desc::class.java)
//                val icon = gson.fromJson(body, Icon::class.java)
//                val prevShown = gson.fromJson(body, PreviouslyShown::class.java)
//                val programmes = gson.fromJson(body, Programme::class.java)

            }

            override fun onFailure(call: Call, e: IOException) {
                e.message?.let { Log.i("Connection Error", it) }
                Log.e("Connection Error", "Problem")
            }
        })
    }


    /**
     * Krijohet objekti SimpleChannel("id", "name", imageUrl)
     */
    fun simpChannel(mainEntryPerApi: RestApi): List<EpgFragment.SimpleChannel> {
        val id: String = mainEntryPerApi.tv.channel.`-id`
        val name: Spanned = SpannedString(mainEntryPerApi.tv.channel.`display-name`)
        val imageUrl: String? = mainEntryPerApi.tv.channel.icon.`-src`
        return listOf(EpgFragment.SimpleChannel(id, name, imageUrl))
    }
}