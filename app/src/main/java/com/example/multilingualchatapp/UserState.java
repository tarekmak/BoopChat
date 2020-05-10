package com.example.multilingualchatapp;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

//Singleton class of the UserState used to notify the database of the state of the user, that is, if
//he is online or offline (the reason why the singleton pattern was used is because we only need one
//instance of the UserState object to get the app to work properly)
public class UserState {

    private DatabaseReference rootRef;
    private String currentUserId;

    //the unique instance of the UserState Object
    private static UserState instance;

    private UserState() {
        rootRef = FirebaseDatabase.getInstance().getReference();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    //method used to notify the database that the user is either online or offline according to the
    //state passed. The date and time are also saved so we can show other users when was the last time
    //the user has opened the app to his contacts when they want to chat with him.
    void updateUserStatus(String state) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");
        String currentDate = dateFormat.format(Calendar.getInstance().getTime());

        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a");
        String currentTime = timeFormat.format(Calendar.getInstance().getTime());

        Map<String, Object> onlineState = new HashMap<>();
        onlineState.put("date", currentDate);
        onlineState.put("time", currentTime);
        onlineState.put("state", state);

        rootRef.child("Users").child(currentUserId).child("UserState").updateChildren(onlineState);
    }


    //method used to return the unique instance of the UserState object
    public static UserState getUserStateInstance() {
        if (instance == null) {
            synchronized (UserState.class) {
                instance = new UserState();
            }
        }
        return instance;
    }

    //method used to nullify the UserState instance so when the user logs off from his account and
    //logs in into another account, the the current user id will correspond to the latest account the
    //user has connected to
    public static void nullifyUserStateInstance() {
        instance = null;
    }
}
