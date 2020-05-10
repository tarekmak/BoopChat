package com.example.multilingualchatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.ObservableSnapshotArray;
import com.firebase.ui.database.SnapshotParser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;

public class FindFriendsActivity extends AppCompatActivity {

    private Toolbar tbar;
    private RecyclerView rlist;
    private DatabaseReference usersRef;
    private TextView noResultsText;
    private ProgressBar progressBar;

    private SearchView mSearchView;

    private FirebaseAuth mAuth;

    private UserState userState;

    String savedQuery;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_friends);

        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        mAuth = FirebaseAuth.getInstance();

        userState = UserState.getUserStateInstance();

        initializeFields();

        setSupportActionBar(tbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Find Friends");
        rlist.setLayoutManager(new LinearLayoutManager(this));
    }

    //method used to initialize the fields of the activity
    private void initializeFields() {
        tbar = findViewById(R.id.find_friends_toolbar);
        rlist = findViewById(R.id.find_friends_recycler_list);
        noResultsText = findViewById(R.id.no_results_text);
        progressBar = findViewById(R.id.search_progress_bar);
    }

    //ViewHolder used for the results of the search query
    public static class FindFriendViewHolder extends RecyclerView.ViewHolder {

        TextView username, status;
        CircleImageView profileImg;
        ImageView userStateIcon;

        FindFriendViewHolder(@NonNull View itemView) {
            super(itemView);

            initializeFields(itemView);
        }

        //method used to initialize the fields of the ViewHolder
        public void initializeFields(View itemView) {
            username = itemView.findViewById(R.id.find_friends_username);
            status = itemView.findViewById(R.id.find_friends_status);
            profileImg = itemView.findViewById(R.id.find_friends_profile_image);
            userStateIcon = itemView.findViewById(R.id.find_friends_user_state_icon);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        //when the activity is started, notify the database that the user is online
        userState.updateUserStatus("online");
    }

    @Override
    protected void onResume() {
        super.onResume();

        //when the activity is resumed, notify the database that the user is online
        userState.updateUserStatus("online");
    }

    @Override
    protected void onPause() {
        super.onPause();

        //when the activity is paused, notify the database that the user is offline
        //(this will only be temporary if the user opened another activity)
        userState.updateUserStatus("offline");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        //when the configuration of the activity changes and a query was already submitted, restore
        //the query and resubmit it
        if (savedQuery != null && mSearchView != null) {
            mSearchView.setQuery(savedQuery, true);
        }
    }


    //method used to send the user to the ProfileActivity of the selected user
    private void sendUserToProfileActivity(String uid) {
        Intent profileIntent = new Intent(FindFriendsActivity.this, ProfileActivity.class);
        profileIntent.putExtra("selected_user_id", uid);
        startActivity(profileIntent);
    }

    //setting up the search feature
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.searchbar_menu, menu);
        MenuItem mSearch = menu.findItem(R.id.appSearchBar);
        mSearchView = (SearchView) mSearch.getActionView();
        mSearchView.setQueryHint("Search...");
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(final String query) {
                progressBar.setVisibility(View.VISIBLE);

                //save the query to restore it if the context is changed
                savedQuery = query;

                //hide keyboard when query is submitted
                hideSoftKeyboard(FindFriendsActivity.this);

                //hide the "no result" text everytime a query is submitted in case the user tries to
                //search for a friend after being unsuccessful in his previous attempt
                noResultsText.setVisibility(View.INVISIBLE);

                final String currentUserId = mAuth.getCurrentUser().getUid();

                //going though the users to check which one fits the query
                usersRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        List<DataSnapshot> searchResults = new ArrayList<>();
                        for (DataSnapshot val : dataSnapshot.getChildren()) {
                            //if the datasnapshot doesn't correspond to the current user's id and if
                            //it conrresponds to an user whose username contains the query the current
                            //user has submitted, add it to the search results
                            if (val.hasChild("name")) {
                                if (!currentUserId.equals(val.getKey()) && val.child("name").getValue().toString().toLowerCase().contains(query.toLowerCase())) {
                                    searchResults.add(val);
                                }
                            }
                        }

                        final List<DataSnapshot> usersFound = searchResults;

                        //setting up the results in the form of Contacts
                        SnapshotParser<Contacts> contactsSnapshotParser = new SnapshotParser<Contacts>() {
                            @NonNull
                            @Override
                            public Contacts parseSnapshot(@NonNull DataSnapshot snapshot) {
                                return snapshot.getValue(Contacts.class);
                            }
                        };
                        ObservableSnapshotArray<Contacts> searchResultsSnapshot = new ObservableSnapshotArray<Contacts>(contactsSnapshotParser) {
                            @NonNull
                            @Override
                            protected List<DataSnapshot> getSnapshots() {
                                return usersFound;
                            }
                        };
                        FirebaseRecyclerOptions<Contacts> options = new FirebaseRecyclerOptions.Builder<Contacts>().setSnapshotArray(searchResultsSnapshot).build();//.setQuery(searchResults, Contacts.class).build();

                        //displaying the results in an recycler view with the help of an adapter
                        FirebaseRecyclerAdapter<Contacts, FindFriendViewHolder> adapter = new FirebaseRecyclerAdapter<Contacts, FindFriendViewHolder>(options) {
                            @Override
                            protected void onBindViewHolder(@NonNull final FindFriendViewHolder findFriendViewHolder, final int i, @NonNull Contacts contacts) {
                                findFriendViewHolder.username.setText(contacts.getName());
                                findFriendViewHolder.status.setText(contacts.getStatus());
                                final String selected_user_id = getRef(i).getKey();

                                usersRef.child(selected_user_id).child("UserState").addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.exists() && ("online").equals(dataSnapshot.child("state").getValue().toString())) {
                                            findFriendViewHolder.userStateIcon.setVisibility(View.VISIBLE);
                                        } else {
                                            findFriendViewHolder.userStateIcon.setVisibility(View.INVISIBLE);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });

                                Picasso.get().load(contacts.getImage()).placeholder(R.drawable.profile_img).into(findFriendViewHolder.profileImg);
                                findFriendViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        sendUserToProfileActivity(selected_user_id);
                                    }
                                });

                                //if the last contact has loaded, make the progress bar invisible
                                if (i == getItemCount() - 1) {
                                    progressBar.setVisibility(View.INVISIBLE);
                                }
                            }

                            @NonNull
                            @Override
                            public FindFriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.users_display_layout, parent, false);
                                FindFriendViewHolder viewHolder = new FindFriendViewHolder(view);
                                return viewHolder;
                            }
                        };
                        rlist.setAdapter(adapter);
                        adapter.startListening();

                        //If no users are found, make "No Results Found" text visible and make the progress bar invisible
                        if (usersFound.isEmpty()) {
                            progressBar.setVisibility(View.INVISIBLE);
                            noResultsText.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    //this code snippet hides the keyboard
    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }
}
