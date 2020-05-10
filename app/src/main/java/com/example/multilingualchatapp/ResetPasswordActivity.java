package com.example.multilingualchatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText email;
    private Button sendEmailButton;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        initializeFields();

        if (savedInstanceState != null) {
            restoreSavedInstance(savedInstanceState);
        }

        mAuth = FirebaseAuth.getInstance();

        sendEmailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendResetEmail();
            }
        });
    }

    //method used to send an email to the email address that the user has entered to the user that
    //contains the link that permits him to change the password of the account that is associated to
    //the email address
    private void sendResetEmail() {
        String emailInput = email.getText().toString();

        //making sure the email field was left empty
        if (TextUtils.isEmpty(emailInput)) {
            Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show();
        //making sure the email entered by the user is valid
        } else if (!emailInput.matches("^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,6}$")){
            Toast.makeText(this, "Please write a valid email address.", Toast.LENGTH_SHORT).show();
        } else {
            mAuth.sendPasswordResetEmail(emailInput).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(ResetPasswordActivity.this, "The email was successfully sent!\nPlease check your inbox.", Toast.LENGTH_SHORT).show();
                        sendUserToLoginActivity();
                    } else {
                        Toast.makeText(ResetPasswordActivity.this, "Please make sure you entered a valid email.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void sendUserToLoginActivity() {
        Intent loginIntent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }

    //method used to initialize the fields of the activity
    private void initializeFields() {
        email = findViewById(R.id.reset_email);
        sendEmailButton = findViewById(R.id.reset_password_send_mail_button);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        
        outState.putString("email", email.getText().toString());
    }

    private void restoreSavedInstance(Bundle savedInstanceState) {
        String savedEmail = savedInstanceState.getString("email");
        if (savedEmail != null) {
            email.setText(savedEmail);
        }
    }
}
