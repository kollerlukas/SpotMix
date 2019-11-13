package edu.illinois.cs465.spotmix.api.firebase

import com.google.firebase.database.*
import edu.illinois.cs465.spotmix.api.firebase.models.Attendee
import edu.illinois.cs465.spotmix.api.firebase.models.Party
import edu.illinois.cs465.spotmix.api.firebase.models.QueueTrack
import edu.illinois.cs465.spotmix.api.spotify.models.Track
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
        fun onPartyCreated(party: Party?, attendee: Attendee?)
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
        fun onPartyJoined(party: Party?, attendee: Attendee?)
    }

    /**
     * Interface for receiving changes to a party.
     * */
    interface PartyListener {

        /**
         * Called if there was a change in the party.
         * @param party
         * */
        fun onPartyChanged(party: Party)
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
        fun onAttendeeAdded(attendees: List<Attendee>, position: Int)

        /**
         * Called when attendee was removed from the party.
         * @param attendees
         * @param position
         * */
        fun onAttendeeRemoved(attendees: List<Attendee>, position: Int)

        /**
         * Called when attendee state changed, e.g. becoming an admin.
         * @param attendees
         * @param position
         * */
        fun onAttendeeChanged(attendees: List<Attendee>, position: Int)

        /**
         * Called when whole list changed.
         * @param attendees
         * */
        fun onAttendeeListChanged(attendees: List<Attendee>)
    }

    /**
     * Callback for adding track to the queue.
     * */
    interface AddToQueueCallback {

        /**
         * Called when track was added to the queue.
         * @param track; null in case of an error
         * */
        fun onAddedTrackToQueue(track: QueueTrack?)
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
        callback.onPartyCreated(party, attendee)
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
                callback.onPartyJoined(null, null)
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
                callback.onPartyJoined(party, attendee)
            }
        })
    }

    /**
     * Admin only: close a given party. Entirely delete the party from Firebase.
     * @param party
     * */
    fun closeParty(party: Party) {
        // get party instance from Firebase
        val partyDb = database.child(party.key!!)
        // delete party from Firebase
        partyDb.removeValue()
    }

    /**
     * Remove a given Attendee from a party.
     * @param party
     * @param attendee the attendee to remove
     * */
    fun removeAttendee(party: Party, attendee: Attendee) {
        party.attendees.remove(attendee)
        // get party instance from Firebase
        val partyDb = database.child(party.key!!)
        // update firebase
        // TODO: update value instead of setting it
        partyDb.child("attendees").setValue(party.attendees)
    }

    /**
     * Add a Spotify track to the current queue of the party.
     * @param party
     * @param track
     * */
    fun addTrackToQueue(party: Party, track: Track, callback: AddToQueueCallback) {
        // add track to queue
        val queueTrack = QueueTrack(track)
        party.queue.add(queueTrack)
        // get party instance from Firebase
        val partyDb = database.child(party.key!!)
        // update queue
        partyDb.child("queue").setValue(party.queue)
            .addOnSuccessListener { callback.onAddedTrackToQueue(queueTrack) }
            .addOnFailureListener { callback.onAddedTrackToQueue(null) }
    }

    /**
     * Upvote a track from the current party queue.
     * @param party
     * @param track
     * @param attendee the attendee voting on the track
     * */
    fun upvoteTrack(party: Party, track: QueueTrack, attendee: Attendee) {
        // upvote the track
        track.upvotes.add(attendee)
        // get party instance from Firebase
        val partyDb = database.child(party.key!!)
        // update queue
        partyDb.child("queue").setValue(party.queue)
    }

    /**
     * Downvote a track from the current party queue.
     * @param party
     * @param track
     * @param attendee the attendee voting on the track
     * */
    fun downvoteTrack(party: Party, track: QueueTrack, attendee: Attendee) {
        // downvote the track
        track.downvotes.add(attendee)
        // get party instance from Firebase
        val partyDb = database.child(party.key!!)
        // update queue
        partyDb.child("queue").setValue(party.queue)
    }

    /**
     * Get notified when the state of the party changes.
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
                    partyListeners.forEach { it.onPartyChanged(newParty) }
                }
            }
            database.child(party.key!!).addValueEventListener(partyValueEventListener!!)
        }
        partyListeners.add(listener)
    }

    /**
     * Unsubscribe from receiving party change updates.
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
     * Get notified when the attendees list changes.
     * @param party
     * @param listener
     * */
    fun addAttendeeListener(party: Party, listener: AttendeeListener) {
        if (attendeeChildEventListener == null) {
            // start with empty list
            val attendees = mutableListOf<Attendee>()
            attendeeChildEventListener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
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
                        attendeeListeners.forEach { it.onAttendeeAdded(attendees, position!!) }
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    // get attendee instance
                    val attendee = snapshot.getValue(Attendee::class.java)
                    if (attendee != null) {
                        // remove attendee
                        val position = attendees.indexOf(attendee)
                        attendees.remove(attendee)
                        // notify subscribers
                        attendeeListeners.forEach { it.onAttendeeRemoved(attendees, position) }
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
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
                        attendeeListeners.forEach { it.onAttendeeChanged(attendees, position!!) }
                    }
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                    // not handled, because shouldn't happen
                }

                override fun onCancelled(error: DatabaseError) {
                    // nothing
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
     * Unsubscribe from attendee list changes.
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