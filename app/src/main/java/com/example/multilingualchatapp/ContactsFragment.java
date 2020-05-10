package com.example.multilingualchatapp;


import android.content.Intent;
import android.content.res.Configuration;
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
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;


/**
 * A simple {@link Fragment} subclass.
 */
public class ContactsFragment extends Fragment {

    private View contactsView;
    private RecyclerView contactsList;
    private TextView noContactsText;

    private DatabaseReference userContactsRef, usersRef;
    private FirebaseAuth mAuth;

    private String currentUserId;

    public ContactsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        contactsView = inflater.inflate(R.layout.fragment_contacts, container, false);

        initializeFields();

        contactsList.setLayoutManager(new LinearLayoutManager(getContext()));

        mAuth = FirebaseAuth.getInstance();

        //making sure the user isn't logged out
        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        }

        userContactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts").child(currentUserId);
        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        return contactsView;
    }

    //load the contacts of the user in the onStart method
    @Override
    public void onStart() {
        super.onStart();
        loadContacts();
    }

    //make sure the contacts are loaded when the configurations of the activity is changed
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        loadContacts();
    }

    //method used to send the user to user i's ProfileActivity when he clicks on his cell in the
    //recycler view
    private void sendUserToProfileActivity(String uid) {
        Intent profileIntent = new Intent(getContext(), ProfileActivity.class);
        profileIntent.putExtra("selected_user_id", uid);
        startActivity(profileIntent);
    }

    //method used to initialize the fields of the activity
    private void initializeFields() {
        contactsList = contactsView.findViewById(R.id.contact_list);
        noContactsText = contactsView.findViewById(R.id.no_contacts_text);
    }


    //ViewHolder used for the layouts of the contacts of the user
    public static class ContactsViewHolder extends RecyclerView.ViewHolder {
        TextView username, status;
        CircleImageView profileImg;
        ImageView userStateIcon;

        public ContactsViewHolder(@NonNull View itemView) {
            super(itemView);

            initializeFields();
        }

        //method used to initialize the fields of the ViewHolder
        public void initializeFields() {
            username = itemView.findViewById(R.id.find_friends_username);
            status = itemView.findViewById(R.id.find_friends_status);
            profileImg = itemView.findViewById(R.id.find_friends_profile_image);
            userStateIcon = itemView.findViewById(R.id.find_friends_user_state_icon);
        }
    }

    //method used to load the contacts of the user
    public void loadContacts() {
        userContactsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // if user has requests, show them else make "You Have no Contacts" text visible
                if (dataSnapshot.exists()) {

                    //hide the "You Have no Chats" text in the onDataChange method so if the user gets
                    //his first friend during the session, it doesn't show the "You Have no Contacts"
                    //text, which would be fallacious
                    noContactsText.setVisibility(View.INVISIBLE);

                    FirebaseRecyclerOptions options = new FirebaseRecyclerOptions.Builder<Contacts>().setQuery(userContactsRef, Contacts.class).build();

                    FirebaseRecyclerAdapter<Contacts, ContactsViewHolder> adapter = new FirebaseRecyclerAdapter<Contacts, ContactsViewHolder>(options) {
                        @Override
                        protected void onBindViewHolder(@NonNull final ContactsViewHolder contactsViewHolder, int i, @NonNull Contacts contacts) {

                            // getting id of contact at position i in the current user's contact list
                            final String userId = getRef(i).getKey();

                            usersRef.child(userId).addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                    //deserializing the datasnapshot containing user i's data
                                    //into a Contacts object
                                    Contacts contact = dataSnapshot.getValue(Contacts.class);

                                    String username = contact.getName();
                                    String status = contact.getStatus();
                                    String profileImg = contact.getImage();

                                    //load user i's profile image, and load the default profile image
                                    //if it is null
                                    Picasso.get().load(profileImg).placeholder(R.drawable.profile_img).into(contactsViewHolder.profileImg);

                                    contactsViewHolder.username.setText(username);
                                    contactsViewHolder.status.setText(status);

                                    //retrieving the user state of user i (that is, if he is online or not)
                                    usersRef.child(userId).child("UserState").addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            //make the "online" icon next to user i's username visible
                                            //if he is online
                                            if (dataSnapshot.exists() && ("online").equals(dataSnapshot.child("state").getValue().toString())) {
                                                contactsViewHolder.userStateIcon.setVisibility(View.VISIBLE);
                                            } else {
                                                contactsViewHolder.userStateIcon.setVisibility(View.INVISIBLE);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });

                                    //set an on click listener on the ViewHolder that sends the user
                                    //to the ProfileActivity of the selected user
                                    contactsViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            sendUserToProfileActivity(userId);
                                        }
                                    });

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        }

                        @NonNull
                        @Override
                        public ContactsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.users_display_layout, parent, false);
                            ContactsViewHolder viewHolder = new ContactsViewHolder(view);
                            return viewHolder;
                        }
                    };

                    contactsList.setAdapter(adapter);
                    adapter.startListening();
                } else {
                    //if the user has no contacts yet, make the "You Have no Contacts" label visible,
                    noContactsText.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
