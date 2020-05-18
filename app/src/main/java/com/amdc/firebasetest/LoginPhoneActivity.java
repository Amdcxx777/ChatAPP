package com.amdc.firebasetest;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class LoginPhoneActivity extends AppCompatActivity {
    private Button SendVerificationCodeButton, VerifyButton;
    private EditText InputPhoneNumber, InputVerificationCode;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;
    private FirebaseAuth mAuth;
    private DatabaseReference RootRef;
    private ProgressDialog loadingBar;
    private String mVerificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_phone);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); //full screen
        setSupportActionBar(findViewById(R.id.activity_login_phone_toolbar)); // my toolbar
        Objects.requireNonNull(getSupportActionBar()).setTitle(Html.fromHtml("<font color='#F3FB00'>" + "Login via phone" + "</font>"));
        mAuth = FirebaseAuth.getInstance();
        RootRef = FirebaseDatabase.getInstance().getReference();
        SendVerificationCodeButton = findViewById(R.id.send_ver_code_button);
        VerifyButton = findViewById(R.id.verify_button);
        InputPhoneNumber = findViewById(R.id.phone_nnumber_input);
        InputVerificationCode = findViewById(R.id.verification_code_input);
        loadingBar = new ProgressDialog(this);
        SendVerificationCodeButton.setOnClickListener(view -> {
            String phoneNumber = InputPhoneNumber.getText().toString();
            if (TextUtils.isEmpty(phoneNumber)) {
                Toast.makeText(LoginPhoneActivity.this, "Please enter your phone number first...", Toast.LENGTH_SHORT).show();
            }
            else {
                loadingBar.setTitle("Phone Verification");
                loadingBar.setMessage("Please wait, while we are authenticating your phone...");
                loadingBar.setCanceledOnTouchOutside(false);
                loadingBar.show();
                // Phone number to verify // Timeout duration // Timeout duration // Activity (for callback binding) // OnVerificationStateChangedCallbacks
                PhoneAuthProvider.getInstance().verifyPhoneNumber(phoneNumber, 60, TimeUnit.SECONDS, LoginPhoneActivity.this, callbacks);
            }
        });

        VerifyButton.setOnClickListener(view -> {
            SendVerificationCodeButton.setVisibility(View.INVISIBLE);
            InputPhoneNumber.setVisibility(View.INVISIBLE);
            String verificationCode = InputVerificationCode.getText().toString();
            if (TextUtils.isEmpty(verificationCode)) {
                Toast.makeText(LoginPhoneActivity.this, "Please write verification code first...", Toast.LENGTH_SHORT).show();
            } else {
                loadingBar.setTitle("Verification Code");
                loadingBar.setMessage("please wait, while we are verifying verification code...");
                loadingBar.setCanceledOnTouchOutside(false);
                loadingBar.show();
                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, verificationCode);
                signInWithPhoneAuthCredential(credential);
            }
        });

        callbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                signInWithPhoneAuthCredential(phoneAuthCredential);
            }
            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                loadingBar.dismiss();
                Toast.makeText(LoginPhoneActivity.this, "Invalid Phone Number, Please enter phone number with your country code (sample format: +380991234567)", Toast.LENGTH_LONG).show();
                SendVerificationCodeButton.setVisibility(View.VISIBLE);
                InputPhoneNumber.setVisibility(View.VISIBLE);
                VerifyButton.setVisibility(View.INVISIBLE);
                InputVerificationCode.setVisibility(View.INVISIBLE);
            }

            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                mVerificationId = verificationId;
//                mResendToken = token;
                loadingBar.dismiss();
                Toast.makeText(LoginPhoneActivity.this, "Code has been sent, please check and verify...", Toast.LENGTH_SHORT).show();
                SendVerificationCodeButton.setVisibility(View.INVISIBLE);
                InputPhoneNumber.setVisibility(View.INVISIBLE);
                VerifyButton.setVisibility(View.VISIBLE);
                InputVerificationCode.setVisibility(View.VISIBLE);
            }
        };
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(instanceIdResult -> {
                    String currentUserID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                    RootRef.child("Users").child(currentUserID).child("device_token").setValue(instanceIdResult.getToken());
                    loadingBar.dismiss();
                    Toast.makeText(LoginPhoneActivity.this, "Congratulations, you're logged in successfully...", Toast.LENGTH_SHORT).show();
                    SendUserToMainActivity();
                });
            } else {
                String message = Objects.requireNonNull(task.getException()).toString();
                Toast.makeText(LoginPhoneActivity.this, "Error : "  +  message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void SendUserToMainActivity() {
        Intent mainIntent = new Intent(LoginPhoneActivity.this, MainActivity.class);
        startActivity(mainIntent);
        finish();
    }
}
