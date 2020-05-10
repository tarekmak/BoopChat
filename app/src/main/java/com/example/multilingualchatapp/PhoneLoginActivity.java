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
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.concurrent.TimeUnit;

public class PhoneLoginActivity extends AppCompatActivity {

    //this class will be used to know at what stage of the verification the user is so when the context
    //is changed, we know which view should be visible or invisible
    public class Stage {
        static final String ENTERING_PHONE_NUMBER = "STAGE_0";
        static final String ENTERING_CODE = "STAGE_1";
    }

    //this variable is going to contain the string value that represents at which stage of the
    //verification the user is
    private String stage;

    protected Button sendVerificationButton, verifyButton;
    protected EditText inputPhoneNumber, inputCode;

    protected ProgressDialog loadingBar;

    protected PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    protected String mVerificationId;

    protected FirebaseAuth mAuth;
    protected DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_login);

        mAuth = FirebaseAuth.getInstance();

        initializeField();

        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        loadingBar = new ProgressDialog(this);

        stage = Stage.ENTERING_PHONE_NUMBER;

        //if there is data saved in savedInstanceState, restore this data
        if (savedInstanceState != null) {
            restoreSavedState(savedInstanceState);
            handleStage();
        }

        //initializing the callbacks that are going to be used for the whole verification process
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            //setting the appropriate behavior if the verification process was a success
            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            //setting the appropriate behavior if the verification process was a failure
            @Override
            public void onVerificationFailed(FirebaseException e) {
                Toast.makeText(PhoneLoginActivity.this, "Verification failed.\n" +
                        "Make sure you entered a valid phone number (Alongside Your Country Code).",
                        Toast.LENGTH_SHORT).show();

                //if the verification fails, the user is back to enter his phone number
                stage = Stage.ENTERING_PHONE_NUMBER;

                //dismissing the loading bar that prompts the user to wait for the code to be verified
                loadingBar.dismiss();

                //making the phone number field and send verification code button visible and the send
                //verification code field and verify code button visible so the user can reenter his
                //phone number and get sent a new code so he can attempt to login again
                inputPhoneNumber.setVisibility(View.VISIBLE);
                sendVerificationButton.setVisibility(View.VISIBLE);
                inputCode.setVisibility(View.INVISIBLE);
                verifyButton.setVisibility(View.INVISIBLE);
            }

            //setting the appropriate behavior when the code is sent
            @Override
            public void onCodeSent(String verificationId,
                                   PhoneAuthProvider.ForceResendingToken token) {

                //dismissing the loading bar that prompts the user to wait for the code to be sent
                loadingBar.dismiss();

                mVerificationId = verificationId;

                //making the phone number field and the send verification code button invisible and
                //making the verification code field and the verify code button visible when the
                //verification code has been sent
                inputPhoneNumber.setVisibility(View.INVISIBLE);
                sendVerificationButton.setVisibility(View.INVISIBLE);
                inputCode.setVisibility(View.VISIBLE);
                verifyButton.setVisibility(View.VISIBLE);
            }

        };

        sendVerificationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendVerificationCode();
            }
        });

        verifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyVerificationCode();
            }
        });
    }

    //method used to initialize the fields of the activity
    private void initializeField() {
        sendVerificationButton = findViewById(R.id.send_code_button);
        verifyButton = findViewById(R.id.verify_phone_button);
        inputPhoneNumber = findViewById(R.id.phone_number_input);
        inputCode = findViewById(R.id.verification_code_input);
    }

    //method used to send the verification code to the user's phone
    private void sendVerificationCode() {
        String phoneNumber = inputPhoneNumber.getText().toString();
        //making sure the user hasn't left the phone number field empty
        if (TextUtils.isEmpty(phoneNumber)) {
            Toast.makeText(PhoneLoginActivity.this, "Please Enter your Phone Number Before Proceeding", Toast.LENGTH_SHORT).show();
        } else {
            //once the code is sent, the user is now in the process of entering his verification code
            stage = Stage.ENTERING_CODE;

            //showing a loading bar that prompts the user to wait while the code is being sent to
            //his phone
            loadingBar.setTitle("Sending Verification Code");
            loadingBar.setMessage("Please Wait...");
            loadingBar.setCanceledOnTouchOutside(false);
            loadingBar.show();

            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    phoneNumber,        // Phone number to verify
                    60,                 // Timeout duration
                    TimeUnit.SECONDS,   // Unit of timeout
                    PhoneLoginActivity.this,       // Activity (for callback binding)
                    mCallbacks);        // OnVerificationStateChangedCallbacks
        }
    }

    //method used to verify the verification code that the user has entered
    private void verifyVerificationCode() {
        inputPhoneNumber.setVisibility(View.INVISIBLE);
        sendVerificationButton.setVisibility(View.INVISIBLE);

        String code = inputCode.getText().toString();
        //making sure the user hasn't left the code field empty
        if (TextUtils.isEmpty(code)) {
            Toast.makeText(PhoneLoginActivity.this, "Please Enter Your Code Before Proceeding", Toast.LENGTH_SHORT).show();
        } else {
            //showing a loading bar that prompts the user to wait while the code he has entered is verified
            loadingBar.setTitle("Verifying Code");
            loadingBar.setMessage("Please Wait..");
            loadingBar.setCanceledOnTouchOutside(false);
            loadingBar.show();

            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);
            signInWithPhoneAuthCredential(credential);
        }
    }

    //save what the user has entered in the phone number or verification code fields so they can be
    //restored when the context is changed
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("inputPhoneNumber", inputPhoneNumber.getText().toString());
        outState.putString("inputCode", inputCode.getText().toString());
        outState.putString("stage", stage);
    }

    //method used to restore the email and the password fields when the context is changed
    private void restoreSavedState(Bundle savedInstanceState) {
        String savedInputPhoneNumber = savedInstanceState.getString("inputPhoneNumber");
        if (savedInputPhoneNumber != null) {
            inputPhoneNumber.setText(savedInputPhoneNumber);
        }

        String savedInputCode = savedInstanceState.getString("inputCode");
        if (savedInputCode != null) {
            inputCode.setText(savedInputCode);
        }

        String savedStage = savedInstanceState.getString("stage");
        if (savedStage != null) {
            stage = savedStage;
        }

    }

    //this method is used to make the appropriate views visible and invisible when the context is changed
    private void handleStage() {
        if ((Stage.ENTERING_PHONE_NUMBER).equals(stage)) {
            inputPhoneNumber.setVisibility(View.VISIBLE);
            sendVerificationButton.setVisibility(View.VISIBLE);
            inputCode.setVisibility(View.INVISIBLE);
            verifyButton.setVisibility(View.INVISIBLE);
        } else {
            inputPhoneNumber.setVisibility(View.INVISIBLE);
            sendVerificationButton.setVisibility(View.INVISIBLE);
            inputCode.setVisibility(View.VISIBLE);
            verifyButton.setVisibility(View.VISIBLE);
        }
    }

    //method used to send the user to the MainActivity
    private void sendUserToMainActivity() {
        Intent mainIntent = new Intent(PhoneLoginActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }


    //this method is used to sign in the user after he entered the verification code
    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
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
                                loadingBar.dismiss();
                                sendUserToMainActivity();
                            }
                        }
                    });
                } else {
                    loadingBar.dismiss();
                    Toast.makeText(PhoneLoginActivity.this, "" + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


}
