package com.example.multilingualchatapp;

import android.app.ProgressDialog;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.media.audiofx.Equalizer;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private Button registerButton;
    private EditText email, password, retypedPassword;
    private TextView alreadyHaveAccountLink;

    private FirebaseAuth mAuth;
    private DatabaseReference ref;

    private ProgressDialog loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        FirebaseApp.initializeApp(this);

        mAuth = FirebaseAuth.getInstance();
        ref = FirebaseDatabase.getInstance().getReference();

        initializeFields();

        if (savedInstanceState != null) {
            restoreSavedInstance(savedInstanceState);
        }

        alreadyHaveAccountLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendUserToLoginActivity();
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createNewAccount();
            }
        });
    }

    //method used to initialize the fields of the activity
    private void initializeFields() {
        registerButton = findViewById(R.id.register_button);
        email = findViewById(R.id.register_email);
        password = findViewById(R.id.register_password);
        retypedPassword = findViewById(R.id.retyped_password);
        alreadyHaveAccountLink = findViewById(R.id.already_have_account_link);
        loadingBar = new ProgressDialog(this);
    }

    //method used to send the user to LoginActivity if he already has an account
    private void sendUserToLoginActivity() {
        Intent loginIntent = new Intent(RegisterActivity.this, LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }

    //method used to send the user to the SettingsActivity so he can set up his account after creating it
    private void sendUserToSettingsActivity() {
        Intent mainIntent = new Intent(RegisterActivity.this, SettingsActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }

    //save what the user has entered in the email, password and retyped password fields so they can
    //be restored when the context is changed
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("email", email.getText().toString());
        outState.putString("password", password.getText().toString());
        outState.putString("retypedPassword", retypedPassword.getText().toString());
    }

    //method used to restore the email, the password and the retyped password fields when the context
    //is changed
    private void restoreSavedInstance(Bundle savedInstanceState) {
        String savedEmail = savedInstanceState.getString("email");
        if (savedEmail != null) {
            email.setText(savedEmail);
        }

        String savedPassword = savedInstanceState.getString("password");
        if (savedPassword != null) {
            password.setText(savedPassword);
        }

        String savedRetypedPassword = savedInstanceState.getString("retypedPassword");
        if (savedRetypedPassword != null) {
            retypedPassword.setText(savedRetypedPassword);
        }
    }

    //method used to create a new account with the email and password the user has entered after verifying
    //it is valid
    private void createNewAccount() {
        String userEmail = email.getText().toString();
        String userPassword = password.getText().toString();
        String userRetypedPassword = retypedPassword.getText().toString();

        //making sure the email field wasn't left blank
        if (TextUtils.isEmpty(userEmail)) {
            Toast.makeText(this, "Please enter email", Toast.LENGTH_SHORT).show();
            //making sure the email entered by the user is valid
        } else if (!userEmail.matches("^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,6}$")){
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
            //making sure the password field wasn't left blank
        } else if (TextUtils.isEmpty(userPassword)) {
            Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show();
            //making sure the retype password field wasn't left blank
        } else if (TextUtils.isEmpty(userRetypedPassword)) {
            Toast.makeText(this, "Please re-enter password", Toast.LENGTH_SHORT).show();
            //making the password chosen by the user is at least 8 characters long
        } else if (userPassword.length() < 8) {
            Toast.makeText(this, "Password has to be at least 8 characters long", Toast.LENGTH_SHORT).show();
            //making the password and retyped password match
        } else if (!userPassword.contentEquals(userRetypedPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
        } else {

            //displaying a progress bar prompting the user to wait while his account is created
            loadingBar.setTitle("Creating New Account");
            loadingBar.setMessage("Please Wait...");
            loadingBar.setCanceledOnTouchOutside(true);
            loadingBar.show();

            //creating the account of the user with the email and password he entered after making sure
            //they are valid
            mAuth.createUserWithEmailAndPassword(userEmail, userPassword).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        final String currentUserId = mAuth.getCurrentUser().getUid();
                        Map<String, Object> profileMap = new HashMap<>();
                        profileMap.put("uid", currentUserId);
                        ref.child("Users").child(currentUserId).setValue(profileMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(RegisterActivity.this, "Account Created Successfully!", Toast.LENGTH_SHORT).show();
                                    String deviceToken = FirebaseInstanceId.getInstance().getToken();

                                    //adding the device token of the user to the database so that the notification
                                    //function sends the notification to the last device the user used to login
                                    //into his account
                                    DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference().child("Users");
                                    usersRef.child(currentUserId).child("device_token").setValue(deviceToken).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            //the device token was successfully saved, send the user
                                            //to the settings activity so he can set up his account
                                            //(choose a unique username, choose his designated language,
                                            //choose a profile image, etc.)
                                            if (task.isSuccessful()) {
                                                sendUserToSettingsActivity();
                                                loadingBar.dismiss();
                                            } else {
                                                Toast.makeText(RegisterActivity.this, "" + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                loadingBar.dismiss();
                                            }
                                        }
                                    });

                                } else {
                                    Toast.makeText(RegisterActivity.this, "" + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    loadingBar.dismiss();
                                }
                            }
                        });
                        loadingBar.dismiss();
                    } else {
                        Toast.makeText(RegisterActivity.this, "" + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }

                }
            });
        }
    }
}
