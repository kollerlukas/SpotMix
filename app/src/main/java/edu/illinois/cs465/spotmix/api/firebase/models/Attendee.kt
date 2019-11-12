package edu.illinois.cs465.spotmix.api.firebase.models

import android.os.Parcelable
import com.google.firebase.database.Exclude
import kotlinx.android.parcel.Parcelize

/**
 * Model for a party attendee.
 * */
@Parcelize
data class Attendee @JvmOverloads constructor(
    val name: String,
    // TODO: get device unique id
    var id: String = "",
    // whether the attendee has admin rights
    var admin: Boolean = false
) : Parcelable {

    companion object {

        /**
         * To pass the attendee obj between activities.
         * */
        const val PARCEL_KEY = "api.firebase.Attendee.PARCEL_KEY"
    }

    // used to reconstruct obj when reading from Firebase
    @Suppress("unused")
    constructor() : this("")

    /**
     * To easier use the function updateChildren() for updating elements in the Firebase Database.
     * */
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "name" to name,
            "id" to id,
            "admin" to admin
        )
    }
}