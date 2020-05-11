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
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.Sinch;
import com.sinch.android.rtc.SinchClient;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    static long[] pattern = { 200, 200, 200, 200, 200, 500, 200, 200, 200, 200, 200, 500, 200, 200, 200, 200, 200, 500 };
    private FirebaseAuth mAuth;
    private DatabaseReference RootRef;
    private String currentUserID;
    static AlertDialog alertDialogCall;
    static SinchClient sinchClient;
    static Call call;
    static Vibrator vibrator;
    static MediaPlayer sound;
    static String userSet;

//    static SensorManager sensorManager;
//    static Sensor proximitySensor;
    static boolean bell = true, vibro = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
//        proximitySensor = Objects.requireNonNull(sensorManager).getDefaultSensor(Sensor.TYPE_PROXIMITY);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
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
        currentUserID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Voice Calling ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        sinchClient = Sinch.getSinchClientBuilder().context(this).userId(currentUserID)
                .applicationKey("00ef7168-1938-40ae-a2eb-eba4ea2b5b19")
                .applicationSecret("E2ZKJPMif06ufEVFqkoZOA==")
                .environmentHost("clientapi.sinch.com")
                .build();
        sinchClient.setSupportCalling(true);
        sinchClient.startListeningOnActiveConnection();
        sinchClient.getCallClient().addCallClientListener((callClient, incomingCall) -> { // incoming calls
            sound = MediaPlayer.create(this, R.raw.ring);
            sound.start();
            vibrator.vibrate(pattern, 2); // Vibration when incoming voice call
            alertDialogCall = new AlertDialog.Builder(MainActivity.this).create();
            alertDialogCall.setTitle("Incoming Call");
            alertDialogCall.setCancelable(false);
            alertDialogCall.setIcon(android.R.drawable.sym_call_incoming);
            alertDialogCall.setButton(AlertDialog.BUTTON_NEUTRAL, "Cancel", (dialog, which) -> {
                vibrator.cancel();
                sound.stop();
                call = incomingCall;
                call.hangup();
                dialog.dismiss();
            });
            alertDialogCall.setButton(AlertDialog.BUTTON_POSITIVE, "Talk", (dialog, which) -> {
                call = incomingCall;
                vibrator.cancel();
//                sound.stop();
                call.answer();
                call.addCallListener(new SinchCallListener());
            });
            alertDialogCall.show();
        });
        sinchClient.start();
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Listener Voice Call ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    public class SinchCallListener implements CallListener {
        @Override
        public void onCallProgressing(com.sinch.android.rtc.calling.Call call) {
            Toast.makeText(getApplicationContext(), "Ringing...", Toast.LENGTH_SHORT).show();
        }
        @Override
        public void onCallEstablished(com.sinch.android.rtc.calling.Call speakCall) {
            sound.stop();
            alertDialogCall.dismiss();
            alertDialogCall = new AlertDialog.Builder(MainActivity.this).create();
            alertDialogCall.setTitle("Speaking");
            alertDialogCall.setCancelable(false);
            alertDialogCall.setIcon(android.R.drawable.sym_action_call);
            alertDialogCall.setButton(AlertDialog.BUTTON_NEUTRAL, "Hang up", (dialog, which) -> {
                dialog.dismiss();
                call = speakCall;
                call.hangup();
            });
            alertDialogCall.show();
        }
        @Override
        public void onCallEnded(com.sinch.android.rtc.calling.Call endedCall) { call = endedCall;
            Toast.makeText(getApplicationContext(), "Call Ended", Toast.LENGTH_SHORT).show();
            alertDialogCall.dismiss();
            sound.stop();
        }
        @Override
        public void onShouldSendPushNotification(com.sinch.android.rtc.calling.Call call, List<PushPair> list) { }
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
//        currentUserID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
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

    private void RequestNewGroup() { // menu add new group
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this,R.style.AlertDialog);
        builder.setTitle("Create new Group");
        final EditText groupNameField = new EditText(MainActivity.this);
        groupNameField.setHint("   title for new group");
        builder.setView(groupNameField);
        builder.setPositiveButton("Create", (dialogInterface, i) -> {
            String groupName = groupNameField.getText().toString();
            if (TextUtils.isEmpty(groupName)) Toast.makeText(MainActivity.this, "Please write Group Name...", Toast.LENGTH_SHORT).show();
            else CreateNewGroup(groupName);
        });
        builder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.cancel());
        builder.show();
    }

    @SuppressLint("SimpleDateFormat")
    private void CreateNewGroup(final String groupName) { // create new group with admin copyright and with settings
//        currentUserID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        String messagePushID = RootRef.child("Groups").child("Users").push().getKey();
        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("userID", currentUserID);
        userMap.put("status", "admin");
        userMap.put("time", new SimpleDateFormat("HH:mm").format(Calendar.getInstance().getTime()));
        userMap.put("date", new SimpleDateFormat("dd.MMM.yyyy", Locale.US).format(Calendar.getInstance().getTime()));

        HashMap<String, Object> groupMap = new HashMap<>();
        groupMap.put("admin", currentUserID);
        groupMap.put("group_name", groupName);
        groupMap.put("messageID", messagePushID);
        groupMap.put("key", Objects.requireNonNull(messagePushID).substring(4));
        groupMap.put("time", new SimpleDateFormat("HH:mm").format(Calendar.getInstance().getTime()));
        groupMap.put("date", new SimpleDateFormat("dd.MMM.yyyy", Locale.US).format(Calendar.getInstance().getTime()));
        RootRef.child("Groups").child(groupName).child("Users").child(currentUserID).setValue(userMap).addOnCompleteListener(task -> {
            if (task.isSuccessful()) RootRef.child("Groups").child(groupName).child("Settings").setValue(groupMap).addOnCompleteListener(task1 -> {
                if (task1.isSuccessful()) Toast.makeText(MainActivity.this, "Group is Created Successfully", Toast.LENGTH_SHORT).show();
            });
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
//        currentUserID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        RootRef.child("Users").child(currentUserID).child("userState").updateChildren(onlineStateMap);
    }

    private void RequestLogOut() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this,R.style.AlertDialog);
        builder.setTitle("Are you sure you want to log out and exit?");
        builder.setPositiveButton("Exit", (dialogInterface, i) -> {
            updateUserStatus("offline");
            mAuth.signOut();
            SendUserToLoginActivity();
        });
        builder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.cancel());
        builder.show();
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

