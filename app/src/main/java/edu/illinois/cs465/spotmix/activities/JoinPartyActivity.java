package edu.illinois.cs465.spotmix.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import edu.illinois.cs465.spotmix.R;
import edu.illinois.cs465.spotmix.api.firebase.FirebaseHelper;
import edu.illinois.cs465.spotmix.api.firebase.models.Attendee;
import edu.illinois.cs465.spotmix.api.firebase.models.Party;

public class JoinPartyActivity extends AppCompatActivity
        implements View.OnClickListener, FirebaseHelper.JoinCallback {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_party);
    }

    @Override
    public void onClick(View v) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (v.getId()) {
            case R.id.join_btn:
                // get party code from edit text
                EditText editText = findViewById(R.id.party_code_edit_txt);
                final String partyCode = editText.getText().toString();
                // ask user for name with AlertDialog
                // create EditText to show in AlertDialog
                final EditText nameEditText = new EditText(this);
                nameEditText.setInputType(InputType.TYPE_CLASS_TEXT);
                // create AlertDialog
                new AlertDialog.Builder(this)
                        .setView(nameEditText)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // get name from dialog text
                                String attendeeName = nameEditText.getText().toString();
                                // request to join a party
                                new FirebaseHelper().joinParty(partyCode, attendeeName,
                                        JoinPartyActivity.this);
                            }
                        })
                        // listener null, because just dismissing the dialog, doing nothing else
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                break;
            default:
                break;
        }
    }

    @Override
    public void onPartyJoined(@org.jetbrains.annotations.Nullable Party party,
                              @org.jetbrains.annotations.Nullable Attendee attendee) {
        if (party != null && attendee != null) {
            // start party activity with party
            Intent intent = new Intent(this, PartyActivity.class);
            // pass party & attendee instance to party activity
            intent.putExtra(Party.PARCEL_KEY, party);
            intent.putExtra(Attendee.PARCEL_KEY, attendee);
            // start party activity
            startActivity(intent);
            // close Join activity
            finish();
        } else {
            // TODO: error handling
            Toast.makeText(this, "Some error...", Toast.LENGTH_SHORT).show();
        }
    }
}
