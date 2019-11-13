package edu.illinois.cs465.spotmix.api.firebase.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Model for a party instance.
 * */
@Parcelize
data class Party @JvmOverloads constructor(
    val name: String,
    var key: String? = null,
    val queue: MutableList<QueueTrack> = mutableListOf(),
    val attendees: MutableList<Attendee> = mutableListOf(),
    // accessToken to use Spotify Api; provided by host
    var accessToken: String? = null,
    var playing: Boolean = false,
    var currentTrack: QueueTrack? = null
) : Parcelable {

    companion object {

        /**
         * To pass the party obj between activities.
         * */
        const val PARCEL_KEY = "api.firebase.Party.PARCEL_KEY"
    }

    // used to reconstruct Party obj when reading from Firebase
    @Suppress("unused")
    constructor() : this("", null)
}