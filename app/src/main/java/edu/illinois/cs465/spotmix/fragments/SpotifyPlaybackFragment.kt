package edu.illinois.cs465.spotmix.fragments

import android.animation.Animator
import android.animation.ArgbEvaluator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColor
import androidx.fragment.app.Fragment
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
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

    var spotifyHelper: SpotifyHelper? = null

    val firebaseHelper: FirebaseHelper = FirebaseHelper()

    var previousGradient: IntArray = intArrayOf(
        Color.parseColor("#212121"),
        Color.parseColor("#121212")
    )

    var animator: Animator? = null

    var gd: GradientDrawable? = null

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

        gd = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, previousGradient)
        view?.findViewById<LinearLayout>(R.id.album_background)?.background = gd


        if (attendee.admin) {
            // set visibility to gone
            view?.playback_ctrls?.visibility = View.VISIBLE
        } else {
            // set visibility to gone
            view?.playback_ctrls?.visibility = View.GONE
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
        // reload album cover
        loadAlbumCover()
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
            R.drawable.ic_play_arrow_24dp else R.drawable.ic_pause_24dp
        view?.findViewById<ImageButton>(R.id.play_pause_img_btn)?.setImageResource(imgRes)

        loadAlbumCover()
    }

    private fun loadAlbumCover() {
        val imageUri =
            if (party.queue.isNotEmpty()) party.queue[0].track.album.images[0].url else null
        val trackTitle = if (party.queue.isNotEmpty()) party.queue[0].track.name else null
        val trackArists =
            if (party.queue.isNotEmpty()) party.queue[0].track.getArtistNames() else null

        // update album cover
        if (context != null && view != null) {

            // set current track title and artist
            view!!.findViewById<TextView>(R.id.track_title_txt_view).text = trackTitle
            view!!.findViewById<TextView>(R.id.artist_name_txt_view).text = trackArists

            Glide.with(context!!)
                .load(imageUri)
                .placeholder(R.drawable.ic_broken_image_48dp)
                .into(view!!.findViewById(R.id.current_track_album_cover_img_view))

            if (imageUri != null) {
                // Update background color to most dominant color in album cover
                Glide.with(context!!)
                    .asBitmap()
                    .load(imageUri)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            val palette = Palette.from(resource).generate()
                            val color1 = palette.getMutedColor(palette.getDominantColor(0))

                            animateGradient(
                                intArrayOf(
                                    color1,
                                    // Using R.color.colorPrimaryDark does not provide the correct color
                                    Color.parseColor("#121212")
                                )
                            )
                            // Set color to most vibrant color -> else most dominant color -> else transparent
                            view?.findViewById<LinearLayout>(R.id.album_background)?.background = gd
                        }

                        override fun onLoadStarted(placeholder: Drawable?) {}
                        override fun onLoadFailed(errorDrawable: Drawable?) {}
                        override fun onLoadCleared(placeholder: Drawable?) {}
                    })
            }
        }
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

    private fun animateGradient(targetColors: IntArray) {
        val from = previousGradient
        require(from.size == targetColors.size)
        animator?.cancel()
        val arraySize = from.size
        val props = Array<PropertyValuesHolder>(arraySize) {
            PropertyValuesHolder.ofObject(
                it.toString(),
                ArgbEvaluator(),
                from[it],
                targetColors[it]
            )
        }
        val anim = ValueAnimator.ofPropertyValuesHolder(*props)
        anim.addUpdateListener { valueAnim ->
            IntArray(arraySize) { i ->
                valueAnim.getAnimatedValue(i.toString()) as Int
            }.let {
                previousGradient = it
                gd?.colors = it
            }
        }
        anim.duration = 500
        anim.start()
        animator = anim
    }
}