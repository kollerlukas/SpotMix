package edu.illinois.cs465.spotmix.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
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
import edu.illinois.cs465.spotmix.api.spotify.models.Track;
import edu.illinois.cs465.spotmix.api.spotify.models.TrackList;

public class SearchTracksActivity extends AppCompatActivity
        implements View.OnClickListener, SpotifyHelper.SearchTrackCallback,
        FirebaseHelper.PartyListener, FirebaseHelper.AddToQueueCallback {

    // instance of a party to display
    private Party party;

    // instance of Attendee resembled by the user
    private Attendee attendee;

    private SpotifyHelper spotifyHelper;

    private FirebaseHelper firebaseHelper;

    private TrackAdapter rvAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_tracks);

        // show back button
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

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

        // find recyclerView
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        // set a LayoutManager
        recyclerView.setLayoutManager(new LinearLayoutManager(this) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return true;
            }
        });
        // set an Adapter
        rvAdapter = new TrackAdapter();
        rvAdapter.setHasStableIds(true);
        recyclerView.setAdapter(rvAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseHelper.addPartyListener(party, this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        firebaseHelper.removePartyListener(party, this);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // save party and attendee
        outState.putParcelable(Party.PARCEL_KEY, party);
        outState.putParcelable(Attendee.PARCEL_KEY, attendee);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.search_btn:
                // read text from search field
                EditText searchEditText = findViewById(R.id.search_track_edit_txt);
                String trackName = searchEditText.getText().toString();
                // search Spotify
                spotifyHelper.searchTrack(trackName, this);
                break;
            case R.id.add_track_to_queue_btn:
                // get track to add from view tag
                Track track = (Track) v.getTag();
                if (!party.isTrackInQueue(track)) {
                    // add track to queue
                    firebaseHelper.addTrackToQueue(party, track, this);
                } else {
                    Toast.makeText(this, R.string.track_already_in_queue, Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSearchResults(TrackList trackList) {
        if (trackList == null) {
            // TODO: error handling; most likely expired/invalid accessToken
            Toast.makeText(this, "Some error ...", Toast.LENGTH_SHORT).show();
            return;
        }
        // update track adapter
        rvAdapter.setTracks(trackList.getItems());
        rvAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPartyChanged(@NotNull Party party) {
        // update party instance
        this.party = party;
    }

    @Override
    public void onAddedTrackToQueue(@org.jetbrains.annotations.Nullable QueueTrack track) {
        if (track != null) {
            Toast.makeText(this, "added to queue", Toast.LENGTH_SHORT).show();
        } else {
            // TODO: error handling
            Toast.makeText(this, "Some error ...", Toast.LENGTH_SHORT).show();
        }

    }

    private static class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackHolder> {

        private List<Track> tracks = new LinkedList<>();

        void setTracks(List<Track> tracks) {
            this.tracks = tracks;
        }

        @NonNull
        @Override
        public TrackHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_view_search_track, parent, false);
            return new TrackHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull TrackHolder holder, int position) {
            // bind track to view holder
            holder.bind(tracks.get(position));
        }

        @Override
        public int getItemCount() {
            return tracks.size();
        }

        @Override
        public long getItemId(int position) {
            return tracks.get(position).getId().hashCode();
        }

        private static class TrackHolder extends RecyclerView.ViewHolder {

            TrackHolder(@NonNull View itemView) {
                super(itemView);
            }

            void bind(Track track) {
                TextView trackTitleTxtView = itemView.findViewById(R.id.track_title_txt_view);
                // set track title
                trackTitleTxtView.setText(track.getName());

                TextView artistNameTxtView = itemView.findViewById(R.id.artist_name_txt_view);
                // set artist name
                artistNameTxtView.setText(track.getArtistNames());

                // load album cover
                ImageView albumCoverImgView = itemView.findViewById(R.id.album_cover_img_view);
                Glide.with(itemView.getContext())
                        .load(track.getAlbum().getImages().get(0).getUrl())
                        .placeholder(R.drawable.ic_broken_image_48dp)
                        .into(albumCoverImgView);

                ImageButton addToQueueBtn = itemView.findViewById(R.id.add_track_to_queue_btn);
                // set tag to Image button, to know which track to add
                addToQueueBtn.setTag(track);
            }
        }
    }
}
