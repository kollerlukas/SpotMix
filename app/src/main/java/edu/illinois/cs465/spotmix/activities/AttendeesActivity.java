package edu.illinois.cs465.spotmix.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
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

import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

import edu.illinois.cs465.spotmix.R;
import edu.illinois.cs465.spotmix.api.firebase.FirebaseHelper;
import edu.illinois.cs465.spotmix.api.firebase.models.Attendee;
import edu.illinois.cs465.spotmix.api.firebase.models.Party;

public class AttendeesActivity extends AppCompatActivity
        implements View.OnClickListener, FirebaseHelper.AttendeeListener {

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

        if (savedInstanceState != null) {
            // retsore state
            party = savedInstanceState.getParcelable(Party.PARCEL_KEY);
        } else {
            // extract party instance from intent
            party = getIntent().getParcelableExtra(Party.PARCEL_KEY);
        }


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
        rvAdapter = new AttendeeAdapter();
        recyclerView.setAdapter(rvAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // clear attendees list
        rvAdapter.setAttendees(new LinkedList<>());
        rvAdapter.notifyDataSetChanged();
        // register attendee listener
        firebaseHelper.addAttendeeListener(party, this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // unregister attendee listener
        firebaseHelper.removeAttendeeListener(party, this);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // save state
        outState.putParcelable(Party.PARCEL_KEY, party);
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
    public void onAttendeeAdded(@NotNull List<Attendee> attendees, int position) {
        Log.d("AttendeeActivity", "onAttendeeAdded() called with: attendees = [" + attendees + "], position = [" + position + "]");
        rvAdapter.setAttendees(attendees);
        rvAdapter.notifyItemInserted(position);
    }

    @Override
    public void onAttendeeRemoved(@NotNull List<Attendee> attendees, int position) {
        Log.d("AttendeeActivity", "onAttendeeRemoved() called with: attendees = [" + attendees + "], position = [" + position + "]");
        rvAdapter.setAttendees(attendees);
        rvAdapter.notifyItemRemoved(position);
    }

    @Override
    public void onAttendeeChanged(@NotNull List<Attendee> attendees, int position) {
        Log.d("AttendeeActivity", "onAttendeeChanged() called with: attendees = [" + attendees + "], position = [" + position + "]");
        rvAdapter.setAttendees(attendees);
        rvAdapter.notifyItemChanged(position);
    }

    @Override
    public void onAttendeeListChanged(@NotNull List<Attendee> attendees) {
        Log.d("AttendeeActivity", "onAttendeeListChanged() called with: attendees = [" + attendees + "]");
        rvAdapter.setAttendees(attendees);
        rvAdapter.notifyDataSetChanged();
    }

    private static class AttendeeAdapter extends RecyclerView.Adapter<AttendeeAdapter.AttendeeHolder> {

        private List<Attendee> attendees;

        AttendeeAdapter() {
            this.attendees = new LinkedList<>();
        }

        void setAttendees(List<Attendee> attendees) {
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

                itemView.findViewById(R.id.admin_indicator)
                        .setVisibility(attendee.getAdmin() ? View.VISIBLE : View.GONE);
            }
        }
    }
}
