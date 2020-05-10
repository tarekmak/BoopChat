package com.example.multilingualchatapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

public class ImageViewerActivity extends AppCompatActivity {
    private ImageView imageView;
    private String imageUrl, currentUserId;
    private UserState userState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        initializeFields();

        imageUrl = getIntent().getStringExtra("imageUrl");
        Picasso.get().load(imageUrl).into(imageView);

        currentUserId = getIntent().getStringExtra("currentUserId");
        userState = UserState.getUserStateInstance();
    }

    //method used to initialize the fields of the activity
    private void initializeFields() {
        imageView = findViewById(R.id.image_viewer_image);
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
}
