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
import edu.illinois.cs465.spotmix.api.spotify.models.User
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
class SpotifyHelper(var accessToken: String) : Subscription.EventCallback<PlayerState>, Parcelable {

    interface UserCallback {
        fun onUser(user: User)
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
     * */
    fun getUser(callback: SimpleRetrofitCallback<User>) =
        service.fetchUser(accessToken).enqueue(callback)

    /**
     * Search tracks on Spotify.
     * */
    fun searchTrack(keyword: String) {
        TODO("not implemented")
    }

    /**
     *
     * */
    fun play() {
        TODO("not implemented")
    }

    /**
     * Pause the current Spotify playback.
     * */
    fun pause() {
        spotifyAppRemote?.playerApi?.pause()
    }

    /**
     * Receive updates of the PlayerState
     * */
    override fun onEvent(playerState: PlayerState?) {
        TODO("not implemented")
    }
}