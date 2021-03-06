@file:Suppress("unused")

package edu.illinois.cs465.spotmix.api.spotify

import android.content.Context
import android.os.Parcelable
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.client.Subscription
import com.spotify.protocol.types.PlayerState
import edu.illinois.cs465.spotmix.api.firebase.models.QueueTrack
import edu.illinois.cs465.spotmix.api.spotify.models.TrackList
import edu.illinois.cs465.spotmix.api.spotify.models.TrackSearchRequestBase
import edu.illinois.cs465.spotmix.api.spotify.models.User
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Helper class to make the interactions with the Spotify Api and Spotify App Remote easier.
 * Manages both sign into Spotify Api and connection to Spotify App Remote.
 * The Spotify Auth Api is needed to search tracks on Spotify and get their ids.
 * The Spotify App Remote is needed to handle the music playback
 * @param accessToken token to access Spotify Auth Api
 * */
@Parcelize
class SpotifyHelper @JvmOverloads constructor(
    var accessToken: String,
    private var currentTrack: QueueTrack? = null
) :
    Subscription.EventCallback<PlayerState>, Parcelable {

    /**
     * Callback to return search results to caller.
     * */
    interface SearchTrackCallback {

        /**
         * Called when search results are available.
         * @param trackList, null if error encountered
         * */
        fun onSearchResults(trackList: TrackList?)
    }

    /**
     * Interface for getting the next track in the queue.
     * */
    interface QueueCallback {

        /**
         * Called when next track from the queue is needed.
         * @return the next track in the queue. null if queue empty
         * */
        fun getNextTrackFromQueue(): QueueTrack?
    }

    /**
     * Callback for playback state.
     * */
    interface PlaybackStateListener {

        /**
         * Called everytime the playback state changes.
         * @param state
         * */
        fun onPlaybackEvent(state: PlayerState?)
    }

    companion object {

        const val PARCEL_KEY = "api.spotify.SpotifyHelper.PARCEL_KEY"

        // Spotify client id from the Spotify console
        internal const val CLIENT_ID = "c89a81730c724897b3ad7bc91a49c9ee"
        // auth activity request code
        internal const val AUTH_TOKEN_REQUEST_CODE = 1
        // for the redirect url just put something, doesn't really matter
        // but needs to be set in Spotify console, doesn't work otherwise
        internal const val REDIRECT_URL = "callback://cs465.spotmix"
        // define Auth scopes for the Spotify Api
        internal val AUTH_SCOPES = arrayOf(
            "user-read-email",
            "app-remote-control"
        )

        // base url for the Spotify Api
        private val BASE_URL = "https://api.spotify.com/"
    }

    // handle to the Spotify App Remote
    @Suppress("PLUGIN_WARNING")
    private var spotifyAppRemote: SpotifyAppRemote? = null

    // handle for the Spotify Api
    @Suppress("PLUGIN_WARNING")
    private val service: SpotifyService

    @IgnoredOnParcel
    var queueCallback: QueueCallback? = null

    // all subscribed playback listeners
    @IgnoredOnParcel
    private var playbackListeners: MutableList<PlaybackStateListener> = mutableListOf()

    init {
        // init Gson
        val gson: Gson = GsonBuilder().setLenient().create()
        // to debug retrofit create Http interceptor
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        // init retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        // create an Api Service instance
        service = retrofit.create(SpotifyService::class.java)
    }

    /**
     * Connect the App remote.
     * @param context
     * */
    fun connect(context: Context) {
        // Set the connection parameters
        val connectionParams = ConnectionParams.Builder(CLIENT_ID)
            .setRedirectUri(REDIRECT_URL)
            .showAuthView(false)
            .build()

        SpotifyAppRemote.connect(context, connectionParams,
            object : Connector.ConnectionListener {

                override fun onConnected(spotifyAppRemote: SpotifyAppRemote) {
                    Log.d("SpotifyHelper", "App Remote connected")
                    this@SpotifyHelper.spotifyAppRemote = spotifyAppRemote
                    // subscribe to listen for PlayerState updates
                    spotifyAppRemote.playerApi
                        .subscribeToPlayerState()
                        .setEventCallback(this@SpotifyHelper)
                }

                override fun onFailure(throwable: Throwable) {
                    Log.e(
                        "SpotifyHelper",
                        "App Remote connection failed, ${throwable.message}",
                        throwable
                    )
                }
            })
    }

    /**
     * Disconnect the App remote, so the object can be passed between activities.
     * */
    fun disconnect() {
        SpotifyAppRemote.disconnect(spotifyAppRemote)
    }

    /**
     * Get the currently signed in user.
     * @param callback
     * */
    fun getUser(callback: SimpleRetrofitCallback<User>) =
        service.fetchUser(accessToken).enqueue(callback)

    /**
     * Search tracks on Spotify.
     * @param keyword
     * @param callback
     * */
    fun searchTrack(keyword: String, callback: SearchTrackCallback) {
        service.searchTracks(accessToken, keyword)
            .enqueue(object : SimpleRetrofitCallback<TrackSearchRequestBase>() {
                override fun onResult(result: TrackSearchRequestBase?) {
                    super.onResult(result)
                    // notify callback
                    callback.onSearchResults(result?.trackList)
                }
            })
    }

    /**
     * Start the music playback. Either continue paused playback or play the next song in the queue.
     * */
    fun play() {
        Log.d("SpotifyHelper", "play() called")
        if (currentTrack != null) {
            spotifyAppRemote?.playerApi?.resume()
        } else {
            currentTrack = queueCallback?.getNextTrackFromQueue()
            spotifyAppRemote?.playerApi?.play(currentTrack?.track?.uri)
        }
    }

    /**
     * Pause the current Spotify playback.
     * */
    fun pause() {
        spotifyAppRemote?.playerApi?.pause()
    }

    /**
     * Receive updates of the PlayerState
     * @param playerState
     * */
    override fun onEvent(playerState: PlayerState?) {
        // notify all suscribers
        playbackListeners.forEach { it.onPlaybackEvent(playerState) }

        if (currentTrack != null && playerState?.track?.uri != currentTrack?.track?.uri) {
            // next track has started => play next track from the queue
            currentTrack = queueCallback?.getNextTrackFromQueue()
            if (currentTrack != null) {
                spotifyAppRemote?.playerApi?.play(currentTrack!!.track.uri)
            } else {
                pause()
            }
        }
    }

    /**
     * Subscribe to the playback changes.
     * @param listener
     * */
    fun addPlaybackStateListener(listener: PlaybackStateListener) {
        playbackListeners.add(listener)
    }

    /**
     * Unsubscribe from playback changes.
     * @param listener
     * */
    fun removePlaybackStateListener(listener: PlaybackStateListener) {
        playbackListeners.remove(listener)
    }
}