package com.amdc.firebasetest;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;
import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {
    private String currentUserID, photoUrl;
    private DatabaseReference RootRef;
    private Button UpdateAccountSettings;
    private EditText userName, userStatus;
    private ImageView userProfileImage;
    private static final int GalleryPick = 1;
    private ProgressDialog loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        currentUserID = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        RootRef = FirebaseDatabase.getInstance().getReference();
        InitializeFields();
        UpdateAccountSettings.setOnClickListener(view -> UpdateSettings());
        RetrieveUserInfo();
        userProfileImage.setOnClickListener(view -> { // gallery
            Intent galleryIntent = new Intent();
            galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
            galleryIntent.setType("image/*");
            startActivityForResult(galleryIntent, GalleryPick);
        });
    }

    private void InitializeFields() {
        UpdateAccountSettings = findViewById(R.id.update_settings_button);
        userName = findViewById(R.id.set_user_name);
        userStatus = findViewById(R.id.set_profile_status);
        userProfileImage = findViewById(R.id.set_profile_image);
        loadingBar = new ProgressDialog(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); //full screen
        setSupportActionBar(findViewById(R.id.activity_settings_toolbar)); // my toolbar
        Objects.requireNonNull(getSupportActionBar()).setTitle(Html.fromHtml("<font color='#F3FB00'>" + "Setting Account" + "</font>"));

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
                if(task.isSuccessful()) { SendUserToMainActivity();
                    Toast.makeText(SettingsActivity.this, "Profile Updated Successfully...", Toast.LENGTH_SHORT).show();
                } else { String message = Objects.requireNonNull(task.getException()).toString();
                    Toast.makeText(SettingsActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void RetrieveUserInfo() { // извлечение информации о пользователе
        RootRef.child("Users").child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if ((dataSnapshot.exists()) && (dataSnapshot.hasChild("name"))) {
                    userName.setText((String) dataSnapshot.child("name").getValue()); // name
                    userStatus.setText((String) dataSnapshot.child("status").getValue()); //status
                }if((dataSnapshot.exists()) && (dataSnapshot.hasChild("image"))) {
                    photoUrl = (String) dataSnapshot.child("image").getValue();
                    Picasso.get().load(photoUrl).resize(300, 300).placeholder(R.drawable.profile_image).into(userProfileImage);
                } else Toast.makeText(SettingsActivity.this, "Please set & update your profile information...", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GalleryPick  &&  resultCode == RESULT_OK  &&  data != null) {
//            Uri ImageUri = data.getData();
        CropImage.activity().setGuidelines(CropImageView.Guidelines.ON)
                    .setMinCropResultSize(100,100)
                    .setMaxCropResultSize(300, 300)
                    .setAspectRatio(1, 1).start(this);
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                loadingBar.setTitle("Set Profile Image");
                loadingBar.setMessage("Please wait, your profile image is updating...");
                loadingBar.setCanceledOnTouchOutside(false);
                loadingBar.show();
//                Uri resultUri = Objects.requireNonNull(result).getUri();
                final StorageReference filePath = FirebaseStorage.getInstance().getReference().child("Profile images").child(currentUserID + ".jpg");
                filePath.putFile(Objects.requireNonNull(result).getUri()).addOnSuccessListener(taskSnapshot -> filePath.getDownloadUrl().addOnSuccessListener(uri -> { // resultUr
                    RootRef.child("Users").child(currentUserID).child("image").setValue(uri.toString()).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(SettingsActivity.this, "Profile image stored successfully.", Toast.LENGTH_SHORT).show();
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
