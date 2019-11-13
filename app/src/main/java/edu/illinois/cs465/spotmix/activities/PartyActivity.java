package edu.illinois.cs465.spotmix.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

import edu.illinois.cs465.spotmix.R;
import edu.illinois.cs465.spotmix.api.firebase.FirebaseHelper;
import edu.illinois.cs465.spotmix.api.firebase.models.Attendee;
import edu.illinois.cs465.spotmix.api.firebase.models.Party;
import edu.illinois.cs465.spotmix.api.firebase.models.QueueTrack;
import edu.illinois.cs465.spotmix.api.spotify.SpotifyHelper;
import edu.illinois.cs465.spotmix.fragments.SpotifyPlaybackFragment;

public class PartyActivity extends AppCompatActivity
        implements View.OnClickListener, FirebaseHelper.PartyListener {

    // instance of a party to display
    private Party party;

    // instance of Attendee resembled by the user
    private Attendee attendee;

    private SpotifyHelper spotifyHelper;
    private FirebaseHelper firebaseHelper;

    private SpotifyPlaybackFragment playBackFragment;

    private QueueAdapter rvAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_party);

        if (savedInstanceState != null) {
            // restore state
            party = savedInstanceState.getParcelable(Party.PARCEL_KEY);
            attendee = savedInstanceState.getParcelable(Attendee.PARCEL_KEY);
        } else {
            // extract party instance from intent
            party = getIntent().getParcelableExtra(Party.PARCEL_KEY);
            // extract attendee instance
            attendee = getIntent().getParcelableExtra(Attendee.PARCEL_KEY);
        }


        if (party == null || attendee == null) {
            // TODO: error handling
            Toast.makeText(this, "Some error...", Toast.LENGTH_SHORT).show();
            finish();
        }

        // instantiate Spotify Helper
        spotifyHelper = new SpotifyHelper(party.getAccessToken());

        firebaseHelper = new FirebaseHelper();

        // find static spotify fragment
        playBackFragment = (SpotifyPlaybackFragment)
                getSupportFragmentManager().findFragmentById(R.id.spotify_play_back_fragment);
        if (playBackFragment != null) {
            // set spotify helper
            playBackFragment.setSpotifyHelper(spotifyHelper);
            // set party
            playBackFragment.setParty(party);
        }

        // find recyclerView
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        // set a LayoutManager
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // set an Adapter
        rvAdapter = new QueueAdapter();
        recyclerView.setAdapter(rvAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_party, menu);
        // set visibility of menu items, based on whether attendee is admin
        menu.findItem(R.id.close_party).setVisible(attendee.getAdmin());
        menu.findItem(R.id.leave_party).setVisible(!attendee.getAdmin());
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        // get notified when party state changes
        firebaseHelper.addPartyListener(party, this);
        // connect app remote
        spotifyHelper.connect(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // remove listener
        firebaseHelper.removePartyListener(party, this);
        // disconnect app remote
        spotifyHelper.disconnect();
    }

    @Override
    public void onPartyChanged(@NotNull Party party) {
        this.party = party;

        // TODO: better impl
        rvAdapter.setQueue(party.getQueue());
        rvAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // save party and attendee
        outState.putParcelable(Party.PARCEL_KEY, party);
        outState.putParcelable(Attendee.PARCEL_KEY, attendee);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.close_party:
                // delete party from Firebase
                firebaseHelper.closeParty(party);
                finish();
                return true;
            case R.id.leave_party:
                // remove self from party
                firebaseHelper.removeAttendee(party, attendee);
                finish();
                return true;
            case R.id.attendees:
                Intent attendeeIntent = new Intent(this, AttendeesActivity.class);
                // pass party instance to attendee activity
                attendeeIntent.putExtra(Party.PARCEL_KEY, party);
                // start Attendee Activity
                startActivity(attendeeIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View v) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (v.getId()) {
            case R.id.fab:
                Intent searchTracksIntent = new Intent(this, SearchTracksActivity.class);
                // pass party & attendee instance to party activity
                searchTracksIntent.putExtra(Party.PARCEL_KEY, party);
                searchTracksIntent.putExtra(Attendee.PARCEL_KEY, attendee);
                // start search Track activity
                startActivity(searchTracksIntent);
                break;
            default:
                break;
        }
    }

    private static class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.QueueHolder> {

        private List<QueueTrack> queue = new LinkedList<>();

        public void setQueue(List<QueueTrack> queue) {
            this.queue = queue;
        }

        @NonNull
        @Override
        public QueueHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // inflate item view
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_view_queue_track, parent, false);
            // return new View holder
            return new QueueHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull QueueHolder holder, int position) {
            holder.bind(queue.get(position));
        }

        @Override
        public int getItemCount() {
            return queue.size();
        }

        private static class QueueHolder extends RecyclerView.ViewHolder {

            QueueHolder(@NonNull View itemView) {
                super(itemView);
            }

            void bind(QueueTrack track) {
                TextView trackTitleTxtView = itemView.findViewById(R.id.track_title_txt_view);
                // set track title
                trackTitleTxtView.setText(track.getTrack().getName());

                TextView artistNameTxtView = itemView.findViewById(R.id.artist_name_txt_view);
                // set artist name
                artistNameTxtView.setText(track.getTrack().getArtistNames());

                // load album cover
                ImageView albumCoverImgView = itemView.findViewById(R.id.album_cover_img_view);
                Glide.with(itemView.getContext())
                        .load(track.getTrack().getAlbum().getImages().get(0).getUrl())
                        .placeholder(R.drawable.ic_broken_image_black_48dp)
                        .into(albumCoverImgView);
            }
        }
    }
}
