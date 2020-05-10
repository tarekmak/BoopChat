package com.example.multilingualchatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import de.hdodenhof.circleimageview.CircleImageView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private String selectedUid, currentUserId, currentState, profileImgLink;
    private CircleImageView selectedUserProfileImg;
    private TextView selectedUsername, selectedUserStatus, selectedUserLanguage;
    private Button sendRequestButton, declineButton, chatButton;

    private DatabaseReference usersRef, chatRequestRef, contactsRef, notificationRef, messagesRef;
    private FirebaseAuth mAuth;

    private UserState userState;

    private HashMap<String, String> languages_label;

    private ProgressDialog loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        selectedUid = getIntent().getExtras().get("selected_user_id").toString();

        //making sure the user is logged in
        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        }

        userState = UserState.getUserStateInstance();

        loadingBar = new ProgressDialog(this);

        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        chatRequestRef  = FirebaseDatabase.getInstance().getReference().child("Chat Requests");
        contactsRef  = FirebaseDatabase.getInstance().getReference().child("Contacts");
        notificationRef = FirebaseDatabase.getInstance().getReference().child("Notifications");
        messagesRef = FirebaseDatabase.getInstance().getReference().child("Messages");

        initializeFields();

        initializeLanguageVariables();

        //if there is data saved in savedInstanceState, restore this data
        if (savedInstanceState == null) {
            //displaying a loading bar that prompts the user to wait while the selected user's info
            //is loading
            loadingBar.setTitle("Loading the user's info");
            loadingBar.setMessage("Please Wait...");
            loadingBar.setCanceledOnTouchOutside(true); //
            loadingBar.show();

            retrieveUserInfo();
        } else {
            restoreSavedState(savedInstanceState);
        }

        declineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelChatRequest();
            }
        });


        chatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                usersRef.child(selectedUid).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        //demoralizing the datasnapshot into a Contacts object
                        Contacts contact = dataSnapshot.getValue(Contacts.class);
                        sendToChatActivity(selectedUid, dataSnapshot.child("name").getValue().toString(), contact.getImage());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        });
    }

    //method used to initialize the fields of the activity
    private void initializeFields() {
        selectedUserProfileImg = findViewById(R.id.selected_user_profileImg);
        selectedUsername = findViewById(R.id.selected_user_username_label);
        selectedUserStatus = findViewById(R.id.selected_user_status);
        selectedUserLanguage = findViewById(R.id.selected_user_language);
        sendRequestButton = findViewById(R.id.selected_user_send_request_Button);
        declineButton = findViewById(R.id.decline_request_Button);
        chatButton = findViewById(R.id.chat_button);
        currentState = "new";
    }

    //method used to initialize the variable used to determine the supported languages' abbreviations
    private void initializeLanguageVariables() {
        languages_label = new HashMap<>();
        languages_label.put("fr", "French");
        languages_label.put("en", "English");
        languages_label.put("ar", "Arabic");
        languages_label.put("tr", "Turkish");
        languages_label.put("de", "German");
        languages_label.put("ru", "Russian");
        languages_label.put("ja", "Japanese");
        languages_label.put("pt", "Portuguese");
        languages_label.put("es", "Spanish");
        languages_label.put("it", "Italian");
        languages_label.put("zh", "Chinese");
    }

    @Override
    protected void onStart() {
        super.onStart();

        //when the activity is started, notify the database that user is online
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

    //save what the selected user to restore it instead of retrieving it from the database (this  is
    //done for the sake of performance)
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("profileImgLink", profileImgLink);
        outState.putString("selectedUsername", selectedUsername.getText().toString());
        outState.putString("selectedUserStatus", selectedUserStatus.getText().toString());
        outState.putString("selectedUserLanguage", selectedUserLanguage.getText().toString());
    }

    //method used to restore the selected user's info when the context is changed
    private void restoreSavedState(Bundle savedInstanceState) {
        profileImgLink = savedInstanceState.getString("profileImgLink");
        Picasso.get().load(profileImgLink).placeholder(R.drawable.profile_img).into(selectedUserProfileImg);

        String savedSelectedUsername = savedInstanceState.getString("selectedUsername");
        if (savedSelectedUsername != null) {
            selectedUsername.setText(savedSelectedUsername);
        }

        String savedSelectedUserStatus = savedInstanceState.getString("selectedUserStatus");
        if (savedSelectedUserStatus != null) {
            selectedUserStatus.setText(savedSelectedUserStatus);
        }

        String savedSelectedUserLanguage = savedInstanceState.getString("selectedUserLanguage");
        if (savedSelectedUserLanguage != null) {
            selectedUserLanguage.setText(savedSelectedUserLanguage);
        }

        //after the selected user's info is collected and displayed, display the appropriate buttons
        //in accordance and give them appropriate behaviour in accordance to the relationship the
        //user has with the selected user
        manageChatRequests();
    }

    //retrieving the selected user's info from the database
    private void retrieveUserInfo() {
        usersRef.child(selectedUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {

                    String username = dataSnapshot.child("name").getValue().toString();
                    String status = dataSnapshot.child("status").getValue().toString();
                    String language = dataSnapshot.child("language").getValue().toString();

                    selectedUsername.setText(username);
                    selectedUserStatus.setText(status);
                    selectedUserLanguage.setText("(Speaks " + languages_label.get(language) + ")");

                    //retrieve the selected user's profile image (if he has one) and load it in the
                    //selectedUserProfileImg view
                    if (dataSnapshot.hasChild("image")) {
                        profileImgLink = dataSnapshot.child("image").getValue().toString();
                        Picasso.get().load(profileImgLink).placeholder(R.drawable.profile_img).into(selectedUserProfileImg);
                        loadingBar.dismiss();
                    } else {
                        loadingBar.dismiss();
                    }

                    //after the selected user's info is collected and displayed, display the appropriate
                    //buttons in accordance and give them appropriate behaviour in accordance to the
                    //relationship the user has with the selected user
                    manageChatRequests();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    //method used to display the appropriate buttons and give them appropriate behaviour in accordance
    //to the relationship the user has with the selected user (either they are complete strangers, friends,
    //the user sent a friend to the selected user or the selected user has sent a friend request to
    //the user)
    private void manageChatRequests() {
        //setting the appropriate text to each button and giving them appropriate behaviour
        chatRequestRef.child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild(selectedUid)) {
                    String request_type = dataSnapshot.child(selectedUid).child("request_type").getValue().toString();

                    if (("sent").equals(request_type)) {
                        currentState = "request_sent";
                        sendRequestButton.setText(getString(R.string.cancel_friend_request));
                        chatButton.setVisibility(View.INVISIBLE);
                    } else if (("received").equals(request_type)) {
                        currentState = "request_received";
                        sendRequestButton.setText(getString(R.string.accept_friend_request));
                        declineButton.setText(getString(R.string.decline_friend_request));

                        declineButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                cancelChatRequest();
                            }
                        });

                        declineButton.setVisibility(View.VISIBLE);


                    }
                } else {
                    contactsRef.child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.hasChild(selectedUid)) {
                                currentState = "friends";

                                sendRequestButton.setText(R.string.unfriend);

                                chatButton.setVisibility(View.VISIBLE);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //give the send request button the appropriate behaviour in accordance with the relationship
        //the user has with the selected user or making it invisible if the user is on his own profile
        if (!currentUserId.equals(selectedUid)) {
            sendRequestButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendRequestButton.setEnabled(false);

                    switch (currentState) {
                        case "new":
                            sendChatRequest();
                            break;
                        case "request_sent":
                            cancelChatRequest();
                            break;
                        case "request_received":
                            acceptChatRequest();
                            break;
                        case "friends":
                            unfriend();
                            break;
                    }
                }
            });
        } else {
            sendRequestButton.setVisibility(View.INVISIBLE);
        }
    }

    //method used to unfriend the user and the selected user (only available if the user and the selected
    //user are friends)
    private void unfriend() {
        //displaying a loading bar that prompts the user to wait while the friend request is being
        //accepted
        loadingBar.setTitle("Unfriending");
        loadingBar.setMessage("Please Wait...");
        loadingBar.setCanceledOnTouchOutside(true); //
        loadingBar.show();

        //disabling the the option for the user to chat with the user he just unfriended by making hte chat button invisible
        chatButton.setVisibility(View.INVISIBLE);

        //removing the current user from the selected user's contact list
        contactsRef.child(selectedUid).child(currentUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    //removing the selected user from the current user's contact list
                    contactsRef.child(currentUserId).child(selectedUid).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            //setting the current state of the relationship between the user and the
                            //selected user as strangers so the send request button acts accordingly
                            currentState = "new";
                            sendRequestButton.setEnabled(true);
                            sendRequestButton.setText(getString(R.string.send_friend_request));

                            declineButton.setVisibility(View.INVISIBLE);
                            declineButton.setEnabled(false);

                            loadingBar.dismiss();

                            //removing the messages between the selected user and the current user
                            messagesRef.child(selectedUid).child(currentUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        messagesRef.child(currentUserId).child(selectedUid).removeValue();
                                    }
                                }
                            });
                        }
                    });
                } else {
                    Toast.makeText(ProfileActivity.this, "" + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();
                }
            }
        });
    }

    //method used to make the user and the selected user friends
    private void acceptChatRequest() {
        //displaying a loading bar that prompts the user to wait while the friend request is being
        //accepted
        loadingBar.setTitle("Accepting friend request");
        loadingBar.setMessage("Please Wait...");
        loadingBar.setCanceledOnTouchOutside(true); //
        loadingBar.show();

        //marking the selected user as a friend in the user's contact list
        contactsRef.child(currentUserId).child(selectedUid).child("Contacts").setValue("saved").addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    //marking the user as a friend in the selected user's contact list
                    contactsRef.child(selectedUid).child(currentUserId).child("Contacts").setValue("saved").addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        //Removing chat request for both receiver and sender
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                //removing the friend request from the requests list of the current user
                                chatRequestRef.child(currentUserId).child(selectedUid).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            //removing the friend request from the requests list of the selected user
                                            chatRequestRef.child(selectedUid).child(currentUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        //setting the current state of the relationship
                                                        //between the user and the selected user as friends
                                                        //so the send request button (which now serves as the
                                                        //unfriend button) acts accordingly
                                                        currentState = "friends";
                                                        sendRequestButton.setEnabled(true);
                                                        sendRequestButton.setText(getString(R.string.unfriend));

                                                        //making the chat button, which permits users
                                                        //that are friends to chat with each other,
                                                        //visible
                                                        chatButton.setVisibility(View.VISIBLE);

                                                        //notifying the notification node in the database that the user has accepted
                                                        //user i's friend request so our cloud function gets triggered and sends a notification
                                                        //to user i
                                                        Map<String, String> messageNotification = new HashMap<>();
                                                        messageNotification.put("from", currentUserId);
                                                        messageNotification.put("type", "accepted");

                                                        notificationRef.child(selectedUid).push().setValue(messageNotification);
                                                        loadingBar.dismiss();
                                                    }
                                                }
                                            });
                                        } else {
                                            Toast.makeText(ProfileActivity.this, "" + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                            loadingBar.dismiss();
                                        }
                                    }
                                });
                            } else {
                                Toast.makeText(ProfileActivity.this, "" + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                loadingBar.dismiss();
                            }
                        }
                    });
                }
            }
        });
    }

    //method that serves to cancel/decline the friend request to/from the selected user
    private void cancelChatRequest() {
        //displaying a loading bar that prompts the user to wait while the friend request is being
        //cancelled
        loadingBar.setTitle("Canceling friend request");
        loadingBar.setMessage("Please Wait...");
        loadingBar.setCanceledOnTouchOutside(true); //
        loadingBar.show();

        //removing the friend request from the request list of the current user
        chatRequestRef.child(selectedUid).child(currentUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    //removing the friend request from the request list of the selected user
                    chatRequestRef.child(currentUserId).child(selectedUid).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            //setting the current state of the relationship between the user and the
                            //selected user as strangers so the send request button acts accordingly
                            currentState = "new";
                            sendRequestButton.setEnabled(true);
                            sendRequestButton.setText(getString(R.string.send_friend_request));

                            declineButton.setVisibility(View.INVISIBLE);
                            declineButton.setEnabled(false);
                            loadingBar.dismiss();
                        }
                    });
                } else {
                    Toast.makeText(ProfileActivity.this, "" + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();
                }
            }
        });
    }

    //method used for the user to send a friend request to selected user
    private void sendChatRequest() {
        //displaying a loading bar that prompts the user to wait while the friend request is being sent
        loadingBar.setTitle("Sending friend request");
        loadingBar.setMessage("Please Wait...");
        loadingBar.setCanceledOnTouchOutside(true); //
        loadingBar.show();

        //add the friend request to the request list of the current user
        chatRequestRef.child(currentUserId).child(selectedUid).child("request_type").setValue("sent").addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    //add the friend request to the request list of the selected user
                    chatRequestRef.child(selectedUid).child(currentUserId).child("request_type").setValue("received").addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                //notifying the notification node in the database that the request
                                //has been sent so our cloud function gets triggered and sends a
                                //notification to the user who is on the receiving end
                                Map<String, String> requestNotification = new HashMap<>();
                                requestNotification.put("from", currentUserId);
                                requestNotification.put("type", "request");

                                notificationRef.child(selectedUid).push().setValue(requestNotification).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        sendRequestButton.setEnabled(true);
                                        currentState = "request_sent";
                                        sendRequestButton.setText(getString(R.string.cancel_friend_request));
                                        loadingBar.dismiss();
                                    }
                                });

                            } else {
                                Toast.makeText(ProfileActivity.this, "" + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                loadingBar.dismiss();
                            }
                        }
                    });
                } else {
                    Toast.makeText(ProfileActivity.this, "" + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();
                }
            }
        });
    }

    //method used to send the user to the ChatActivity where he can chat with the selected user
    //(available only if the user and the selected user are friends)
    private void sendToChatActivity(String userId, String username, String profileImg) {
        Intent chatIntent = new Intent(ProfileActivity.this, ChatActivity.class);
        chatIntent.putExtra("receiver_userId", userId);
        chatIntent.putExtra("receiver_username", username);
        chatIntent.putExtra("receiver_profileImg", profileImg);
        startActivity(chatIntent);
        finish();
    }
}
