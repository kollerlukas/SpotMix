package edu.illinois.cs465.spotmix.api.firebase

import android.util.Log
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
         * @param party
         * @param attendee
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
         * @param party
         * @param attendee
         * */
        fun onJoined(party: Party?, attendee: Attendee?)
    }

    /**
     * Interface for receiving changes to a party.
     * */
    interface PartyListener {

        /**
         * Called if there was a change in the party.
         * @param party
         * */
        fun onChange(party: Party)
    }

    /**
     * Interface for receiving changes to the attendees list.
     * */
    interface AttendeeListener {

        /**
         * Called when new attendee was added to the party.
         * @param attendees
         * @param position
         * */
        fun onAdded(attendees: List<Attendee>, position: Int)

        /**
         * Called when attendee was removed from the party.
         * @param attendees
         * @param position
         * */
        fun onRemoved(attendees: List<Attendee>, position: Int)

        /**
         * Called when attendee state changed, e.g. becoming an admin.
         * @param attendees
         * @param position
         * */
        fun onChanged(attendees: List<Attendee>, position: Int)

        /**
         * Called when whole list changed.
         * @param attendees
         * */
        fun listChanged(attendees: List<Attendee>)
    }

    // handle to Firebase Database
    private var database = FirebaseDatabase.getInstance().reference

    // reference to ValueEventListener to remove it later
    private var partyValueEventListener: ValueEventListener? = null
    // reference to childEventListener to remove it later
    private var attendeeChildEventListener: ChildEventListener? = null

    // all subscribed party listeners
    private var partyListeners: MutableList<PartyListener> = mutableListOf()
    // all subscribed attendee listeners
    private var attendeeListeners: MutableList<AttendeeListener> = mutableListOf()

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

            override fun onDataChange(snapshot: DataSnapshot) {
                // unregister listener
                partyDb.removeEventListener(this)
                // get party instance
                val party = snapshot.getValue(Party::class.java)
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

    /**
     * TODO
     * @param party
     * @param listener
     * */
    fun addPartyListener(party: Party, listener: PartyListener) {
        if (partyValueEventListener == null) {
            partyValueEventListener = object : ValueEventListener {
                override fun onCancelled(dataSnapshot: DatabaseError) {
                    // nothing
                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    // get party instance
                    val newParty = snapshot.getValue(Party::class.java)!!
                    // notfiy all subcribers
                    partyListeners.forEach { it.onChange(newParty) }
                }
            }
            database.child(party.key!!).addValueEventListener(partyValueEventListener!!)
        }
        partyListeners.add(listener)
    }

    /**
     * TODO
     * @param party
     * @param listener
     * */
    fun removePartyListener(party: Party, listener: PartyListener) {
        partyListeners.remove(listener)
        if (partyListeners.isEmpty()) {
            database.child(party.key!!).removeEventListener(partyValueEventListener!!)
            partyValueEventListener = null
        }
    }

    /**
     * TODO
     * @param party
     * @param listener
     * */
    fun addAttendeeListener(party: Party, listener: AttendeeListener) {
        if (attendeeChildEventListener == null) {
            // start with empty list
            val attendees = mutableListOf<Attendee>()
            attendeeChildEventListener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    Log.d("FirebaseHelper", "onChildAdded($snapshot, $previousChildName)")
                    // retrieve position of new attendee; +1 because previousChildName is reference to element in front in the list
                    var position = previousChildName?.toInt()?.plus(1)
                    if (position == null) {
                        // is first element in list
                        position = 0
                    }
                    // get attendee instance
                    val attendee = snapshot.getValue(Attendee::class.java)
                    if (attendee != null) {
                        // add new attendee
                        attendees.add(position, attendee)
                        // notify subscribers
                        attendeeListeners.forEach { it.onAdded(attendees, position!!) }
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    Log.d("FirebaseHelper", "onChildRemoved($snapshot)")
                    // get attendee instance
                    val attendee = snapshot.getValue(Attendee::class.java)
                    if (attendee != null) {
                        // remove attendee
                        val position = attendees.indexOf(attendee)
                        attendees.remove(attendee)
                        // notify subscribers
                        attendeeListeners.forEach { it.onRemoved(attendees, position) }
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    Log.d("FirebaseHelper", "onChildChanged($snapshot, $previousChildName)")
                    // retrieve position of changed attendee; +1 because previousChildName is reference to element in front in the list
                    var position = previousChildName?.toInt()?.plus(1)
                    if (position == null) {
                        // is first element in list
                        position = 0
                    }
                    // get attendee instance
                    val attendee = snapshot.getValue(Attendee::class.java)
                    if (attendee != null) {
                        // change attendee
                        attendees[position!!] = attendee
                        // notify subscribers
                        attendeeListeners.forEach { it.onChanged(attendees, position!!) }
                    }
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                    Log.d("FirebaseHelper", "onChildMoved($snapshot, $previousChildName)")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("FirebaseHelper", "onCancelled($error)")
                }

            }
            database
                .child(party.key!!)
                .child("attendees")
                .addChildEventListener(attendeeChildEventListener!!)
        }
        attendeeListeners.add(listener)
    }

    /**
     * TODO
     * @param party
     * @param listener
     * */
    fun removeAttendeeListener(party: Party, listener: AttendeeListener) {
        attendeeListeners.remove(listener)
        if (attendeeListeners.isEmpty()) {
            database
                .child(party.key!!)
                .child("attendees")
                .removeEventListener(attendeeChildEventListener!!)
            attendeeChildEventListener = null
        }
    }
}