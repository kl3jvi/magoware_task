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
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.ChronoUnit
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.TimeUnit
import kotlin.random.Random


class EpgFragment : ProgramGuideFragment<EpgFragment.SimpleProgram>() {

    // Feel free to change configuration values like this:
    //
    // override val DISPLAY_CURRENT_TIME_INDICATOR = false
    // override val DISPLAY_SHOW_PROGRESS = false
//    var channels: List<SimpleChannel> = ArrayList()
    var channels: List<SimpleChannel> = ArrayList()
    var showNames: List<String> = ArrayList()
    var descrip: List<String> = ArrayList()

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

        val MIN_CHANNEL_START_TIME =
            localDate.atStartOfDay().withHour(2).truncatedTo(ChronoUnit.HOURS)
                .atZone(DISPLAY_TIMEZONE)

        val MAX_CHANNEL_START_TIME =
            localDate.atStartOfDay().withHour(8).truncatedTo(ChronoUnit.HOURS)
                .atZone(DISPLAY_TIMEZONE)

        val MIN_CHANNEL_END_TIME =
            localDate.atStartOfDay().withHour(21).truncatedTo(ChronoUnit.HOURS)
                .atZone(DISPLAY_TIMEZONE)

        val MAX_CHANNEL_END_TIME =
            localDate.plusDays(1).atStartOfDay().withHour(4).truncatedTo(ChronoUnit.HOURS)
                .atZone(DISPLAY_TIMEZONE)

        val MIN_SHOW_LENGTH_SECONDS = TimeUnit.MINUTES.toSeconds(20)
        val MAX_SHOW_LENGTH_SECONDS = TimeUnit.MINUTES.toSeconds(200)


        // marim te dhenat nga api client


        val apiInterface = ApiInterface.create().getData()
        apiInterface.enqueue(object : Callback<RestApi> {


            override fun onResponse(call: Call<RestApi>, response: Response<RestApi>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()!!

                    val channels = simpChannel(responseBody)
                    val descriptionsOfMovies = descriptionTxt(responseBody)
                    val showNames = movieNames(responseBody)

                    val channelMap =
                        mutableMapOf<String, List<ProgramGuideSchedule<SimpleProgram>>>()

                    channels.forEach { channel ->
                        val scheduleList = mutableListOf<ProgramGuideSchedule<SimpleProgram>>()
                        var nextTime =
                            randomTimeBetween(MIN_CHANNEL_START_TIME, MAX_CHANNEL_START_TIME)
                        while (nextTime.isBefore(MIN_CHANNEL_END_TIME)) {
                            val endTime = ZonedDateTime.ofInstant(
                                Instant.ofEpochSecond(
                                    nextTime.toEpochSecond() + Random.nextLong(
                                        MIN_SHOW_LENGTH_SECONDS,
                                        MAX_SHOW_LENGTH_SECONDS
                                    )
                                ), ZoneOffset.UTC
                            )

                            showNames.zip(descriptionsOfMovies) { a, b ->
                                val schedule =
                                    createSchedule(a, b, nextTime, endTime)
                                scheduleList.add(schedule)
                            }
                            nextTime = endTime
                        }
                        val endTime =
                            if (nextTime.isBefore(MAX_CHANNEL_END_TIME)) randomTimeBetween(
                                nextTime,
                                MAX_CHANNEL_END_TIME
                            ) else MAX_CHANNEL_END_TIME

                        showNames.zip(descriptionsOfMovies) { a, b ->
                            val finalSchedule =
                                createSchedule(a, b, nextTime, endTime)
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
        startTime: ZonedDateTime,
        endTime: ZonedDateTime
    ): ProgramGuideSchedule<SimpleProgram> {
        val id = Random.nextLong(100_000L)
        val metadata = DateTimeFormatter.ofPattern("'Starts at' HH:mm").format(startTime)
        return ProgramGuideSchedule.createScheduleWithProgram(
            id,
            startTime.toInstant(),
            endTime.toInstant(),
            true,
            scheduleName,
            SimpleProgram(
                id.toString(),
                description,
                metadata
            )
        )
    }

    private fun randomTimeBetween(min: ZonedDateTime, max: ZonedDateTime): ZonedDateTime {
        val randomEpoch = Random.nextLong(min.toEpochSecond(), max.toEpochSecond())
        return ZonedDateTime.ofInstant(Instant.ofEpochSecond(randomEpoch), ZoneOffset.UTC)
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
        for(element in programmeList){
            try {
                descList.add(element.desc.text)
            } catch (e:Exception){
                e.stackTraceToString()
            }
        }
        return descList
    }


}
