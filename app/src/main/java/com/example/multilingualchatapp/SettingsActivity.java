package com.example.multilingualchatapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.appcompat.widget.Toolbar;
import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    //this class will be used to know if the user is setting up his account after registering or if
    //he is simply editing it so when the context is changed, we know which view should be visible
    //or invisible
    public class Action {
        static final String SETTING_UP = "ACTION_0";
        static final String EDITTING = "ACTION_1";
    }

    //this variable is going to contain the string value that represents what action the user is
    //doing (whether he is setting up his account after registering or simply editing it)
    private String action;

    private Button done_button;
    private ImageButton delete_profileImg_button;
    private EditText username, userStatus;
    private CircleImageView profileImg;
    private TextView usernameLabel;
    private Spinner languageSpinner;
    private Switch translationMode;

    private FirebaseAuth mAuth;
    private DatabaseReference rootRef;
    private String currentUserId, profileImgLink;

    private StorageReference profileImgsRef;

    private ProgressDialog loadingBar;

    protected final int gallery_pick = 1;

    private HashMap<String, String> languages_label;
    private List<String> languages_list;

    private Toolbar tbar;

    private UserState userState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initializeFields();

        initializeLanguageVariables();

        loadingBar = new ProgressDialog(this);

        //setting up the toolbar
        setSupportActionBar(tbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setTitle("Account Settings");

        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();
        userState = UserState.getUserStateInstance();
        rootRef = FirebaseDatabase.getInstance().getReference();
        profileImgsRef = FirebaseStorage.getInstance().getReference().child("Profile Images");

        done_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateProfile();
            }
        });

        delete_profileImg_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                profileImgLink = null;
                Picasso.get().load(profileImgLink).placeholder(R.drawable.profile_img).into(profileImg);
                Toast.makeText(SettingsActivity.this,"Profile Image removed Successfully.", Toast.LENGTH_SHORT).show();
                delete_profileImg_button.setVisibility(View.INVISIBLE);
            }
        });

        //verifying if the context was changed, in which case we restore the fields that were saved,
        //or if the user just opened the settings activity, in which case we load his data from the database
        if (savedInstanceState == null) {
            //displaying a loading bar that prompts the user to wait while his info is loading
            loadingBar.setTitle("Loading your info");
            loadingBar.setMessage("Please Wait...");
            loadingBar.setCanceledOnTouchOutside(true); //
            loadingBar.show();

            retrieveUserInfo();
        } else {
            restoreSavedState(savedInstanceState);
        }

        //setting an on click lick listener so that when the user presses on the profile pic, he'll get sent
        //to his gallery to chose a new one
        profileImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Saving all changes made to the user's profile before sending him to his gallery to pick a new profile pic
                String translation;
                if (translationMode.isChecked()) {
                    translation = "on";
                } else {
                    translation = "off";
                }
                String setStatus = userStatus.getText().toString();
                String language = languages_label.get(languageSpinner.getSelectedItem().toString());
                Map<String, Object> profileMap = new HashMap<>();
                profileMap.put("status", setStatus);
                profileMap.put("language", language);
                profileMap.put("translation", translation);
                rootRef.child("Users").child(currentUserId).updateChildren(profileMap);
                sendUserToGallery();
            }
        });

    }

    //method used to initialize the fields of the activity
    private void initializeFields() {
        done_button = findViewById(R.id.update_profile_button);
        delete_profileImg_button = findViewById(R.id.delete_profileImg_button);
        username = findViewById(R.id.set_username);
        userStatus = findViewById(R.id.set_status);
        profileImg = findViewById(R.id.profile_image);
        usernameLabel = findViewById(R.id.profile_username_label);
        languageSpinner = findViewById(R.id.set_language);
        translationMode = findViewById(R.id.set_translation);
        tbar = findViewById(R.id.settings_toolbar);
    }

    //method used to initialize rhe variables used to determine the supported languages' abbreviations
    //and their position in the spinner
    private void initializeLanguageVariables() {
        //initializing an ArrayList that contains the abbreviations of the supported languages used
        //by the Google Translate API
        //this list will be used to know at which index the designated language of the user is so we
        //can display it on the spinner (this is why the language abbreviations in the list are in
        //the same order as their associated language strings in the spinner (see the languages string
        //array in values/strings.xml))
        languages_list = new ArrayList<>(Arrays.asList("en", "fr", "ar", "es", "de", "it", "pt", "ru", "ja", "tr", "zh"));

        //initializing a HashMap that contains both the languages and their associated abbreviations
        //used by the Google Translate API
        languages_label = new HashMap<>();
        languages_label.put("French", "fr");
        languages_label.put("English", "en");
        languages_label.put("Arabic", "ar");
        languages_label.put("Turkish", "tr");
        languages_label.put("German", "de");
        languages_label.put("Russian", "ru");
        languages_label.put("Japanese", "ja");
        languages_label.put("Portuguese", "pt");
        languages_label.put("Spanish", "es");
        languages_label.put("Italian", "it");
        languages_label.put("Chinese", "zh");
    }

    @Override
    protected void onStart() {
        super.onStart();

        //when the activity is started, notify the database that the user is online
        userState.updateUserStatus("online");

        //making the color of the languages in the languageSpinner white
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent.getChildAt(0) != null) {
                    ((TextView) parent.getChildAt(0)).setTextColor(Color.WHITE);
                    ((TextView) parent.getChildAt(0)).setTypeface(null, Typeface.BOLD);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (parent.getChildAt(0) != null) {
                    ((TextView) parent.getChildAt(0)).setTextColor(Color.WHITE);
                    ((TextView) parent.getChildAt(0)).setTypeface(null, Typeface.BOLD);
                }
            }
        });
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

    //called after the user selects his profile image form his gallery
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //checking that the method is being called after the user has selected an image that he
        //wants to set as his profile picture and not for any other event
        if (requestCode == gallery_pick && resultCode ==  RESULT_OK && data != null) {
            Uri imageUri = data.getData();

            //make the user crop the image into a square shape (once he'll crop the image the
            //onActivityResult will be called again)
            CropImage.activity(imageUri)
                    .setAspectRatio(1, 1)
                    .start(this);
        }

        //checking if the method has been called after that the user has cropped the image he chose
        //as his profile image
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK) {

                //displaying a loading bar that prompts the user to wait while his profile image is
                //being updated
                loadingBar.setTitle("Profile Settings");
                loadingBar.setMessage("Updating Profile Image...");
                loadingBar.setCanceledOnTouchOutside(false);
                loadingBar.show();

                //getting the uri of the image
                Uri resultUri = result.getUri();

                final StorageReference filePath = profileImgsRef.child(currentUserId + ".jpg");

                //storing the image in our database
                filePath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()) {

                            filePath.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task) {
                                    if (task.isSuccessful()) {
                                        profileImgLink = task.getResult().toString();
                                        Picasso.get().load(profileImgLink).placeholder(R.drawable.profile_img).into(profileImg);
                                        delete_profileImg_button.setVisibility(View.VISIBLE);
                                        loadingBar.dismiss();
                                    } else {
                                        Toast.makeText(SettingsActivity.this, "" + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                        loadingBar.dismiss();
                                    }
                                }
                            });


                        } else {
                            Toast.makeText(SettingsActivity.this, "" + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            loadingBar.dismiss();
                        }
                    }
                });


            }
        }
    }

    //method used to save the changes made by the user to his profile
    private void updateProfile() {

        //displaying a loading bar that prompts the user to wait while his profile is being updated
        loadingBar.setTitle("Profile Settings");
        loadingBar.setMessage("Updating Profile...");
        loadingBar.setCanceledOnTouchOutside(false);
        loadingBar.show();

        String translationTemp;
        if (translationMode.isChecked()) {
            translationTemp = "on";
        } else {
            translationTemp = "off";
        }

        final String setUsername = username.getText().toString();
        final String setStatus = userStatus.getText().toString();

        //getting the abbreviation used by the Google Translate API of the user's designated language
        final String language = languages_label.get(languageSpinner.getSelectedItem().toString());
        final String translationMode = translationTemp;

        //making sure the username field wasn't left blank
        //(in the case the user is only editing his profile, the username field is set to contain
        //the user's username even though the field is indivisible)
        if (TextUtils.isEmpty(setUsername)) {
            Toast.makeText(this, "Please enter your username", Toast.LENGTH_SHORT).show();
            loadingBar.dismiss();
        //making sure the username entered doesn't contain any non-numerical or alphabetical characters or any spaces
        } else if (setUsername.matches("[^A-Za-z0-9]") || setUsername.contains(" ")) {
            Toast.makeText(this, "Please enter an enter an username that contains only alphabetical and numerical numerical characters", Toast.LENGTH_SHORT).show();
            loadingBar.dismiss();
        //making sure the username entered isn't too short or too long
        } else if (setUsername.length() < 6 || setUsername.length() > 15) {
            Toast.makeText(this, "Please enter an enter an username that is between 6 and 15 characters", Toast.LENGTH_SHORT).show();
            loadingBar.dismiss();
        } else {
            Query query = rootRef.child("Users").orderByChild("name").equalTo(setUsername);

            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    //making sure that setUsername isn't already in use
                    if (!dataSnapshot.exists()) {
                        Map<String, Object> profileMap = new HashMap<>();
                        profileMap.put("uid", currentUserId);
                        profileMap.put("name", setUsername);
                        profileMap.put("status", setStatus);
                        profileMap.put("language", language);
                        profileMap.put("translation", translationMode);
                        rootRef.child("Users").child(currentUserId).updateChildren(profileMap);
                        profileMap.clear();
                        loadingBar.dismiss();
                        sendUserToMainActivity();
                    } else {
                        //if the username is already in use, retrieving the uid of the user detaining the username
                        String un = dataSnapshot.getChildren().iterator().next().getKey();

                        //if the uid of the detainer of the username is the same as the one currently
                        //using the app, then the user is only editing the profile and is allowed to
                        //update it
                        if (TextUtils.equals(un, currentUserId)) {
                            Map<String, Object> profileMap = new HashMap<>();
                            profileMap.put("uid", currentUserId);
                            //profileMap.put("name", setUsername);
                            profileMap.put("status", setStatus);
                            profileMap.put("language", language);
                            profileMap.put("translation", translationMode);
                            profileMap.put("image", profileImgLink);

                            rootRef.child("Users").child(currentUserId).updateChildren(profileMap);
                            profileMap.clear();

                            Toast.makeText(SettingsActivity.this, "Profile Updated Successfully!", Toast.LENGTH_SHORT).show();
                            loadingBar.dismiss();
                            sendUserToMainActivity();
                        } else {
                            //if it is not the case, then the user is currently creating his profile
                            //and trying to use an username that is already in use
                            Toast.makeText(SettingsActivity.this, "This username is already in use!\nPlease enter another username.", Toast.LENGTH_SHORT).show();
                            loadingBar.dismiss();
                        }

                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    System.out.println(databaseError.getMessage());
                    loadingBar.dismiss();
                }
            });
        }

    }

    //method used to restore the username (if the user is setting up his account after registering),
    //the status, the language and the translation option fields when the context
    //is changed instead of getting them again from the database (this is done for the sake of performance)
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("profileImgLink", profileImgLink);

        outState.putString("action", action);

        //verifying if the user is creating his profile or just editing it
        if ((Action.SETTING_UP).equals(action)) {
            outState.putString("name", username.getText().toString());
        } else if ((Action.EDITTING).equals(action)) {
            outState.putString("name", usernameLabel.getText().toString());
        }

        outState.putString("status", userStatus.getText().toString());

        outState.putString("language", languages_label.get(languageSpinner.getSelectedItem().toString()));

        if (translationMode.isChecked()) {
            outState.putString("translationOption", "on");
        } else {
            outState.putString("translationOption", "off");
        }

    }

    //method used to restore the user's info when the context is changed
    private void restoreSavedState(Bundle savedInstanceState) {
        profileImgLink = savedInstanceState.getString("profileImgLink");
        Picasso.get().load(profileImgLink).placeholder(R.drawable.profile_img).into(profileImg);

        if (profileImgLink != null) {
            delete_profileImg_button.setVisibility(View.VISIBLE);
        }

        String savedName = savedInstanceState.getString("name");
        String savedAction = savedInstanceState.getString("action");
        if (savedName != null) {

            if (savedAction != null) {

                //checking if the user is setting up his account or just editing it
                if ((Action.SETTING_UP).equals(savedAction)) {
                    //if the is setting up his account, make username field visible and restore what
                    //he had entered before the change in context, and restore the value of the action
                    action = Action.SETTING_UP;
                    username.setText(savedName);
                    username.setVisibility(View.VISIBLE);
                } else if ((Action.EDITTING).equals(savedAction)) {
                    //if the user is only editing his account, make username label visible, and restore
                    //the value of the action
                    action = Action.EDITTING;
                    username.setText(savedName);
                    username.setVisibility(View.INVISIBLE);
                    usernameLabel.setText(savedName);
                }
            }
        }

        String savedStatus = savedInstanceState.getString("status");
        if (savedStatus != null) {
            userStatus.setText(savedStatus);
        }

        String savedLanguage = savedInstanceState.getString("language");
        if (savedLanguage != null) {
            languageSpinner.setSelection(languages_list.indexOf(savedLanguage));
        }

        String savedTranslationOption = savedInstanceState.getString("translationOption");
        if (savedTranslationOption != null) {
            if (("on").equals(savedTranslationOption)) {
                translationMode.setChecked(true);
            } else {
                translationMode.setChecked(false);
            }
        }
    }

    //retrieving the info of the user so it can be displayed and modified in the activity
    private void retrieveUserInfo() {
        rootRef.child("Users").child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {

                    if (dataSnapshot.hasChild("image")) {
                        profileImgLink = dataSnapshot.child("image").getValue().toString();

                        delete_profileImg_button.setVisibility(View.VISIBLE);
                    }

                    Picasso.get().load(profileImgLink).placeholder(R.drawable.profile_img).into(profileImg);

                    //checking if the user already has a unique username
                    if (dataSnapshot.hasChild("name")) {
                        //if he does, it means he's only editing his account
                        action = Action.EDITTING;
                        String orgUsername = dataSnapshot.child("name").getValue().toString();
                        username.setText(orgUsername);
                        username.setVisibility(View.INVISIBLE);
                        usernameLabel.setText(orgUsername);
                    } else {
                        //if he doesn't, it means he's currently setting up his account
                        action = Action.SETTING_UP;
                    }

                    if (dataSnapshot.hasChild("status")) {
                        String orgStatus = dataSnapshot.child("status").getValue().toString();
                        userStatus.setText(orgStatus);
                    }

                    if (dataSnapshot.hasChild("language")) {
                        String language = dataSnapshot.child("language").getValue().toString();
                        languageSpinner.setSelection(languages_list.indexOf(language));
                    }

                    if (dataSnapshot.hasChild("translation")) {
                        String mode = dataSnapshot.child("translation").getValue().toString();

                        if (("on").equals(mode)) {
                            translationMode.setChecked(true);
                        } else if (("off").equals(mode)) {
                            translationMode.setChecked(false);
                        }
                    }

                    loadingBar.dismiss();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    //method used to send the user to the main activity after he is done setting up/editing his account
    private void sendUserToMainActivity() {
        Intent mainIntent = new Intent(SettingsActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }

    //method used to send the user to his gallery when he clicks on his profile image so he can choose
    //a new profile image
    private void sendUserToGallery() {
        Intent galleryIntent = new Intent();
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, gallery_pick);
    }

}
