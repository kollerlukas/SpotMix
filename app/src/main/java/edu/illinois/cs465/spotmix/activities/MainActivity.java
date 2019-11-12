package edu.illinois.cs465.spotmix.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.jetbrains.annotations.NotNull;

import edu.illinois.cs465.spotmix.R;
import edu.illinois.cs465.spotmix.api.spotify.SpotifyAuthApiFragment;
import edu.illinois.cs465.spotmix.api.spotify.SpotifyHelper;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SpotifyAuthApiFragment.Callback {

    private SpotifyHelper helper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // find static sign in fragment
        SpotifyAuthApiFragment fragment = (SpotifyAuthApiFragment)
                getSupportFragmentManager().findFragmentById(R.id.spotify_api_fragment);
        if (fragment != null) {
            // set sign in callback
            fragment.setCallback(this);
        }
    }

    @Override
    public void onSignedIn(@NotNull SpotifyHelper helper) {
        Log.d("MainActivity", "onSignedIn() called with: helper = [" + helper + "]");
        this.helper = helper;
        // enable Create party button
        findViewById(R.id.create_party_btn).setEnabled(true);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.join_party_btn:
                // start Join Activity
                startActivity(new Intent(this, JoinPartyActivity.class));
                break;
            case R.id.create_party_btn:
                // construct explicit intent
                Intent createPartyIntent = new Intent(this, CreatePartyActivity.class);
                // put SpotifyHelper as Parcelable extra
                createPartyIntent.putExtra(SpotifyHelper.PARCEL_KEY, helper);
                // start Create party activity
                startActivity(createPartyIntent);
                break;
            default:
                break;
        }
    }
}
