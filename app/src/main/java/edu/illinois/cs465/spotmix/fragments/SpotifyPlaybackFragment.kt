package edu.illinois.cs465.spotmix.fragments

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.transition.Transition
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.graphics.toColor
import androidx.fragment.app.Fragment
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.ImageViewTarget
import com.spotify.protocol.types.PlayerState
import edu.illinois.cs465.spotmix.R
import edu.illinois.cs465.spotmix.api.firebase.FirebaseHelper
import edu.illinois.cs465.spotmix.api.firebase.models.Attendee
import edu.illinois.cs465.spotmix.api.firebase.models.Party
import edu.illinois.cs465.spotmix.api.firebase.models.QueueTrack
import edu.illinois.cs465.spotmix.api.spotify.SpotifyHelper
import kotlinx.android.synthetic.main.spotify_play_back_fragment.view.*

class SpotifyPlaybackFragment : Fragment(), View.OnClickListener, FirebaseHelper.PartyListener,
    SpotifyHelper.QueueCallback, SpotifyHelper.PlaybackStateListener {

    lateinit var party: Party

    lateinit var attendee: Attendee

    lateinit var bitmap: Bitmap

    lateinit var palette: Palette

    var spotifyHelper: SpotifyHelper? = null

    val firebaseHelper: FirebaseHelper = FirebaseHelper()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.spotify_play_back_fragment, container, false)
        // set click listeners
        v.prev_track_img_btn.setOnClickListener(this)
        v.play_pause_img_btn.setOnClickListener(this)
        v.next_track_img_btn.setOnClickListener(this)
        return v
    }

    override fun onStart() {
        super.onStart()
        if (attendee.admin) {
            // set visibility to gone
            view?.prev_track_img_btn?.visibility = View.VISIBLE
            view?.play_pause_img_btn?.visibility = View.VISIBLE
            view?.next_track_img_btn?.visibility = View.VISIBLE
        } else {
            // set visibility to gone
            view?.prev_track_img_btn?.visibility = View.GONE
            view?.play_pause_img_btn?.visibility = View.GONE
            view?.next_track_img_btn?.visibility = View.GONE
        }
        // get notified when party state changes
        firebaseHelper.addPartyListener(party, this)
        // add playback listener to update album covers
        spotifyHelper?.addPlaybackStateListener(this)
        spotifyHelper?.queueCallback = this
    }

    override fun onStop() {
        super.onStop()
        // remove listener
        firebaseHelper.removePartyListener(party, this)
        spotifyHelper?.removePlaybackStateListener(this)
        spotifyHelper?.queueCallback = null
    }

    override fun onPartyChanged(party: Party) {
        this.party = party
    }

    override fun getNextTrackFromQueue(): QueueTrack? {
        Log.d("SpotifyPlaybackFragment", "getNextTrackFromQueue() called")
        return if (party.queue.isNotEmpty()) party.queue[0] else null
    }

    override fun onPlaybackEvent(state: PlayerState?) {
        if (state == null) {
            return
        }

        if (state.isPaused) {
            firebaseHelper.paused(party)
        } else {
            firebaseHelper.playing(party)
        }

        // update play-pause button icon
        val imgRes = if (state.isPaused)
            R.drawable.ic_play_arrow_black_24dp else R.drawable.ic_pause_black_24dp
        view?.findViewById<ImageButton>(R.id.play_pause_img_btn)?.setImageResource(imgRes)

        // update album cover
        if (context != null && view != null) {
            Glide.with(context!!)
                .load(party.queue[0].track.album.images[0].url)
                .placeholder(R.drawable.ic_broken_image_black_48dp)
                .into(view!!.findViewById(R.id.current_track_album_cover_img_view))

        // Update background color to most dominant color in album cover
            bitmap = Glide.with(context!!)
                        .asBitmap()
                        .load(party.queue[0].track.album.images[0].url)
                        .submit().get()

            palette = Palette.from(bitmap).generate()
        }

        view?.findViewById<LinearLayout>(R.id.album_background)?.setBackgroundColor(palette?.getVibrantColor(0))
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.prev_track_img_btn -> {
                // TODO: implement
                Toast.makeText(context, "TODO", Toast.LENGTH_SHORT).show()
            }
            R.id.play_pause_img_btn -> {
                if (party.playing) {
                    spotifyHelper?.pause()
                } else {
                    spotifyHelper?.play()
                }
            }
            R.id.next_track_img_btn -> {
                // TODO: implement
                Toast.makeText(context, "TODO", Toast.LENGTH_SHORT).show()
            }
        }
    }
}