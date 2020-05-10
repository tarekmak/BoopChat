package com.example.multilingualchatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private String receiverUserId, receiverUsername, receiverProfileImg, currentUserId, translationOption;

    private TextView chatUsername, chatLastSeen;
    private CircleImageView chatProfileImg;

    private ImageButton sendMsgButton, sendImgButton;
    private EditText msgInput;
    private Toolbar tbar;

    private FirebaseAuth mAuth;
    private DatabaseReference rootRef, currentUserRef, notificationRef;

    private List<Messages> msgsList;
    private LinearLayoutManager linearLayoutManager;
    private MessageAdapter msgAdapter;
    private RecyclerView rView;
    protected static ProgressDialog loadMessagesProgressBar;
    private ProgressDialog loadingBar;

    private UserState userState;
    private TranslationService translationService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mAuth = FirebaseAuth.getInstance();

        //making sure the user is logged in
        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        }

        userState = UserState.getUserStateInstance();
        rootRef = FirebaseDatabase.getInstance().getReference();
        notificationRef = FirebaseDatabase.getInstance().getReference().child("Notifications");
        currentUserRef = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserId);

        loadMessagesProgressBar = new ProgressDialog(this);
        loadMessagesProgressBar.setTitle("Loading Messages");
        loadMessagesProgressBar.setMessage("Please Wait...");
        loadMessagesProgressBar.setCanceledOnTouchOutside(false); //
        loadMessagesProgressBar.show();
        loadingBar = new ProgressDialog(this);

        translationService = TranslationService.getTranslationInstance();

        //getting the user's designated language and whether he has the translation feature turned
        //on and set up the message layout accordingly
        currentUserRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                userLanguage = dataSnapshot.child("language").getValue().toString();
                translationOption = dataSnapshot.child("translation").getValue().toString();
                setUpMessagesLayout();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        getReceiverInfo();

        setUpToolbar();

        initializeFields();

        displayReceiverInfoInToolbar();

        sendMsgButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        //set on click listener that sends user to his gallery so he can send an image of his choosing
        sendImgButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendUserToGallery();
            }
        });

        //set on click listener on the receiver profile image that sends the user to the ProfileActivity of the receiver
        chatProfileImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendUserToProfileActivity(receiverUserId);
            }
        });

        displayReceiverState();
    }

    //displaying if the receiver is currently online or when has he last opened the app if he isn't
    private void displayReceiverState() {
        FirebaseDatabase.getInstance().getReference().child("Users").child(receiverUserId).child("UserState").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    if (("online").equals(dataSnapshot.child("state").getValue().toString())) {
                        chatLastSeen.setText(getString(R.string.online));
                    } else {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");
                        String currentDate = dateFormat.format(Calendar.getInstance().getTime());

                        if (currentDate.equals(dataSnapshot.child("date").getValue().toString())) {
                            chatLastSeen.setText("Last seen at " + dataSnapshot.child("time").getValue().toString());
                        } else {
                            chatLastSeen.setText("Last seen " + dataSnapshot.child("date").getValue().toString() + " at " + dataSnapshot.child("time").getValue().toString());
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    //method used to send the image to the receiver
    private void sendMessage() {
        //adding the text message to the database alongside the date and time it was sent so it can
        //be retrieved to the receiver's device

        //saving the text message before clearing the EditText where the user wrote the message
        String message = msgInput.getText().toString();
        msgInput.setText("");

        //making sure the message input isn't empty
        if (!TextUtils.isEmpty(message)) {

            String msgSenderRef = "Messages/" + currentUserId + "/" + receiverUserId;
            String msgReceiverRef = "Messages/" + receiverUserId + "/" + currentUserId;

            DatabaseReference userMsgKeyRef = rootRef.child("Messages").child(currentUserId).child(receiverUserId).push();

            final String msgPushId = userMsgKeyRef.getKey();

            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");
            String currentDate = dateFormat.format(Calendar.getInstance().getTime());

            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a");
            String currentTime = timeFormat.format(Calendar.getInstance().getTime());

            Map messageBody = new HashMap();
            messageBody.put("from", currentUserId);
            messageBody.put("type", "text");
            messageBody.put("body", message);
            messageBody.put("date", currentDate);
            messageBody.put("time", currentTime);
            messageBody.put("state", "delivered");
            messageBody.put("id", msgPushId);

            Map messageDetails = new HashMap();
            messageDetails.put(msgSenderRef + "/" + msgPushId, messageBody);
            messageDetails.put(msgReceiverRef + "/" + msgPushId, messageBody);

            rootRef.updateChildren(messageDetails).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if (task.isSuccessful()) {
                        //notifying the notification node in the database  that a message has been
                        //sent so our cloud function gets triggered and sends a notification to the
                        //user who is on the receiving end
                        Map<String, String> messageNotification = new HashMap<>();
                        messageNotification.put("from", currentUserId);
                        messageNotification.put("type", "message");
                        messageNotification.put("message_id", msgPushId);

                        notificationRef.child(receiverUserId).push().setValue(messageNotification);
                    }
                }
            });
        }
    }

    //method used to send the user to his gallery
    private void sendUserToGallery() {
        Intent galleryIntent = new Intent();
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent.createChooser(galleryIntent, "Select Image"), 438);
    }


    //method used to send the image to the receiver
    private void sendImage(final String msgPushId, String url) {
        //adding the image to the database alongside the date and time it was sent so it can be retrieved to the receiver's device
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");
        String currentDate = dateFormat.format(Calendar.getInstance().getTime());

        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a");
        String currentTime = timeFormat.format(Calendar.getInstance().getTime());

        Map messageBody = new HashMap();
        messageBody.put("from", currentUserId);
        messageBody.put("type", "image");
        messageBody.put("name", "text");
        messageBody.put("body", url);
        messageBody.put("date", currentDate);
        messageBody.put("time", currentTime);
        messageBody.put("state", "delivered");
        messageBody.put("id", msgPushId);

        String msgSenderRef = "Messages/" + currentUserId + "/" + receiverUserId;
        String msgReceiverRef = "Messages/" + receiverUserId + "/" + currentUserId;

        Map messageDetails = new HashMap();
        messageDetails.put(msgSenderRef + "/" + msgPushId, messageBody);
        messageDetails.put(msgReceiverRef + "/" + msgPushId, messageBody);

        rootRef.updateChildren(messageDetails).addOnCompleteListener(new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task) {
                if (task.isSuccessful()) {
                    //notifying the notification node in the database  that the message has been sent
                    //so our cloud function gets triggered and sends a notification to the user who
                    //is on the receiving end
                    Map<String, String> messageNotification = new HashMap<>();
                    messageNotification.put("from", currentUserId);
                    messageNotification.put("type", "message");
                    messageNotification.put("message_id", msgPushId);

                    notificationRef.child(receiverUserId).push().setValue(messageNotification);

                    //dismissing the loading bar that was displayed in the onActivityResult
                    loadingBar.dismiss();
                }
            }
        });
    }

    //called after the user chooses the image he wants to send
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //making sure that method is being called after the user has selected an image that he
        //wants to send and not for any other event
        if (requestCode == 438 && resultCode == RESULT_OK && data != null && data.getData() != null) {

            loadingBar.setTitle("Sending Image");
            loadingBar.setMessage("Please Wait...");
            loadingBar.setCanceledOnTouchOutside(false);
            loadingBar.show();

            //getting the uri of the image
            final Uri fileUri = data.getData();

            StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Sent Images File");

            DatabaseReference userMsgKeyRef = rootRef.child("Messages").child(currentUserId).child(receiverUserId).push();

            final String msgPushId = userMsgKeyRef.getKey();

            final StorageReference filePath = storageReference.child(msgPushId + ".jpg");

            //storing the image in our database so it can be retrieved by the receiver
            filePath.putFile(fileUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    if (task.isSuccessful()) {
                        filePath.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                if (task.isSuccessful()) {
                                    String url = task.getResult().toString();
                                    sendImage(msgPushId, url);
                                } else {
                                    Toast.makeText(ChatActivity.this, "" + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    loadingBar.dismiss();
                                }
                            }
                        });
                    } else {
                        Toast.makeText(ChatActivity.this, "" + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }
                }
            });
        }

    }

    //method used to send the user to the ProfileActivity of the selected user
    private void sendUserToProfileActivity(String uid) {
        Intent profileIntent = new Intent(ChatActivity.this, ProfileActivity.class);
        profileIntent.putExtra("selected_user_id", uid);
        startActivity(profileIntent);
        finish();
    }


    //setting up the layout of the messages between the user and the receiver
    private void setUpMessagesLayout() {
        msgsList = new ArrayList<>();
        msgAdapter = new MessageAdapter(msgsList, ChatActivity.this, currentUserId, receiverUserId);
        linearLayoutManager = new LinearLayoutManager(ChatActivity.this);
        rView.setLayoutManager(linearLayoutManager);
        rView.setAdapter(msgAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();

        //when the activity is started, notify the database that the user is online
        userState.updateUserStatus("online");

        //if the user and the receiver are no longer friends, close the chat activity and display a Toast message telling the user that he and the receiver are no longer friends,
        //(happens when the user unfriends the receiver from the profile or if the receiver unfriends the user while he is on their chat activity)
        FirebaseDatabase.getInstance().getReference().child("Contacts").child(currentUserId).child(receiverUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    finish();
                    Toast.makeText(ChatActivity.this, receiverUsername + " and you are no longer friends", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //if there are no messages between the user and the receiver, make the progress bar disappear
        rootRef.child("Messages").child(currentUserId).child(receiverUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.hasChildren()) {
                    loadMessagesProgressBar.dismiss();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //setting up appropriate behaviour when a new message is added to the node that represents the
        //the chat between the current user and his friend
        rootRef.child("Messages").child(currentUserId).child(receiverUserId).addChildEventListener(new ChildEventListener() {
            //notify the message adapter when a new message is sent
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                //deserializing the datasnapshot into a Messages object
                Messages msg = dataSnapshot.getValue(Messages.class);

                if (!msgsList.contains(msg)) {
                    //if the message is of type text, and if the user is on the receiving end of the
                    //message and if he has the translation feature turned on, translate the message
                    //to the user's designated language before passing it to the MessageAdapter
                    if(msg.getType().equals("text") && !msg.getfrom().equals(currentUserId) && translationOption.equals("on")){
                        String translatedMessage = translationService.translate(msg.getBody());
                        msg.setBody(translatedMessage);
                    }
                    msgsList.add(msg);

                    msgAdapter.notifyDataSetChanged();

                    rView.smoothScrollToPosition(rView.getAdapter().getItemCount());
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        //when the activity is paused, notify the database that the user is offline
        //(this will only be temporary if the user opened another activity)
        userState.updateUserStatus("offline");
    }


    @Override
    protected void onResume() {
        super.onResume();

        //when the activity is resumed, notify the database that the user is online
        userState.updateUserStatus("online");

        //clearing the message list onResume, so the messages don't appear twice
        if (msgsList != null) {
            msgsList.clear();
        }

    }

    //method used to get the info of the receiver
    private void getReceiverInfo() {
        receiverUserId = getIntent().getExtras().getString("receiver_userId");
        receiverUsername = getIntent().getExtras().getString("receiver_username");
        receiverProfileImg = getIntent().getExtras().getString("receiver_profileImg");
    }

    //method used to set uo the toolbar of the activity
    private void setUpToolbar() {
        tbar = findViewById(R.id.chat_toolbar);
        setSupportActionBar(tbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View actionBarView = layoutInflater.inflate(R.layout.custom_chat_bar, null);
        actionBar.setCustomView(actionBarView);
    }

    //method used to display the username of the receiver and the profile picture of the receiver
    private void displayReceiverInfoInToolbar() {
        chatUsername.setText(receiverUsername);
        Picasso.get().load(receiverProfileImg).placeholder(R.drawable.profile_img).into(chatProfileImg);
    }

    //method used to initialize the fields of the activity
    private void initializeFields() {
        chatUsername = findViewById(R.id.chat_username);
        chatLastSeen = findViewById(R.id.chat_last_seen);
        chatProfileImg = findViewById(R.id.chat_profileImg);
        sendMsgButton = findViewById(R.id.chat_send_message_button);
        sendImgButton = findViewById(R.id.chat_send_img_button);
        msgInput = findViewById(R.id.chat_message_input);
        rView = findViewById(R.id.chat_messages);
    }

    //initializing the option to clear the chat to the user
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.chat_options, menu);

        return true;
    }

    //handling appropriately the option selected by the user
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case R.id.chat_clear_messages:
                clearChat();
                break;
            case android.R.id.home:
                onBackPressed();
                break;
        }

        return true;
    }

    //method used to clear the chat for the user
    private void clearChat() {
        loadingBar.setTitle("Clearing Chat");
        loadingBar.setMessage("Please Wait...");
        loadingBar.setCanceledOnTouchOutside(false);
        loadingBar.show();
        rootRef.child("Messages").child(currentUserId).child(receiverUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                setUpMessagesLayout();
                loadingBar.dismiss();
            }
        });
    }
}
