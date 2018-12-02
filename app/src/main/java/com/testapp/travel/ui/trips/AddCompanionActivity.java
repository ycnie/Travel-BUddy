package com.testapp.travel.ui.trips;

import android.os.Bundle;
import android.os.ParcelFormatException;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ListView;

import com.testapp.travel.R;
import com.testapp.travel.data.model.Place;
import com.testapp.travel.data.model.Trip;
import com.testapp.travel.data.model.User;
import com.testapp.travel.utils.FirebaseUtil;
import com.testapp.travel.utils.MapUtil;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.testapp.travel.utils.Utility;

import org.lucasr.twowayview.TwoWayView;
import org.parceler.Parcels;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AddCompanionActivity extends AppCompatActivity {
    Trip trip;
    private SearchView searchView;
    CompanionListAdapter companionAdapter;
    private ListView lvCompanionList;
    ArrayList<User>companionList;
    private TwoWayView lvAddedCompanions;
    AddedCompanionImageAdapter addedCompanionImageAdapter;
    ArrayList<User> addedCompanionList;
    Set<String> addedUserIds;
    Set<String> userIds;

    private String TAG = "TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_companion);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Add Companion");
        trip=new Trip();
        trip=(Trip) Parcels.unwrap(getIntent()
                .getParcelableExtra("Trip"));

        lvCompanionList=(ListView)findViewById(R.id.lvCompanionList);
        lvAddedCompanions=(TwoWayView)findViewById(R.id.lvAddedCompanions);

        companionList=new ArrayList<User>();
        addedCompanionList=new ArrayList<User>();

        addedUserIds = new HashSet<>();
        userIds = new HashSet<>();

        //Add current user  first

        // Make sure all added companions show up in the added list on the top of screen
        DatabaseReference mAddedCompanionRef= FirebaseUtil.getTripsRef().child(trip.getTripId()).child("companion");
        mAddedCompanionRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot postSnapshot : snapshot.getChildren()) { // Iterate through all users under "companion" node of this trip
                    String value = postSnapshot.getKey(); // userId
                    FirebaseUtil.getUsersRef().child(value).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            User user=snapshot.getValue(User.class);
                            boolean added=false;
                            for(User currentUser:addedCompanionList){
                                if(currentUser.userId.equals(user.userId)){
                                    added=true;
                                }
                            }
                            if(added==false && !FirebaseUtil.getCurrentUserId().equals(user.userId)) {
                                addedCompanionList.add(user);
                                addedUserIds.add(user.userId);
                                Log.i(TAG, "add user now...and addedUserIds size is :" + addedUserIds.size());
                                addedCompanionImageAdapter.notifyDataSetChanged();
                            }
                        }
                        @Override public void onCancelled(DatabaseError databaseError) { }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });

        addedCompanionImageAdapter=new AddedCompanionImageAdapter(this,addedCompanionList);
        lvAddedCompanions.setAdapter(addedCompanionImageAdapter);

        companionAdapter=new CompanionListAdapter(this,companionList);
        lvCompanionList.setAdapter(companionAdapter);

        Log.i(TAG, "the size of companionAdapter: " + companionAdapter.getCount());


        DatabaseReference mTripsRef = FirebaseUtil.getTripsRef();
        mTripsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot tripDataSnapshot : dataSnapshot.getChildren()) { // Iterate each trip
                    String tripId = tripDataSnapshot.getKey();
                    if (tripId.equals(trip.getTripId())) {
                        continue;
                    }
                    Trip tripCandidate = tripDataSnapshot.getValue(Trip.class);
                    if (!isEqualDestination(trip.getSearchDestination(), tripCandidate.getSearchDestination())) {
                        continue;
                    }
                    if (hasMatchedDate(trip, tripCandidate)) {
                        DataSnapshot compCandidatesDS = tripDataSnapshot.child("companion");
                        for (DataSnapshot compDS : compCandidatesDS.getChildren()) {
                            String userId = compDS.getKey();
                            if (!userId.equals(FirebaseUtil.getCurrentUserId())
                                    && !userIds.contains(userId)) {
                                DatabaseReference mUserRef = FirebaseUtil.getUsersRef().child(userId);
                                mUserRef.addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        User user = dataSnapshot.getValue(User.class);
                                        Log.i(TAG, "user fetched: " + userId);
                                        if (!addedUserIds.contains(userId)) {
                                            companionList.add(user);
                                            Log.i(TAG, "companion list size: " + companionList.size());
                                            companionAdapter.notifyDataSetChanged();
                                        }
                                    }
                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });
                                userIds.add(userId);
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        lvCompanionList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,int position, long id)
            {
                User selectedFromList =(User)lvCompanionList.getItemAtPosition(position);
                boolean added=false;
                for(User user:addedCompanionList){
                    if(user.displayName.equals(selectedFromList.displayName)){
                        added=true;
                    }
                }
                if(added==false) {
                    //TODO:Add trip to collaborator's list
                   // FirebaseUtil.getUsersRef().child(selectedFromList.userId).child("trips").child(trip.getTripId()).setValue(true);
                    DatabaseReference mUserRef = FirebaseUtil.getCurrentUserRef();
                    FirebaseUtil.getTripsRef().child(trip.getTripId()).child("companion").child(selectedFromList.userId).setValue(true);
//                    FirebaseUtil.getCompanionsRef().child(selectedFromList.userId).child(mUserRef.getKey()).setValue(true);
//                    FirebaseUtil.getCompanionsRef().child(mUserRef.getKey()).child(selectedFromList.userId).setValue(true);
                    FirebaseUtil.getCompanionsRef().child(selectedFromList.userId).push().setValue(mUserRef.getKey());
                    FirebaseUtil.getCompanionsRef().child(mUserRef.getKey()).push().setValue(selectedFromList.userId);
                    addedCompanionList.add(selectedFromList);
                    addedUserIds.add(selectedFromList.userId);
                    companionList.remove(selectedFromList);
                    addedCompanionImageAdapter.notifyDataSetChanged();
                    companionAdapter.notifyDataSetChanged();
                }
            }});

//        companionAdapter=new CompanionListAdapter(this,companionList);
//        lvCompanionList.setAdapter(companionAdapter);
//        Log.i(TAG, "the size of companionAdapter: " + companionAdapter.getCount());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (item.getItemId() == android.R.id.home) {
            this.finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_companions, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setQueryHint("Search Companion...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // perform query here
                return false;
            }
            @Override
            public boolean onQueryTextChange(String query) {
                companionAdapter.getFilter().filter(query);
                return false;
            }
        });

        return true;
    }

    private boolean isEqualDestination(Place one, Place two) {
        Log.i(TAG, "check equal destinations");
        return one.getName().trim().toLowerCase().equals(two.getName().trim().toLowerCase());
    }

    private boolean hasMatchedDate(Trip one, Trip two) {
        try {
            Timestamp oneBegin = Utility.convertDateToTimestamp(one.getBeginDate());
            Timestamp twoBegin = Utility.convertDateToTimestamp(two.getBeginDate());
            Timestamp oneEnd = Utility.convertDateToTimestamp(one.getEndDate());
            Timestamp twoEnd = Utility.convertDateToTimestamp(two.getEndDate());
            if (twoEnd.before(oneBegin) || twoBegin.after(oneEnd)) {
                return false;
            }
            return true;
        } catch (ParcelFormatException e) {
            Log.e(TAG, "hasMatchedDate Error");
            Log.e(TAG, "");
            System.out.println("Parse time exception" + e);
            return false;
        }
    }

}
