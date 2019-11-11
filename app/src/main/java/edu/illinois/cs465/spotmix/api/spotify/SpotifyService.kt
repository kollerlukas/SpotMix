@file:Suppress("unused", "RedundantVisibilityModifier")

package edu.illinois.cs465.spotmix.api.spotify

import edu.illinois.cs465.spotmix.api.spotify.models.TrackSearchRequestBase
import edu.illinois.cs465.spotmix.api.spotify.models.User
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * Interface for Retrofit for the Spotify API
 */
interface SpotifyService {

    /**
     * Search the Spotify Database for Tracks associated to a given keyword.
     * @param access_token a valid access token
     * @param q query keyword
     * @param type (default: "track") comma-separated list of types to search across
     * @param limit (optional) max number of results
     * @param offset (optional) index offset of first item in results
     * */
    @GET("https://api.spotify.com/v1/search")
    fun searchTracks(
        @Header("Authorization") access_token: String,
        @Query("q") q: String,
        @Query("type") type: String = "track",
        @Query("limit") limit: Int = 10,
        @Query("offset") offset: Int = 0
    ): Call<TrackSearchRequestBase>

    /**
     * Method to get the currently signed in user.
     * @param access_token a valid access token
     * */
    @GET("https://api.spotify.com/v1/me")
    fun fetchUser(@Header("Authorization") access_token: String): Call<User>
}

/**
 * Helper class to make the callback implementation a little easier.
 * Instead of calling onFailure the callback returns a null object.
 * */
public abstract class SimpleRetrofitCallback<T> : Callback<T> {

    override fun onResponse(call: Call<T>, response: Response<T>) {
        onResult(response.body())
    }

    /**
     * Method called with result.
     * */
    open fun onResult(result: T?) {

    }

    override fun onFailure(call: Call<T>, t: Throwable) {
        onResult(null)
    }
}