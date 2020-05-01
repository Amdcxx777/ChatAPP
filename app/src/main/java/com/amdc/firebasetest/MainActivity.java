package com.amdc.firebasetest;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.Html;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private DatabaseReference RootRef;
    private String currentUserID;
    static Vibrator vibrator;
    static MediaPlayer sound;
    static String userSet;
    static boolean bell = true, vibro = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        sound = MediaPlayer.create(this, R.raw.viber);
        mAuth = FirebaseAuth.getInstance();
        RootRef = FirebaseDatabase.getInstance().getReference();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); //full screen
        setSupportActionBar(findViewById(R.id.main_page_toolbar)); // my toolbar
        Objects.requireNonNull(getSupportActionBar()).setTitle(Html.fromHtml("<font color='#03DAC5'>" + "WhatsApp" + "</font>")); // name toolbar
        ViewPager myViewPager = findViewById(R.id.main_tabs_pager);
        MenuSelectionAdapter myTabsAccessorAdapter = new MenuSelectionAdapter(getSupportFragmentManager());
        myViewPager.setAdapter(myTabsAccessorAdapter);
        TabLayout myTabLayout = findViewById(R.id.main_tabs);
        myTabLayout.setupWithViewPager(myViewPager);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null)  SendUserToLoginActivity();
        else {
            userSet = "5afzRx0owl7oDDE6";
            updateUserStatus("online");
            VerifyUserExistence();
        }
    }

    private void VerifyUserExistence() {
        currentUserID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        RootRef.child("Users").child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if((!dataSnapshot.child("name").exists())) SendUserToSettingsActivity();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.options_menu, menu);
        if (bell) menu.findItem(R.id.sound).setChecked(true); // change checker
        if (vibro) menu.findItem(R.id.vibration).setChecked(true); // change checker
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) { // item menu
        super.onOptionsItemSelected(item);
        if(item.getItemId() == R.id.main_create_group_option) RequestNewGroup();
        if(item.getItemId() == R.id.user_settings) SendUserToSettingsActivity();
        if (item.getItemId() == R.id.main_find_friends_option) SendUserToFindFriendsActivity();
        if(item.getItemId() == R.id.main_logout_option) RequestLogOut();
        if(item.getItemId() == R.id.sound) {
            if(item.isChecked()) { item.setChecked(false); bell = false; }
            else { item.setChecked(true); bell = true; }
        }
        if(item.getItemId() == R.id.vibration) {
            if(item.isChecked()) { item.setChecked(false); vibro = false; }
            else { item.setChecked(true); vibro = true; }
        }
        return true;
    }

    private void RequestLogOut() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this,R.style.AlertDialog);
        builder.setTitle("Are you sure you want to log out?");
        builder.setPositiveButton("Exit", (dialogInterface, i) -> {
            updateUserStatus("offline");
            mAuth.signOut();
            SendUserToLoginActivity();
        });
        builder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.cancel());
        builder.show();
    }

    private void RequestNewGroup() { // menu add new group
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this,R.style.AlertDialog);
        builder.setTitle("Create new Group");
        final EditText groupNameField = new EditText(MainActivity.this);
        groupNameField.setHint("  title for new group");
        builder.setView(groupNameField);
        builder.setPositiveButton("Create", (dialogInterface, i) -> {
            String groupName = groupNameField.getText().toString();
            if (TextUtils.isEmpty(groupName)) Toast.makeText(MainActivity.this, "Please write Group Name...", Toast.LENGTH_SHORT).show();
            else CreateNewGroup(groupName);
        });
        builder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.cancel());
        builder.show();
    }

    private void CreateNewGroup(final String groupName) {
        RootRef.child("Groups").child(groupName).setValue("").addOnCompleteListener(task -> {
            if (task.isSuccessful()) Toast.makeText(MainActivity.this, groupName + "Group is Created Successfully", Toast.LENGTH_SHORT).show();
        });
    }

    private void SendUserToLoginActivity() {
        Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
    }

    private void SendUserToSettingsActivity() {
        Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivity(settingsIntent);
    }

    private void SendUserToFindFriendsActivity() {
        Intent findFriendsIntent = new Intent(MainActivity.this, FindFriendsActivity.class);
        startActivity(findFriendsIntent);
    }

    @SuppressLint("SimpleDateFormat")
    private void updateUserStatus(String state) { //status user
        HashMap<String, Object> onlineStateMap = new HashMap<>();
        onlineStateMap.put("time", new SimpleDateFormat("HH:mm").format(Calendar.getInstance().getTime()));
        onlineStateMap.put("date", new SimpleDateFormat("dd.MMM.yyyy", Locale.US).format(Calendar.getInstance().getTime()));
        onlineStateMap.put("state", state);
        currentUserID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        RootRef.child("Users").child(currentUserID).child("userState").updateChildren(onlineStateMap);
    }

    @SuppressLint("SimpleDateFormat")
    @Override
    public void onBackPressed() { // exit program with request
        new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Exit")
                .setMessage("Are you sure you want to exit from chat?").setPositiveButton("Yes", (dialog, which) -> {
                    FirebaseUser currentUser = mAuth.getCurrentUser();
                    if (currentUser != null) updateUserStatus("offline");
                    moveTaskToBack(true);
                }).setNegativeButton("No", null).show();
    }
}

