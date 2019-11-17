package edu.illinois.cs465.spotmix.api.spotify

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.spotify.sdk.android.authentication.AuthenticationClient
import com.spotify.sdk.android.authentication.AuthenticationRequest
import com.spotify.sdk.android.authentication.AuthenticationResponse
import edu.illinois.cs465.spotmix.R
import edu.illinois.cs465.spotmix.api.spotify.models.User
import kotlinx.android.synthetic.main.spotify_sign_in_fragment.*
import kotlinx.android.synthetic.main.spotify_sign_in_fragment.view.*

/**
 * Fragment for handling the Ui and logic of the Spotify Auth Api Sign in.
 * Put this in the MainActivity. When not signed in will display 'Sign-In' button, once signed in
 * it displays the user profile image with the name.
 * Hands back an instance of SpotifyHelper to the Activity.
 * */
class SpotifyAuthApiFragment : Fragment(), View.OnClickListener {

    /**
     * Callback for handing back an instance of SpotifyHelper.
     * */
    interface Callback {
        fun onSignedIn(helper: SpotifyHelper)
    }

    var callback: Callback? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.spotify_sign_in_fragment, container, false)
        // set sign in button listener
        v.sign_in_btn.setOnClickListener(this)
        // return fragment view
        return v
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.sign_in_btn -> {
                // build the Auth request for Spotify Auth Api
                val request: AuthenticationRequest = AuthenticationRequest.Builder(
                    SpotifyHelper.CLIENT_ID,
                    AuthenticationResponse.Type.TOKEN,
                    SpotifyHelper.REDIRECT_URL
                ).setScopes(SpotifyHelper.AUTH_SCOPES).build()
                // create intent to start Spotify LoginActivity
                val intent = AuthenticationClient.createLoginActivityIntent(activity, request)
                // request sign in for the Spotify auth api
                startActivityForResult(intent, SpotifyHelper.AUTH_TOKEN_REQUEST_CODE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d("SpotifyAuthApiFragment", "onActivityResult() called");

        // Result after trying to login
        val response = AuthenticationClient.getResponse(resultCode, data)
        when (response.type) {
            AuthenticationResponse.Type.TOKEN -> {
                // Response was successful and contains auth token
                val accessToken = "Bearer " + response.accessToken

                Log.d("SpotifyAuthApiFragment", "accessToken: $accessToken")

                // create new SpotifyHelper instance
                val helper = SpotifyHelper(accessToken)
                // pass SpotifyHelper to Activity
                callback?.onSignedIn(helper)
                // get user image, name and display
                helper.getUser(object : SimpleRetrofitCallback<User>() {
                    override fun onResult(result: User?) {
                        super.onResult(result)
                        Log.d("SpotifyAuthApiFragment", "result: $result")
                        if (result != null) {
                            // hide sign in button
                            sign_in_btn.visibility = View.GONE
                            // load user profile image
                            Glide.with(context!!)
                                .load(if (result.images.isNotEmpty()) result.images[0].url else null)
                                .placeholder(R.drawable.ic_account_circle_black_48dp)
                                .into(user_profile_img)
                            user_profile_img.visibility = View.VISIBLE
                            // display user name
                            user_name.text = result.display_name
                            user_name.visibility = View.VISIBLE
                        }
                    }
                })
            }
            AuthenticationResponse.Type.ERROR ->
                // Auth flow returned an error
                TODO("not implemented")
            else -> {
            }
        }// Most likely auth flow was cancelled
    }
}