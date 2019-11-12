package edu.illinois.cs465.spotmix.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.ChildEventListener;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import edu.illinois.cs465.spotmix.R;
import edu.illinois.cs465.spotmix.api.firebase.FirebaseHelper;
import edu.illinois.cs465.spotmix.api.firebase.models.Attendee;
import edu.illinois.cs465.spotmix.api.firebase.models.Party;

public class AttendeesActivity extends AppCompatActivity
        implements View.OnClickListener, FirebaseHelper.PartyListener {

    // instance of a party to display attendees
    private Party party;

    private FirebaseHelper firebaseHelper;

    private AttendeeAdapter rvAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendees);

        // show back button
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // extract party instance from intent
        party = getIntent().getParcelableExtra(Party.PARCEL_KEY);

        if (party == null) {
            // TODO: error handling
            Toast.makeText(this, "Some error...", Toast.LENGTH_SHORT).show();
            finish();
        }

        firebaseHelper = new FirebaseHelper();

        // find recyclerView
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        // set a LayoutManager
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // set an Adapter
        rvAdapter = new AttendeeAdapter(party.getAttendees());
        recyclerView.setAdapter(rvAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // register party listener
        firebaseHelper.addPartyListener(party, this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // unregister party listener
        firebaseHelper.removePartyListener(party, this);
    }

    @Override
    public void onClick(View v) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (v.getId()) {
            case R.id.search_btn:
                // TODO: implement
                Toast.makeText(this, "TODO", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
    }

    @Override
    public void onChange(@NotNull Party party) {
        this.party = party;
        // set new attendees
        rvAdapter.setAttendees(party.getAttendees());
        // show update
        rvAdapter.notifyDataSetChanged();
    }

    private static class AttendeeAdapter extends RecyclerView.Adapter<AttendeeAdapter.AttendeeHolder> {

        private List<Attendee> attendees;

        AttendeeAdapter(List<Attendee> attendees) {
            this.attendees = attendees;
        }

        public void setAttendees(List<Attendee> attendees) {
            this.attendees = attendees;
        }

        @NonNull
        @Override
        public AttendeeHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // inflate item view
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_view_attendee, parent, false);
            // return new View holder
            return new AttendeeHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull AttendeeHolder holder, int position) {
            // bind attendee to view holder
            holder.bind(attendees.get(position));
        }

        @Override
        public int getItemCount() {
            return attendees.size();
        }

        private static class AttendeeHolder extends RecyclerView.ViewHolder {

            AttendeeHolder(@NonNull View itemView) {
                super(itemView);
            }

            void bind(Attendee attendee) {
                // find text view
                TextView nameTv = itemView.findViewById(R.id.attendee_name);
                // set attendee name as text
                nameTv.setText(attendee.getName());
            }
        }
    }
}