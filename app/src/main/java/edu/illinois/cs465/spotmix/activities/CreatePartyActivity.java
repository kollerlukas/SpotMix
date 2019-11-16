package edu.illinois.cs465.spotmix.activities;

import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import edu.illinois.cs465.spotmix.R;
import edu.illinois.cs465.spotmix.api.firebase.FirebaseHelper;
import edu.illinois.cs465.spotmix.api.firebase.models.Attendee;
import edu.illinois.cs465.spotmix.api.firebase.models.Party;
import edu.illinois.cs465.spotmix.api.spotify.SimpleRetrofitCallback;
import edu.illinois.cs465.spotmix.api.spotify.SpotifyHelper;
import edu.illinois.cs465.spotmix.api.spotify.models.User;

public class CreatePartyActivity extends AppCompatActivity
        implements View.OnClickListener, FirebaseHelper.CreateCallback, AdapterView.OnItemSelectedListener {

    // spotify helper - testing commit
    private SpotifyHelper helper;

    private Spinner spinner;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_party);

        // get spotify helper from intent
        helper = getIntent().getParcelableExtra(SpotifyHelper.PARCEL_KEY);

        // get spinner from activity_create_party.xml
        spinner =(Spinner)findViewById(R.id.fallback_playlist_spinner);

        spinner.setOnItemSelectedListener(this);

        List<String> playlistOptions = new ArrayList<String>();
        playlistOptions.add("Fallback Playlist");
        playlistOptions.add("Playlist1");
        playlistOptions.add("Playlist2");
        playlistOptions.add("Playlist3");

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, playlistOptions);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(dataAdapter);
    }

    @Override
    public void onClick(View v) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (v.getId()) {
            case R.id.create_party_btn:
                // get party name from edit text
                EditText editText = findViewById(R.id.party_name_edit_txt);
                final String partyName = editText.getText().toString();
                // get signed in user
                helper.getUser(new SimpleRetrofitCallback<User>() {
                    @Override
                    public void onResult(@org.jetbrains.annotations.Nullable User result) {
                        super.onResult(result);
                        if (result != null) {
                            // create new party instance on Firebase
                            new FirebaseHelper().createParty(partyName, result,
                                    helper.getAccessToken(), CreatePartyActivity.this);

                        } else {
                            // couldn't get signed in user?!
                            // TODO: proper error handling
                            Toast.makeText(CreatePartyActivity.this, "Some error...",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                break;
            default:
                break;
        }
    }

    @Override
    public void onPartyCreated(@org.jetbrains.annotations.Nullable Party party,
                               @org.jetbrains.annotations.Nullable Attendee attendee) {
        if (party != null && attendee != null) {
            // construct explicit intent
            Intent intent = new Intent(this, PartyActivity.class);
            // pass party & attendee instance to party activity
            intent.putExtra(Party.PARCEL_KEY, party);
            intent.putExtra(Attendee.PARCEL_KEY, attendee);
            // start party activity
            startActivity(intent);
            // close create activity
            finish();
        } else {
            // TODO: error handling
            Toast.makeText(this, "Some error...", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        // On selecting a spinner item
        String item = adapterView.getItemAtPosition(i).toString();

        // Showing selected spinner item
        Toast.makeText(adapterView.getContext(), "Selected: " + item, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}
