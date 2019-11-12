package edu.illinois.cs465.spotmix.api.firebase

import com.google.firebase.database.*
import edu.illinois.cs465.spotmix.api.firebase.models.Attendee
import edu.illinois.cs465.spotmix.api.firebase.models.Party
import edu.illinois.cs465.spotmix.api.spotify.models.User

/**
 * Helper class to simplify the interactions with Firebase Database.
 * */
class FirebaseHelper {

    /**
     * Callback used for creating parties.
     * */
    interface CreateCallback {
        /**
         * Called when creation process is done, either successful or unsuccessful.
         * When unsuccessful the instances are null.
         * */
        fun onCreated(party: Party?, attendee: Attendee?)
    }

    /**
     * Callback used for joining parties.
     * */
    interface JoinCallback {

        /**
         * Called when join process is done, either successful or unsuccessful.
         * When unsuccessful the instances are null.
         * */
        fun onJoined(party: Party?, attendee: Attendee?)
    }

    // handle to Firebase Database
    private var database = FirebaseDatabase.getInstance().reference

    /**
     * Create a new party instance. Automatically handles creates a unique and writing to Firebase.
     * @param partyName
     * @param host a reference to the Spotify Account of the host
     * @param accessToken the token to access the Spotify Api, this should be provided by the host.
     * @return a new party instance
     * */
    fun createParty(partyName: String, host: User, accessToken: String, callback: CreateCallback) {
        // create new child node
        val partyDb = database.push()
        // create party instance
        val partyKey = partyDb.key
        val party = Party(partyName, partyKey)
        // add accessToken to party so attendees can access the Spotify Api
        party.accessToken = accessToken
        // add host as attendee
        val attendee = Attendee(host.display_name, admin = true)
        party.attendees.add(attendee)
        // set party in party database
        partyDb.setValue(party)
        // return newly created party
        callback.onCreated(party, attendee)
    }

    /**
     * Join a already existing party as an attendee.
     * @param partyCode
     * @param attendeeName
     * */
    fun joinParty(partyCode: String, attendeeName: String, callback: JoinCallback) {
        // get party instance from firebase
        val partyDb = database.child(partyCode)
        // read party data from firebase
        partyDb.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(dataSnapshot: DatabaseError) {
                callback.onJoined(null, null)
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // unregister listener
                partyDb.removeEventListener(this)
                // get party instance
                val party = dataSnapshot.getValue(Party::class.java)
                // add self as attendee to party
                val attendee = Attendee(attendeeName)
                party?.attendees?.add(attendee)
                // write new party state
                // TODO: update value instead of setting it
                partyDb.child("attendees").setValue(party?.attendees)
                // return to callback
                callback.onJoined(party, attendee)
            }
        })
    }

    /**
     * Admin only: close a given party. Entirely delete the party from Firebase.
     * @param party
     * */
    fun closeParty(party: Party) {
        // get party instance from firebase
        val partyDb = database.child(party.key!!)
        // delete party from firebase
        partyDb.removeValue()
    }

    /**
     * Remove a given Attendee from a party.
     * @param party
     * @param attendee the attendee to remove
     * */
    fun removeAttendee(party: Party, attendee: Attendee) {
        party.attendees.remove(attendee)
        // get party instance from firebase
        val partyDb = database.child(party.key!!)
        // update firebase
        // TODO: update value instead of setting it
        partyDb.child("attendees").setValue(party.attendees)
    }
}