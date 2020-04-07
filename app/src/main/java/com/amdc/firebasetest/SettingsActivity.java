package com.amdc.firebasetest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;
import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {
    private String currentUserID;
    private DatabaseReference RootRef;

    private Button UpdateAccountSettings;
    private EditText userName;
    private EditText userStatus;
    private ImageView userProfileImage;
    private static final int GalleryPick = 1;
    private StorageReference UserProfileImagesRef;
    private ProgressDialog loadingBar;
    private String photoUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currentUserID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        RootRef = FirebaseDatabase.getInstance().getReference();
        UserProfileImagesRef = FirebaseStorage.getInstance().getReference().child("Profile images");
        InitializeFields();
//        userName.setVisibility(View.INVISIBLE); // скрывает поле для введения имени
        UpdateAccountSettings.setOnClickListener(view -> UpdateSettings());
        RetrieveUserInfo();
        userProfileImage.setOnClickListener(view -> { // gallery
//            UpdateSettings();
            Intent galleryIntent = new Intent();
            galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
            galleryIntent.setType("image/*");
            startActivityForResult(galleryIntent, GalleryPick);
        });
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Account Settings");
    }

    private void InitializeFields() {
        UpdateAccountSettings = findViewById(R.id.update_settings_button);
        userName = findViewById(R.id.set_user_name);
        userStatus = findViewById(R.id.set_profile_status);
        userProfileImage = findViewById(R.id.set_profile_image);
        loadingBar = new ProgressDialog(this);
    }
    private void UpdateSettings() {
        String setUserName = userName.getText().toString();
        String setStatus = userStatus.getText().toString();
        if (TextUtils.isEmpty(setUserName))  Toast.makeText(this, "Please write your user name first....", Toast.LENGTH_SHORT).show();
        if (TextUtils.isEmpty(setStatus)) Toast.makeText(this, "Please write your status....", Toast.LENGTH_SHORT).show();
        else {
            HashMap<String,Object> profileMap = new HashMap<>();
            profileMap.put("uid",currentUserID);
            profileMap.put("name",setUserName);
            profileMap.put("status",setStatus);
            profileMap.put("image", photoUrl);
            RootRef.child("Users").child(currentUserID).updateChildren(profileMap).addOnCompleteListener(task -> { //updateChildren (setValue)
                if(task.isSuccessful()) {
                    SendUserToMainActivity();
                    Toast.makeText(SettingsActivity.this, "Profile Updated Successfully...", Toast.LENGTH_SHORT).show();
                } else {
                    String message = Objects.requireNonNull(task.getException()).toString();
                    Toast.makeText(SettingsActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void RetrieveUserInfo() { // извлечение информации о пользователе
        RootRef.child("Users").child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if((dataSnapshot.exists()) && (dataSnapshot.hasChild("name") && (dataSnapshot.hasChild("image")))) {
                    String retrieveProfilePhoto = (String) dataSnapshot.child("image").getValue();
                    userName.setText((String) dataSnapshot.child("name").getValue());
                    userStatus.setText((String) dataSnapshot.child("status").getValue()); //status
                    photoUrl = retrieveProfilePhoto;
                    Picasso.get().load(retrieveProfilePhoto).networkPolicy(NetworkPolicy.NO_CACHE).into(userProfileImage);
                }
                else if ((dataSnapshot.exists()) && (dataSnapshot.hasChild("name"))) {
                    userName.setText((String) dataSnapshot.child("name").getValue());
                    userStatus.setText((String) dataSnapshot.child("status").getValue());

                } else if ((dataSnapshot.exists()) && (dataSnapshot.hasChild("image"))) {
                    String retrieveProfilePhoto = (String) dataSnapshot.child("image").getValue();
                    Picasso.get().load(retrieveProfilePhoto).networkPolicy(NetworkPolicy.NO_CACHE).into(userProfileImage);
                    photoUrl = retrieveProfilePhoto;
                    userName.setText((String) dataSnapshot.child("name").getValue());
                    userStatus.setText((String) dataSnapshot.child("status").getValue());
                } else {
//                    userName.setVisibility(View.VISIBLE); // открывает поле для введения имени
                    Toast.makeText(SettingsActivity.this, "Please set & update your profile information...", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode==GalleryPick  &&  resultCode==RESULT_OK  &&  data!=null) {
//            Uri ImageUri = data.getData();
            CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).setAspectRatio(1, 1).start(this);
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                loadingBar.setTitle("Set Profile Image");
                loadingBar.setMessage("Please wait, your profile image is updating...");
                loadingBar.setCanceledOnTouchOutside(false);
                loadingBar.show();
                final Uri resultUri = Objects.requireNonNull(result).getUri();
                final StorageReference filePath = UserProfileImagesRef.child(currentUserID + ".jpg");
                filePath.putFile(resultUri).addOnSuccessListener(taskSnapshot -> filePath.getDownloadUrl().addOnSuccessListener(uri -> {
                    final String downloadUrl = uri.toString();
                    RootRef.child("Users").child(currentUserID).child("image").setValue(downloadUrl).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(SettingsActivity.this, "Profile image stored to firebase database successfully.", Toast.LENGTH_SHORT).show();
                            loadingBar.dismiss();
                        } else {
                            String message = Objects.requireNonNull(task.getException()).getMessage();
                            Toast.makeText(SettingsActivity.this, "Error Occurred..." + message, Toast.LENGTH_SHORT).show();
                            loadingBar.dismiss();
                        }
                    });
                }));
            }
        }
    }

    private void SendUserToMainActivity() {
        Intent mainIntent = new Intent(SettingsActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }
}
