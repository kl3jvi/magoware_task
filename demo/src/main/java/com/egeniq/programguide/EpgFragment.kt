package com.egeniq.programguide

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.text.Spanned
import android.text.SpannedString
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.egeniq.androidtvprogramguide.ProgramGuideFragment
import com.egeniq.androidtvprogramguide.R
import com.egeniq.androidtvprogramguide.entity.ProgramGuideChannel
import com.egeniq.androidtvprogramguide.entity.ProgramGuideSchedule
import com.egeniq.programguide.api.RestApi
import com.egeniq.programguide.utils.ApiInterface
import org.threeten.bp.*

import org.threeten.bp.format.DateTimeFormatter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random


@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class EpgFragment : ProgramGuideFragment<EpgFragment.SimpleProgram>() {

    // Feel free to change configuration values like this:
    //
    // override val DISPLAY_CURRENT_TIME_INDICATOR = false
    // override val DISPLAY_SHOW_PROGRESS = false

    companion object {
        private val TAG = EpgFragment::class.java.name
    }

    data class SimpleChannel(
        override val id: String,
        override val name: Spanned?,
        override val imageUrl: String?
    ) : ProgramGuideChannel

    // You can put your own data in the program class
    data class SimpleProgram(
        val id: String,
        val description: String,
        val metadata: String
    )


    override fun onScheduleClicked(programGuideSchedule: ProgramGuideSchedule<SimpleProgram>) {

        val innerSchedule = programGuideSchedule.program
        if (innerSchedule == null) {
            // If this happens, then our data source gives partial info
            Log.w(TAG, "Unable to open schedule: $innerSchedule")
            return
        }
        if (programGuideSchedule.isCurrentProgram) {
            Toast.makeText(context, "Open live player", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Open detail page", Toast.LENGTH_LONG).show()
        }
        // Example of how a program can be updated. You could also change the underlying program.
        updateProgram(programGuideSchedule.copy(displayTitle = programGuideSchedule.displayTitle + " [clicked]"))
    }

    override fun onScheduleSelected(programGuideSchedule: ProgramGuideSchedule<SimpleProgram>?) {
        val titleView = view?.findViewById<TextView>(R.id.programguide_detail_title)
        titleView?.text = programGuideSchedule?.displayTitle
        val metadataView = view?.findViewById<TextView>(R.id.programguide_detail_metadata)
        metadataView?.text = programGuideSchedule?.program?.metadata
        val descriptionView = view?.findViewById<TextView>(R.id.programguide_detail_description)
        descriptionView?.text = programGuideSchedule?.program?.description
        val imageView = view?.findViewById<ImageView>(R.id.programguide_detail_image) ?: return
        if (programGuideSchedule != null) {
            Glide.with(imageView)
                .load("https://picsum.photos/462/240?random=" + programGuideSchedule.displayTitle.hashCode())
                .centerCrop()
                .error(R.drawable.programguide_icon_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transition(withCrossFade())
                .into(imageView)
        } else {
            Glide.with(imageView).clear(imageView)
        }
    }

    override fun isTopMenuVisible(): Boolean {
        return false
    }

    @SuppressLint("CheckResult")
    override fun requestingProgramGuideFor(localDate: LocalDate) {
        // Faking an asynchronous loading here
        setState(State.Loading)

        // marim te dhenat nga api client
        val apiInterface = ApiInterface.create().getData()
        apiInterface.enqueue(object : Callback<RestApi> {

            override fun onResponse(call: Call<RestApi>, response: Response<RestApi>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()!!

                    val channels = simpChannel(responseBody) // channelList
                    val showNames = movieNames(responseBody) // Programmes Title
                    val descriptionsOfMovies =
                        descriptionTxt(responseBody)// Programmes Description
                    val timePairs = getTimes(responseBody) // Start/Stop Time per program


                    val channelMap =
                        mutableMapOf<String, List<ProgramGuideSchedule<SimpleProgram>>>()

                    for (channel in channels) {
                        val scheduleList = mutableListOf<ProgramGuideSchedule<SimpleProgram>>()
                        for (i in 0..showNames.size - 2) {
                            val title = showNames[i]
                            val description = descriptionsOfMovies[i]
                            val startTime = parseTime(timePairs[i].first)
                            val stopTime = parseTime(timePairs[i].second)

                            val finalSchedule = createSchedule(
                                title, description,
                                startTime,
                                stopTime
                            )
                            scheduleList.add(finalSchedule)

                        }
                        channelMap[channel.id] = scheduleList
                    }
                    val pair = Pair(channels, channelMap)
                    Handler(Looper.getMainLooper()).post { //thread per te bere ndryshime ne UI
                        setData(pair.first, pair.second, localDate)
                        if (pair.first.isEmpty() || pair.second.isEmpty()) {
                            setState(State.Error("No channels loaded."))
                        } else {
                            setState(State.Content)
                        }
                    }
                } else {
                    Log.e("ERROR", "Nuk mund te aksesoj api")
                }
            }

            override fun onFailure(call: Call<RestApi>, t: Throwable) {
                Log.e("ERROR", t.stackTraceToString())
            }
        })
    }


    private fun createSchedule(
        scheduleName: String,
        description: String,
        startTime: Long,
        endTime: Long
    ): ProgramGuideSchedule<SimpleProgram> {
        val id = Random.nextLong(100_000L) // Id per cdo schedule unike
        val dateTime: ZonedDateTime = Instant.ofEpochSecond(startTime)
            .atZone(ZoneOffset.UTC  )
        val metadata = DateTimeFormatter.ofPattern("'Starts at' HH:mm").format(dateTime)
        return ProgramGuideSchedule.createScheduleWithProgram(
            id,
            Instant.ofEpochSecond(startTime),
            Instant.ofEpochSecond(endTime),
            true,
            scheduleName,
            SimpleProgram(
                id.toString(),
                description,
                metadata
            )
        )
    }


    @SuppressLint("SimpleDateFormat")
    private fun parseTime(time: String): Long {
        //2021-07-08T21:43:04.00+02:00[Asia/Calcutta]
        //2021-07-08T21:43-04:00[UTC]
        val df = SimpleDateFormat("yyyyMMddHHmmss");
        val date: Date = df.parse(time);
        val epoch = date.time / 1000
        return epoch
    }


    override fun requestRefresh() {
        // You can refresh other data here as well.
        requestingProgramGuideFor(currentDate)
    }

    fun simpChannel(mainEntryPerApi: RestApi): List<SimpleChannel> {
        val id: String = mainEntryPerApi.tv.channel.id
        val name: Spanned = SpannedString(mainEntryPerApi.tv.channel.displayName)
        val imageUrl: String = mainEntryPerApi.tv.channel.icon.src
        return listOf(SimpleChannel(id, name, imageUrl))
    }

    fun movieNames(mainEntryPerApi: RestApi): List<String> {
        val programmeList = mainEntryPerApi.tv.programme
        val list = ArrayList<String>()
        for (element in programmeList) {
            list.add(element.title.text)
        }
        return list
    }

    fun descriptionTxt(mainEntryPerApi: RestApi): List<String> {
        val programmeList = mainEntryPerApi.tv.programme
        val descList: ArrayList<String> = ArrayList()
        for (element in programmeList) {
            try {
                descList.add(element.desc.text)
            } catch (e: Exception) {
                e.stackTraceToString()
            }
        }
        return descList
    }


    private fun getTimes(responseBody: RestApi): List<Pair<String, String>> {
        val programmeList = responseBody.tv.programme
        val timePairs: ArrayList<Pair<String, String>> = ArrayList()

        for (element in programmeList) {
            try {
                timePairs.add(Pair(element.start, element.stop))
            } catch (e: Exception) {
                e.stackTraceToString()
            }
        }
        return timePairs
    }


}
