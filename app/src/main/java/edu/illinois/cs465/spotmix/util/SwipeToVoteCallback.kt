package edu.illinois.cs465.spotmix.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import edu.illinois.cs465.spotmix.R
import edu.illinois.cs465.spotmix.activities.PartyActivity
import edu.illinois.cs465.spotmix.api.firebase.FirebaseHelper
import edu.illinois.cs465.spotmix.api.firebase.models.Party
import edu.illinois.cs465.spotmix.api.firebase.models.QueueTrack
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
import kotlin.math.abs
import kotlin.math.min

class SwipeToVoteCallback(
    private val rvAdapter: PartyActivity.QueueAdapter,
    context: Context,
    private val partyCallback: PartyCallback
) :
    ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    /**
     * Simple interface to get current party instance from activity
     * */
    interface PartyCallback {
        fun getParty(): Party
    }

    private val upVoteIconRes = R.drawable.ic_thumb_up_24dp
    private val upVoteIconOutlineRes = R.drawable.ic_thumb_up_outline_24dp
    private val upVoteBackgroundColor = ContextCompat.getColor(context, R.color.up_vote_bg_clr)
    private val downVoteIconRes = R.drawable.ic_thumb_down_24dp
    private val downVoteIconOutlineRes = R.drawable.ic_thumb_down_outline_24dp
    private val downVoteBackgroundColor = ContextCompat.getColor(context, R.color.down_vote_bg_clr)

    private val firebaseHelper = FirebaseHelper()

    override fun onMove(
        rV: RecyclerView,
        vH: RecyclerView.ViewHolder,
        t: RecyclerView.ViewHolder
    ): Boolean = false

    override fun onSwiped(vH: RecyclerView.ViewHolder, direction: Int) {
        val attendee = rvAdapter.attendee
        val track = rvAdapter.queue[vH.adapterPosition]

        when (direction) {
            ItemTouchHelper.LEFT -> {
                if (track.hasVoted(attendee)) {
                    Toast.makeText(
                        vH.itemView.context,
                        "You already voted for ${track.track.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    firebaseHelper.downvoteTrack(partyCallback.getParty(), track, attendee)
                    Toast.makeText(
                        vH.itemView.context,
                        "downvoted ${track.track.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            ItemTouchHelper.RIGHT -> {
                if (track.hasVoted(attendee)) {
                    Toast.makeText(
                        vH.itemView.context,
                        "You already voted for ${track.track.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    firebaseHelper.upvoteTrack(partyCallback.getParty(), track, attendee)
                    Toast.makeText(
                        vH.itemView.context,
                        "upvoted ${track.track.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        rvAdapter.notifyItemChanged(vH.adapterPosition)
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float = .5f

    override fun onChildDraw(
        c: Canvas,
        rV: RecyclerView,
        vH: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val attendee = rvAdapter.attendee
        val track =
            if (vH.adapterPosition != -1) rvAdapter.queue[vH.adapterPosition] else QueueTrack()

        val progress = min(abs(dX) / (vH.itemView.right - vH.itemView.left) * 2, 1.0f)

        val builder =
            RecyclerViewSwipeDecorator.Builder(c, rV, vH, dX, dY, actionState, isCurrentlyActive)
        builder
            // Swiping to the right => up vote
            .addSwipeRightBackgroundColor(
                Color.argb(
                    (progress * 255).toInt(),
                    Color.red(upVoteBackgroundColor),
                    Color.green(upVoteBackgroundColor),
                    Color.blue(upVoteBackgroundColor)
                )
            )
            .addSwipeRightActionIcon(if (track.hasUpVoted(attendee)) upVoteIconRes else upVoteIconOutlineRes)
            .addSwipeRightLabel("${track.upvotes.size}")
            .setSwipeRightLabelColor(Color.parseColor("#c8ffffff"))
            // Swiping to the left => down vote
            .addSwipeLeftBackgroundColor(
                Color.argb(
                    (progress * 255).toInt(),
                    Color.red(downVoteBackgroundColor),
                    Color.green(downVoteBackgroundColor),
                    Color.blue(downVoteBackgroundColor)
                )
            )
            .addSwipeLeftActionIcon(if (track.hasDownVoted(attendee)) downVoteIconRes else downVoteIconOutlineRes)
            .addSwipeLeftLabel("${track.downvotes.size}")
            .setSwipeLeftLabelColor(Color.parseColor("#c8ffffff"))
            .create().decorate()
        super.onChildDraw(c, rV, vH, dX / 2, dY, actionState, isCurrentlyActive)
    }
}