package com.example.multilingualchatapp;

import android.content.Intent;
import androidx.annotation.NonNull;
import com.google.android.material.tabs.TabLayout;

import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private Toolbar mainToolbar;
    private ViewPager vPager;
    private TabLayout tabLayout;
    private TabsAccessorAdapter tAccessorAdapter;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    protected String translationOption;
    private DatabaseReference rootRef;

    private UserState userState;

    private ImageView loadingImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        rootRef = FirebaseDatabase.getInstance().getReference();

        //if the user isn't logged in, send him to the LoginActivity, otherwise, notify the database
        //that he is online and verify his existence
        if (currentUser == null) {
            sendUserToLoginActivity();
        } else {
            userState = UserState.getUserStateInstance();
            userState.updateUserStatus("online");
            VerifyUserExistence();
        }

        initializeFields();

        //setting up the main toolbar
        setSupportActionBar(mainToolbar);
        getSupportActionBar().setTitle("  BoopChat");
        mainToolbar.setVisibility(View.INVISIBLE);

        tabLayout.setVisibility(View.INVISIBLE);
    }

    //method used to initialize the fields of the activity
    private void initializeFields() {
        mainToolbar = findViewById(R.id.main_page_toolbar);
        loadingImg = findViewById(R.id.loading_image);
        tabLayout = findViewById(R.id.main_tabs);
        vPager = findViewById(R.id.main_tabs_pager);
    }

    @Override
    protected void onStart() {
        super.onStart();

        //if the user isn't logged in, send him to the LoginActivity, otherwise, notify the database
        //that he is online and verify his existence
        if (currentUser == null) {
            loadingImg.setVisibility(View.INVISIBLE);
            sendUserToLoginActivity();
        } else {
            userState.updateUserStatus("online");
            VerifyUserExistence();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        //when the activity is resumed, notify the database that the user is online
        if (currentUser != null) {
            userState.updateUserStatus("online");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        //when the activity is paused, notify the database that the user is offline
        //(this will only be temporary if the user opened another activity)
        if (currentUser != null) {
            userState.updateUserStatus("offline");
        }
    }

    //method is used to verify the user's existence
    private void VerifyUserExistence() {
        String currentUserId = mAuth.getCurrentUser().getUid();

        rootRef.child("Users").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                //if the user has created his account but hasn't set up yet, make the loading image
                //invisible and send him to the SettingsActivity where he can choose his unique username
                if (!dataSnapshot.child("name").exists()) {
                    loadingImg.setVisibility(View.INVISIBLE);
                    sendUserToSettingsActivity();
                } else {
                    //otherwise, check whether the user has the translation feature turned on or not
                    //and display the fragments the fragments that constitute the MainActivity
                    translationOption = dataSnapshot.child("translation").getValue().toString();

                    //passing the translationOption variable, which indicates if the user has the translation
                    //feature on or off, to the tab accessor adapter so it can be passed to the ChatFragment
                    //to know whether to translate the last messages between the user and his contacts
                    //that are displayed in the ChatFragment
                    tAccessorAdapter = new TabsAccessorAdapter(getSupportFragmentManager(), translationOption);
                    vPager.setAdapter(tAccessorAdapter);
                    tabLayout.setupWithViewPager(vPager);
                    tabLayout.setupWithViewPager(vPager);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    //method called to send the user to the LoginActivity (used when the user is logged out)
    private void sendUserToLoginActivity() {
        Intent loginIntent = new Intent(this, LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }

    //method called to send user to the SettingsActivity where he can edit his account (change his profile
    //image, change his status, change his designated language, etc.)
    private void sendUserToSettingsActivity() {
        startActivity( new Intent(this, SettingsActivity.class));
    }

    //method called to send the user to FindFriendsActivity where he can look up other users and send
    //them chat requests
    private void sendUserToFindFriendsActivity() {
        startActivity(new Intent(MainActivity.this, FindFriendsActivity.class));
    }

    //initializing the options that are available to the user in the MainActivity
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.menu_options, menu);

        return true;
    }

    //handling appropriately the option selected by the user
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case R.id.main_find_friends:
                //when the user chooses the find friends option, send him to the FindFriendActivity
                //where he can search for others users by their username and send them a friend request
                //to eventually become friends with them and be able to chat them
                sendUserToFindFriendsActivity();
                break;
            case R.id.main_settings:
                //when the user chooses the settings option, send him to the SettingsActivity where
                //he can edit his account
                sendUserToSettingsActivity();
                break;
            case R.id.main_logout:
                //when the user chooses to log out, notify the database that he is offline
                userState.updateUserStatus("offline");
                //nullifying the UserState instance so it doesn't update the state of the user that
                //just logged off
                userState.nullifyUserStateInstance();
                mAuth.signOut();
                sendUserToLoginActivity();
                break;
        }

        return true;
    }
}
