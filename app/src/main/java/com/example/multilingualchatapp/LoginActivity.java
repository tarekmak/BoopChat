package com.example.multilingualchatapp;

import android.app.ProgressDialog;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

public class LoginActivity extends AppCompatActivity {

    private Button loginButton, phoneLoginButton, createAccountButton;
    private EditText email, password;
    private TextView forgotPasswordLink;

    private FirebaseAuth mAuth;

    private DatabaseReference usersRef;

    private ProgressDialog loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        loadingBar = new ProgressDialog(this);

        initializeFields();


        //if there is data saved in savedInstanceState, restore this data
        if (savedInstanceState != null) {
            restoreSavedInstance(savedInstanceState);
        }

        createAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendUserToRegisterActivity();
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logIn();
            }
        });

        phoneLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendUserToPhoneLoginActivity();
            }
        });

        forgotPasswordLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, ResetPasswordActivity.class));
            }
        });
    }

    //method used to initialize the fields of the activity
    private void initializeFields() {
        loginButton = findViewById(R.id.login_button);
        phoneLoginButton = findViewById(R.id.phone_login_button);
        email = findViewById(R.id.login_email);
        password = findViewById(R.id.login_password);
        createAccountButton = findViewById(R.id.create_account_button);
        forgotPasswordLink = findViewById(R.id.forgot_password_link);
    }

    //method used to send the user to the MainActivity
    private void sendUserToMainActivity() {
        Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }

    //method used to send the user to the RegisterActivity where he'll be able to create his account
    private void sendUserToRegisterActivity() {
        startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
    }

    //method used to send the user to the PhoneLoginActivity
    private void sendUserToPhoneLoginActivity() {
        startActivity(new Intent(LoginActivity.this, PhoneLoginActivity.class));
    }

    //method used to login the user into his account if he entered valid credentials
    private void logIn() {
        String userEmail = email.getText().toString();
        String userPassword = password.getText().toString();

        //making sure the user hasn't left the email field empty
        if (TextUtils.isEmpty(userEmail)) {
            Toast.makeText(this, "Please enter your email address", Toast.LENGTH_SHORT).show();
        //making sure the email entered by the user is valid
        } else if (!userEmail.matches("^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,6}$")){
            Toast.makeText(this, "Please enter your email address", Toast.LENGTH_SHORT).show();
         //making sure the user hasn't left the password field empty
        } else if ((TextUtils.isEmpty(userPassword))) {
            Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show();
        }  else {
            loadingBar.setTitle("Logging in");
            loadingBar.setMessage("Please Wait...");
            loadingBar.setCanceledOnTouchOutside(true); //
            loadingBar.show();

            //logging in the user to his account with his email and password
            mAuth.signInWithEmailAndPassword(userEmail, userPassword).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {

                    if (task.isSuccessful()) {
                        String currentUserId =  mAuth.getCurrentUser().getUid();
                        String deviceToken = FirebaseInstanceId.getInstance().getToken();

                        //adding the device token of the user to the database so that the notification
                        //function sends the notification to the last device the user used to login
                        //into his account
                        usersRef.child(currentUserId).child("device_token").setValue(deviceToken).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    sendUserToMainActivity();
                                    loadingBar.dismiss();
                                } else {
                                    Toast.makeText(LoginActivity.this, "" + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    loadingBar.dismiss();
                                }
                            }
                        });
                    } else {
                        Toast.makeText(LoginActivity.this, "" + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }
                }
            });
        }
    }

    //save what the user has entered in the email and password fields so they can be restored when
    //the context is changed
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("email", email.getText().toString());
        outState.putString("password", password.getText().toString());
    }

    //method used to restore the email and the password fields when the context is changed
    private void restoreSavedInstance(Bundle savedInstanceState) {
        String savedEmail = savedInstanceState.getString("email");
        if (savedEmail != null) {
            email.setText(savedEmail);
        }

        String savedPassword = savedInstanceState.getString("password");
        if (savedPassword != null) {
            password.setText(savedPassword);
        }
    }
}
