package edu.illinois.cs465.spotmix.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import edu.illinois.cs465.spotmix.R;
import edu.illinois.cs465.spotmix.api.firebase.FirebaseHelper;
import edu.illinois.cs465.spotmix.api.firebase.models.Attendee;
import edu.illinois.cs465.spotmix.api.firebase.models.Party;
import edu.illinois.cs465.spotmix.api.spotify.SpotifyHelper;

public class PartyActivity extends AppCompatActivity {

    // instance of a party to display
    private Party party;

    // instance of Attendee resembled by the user
    private Attendee attendee;

    private SpotifyHelper spotifyHelper;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_party);

        // extract party instance from intent
        party = getIntent().getParcelableExtra(Party.PARCEL_KEY);
        // extract attendee instance
        attendee = getIntent().getParcelableExtra(Attendee.PARCEL_KEY);

        if (party == null || attendee == null) {
            // TODO: error handling
            Toast.makeText(this, "Some error...", Toast.LENGTH_SHORT).show();
            finish();
        }

        // instantiate Spotify Helper
        spotifyHelper = new SpotifyHelper(party.getAccessToken());

        firebaseHelper = new FirebaseHelper();
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
}
