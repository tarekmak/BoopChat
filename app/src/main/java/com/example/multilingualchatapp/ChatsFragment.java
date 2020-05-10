package com.example.multilingualchatapp;


import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.squareup.picasso.Picasso;



/**
 * A simple {@link Fragment} subclass.
 */
public class ChatsFragment extends Fragment {

    private View chatsView;
    private RecyclerView chatsList;
    private TextView noChatsText;

    private DatabaseReference userContactsRef, usersRef, messagesRef;
    protected FirebaseAuth mAuth;

    private String currentUserId, translationOption;

    private TranslationService translationService;

    private TabLayout tabLayout;
    private Toolbar mainToolbar;
    private ImageView loadingImg;

    public ChatsFragment() {
        // Required empty public constructor

    }


    public ChatsFragment(String translationOption) {
        this.translationOption = translationOption;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        chatsView = inflater.inflate(R.layout.fragment_chats, container, false);

        initializeFields();

        chatsList.setLayoutManager(new LinearLayoutManager(getContext()));

        mAuth = FirebaseAuth.getInstance();

        //making sure the user is logged in
        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        }

        userContactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts").child(currentUserId);
        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        messagesRef = FirebaseDatabase.getInstance().getReference().child("Messages").child(currentUserId);

        return chatsView;
    }


    //method used to initialize the fields of the activity
    private void initializeFields() {
        chatsList = chatsView.findViewById(R.id.chats_list);
        noChatsText = chatsView.findViewById(R.id.no_chats_text);

        //getting the loading image and the main toolbar to make them invisible and visible
        //respectively when the chats are loaded
        tabLayout = getActivity().findViewById(R.id.main_tabs);
        mainToolbar = getActivity().findViewById(R.id.main_page_toolbar);
        loadingImg = getActivity().findViewById(R.id.loading_image);
    }

    //load the chats in the onStart method
    @Override
    public void onStart() {
        super.onStart();
        loadChats();
    }

    @Override
    public void onResume() {
        super.onResume();

        //when the life cycle gets to the onResume method, make the loading image invisible and the
        //main toolbar visible
        makeMainToolbarVisible();
    }

    //making sure the chats are loaded when the configurations of the activity is changed
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        loadChats();
    }

    //method used to send the user to the ChatActivity where he can chat with user i when he clicks
    //on his cell in the recycler view
    private void sendUserToChatActivity(String userId, String username, String profileImg) {
        Intent chatIntent = new Intent(getContext(), ChatActivity.class);
        chatIntent.putExtra("receiver_userId", userId);
        chatIntent.putExtra("receiver_username", username);
        chatIntent.putExtra("receiver_profileImg", profileImg);
        startActivity(chatIntent);
    }

    //method used to make the toolbar of the main activity visible
    private void makeMainToolbarVisible() {
        loadingImg.setVisibility(View.INVISIBLE);
        mainToolbar.setVisibility(View.VISIBLE);
        tabLayout.setVisibility(View.VISIBLE);
    }


    //ViewHolder used for the cells of the chats between the user and the contacts
    public static class ChatsViewHolder extends RecyclerView.ViewHolder {
        TextView username, lastMessage;
        CircleImageView profileImg;
        ImageView userStateIcon;
        RelativeLayout userLayout;

        ChatsViewHolder(@NonNull View itemView) {
            super(itemView);

            initializeFields();
        }

        //method used to initialize the fields of the ViewHolder
        private void initializeFields() {
            username = itemView.findViewById(R.id.find_friends_username);
            lastMessage = itemView.findViewById(R.id.find_friends_status);
            profileImg = itemView.findViewById(R.id.find_friends_profile_image);
            userStateIcon = itemView.findViewById(R.id.find_friends_user_state_icon);
            userLayout = itemView.findViewById(R.id.user_display_layout);
        }
    }

    //method used to load the chats of the user
    public void loadChats() {
        //loading all the user's contacts alongside the last messages with those contacts
        userContactsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {

                    //hide the "You Have no Chats" text in the onDataChange method so if the user gets
                    //his first friend during the session, it doesn't show the "You Have no Chats"
                    //text, which would be fallacious
                    noChatsText.setVisibility(View.INVISIBLE);

                    translationService = TranslationService.getTranslationInstance();

                    FirebaseRecyclerOptions options = new FirebaseRecyclerOptions.Builder<Contacts>().setQuery(userContactsRef, Contacts.class).build();

                    final FirebaseRecyclerAdapter<Contacts, ChatsViewHolder> adapter = new FirebaseRecyclerAdapter<Contacts, ChatsViewHolder>(options) {
                        @Override
                        protected void onBindViewHolder(@NonNull final ChatsViewHolder chatsViewHolder, final int i, @NonNull Contacts contacts) {
                            //retrieving the uid of user i
                            final String userId = getRef(i).getKey();

                            if (userId != null && dataSnapshot.hasChild(userId)) {

                                usersRef.child(userId).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.exists()) {

                                            //deserializing the datasnapshot containing user i's data
                                            //into a Contacts object
                                            Contacts contact = dataSnapshot.getValue(Contacts.class);

                                            //retrieving the user state of user i (that is, if he is
                                            //online or not)
                                            usersRef.child(userId).child("UserState").addValueEventListener(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                    //make the "online" icon next to user i's
                                                    //username visible if he is online
                                                    if (dataSnapshot.exists() && ("online").equals(dataSnapshot.child("state").getValue().toString())) {
                                                        chatsViewHolder.userStateIcon.setVisibility(View.VISIBLE);
                                                    } else {
                                                        chatsViewHolder.userStateIcon.setVisibility(View.INVISIBLE);
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                                }
                                            });

                                            final String username = contact.getName();
                                            final String profileImg = contact.getImage();

                                            //load user i's profile image, and load the default
                                            //profile image if it is null
                                            Picasso.get().load(profileImg).placeholder(R.drawable.profile_img).into(chatsViewHolder.profileImg);
                                            
											chatsViewHolder.username.setText(username);

                                            //retrieving the last message between the user and user i
                                            messagesRef.child(userId).orderByKey().limitToLast(1).addValueEventListener(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                    if (dataSnapshot.exists()) {
                                                        //retrieving the datasnapshot containing the
                                                        //last message between the user and user i
                                                        DataSnapshot lastMessageDataSnapshot = dataSnapshot.getChildren().iterator().next();

                                                        //checking whether the last message is a text
                                                        //message or an image message
                                                        if (("text").equals(lastMessageDataSnapshot.child("type").getValue().toString())) {
                                                            String messageBody = lastMessageDataSnapshot.child("body").getValue().toString();

                                                            //if the user sent the last message,
                                                            //simply display the message below
                                                            //user i's message
                                                            if ((currentUserId).equals(lastMessageDataSnapshot.child("from").getValue().toString())) {
                                                                chatsViewHolder.lastMessage.setText("You: " + messageBody);
                                                            } else {
                                                                //otherwise, check if the user has
                                                                //turned on the translation feature
                                                                if (("on").equals(translationOption)) {
                                                                    //if the user has it turned on,
                                                                    //translate the message in the
                                                                    //user's designated language
                                                                    messageBody = translationService.translate(lastMessageDataSnapshot.child("body").getValue().toString());
                                                                }

                                                                //if the user hasn't read the latest
                                                                //message yet, then make the chat
                                                                //section appear lighter
                                                                if (("delivered").equals(lastMessageDataSnapshot.child("state").getValue().toString())) {
                                                                    chatsViewHolder.userLayout.setBackgroundResource(R.color.colorNewMessageBackground);
                                                                }

                                                                chatsViewHolder.lastMessage.setText(username + ": " + messageBody);
                                                            }
                                                        } else {
                                                            //if the latest message is an image,
                                                            //display "You sent an image" if the you
                                                            //were the sender, and indicate that the
                                                            //user i has sent an image otherwise
                                                            if ((currentUserId).equals(lastMessageDataSnapshot.child("from").getValue().toString())  && getActivity() != null) {
                                                                chatsViewHolder.lastMessage.setText(getActivity().getText(R.string.you_sent_an_image));
                                                            } else {
                                                                //if the user hasn't read the latest
                                                                //message yet, then make the chat
                                                                //section appear lighter
                                                                if (("delivered").equals(lastMessageDataSnapshot.child("state").getValue().toString())) {
                                                                    chatsViewHolder.userLayout.setBackgroundResource(R.color.colorNewMessageBackground);
                                                                }

                                                                chatsViewHolder.lastMessage.setText(username + " sent an image.");
                                                            }
                                                        }
                                                    } else {
                                                        //if no messages were sent or if the chat was
                                                        //recently deleted, display "No recent
                                                        //messages" label
                                                        if (getActivity() != null) {
                                                            ((TextView) chatsViewHolder.itemView.findViewById(R.id.find_friends_status)).setText(getString(R.string.no_recent_messages));
                                                        }
                                                    }


                                                    //Make loading image disappear and the toolbar
                                                    //visible when all chats have loaded
                                                    if (i == getItemCount() - 1) {
                                                        makeMainToolbarVisible();
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                                }
                                            });

                                            //set on click listener on user i's cell of the chat that
                                            //will send the user to the ChatActivity where he can chat
                                            //with user i
                                            chatsViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    sendUserToChatActivity(userId, username, profileImg);
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

                        @NonNull
                        @Override
                        public ChatsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.users_display_layout, parent, false);
                            return new ChatsFragment.ChatsViewHolder(view);
                        }
                    };

                    chatsList.setAdapter(adapter);
                    adapter.startListening();
                } else {
                    //if the user has no contacts yet, make the "You Have no Chats" label visible,
                    //make the loading image invisible and make the  main toolbar visible
                    makeMainToolbarVisible();
                    noChatsText.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

}
