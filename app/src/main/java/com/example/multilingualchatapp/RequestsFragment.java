package com.example.multilingualchatapp;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
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


/**
 * A simple {@link Fragment} subclass.
 */
public class RequestsFragment extends Fragment {

    private View requestsFragmentView;
    private RecyclerView requestsList;
    private TextView noRequestsText;

    private DatabaseReference chatRequestsRef, usersRef, contactsRef, notificationRef;
    private FirebaseAuth mAuth;
    private String currentUserId;

    public RequestsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        requestsFragmentView = inflater.inflate(R.layout.fragment_requests, container, false);

        mAuth = FirebaseAuth.getInstance();

        //making sure the user isn't logged out
        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        }


        initializeFields();

        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        chatRequestsRef = FirebaseDatabase.getInstance().getReference().child("Chat Requests");
        contactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");
        notificationRef = FirebaseDatabase.getInstance().getReference().child("Notifications");

        requestsList.setLayoutManager(new LinearLayoutManager(getContext()));

        return requestsFragmentView;
    }

    //method used to initialize the fields of the activity
    private void initializeFields() {
        requestsList = requestsFragmentView.findViewById(R.id.requests_list);
        noRequestsText = requestsFragmentView.findViewById(R.id.no_requests_text);
    }

    //load the requests in the onStart method
    @Override
    public void onStart() {
        super.onStart();
        loadRequests();
    }


    //making sure the Requests are loaded when the configurations of the activity is changed
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        loadRequests();
    }

    //method used to send the user to the ProfileActivity of the selected user when the user clicks
    //on the selected user's request layout
    private void sendUserToProfileActivity(String uid) {
        Intent profileIntent = new Intent(getContext(), ProfileActivity.class);
        profileIntent.putExtra("selected_user_id", uid);
        startActivity(profileIntent);
    }

    //ViewHolder used for the layouts of the requests of the user
    public static class RequestsViewHolder extends RecyclerView.ViewHolder {

        TextView username, status;
        CircleImageView profileImg;
        Button acceptButton, declineButton;
        ImageView userStateIcon;

        RequestsViewHolder(@NonNull View itemView) {
            super(itemView);

            initializeFields();

        }

        //method used to initialize the fields of the ViewHolder
        private void initializeFields() {
            username = itemView.findViewById(R.id.find_friends_username);
            status = itemView.findViewById(R.id.find_friends_status);
            profileImg = itemView.findViewById(R.id.find_friends_profile_image);
            acceptButton = itemView.findViewById(R.id.accept_request_button);
            declineButton = itemView.findViewById(R.id.decline_request_button);
            userStateIcon = itemView.findViewById(R.id.find_friends_user_state_icon);
        }
    }

    //method used to load the friend requests of the user (whether he sent it or whether he is on the
    //receiving end)
    public void loadRequests() {
        chatRequestsRef.child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {

                //checking if the user has any friend requests
                if (dataSnapshot.exists()) {

                    //hide the "No Recent Requests" text in the onDataChange method so if the user gets
                    //any requests during the session, it doesn't show the "No Recent Requests" text,
                    //which would be fallacious
                    noRequestsText.setVisibility(View.INVISIBLE);

                    FirebaseRecyclerOptions<Contacts> options = new FirebaseRecyclerOptions.Builder<Contacts>()
                            .setQuery(chatRequestsRef.child(currentUserId), Contacts.class)
                            .build();

                    FirebaseRecyclerAdapter<Contacts, RequestsViewHolder> adapter = new FirebaseRecyclerAdapter<Contacts, RequestsViewHolder>(options) {
                        @Override
                        protected void onBindViewHolder(@NonNull final RequestsViewHolder requestsViewHolder, int i, @NonNull Contacts contacts) {

                            //retrieving the uid of the user i
                            final String list_user_id = getRef(i).getKey();

                            //making sure the user i's request is still in the user;s request list
                            if (list_user_id != null && dataSnapshot.hasChild(list_user_id)) {

                                //getting the type of the request which indicates if the user sent the
                                //request or whether he is on the receiving end
                                DatabaseReference typeRef = getRef(i).child("request_type").getRef();

                                typeRef.addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.exists()) {

                                            String type = dataSnapshot.getValue().toString();

                                            //checking whether the user is on the receiving end of the request
                                            if (("received").equals(type)) {

                                                //if the user is on the receiving end of the request,
                                                //make the accept button green and set its text to
                                                //"Accept" (as if it was the user who sent the request,
                                                //the accept button would be turned into a cancel button
                                                //which cancels the request)
                                                requestsViewHolder.acceptButton.setText(getString(R.string.accept));
                                                requestsViewHolder.acceptButton.setBackgroundColor(0xff669900);
                                                requestsViewHolder.acceptButton.setVisibility(View.VISIBLE);
                                                requestsViewHolder.declineButton.setVisibility(View.VISIBLE);

                                                //retrieving the user info to display it in the request
                                                //layout
                                                usersRef.child(list_user_id).addValueEventListener(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                        if (dataSnapshot.exists()) {

                                                            //deserializing the datasnapshot containing
                                                            //user i's data into a Contacts object
                                                            Contacts contact = dataSnapshot.getValue(Contacts.class);

                                                            //retrieving the user state of user i (that is, if he is online or not)
                                                            usersRef.child(list_user_id).child("UserState").addValueEventListener(new ValueEventListener() {
                                                                @Override
                                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                    //make the "online" icon next to user i's username visible if he is online
                                                                    if (dataSnapshot.exists() && ("online").equals(dataSnapshot.child("state").getValue().toString())) {
                                                                        requestsViewHolder.userStateIcon.setVisibility(View.VISIBLE);
                                                                    } else {
                                                                        requestsViewHolder.userStateIcon.setVisibility(View.INVISIBLE);
                                                                    }
                                                                }

                                                                @Override
                                                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                                                }
                                                            });

                                                            final String username = contact.getName();
                                                            final String profileImg = contact.getImage();

                                                            //load user i's profile image, and load
                                                            //the default profile image if it is null
                                                            Picasso.get().load(profileImg).placeholder(R.drawable.profile_img).into(requestsViewHolder.profileImg);

                                                            requestsViewHolder.username.setText(username);

                                                            if (getActivity() != null) {
                                                                requestsViewHolder.status.setText(getString(R.string.has_sent_you_a_friend_request));
                                                            }

                                                            //giving the accept button appropriate behaviour
                                                            requestsViewHolder.acceptButton.setOnClickListener(new View.OnClickListener() {
                                                                @Override
                                                                public void onClick(View v) {
                                                                    acceptChatRequest(list_user_id);
                                                                }
                                                            });


                                                            requestsViewHolder.declineButton.setOnClickListener(new View.OnClickListener() {
                                                                @Override
                                                                public void onClick(View v) {
                                                                    cancelChatRequest(list_user_id);
                                                                }
                                                            });

                                                            //set an on click listener on the ViewHolder that sends the user
                                                            //to the ProfileActivity of the selected user
                                                            requestsViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                                                                @Override
                                                                public void onClick(View v) {
                                                                    sendUserToProfileActivity(list_user_id);
                                                                }
                                                            });
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                                    }

                                                });


                                            //checking if the user sent the request
                                            } else if (("sent").equals(type)) {

                                                //if the user is on the receiving end of the request,
                                                //make the accept button red and set its text to
                                                //"Cancel")
                                                requestsViewHolder.acceptButton.setVisibility(View.VISIBLE);

                                                if (getActivity() != null) {
                                                    requestsViewHolder.acceptButton.setText(getString(R.string.cancel));
                                                }

                                                requestsViewHolder.acceptButton.setBackgroundColor(0xffcc0000);

                                                if (getActivity() != null) {
                                                    requestsViewHolder.status.setText(getActivity().getText(R.string.yet_to_reply_to_your_friend_request));
                                                }

                                                //retrieving the user info to display it in the request
                                                //layout
                                                usersRef.child(list_user_id).addValueEventListener(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                        if (dataSnapshot.exists()) {

                                                            //demoralizing the datasnapshot into a Contacts object
                                                            Contacts contact = dataSnapshot.getValue(Contacts.class);

                                                            //retrieving the user state of user i (that is, if he is online or not)
                                                            usersRef.child(list_user_id).child("UserState").addValueEventListener(new ValueEventListener() {
                                                                @Override
                                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                    //make the "online" icon next to user i's username visible if he is online
                                                                    if (dataSnapshot.exists() && ("online").equals(dataSnapshot.child("state").getValue().toString())) {
                                                                        requestsViewHolder.userStateIcon.setVisibility(View.VISIBLE);
                                                                    } else {
                                                                        requestsViewHolder.userStateIcon.setVisibility(View.INVISIBLE);
                                                                    }
                                                                }

                                                                @Override
                                                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                                                }
                                                            });

                                                            final String username = dataSnapshot.child("name").getValue().toString();
                                                            final String profileImg = contact.getImage();

                                                            //load user i's profile image, and load the default profile image if it is null
                                                            Picasso.get().load(profileImg).placeholder(R.drawable.profile_img).into(requestsViewHolder.profileImg);

                                                            requestsViewHolder.username.setText(username);

                                                            //giving the accept button (which, in this context, is a cancel button)
                                                            //appropriate behaviour
                                                            requestsViewHolder.acceptButton.setOnClickListener(new View.OnClickListener() {
                                                                @Override
                                                                public void onClick(View v) {
                                                                    cancelChatRequest(list_user_id);
                                                                }
                                                            });

                                                            //set an on click listener on the ViewHolder that sends the user
                                                            //to the ProfileActivity of the selected user
                                                            requestsViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                                                                @Override
                                                                public void onClick(View v) {
                                                                    sendUserToProfileActivity(list_user_id);
                                                                }
                                                            });
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                                    }
                                                });


                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });

                            }
                        }

                        @NonNull
                        @Override
                        public RequestsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.users_display_layout, parent, false);
                            RequestsViewHolder holder = new RequestsViewHolder(view);
                            return holder;
                        }
                    };

                    requestsList.setAdapter(adapter);
                    adapter.startListening();
                } else {
                    //if the user has no friend requests, make the "No Recent Requests" text visible
                    noRequestsText.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    //method that serves accept a friend request from user with uid; list_user_id
    private void acceptChatRequest(final String list_user_id) {
        //marking the selected user as a
        //friend in the user's contact list
        contactsRef.child(currentUserId).child(list_user_id).child("Contacts").setValue("saved").addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    //marking the user as a friend in the
                    //user i's contact list
                    contactsRef.child(list_user_id).child(currentUserId).child("Contacts").setValue("saved").addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                //removing the friend request from the current user's request list
                                chatRequestsRef.child(currentUserId).child(list_user_id).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            //removing the friend request from user i's request list
                                            chatRequestsRef.child(list_user_id).child(currentUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        //notifying the notification node in the
                                                        //database that the user has accepted user i's
                                                        //friend request so our cloud function gets
                                                        //triggered and sends a notification to user i
                                                        Map<String, String> messageNotification = new HashMap<>();
                                                        messageNotification.put("from", currentUserId);
                                                        messageNotification.put("type", "accepted");

                                                        notificationRef.child(list_user_id).push().setValue(messageNotification);
                                                    }
                                                }
                                            });
                                        }
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });
    }

    //method that serves to cancel/decline the friend request to/from the user with uid; list_user_id
    private void cancelChatRequest(final String list_user_id) {
        //removing the friend request from the user's request list
        chatRequestsRef.child(currentUserId).child(list_user_id).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    //removing the friend request from user i's request list
                    chatRequestsRef.child(list_user_id).child(currentUserId).removeValue();
                }
            }
        });
    }
}
