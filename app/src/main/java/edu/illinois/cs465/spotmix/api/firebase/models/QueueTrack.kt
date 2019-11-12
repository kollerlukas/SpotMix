package edu.illinois.cs465.spotmix.api.firebase.models

import android.os.Parcelable
import edu.illinois.cs465.spotmix.api.spotify.models.Track
import kotlinx.android.parcel.Parcelize

/**
 * Model for a Spotify Track in the queue.
 * */
@Parcelize
data class QueueTrack @JvmOverloads constructor(
    val track: Track,
    val upvotes: MutableList<Attendee> = mutableListOf(),
    val downvotes: MutableList<Attendee> = mutableListOf()
) : Parcelable {

    // used to reconstruct obj when reading from Firebase
    @Suppress("unused")
    constructor() : this(Track.null_track)
}