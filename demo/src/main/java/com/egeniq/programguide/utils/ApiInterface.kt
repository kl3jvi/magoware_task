package com.egeniq.programguide.utils

import com.egeniq.programguide.api.RestApi
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit


interface ApiInterface {
    @GET("/epg.json")
    fun getData(): Call<RestApi>

    companion object {
        var BASE_URL = "https://admin.magoware.tv"



        fun create(): ApiInterface {
            val retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(BASE_URL)
                .build()
            return retrofit.create(ApiInterface::class.java)
        }
    }
}