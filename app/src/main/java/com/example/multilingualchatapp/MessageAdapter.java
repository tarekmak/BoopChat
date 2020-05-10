package com.example.multilingualchatapp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private List<Messages> messagesList;
    private DatabaseReference senderRef, messagesRef;
    private Context context;
    private String otherUserId, currentUserId;

    MessageAdapter(List<Messages> messagesList, final Context context, String currentUserId, String otherUserId) {
        this.messagesList = messagesList;
        this.context = context;
        this.currentUserId = currentUserId;
        this.otherUserId = otherUserId;
    }


    //ViewHolder used for the messages between the user and his friend
    class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView senderMsgText, senderDatetime, senderMsgState, receiverMsgText, receiverDatetime;
        ImageView senderMsgImg, receiverMsgImg;
        CircleImageView receiverProfileImg;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);

            initializeFields();
        }

        //method used to initialize the fields of the ViewHolder
        private void initializeFields() {
            senderMsgText = itemView.findViewById(R.id.sender_msg_text);
            senderDatetime = itemView.findViewById(R.id.sender_msg_datetime);
            senderMsgState = itemView.findViewById(R.id.sender_msg_state);
            receiverMsgText = itemView.findViewById(R.id.receiver_msg_text);
            receiverDatetime = itemView.findViewById(R.id.receiver_msg_datetime);
            receiverProfileImg = itemView.findViewById(R.id.receiver_msg_profileImg);
            senderMsgImg = itemView.findViewById(R.id.sender_msg_image);
            receiverMsgImg = itemView.findViewById(R.id.receiver_msg_image);
        }


    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_msgs_layout, parent, false);


        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final MessageViewHolder holder, int position) {
        //retrieving the message at position
        final Messages message = messagesList.get(position);

        //retrieving the uid of the sender of the message
        final String fromUid = message.getfrom();

        //retrieving the type of the message
        String type = message.getType();

        //initializing a database reference to the sender of the message
        senderRef = FirebaseDatabase.getInstance().getReference().child("Users").child(fromUid);

        //initializing a database reference to the messages node
        messagesRef = FirebaseDatabase.getInstance().getReference().child("Messages");

        senderRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //retrieving the profile picture of the sender of the message (if he has one)
                String profileImg = null;
                if (dataSnapshot.hasChild("image")) {
                    profileImg = dataSnapshot.child("image").getValue().toString();
                }

                //loading the profile image of the sender (or the default profile image if he doesn't
                //have one) in the ImageView that appears next to messages received by the user (this
                //ImageView will only be visible in case the user is on the receiving end of the message)
                Picasso.get().load(profileImg).placeholder(R.drawable.profile_img).into(holder.receiverProfileImg);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //retrieving the date and time of the message
        String datetime;
        if (!message.wasSentToday()) {
            datetime = message.getDate() + ", " + message.getTime();
        } else {
            datetime = message.getTime();
        }

        //making all the views in the custom_msgs_layout invisible, as the ones that will be used will become visible,
        //as we get more information about the message
        holder.receiverMsgText.setVisibility(View.INVISIBLE);
        holder.receiverDatetime.setVisibility(View.INVISIBLE);
        holder.receiverProfileImg.setVisibility(View.INVISIBLE);
        holder.senderMsgText.setVisibility(View.INVISIBLE);
        holder.senderDatetime.setVisibility(View.INVISIBLE);
        holder.senderMsgState.setVisibility(View.INVISIBLE);
        holder.senderMsgImg.setVisibility(View.INVISIBLE);
        holder.receiverMsgImg.setVisibility(View.INVISIBLE);

        //checking whether the message is a text or an image
        if (("text").equals(type)) {

            //check whether or not the user is the one who sent the message, and make the appropriate views visible
            if (fromUid.equals(currentUserId)) {
                //TODO very rarely the receiverMsgImg is not isn't changed so an unusual space between
                // 2 messages appears
                //if the message is an image, then give the ImageView's that would have been used if the message was a image non-existent,
                //width and height so it doesn't affect the size of the message layout
                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.senderMsgImg.getLayoutParams();
                lp.height = 0;
                lp.width = 0;
                holder.senderMsgImg.setLayoutParams(lp);

                //make the date and time of the message appear below the TextView that is used to display text message, so they don't appear under the ImageViw which,
                //in this case, is not being used
                lp = (RelativeLayout.LayoutParams) holder.senderDatetime.getLayoutParams();
                lp.removeRule(RelativeLayout.BELOW);
                lp.addRule(RelativeLayout.BELOW, holder.senderMsgText.getId());
                holder.senderDatetime.setLayoutParams(lp);

                holder.senderMsgText.setVisibility(View.VISIBLE);
                holder.senderDatetime.setVisibility(View.VISIBLE);

                //checking if the message is the last message sent between the user and the receiver
                if (getItemCount() - 1 == position) {
                    holder.senderMsgState.setVisibility(View.VISIBLE);

                    //if this is the last message in the chat, then verify if the receiver has seen the message, in which case the "Seen" label under the message
                    //will be displayed, if the receiver hasn't read the message yet, then the default "Delivered" will be displayed
                    messagesRef.child(otherUserId).child(currentUserId).orderByKey().limitToLast(1).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                DataSnapshot lastMessageDataSnapshot = dataSnapshot.getChildren().iterator().next();
                                if (lastMessageDataSnapshot.hasChild("state")) {
                                    if (("seen").equals(lastMessageDataSnapshot.child("state").getValue().toString())) {
                                        holder.senderMsgState.setText(context.getString(R.string.seen_text));
                                    }
                                }
                            } else {
                                //If the datasnapshot doesn't exist, it means that the other user deleted the chat, which he can only do if he has seen the last message already
                                holder.senderMsgState.setText(context.getString(R.string.seen_text));
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

                    //if the last message has loaded, then make the progress bar showing the progress of messages loading, disappear
                    ChatActivity.loadMessagesProgressBar.dismiss();
                }

                holder.senderMsgText.setBackgroundResource(R.drawable.sender_msg_layout);
                holder.senderMsgText.setText(message.getBody());
                holder.senderDatetime.setText(datetime);
            } else {
                //TODO very rarely the receiverMsgImg isn't changed so an unusual space between
                // 2 messages appears
                //if the message is a text message from the user himself, then make the ImageView's
                //width and height that would have been used if the message was a image,
                //non-existent so it doesn't affect the size of the message layout
                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.receiverMsgImg.getLayoutParams();
                lp.height = 0;
                lp.width = 0;
                holder.receiverMsgImg.setLayoutParams(lp);

                //make the date and time of the message appear below the TextView that is used to
                //display text message, so they don't appear under the ImageViw which,
                //in this case, is not being used
                lp = (RelativeLayout.LayoutParams) holder.receiverDatetime.getLayoutParams();
                lp.removeRule(RelativeLayout.BELOW);
                lp.addRule(RelativeLayout.BELOW, holder.receiverMsgText.getId());
                holder.receiverDatetime.setLayoutParams(lp);

                //make the profile image of the sender of the message appear next to the TextView that is used to display text message,
                // so they don't appear under the ImageView which, in this case, is not being used
                lp = (RelativeLayout.LayoutParams) holder.receiverProfileImg.getLayoutParams();
                lp.removeRule(RelativeLayout.ALIGN_BOTTOM);
                lp.addRule(RelativeLayout.ALIGN_BOTTOM, holder.receiverMsgText.getId());
                holder.receiverProfileImg.setLayoutParams(lp);

                holder.receiverMsgText.setVisibility(View.VISIBLE);
                holder.receiverDatetime.setVisibility(View.VISIBLE);
                holder.receiverProfileImg.setVisibility(View.VISIBLE);

                //notify the database that the messages appearing in the ChatActivity have all been read by the user
                messagesRef.child(currentUserId).child(fromUid).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot message : dataSnapshot.getChildren()) {
                                Map<String, Object> messageState = new HashMap<>();
                                messageState.put("state", "seen");
                                message.getRef().updateChildren(messageState);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                //checking if the message is the last message sent between the user and the receiver, and making
                //the progress bar showing the progress of messages loading disappear as all messages would've loaded
                if (getItemCount() - 1 == position) {
                    ChatActivity.loadMessagesProgressBar.dismiss();
                }

                holder.receiverMsgText.setBackgroundResource(R.drawable.receiver_msg_layout);
                holder.receiverMsgText.setText(message.getBody());
                holder.receiverDatetime.setText(datetime);
            }
        } else if (("image").equals(type)) {

            //check whether or not the user is the one who sent the message, and make the appropriate views visible
            if (fromUid.equals(currentUserId)) {

                //if the message is an image, then give the ImageView used to display the image sent an appropriate width and height
                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.senderMsgImg.getLayoutParams();
                lp.height = 550;
                lp.width = 550;
                holder.senderMsgImg.setLayoutParams(lp);

                //make the date and time of the message appear below the ImageView that is used to display the image sent, so they don't appear under the TextView which,
                //in this case, is not being used
                lp = (RelativeLayout.LayoutParams) holder.senderDatetime.getLayoutParams();
                lp.removeRule(RelativeLayout.BELOW);
                lp.addRule(RelativeLayout.BELOW, holder.senderMsgImg.getId());
                holder.senderDatetime.setLayoutParams(lp);

                holder.senderMsgImg.setVisibility(View.VISIBLE);
                holder.senderDatetime.setVisibility(View.VISIBLE);

                //set an on click listener on the image that would send the user to the ImageViewerActivity where he gets to see the full image sent
                holder.senderMsgImg.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendUserToImageViewerActivity(message.getBody());
                    }
                });

                //checking if the message is the last message sent between the user and the receiver
                if (getItemCount() - 1 == position) {
                    holder.senderMsgState.setVisibility(View.VISIBLE);

                    //if this is the last message in the chat, then verify if the receiver has seen the message, in which case the "Seen" label under the message
                    //will be displayed, if the receiver hasn't read the message yet, then the default "Delivered" will be displayed
                    messagesRef.child(otherUserId).child(currentUserId).orderByKey().limitToLast(1).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                DataSnapshot lastMessageDataSnapshot = dataSnapshot.getChildren().iterator().next();
                                if (lastMessageDataSnapshot.hasChild("state")) {
                                    if (("seen").equals(lastMessageDataSnapshot.child("state").getValue().toString())) {
                                        holder.senderMsgState.setText(context.getString(R.string.seen_text));
                                    }
                                }
                            } else {
                                //If the datasnapshot doesn't exist, it means that the other user deleted the chat, which he can only do if he has seen the last message already
                                holder.senderMsgState.setText(context.getString(R.string.seen_text));
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

                    //if the last message has loaded, then make the progress bar showing the progress of messages loading, disappear
                    ChatActivity.loadMessagesProgressBar.dismiss();
                }

                //load the image sent into the appropriate ImageView
                Picasso.get().load(message.getBody()).into(holder.senderMsgImg);
                holder.senderDatetime.setText(datetime);

            } else {

                //if the message is an image, then give the ImageView used to display the image sent an appropriate width and height
                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.receiverMsgImg.getLayoutParams();
                lp.height = 550;
                lp.width = 550;
                holder.receiverMsgImg.setLayoutParams(lp);
//                holder.receiverMsgImg.invalidate();

                //make the date and time of the message appear below the ImageView that is used to display the image sent, so they don't appear under the TextView which,
                //in this case, is not being used
                lp = (RelativeLayout.LayoutParams) holder.receiverDatetime.getLayoutParams();
                lp.removeRule(RelativeLayout.BELOW);
                lp.addRule(RelativeLayout.BELOW, holder.receiverMsgImg.getId());
                holder.receiverDatetime.setLayoutParams(lp);

                //make the profile image of the sender of the message appear next to the TextView that is used to display text message,
                // so they don't appear under the ImageView which, in this case, is not being used
                lp = (RelativeLayout.LayoutParams) holder.receiverProfileImg.getLayoutParams();
                lp.removeRule(RelativeLayout.ALIGN_BOTTOM);
                lp.addRule(RelativeLayout.ALIGN_BOTTOM, holder.receiverMsgImg.getId());
                holder.receiverProfileImg.setLayoutParams(lp);

                holder.receiverMsgImg.setVisibility(View.VISIBLE);
                holder.receiverDatetime.setVisibility(View.VISIBLE);
                holder.receiverProfileImg.setVisibility(View.VISIBLE);

                //set an on click listener on the image that would send the user to the ImageViewerActivity where he gets to see the full image sent
                holder.receiverMsgImg.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendUserToImageViewerActivity(message.getBody());
                    }
                });

                //notify the database that the messages appearing in the ChatActivity have all been read by the user
                messagesRef.child(currentUserId).child(fromUid).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot message : dataSnapshot.getChildren()) {
                                if (!("seen").equals(message.child("state").getValue().toString())) {
                                    Map<String, Object> messageState = new HashMap<>();
                                    messageState.put("state", "seen");
                                    message.getRef().updateChildren(messageState);
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                //checking if the message is the last message sent between the user and the receiver, and making
                //the progress bar showing the progress of messages loading, disappear as all messages would've loaded
                if (getItemCount() - 1 == position) {
                    holder.senderMsgState.setText(context.getString(R.string.seen_text));
                    ChatActivity.loadMessagesProgressBar.dismiss();
                }

                //load the image sent into the appropriate ImageView
                Picasso.get().load(message.getBody()).into(holder.receiverMsgImg);

                holder.receiverDatetime.setText(datetime);
            }
        }
    }

    //sends the user to the ImageViewerActivity whose sole purpose is to show the image whose ulr is
    //passed in the intent imageUrl
    private void sendUserToImageViewerActivity(String imageUrl) {
        Intent imageViewerIntent = new Intent(context, ImageViewerActivity.class);
        imageViewerIntent.putExtra("imageUrl", imageUrl);
        imageViewerIntent.putExtra("currentUserId", currentUserId);
        context.startActivity(imageViewerIntent);
    }

    @Override
    public int getItemCount() {
        return messagesList.size();
    }



}
