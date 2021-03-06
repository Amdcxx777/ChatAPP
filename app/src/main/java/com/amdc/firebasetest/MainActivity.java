package com.amdc.firebasetest;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.text.Html;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
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
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.os.PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    static long[] pattern = { 200, 200, 200, 200, 200, 500, 200, 200, 200, 200, 200, 500, 200, 200, 200, 200, 200, 500 };
    private FirebaseAuth mAuth;
    private DatabaseReference RootRef;
    private TextView incomingUserName;
    private CircleImageView incomingUserImage;
    private String currentUserID, incomingCallUser = "Unknown";
    static boolean bell = true, vibro = true, melody1, melody2, displayOFF = false;
    private AlertDialog alertDialogCall;
    static SinchClient sinchClient;
//    static long incomingCallTime;
    static Call call;
    static Vibrator vibrator;
    static MediaPlayer sound;
    static String userSet;
    static PowerManager.WakeLock proximityWakeLock;
    static SensorManager sensorManager;
    static Sensor proximitySensor;
    private LinearLayout view;
    final String[] retImage = {"default_image"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        RootRef = FirebaseDatabase.getInstance().getReference();
        InitializeControllers();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Voice Calling Setting Sinch ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        sinchClient = Sinch.getSinchClientBuilder().context(this).userId(currentUserID)
                .applicationKey("00ef7168-1938-40ae-a2eb-eba4ea2b5b19")
                .applicationSecret("E2ZKJPMif06ufEVFqkoZOA==")
                .environmentHost("clientapi.sinch.com")
                .build();
        //~~~~~~~~~~~~~~~~~~~~~~~~~ Listener for incoming voice call ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        sinchClient.setSupportCalling(true);
        sinchClient.startListeningOnActiveConnection();
        //~~~~~~~~~~~~~~~~~~~~~~~~~ Listener for incoming voice call ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        sinchClient.getCallClient().addCallClientListener((callClient, incomingCall) -> { // incoming calls
            if (melody2) sound = MediaPlayer.create(this, R.raw.ring_my);
            else sound = MediaPlayer.create(this, R.raw.ring);
            sound.setLooping(true);
            if (bell) sound.start();
            if (vibro) vibrator.vibrate(pattern, 2); // Vibration when incoming voice call
            call = incomingCall;
            RootRef.child("Users").child(incomingCall.getRemoteUserId()).addValueEventListener(new ValueEventListener() {
                @SuppressLint({"InflateParams", "SetTextI18n", "ResourceAsColor"})
                @Override // search Name incoming user
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        incomingCallUser = (String) dataSnapshot.child("name").getValue();
                        if (dataSnapshot.hasChild("image")) {
                            retImage[0] = (String) dataSnapshot.child("image").getValue();
                        }
                    }
                    if (call == incomingCall) {
                        view = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_incoming_call, null);
                        alertDialogCall = new AlertDialog.Builder(MainActivity.this).setCancelable(false).setView(view).create();
                        incomingUserName = view.findViewById(R.id.incoming_user_name);
                        incomingUserImage = view.findViewById(R.id.incoming_user_image);
                        view.findViewById(R.id.incoming_accept_btn).setVisibility(View.VISIBLE);
                        view.findViewById(R.id.incoming_cancel_btn).setVisibility(View.VISIBLE);
//                        view.findViewById(R.id.speaking_cancel_btn).setVisibility(View.INVISIBLE);
                        incomingUserImage = view.findViewById(R.id.incoming_user_image);
                        incomingUserName.setText(incomingCallUser + " is calling");
                        incomingUserName.setTextColor(R.color.colorPrimaryDark);
                        try {
                            Picasso.get().load(retImage[0]).resize(90, 90).placeholder(R.drawable.profile_image).into(incomingUserImage); // for chat bar
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "Error download User-Image", Toast.LENGTH_SHORT).show();
                        }
                        if (alertDialogCall != null && !alertDialogCall.isShowing()) alertDialogCall.show();
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) { }
            });
        });
        sinchClient.start();
        }
    }

    @SuppressLint("InflateParams")
    private void InitializeControllers() {
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Sensor Proximity ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        proximitySensor = Objects.requireNonNull(sensorManager).getDefaultSensor(Sensor.TYPE_PROXIMITY);
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); //full screen
        setSupportActionBar(findViewById(R.id.main_page_toolbar)); // my toolbar
        Objects.requireNonNull(getSupportActionBar()).setTitle(Html.fromHtml("<font color='#03DAC5'>" + "WhatsApp" + "</font>")); // name toolbar
        ViewPager myViewPager = findViewById(R.id.main_tabs_pager);
        MenuSelectionAdapter myTabsAccessorAdapter = new MenuSelectionAdapter(getSupportFragmentManager());
        myViewPager.setAdapter(myTabsAccessorAdapter);
        TabLayout myTabLayout = findViewById(R.id.main_tabs);
        myTabLayout.setupWithViewPager(myViewPager);
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~ Listener proximity sensor for screen off ~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("InvalidWakeLockTag")
    @Override
    public void onSensorChanged(SensorEvent event) {
        PowerManager powerManager = (PowerManager) this.getSystemService(POWER_SERVICE);
        if (event.values[0] == 0 && displayOFF) { // When something is near.
            if (proximityWakeLock != null) return;
            proximityWakeLock = Objects.requireNonNull(powerManager).newWakeLock(PROXIMITY_SCREEN_OFF_WAKE_LOCK, "OnOffScreen");
            proximityWakeLock.acquire(60*60*1000L /*60 minutes*/);
        } else { if (proximityWakeLock != null) { proximityWakeLock.release(); proximityWakeLock = null; } }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Listener Voice Call ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private class SinchCallListener implements CallListener {
        @Override
        public void onCallProgressing(com.sinch.android.rtc.calling.Call call) { }
        @SuppressLint({"InflateParams", "SetTextI18n"})
        @Override
        public void onCallEstablished(com.sinch.android.rtc.calling.Call speakCall) { call = speakCall;
            if (sound != null && sound.isPlaying()) sound.stop();
            displayOFF = true;
            if (alertDialogCall.isShowing()) alertDialogCall.dismiss();
            view = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_incoming_call, null);
            alertDialogCall = new AlertDialog.Builder(MainActivity.this).setCancelable(false).setView(view).create();
            incomingUserName = view.findViewById(R.id.incoming_user_name);
            incomingUserImage = view.findViewById(R.id.incoming_user_image);
            view.findViewById(R.id.speaking_cancel_btn).setVisibility(View.VISIBLE);
            incomingUserName.setText(incomingCallUser + " online");
            incomingUserName.setTextColor(Color.RED);
            try { Picasso.get().load(retImage[0]).resize(90, 90).placeholder(R.drawable.profile_image).into(incomingUserImage); // for chat bar
            } catch (Exception e) { Toast.makeText(MainActivity.this, "Error download User-Image", Toast.LENGTH_SHORT).show(); }
            if (!alertDialogCall.isShowing()) alertDialogCall.show();
        }
        @Override
        public void onCallEnded(com.sinch.android.rtc.calling.Call endedCall) {
            Toast.makeText(getApplicationContext(), "Call Ended", Toast.LENGTH_SHORT).show();
            if (alertDialogCall != null && alertDialogCall.isShowing()) alertDialogCall.dismiss();
            if (sound != null && sound.isPlaying()) sound.stop();
            call = endedCall;
            call.hangup();
            call = null;
            sound = MediaPlayer.create(MainActivity.this, R.raw.telephone_busy);
            sound.start();
            displayOFF = false;
        }
        @Override
        public void onShouldSendPushNotification(com.sinch.android.rtc.calling.Call call, List<PushPair> list) { }
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
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

    protected void onResume() {
        super.onResume();
        if (proximitySensor != null) sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        sinchClient.stopListeningOnActiveConnection(); // Sinch Client STOP Listener This Activity
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Verify User and add user date ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
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

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Option Menu ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.options_menu, menu);
        if (bell) menu.findItem(R.id.sound).setChecked(true); // change checker
        if (vibro) menu.findItem(R.id.vibration).setChecked(true); // change checker
        return true;
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~ Option Menu Select ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
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
        switch (item.getItemId()) {
            case R.id.melody1:
            case R.id.melody2:
                if (item.isChecked()) item.setChecked(false);
                else item.setChecked(true);
        }
        if(item.getItemId() == R.id.melody1) { if (item.isChecked()) { melody1 = true; melody2 = false; } }
        if(item.getItemId() == R.id.melody2) { if (item.isChecked()) { melody2 = true; melody1 = false; } }
        return true;
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Menu for add new group ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private void RequestNewGroup() {
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

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Create new group ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @SuppressLint("SimpleDateFormat")
    private void CreateNewGroup(final String groupName) { // create new group with admin copyright and with settings
        currentUserID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
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

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ User status ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @SuppressLint("SimpleDateFormat")
    private void updateUserStatus(String state) { //status user
        HashMap<String, Object> onlineStateMap = new HashMap<>();
        onlineStateMap.put("time", new SimpleDateFormat("HH:mm").format(Calendar.getInstance().getTime()));
        onlineStateMap.put("date", new SimpleDateFormat("dd.MMM.yyyy", Locale.US).format(Calendar.getInstance().getTime()));
        onlineStateMap.put("state", state);
        currentUserID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        RootRef.child("Users").child(currentUserID).child("userState").updateChildren(onlineStateMap);
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Logout from chat ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private void RequestLogOut() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialog);
        builder.setTitle("You want to logout and exit?");
        builder.setPositiveButton("Exit", (dialogInterface, i) -> {
            updateUserStatus("offline");
            mAuth.signOut();
            SendUserToLoginActivity();
        });
        builder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.cancel());
        builder.show();
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~ Buttons Speaking Call ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

//    public void incomingCallButton() {
//        incomingCallTime = System.currentTimeMillis();
//        do {
////            LinearLayout view = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_incoming_call, null);
//            view.findViewById(R.id.incoming_accept_btn).setOnClickListener(v -> {
//                if (sound != null && sound.isPlaying()) sound.stop();
//                if (vibrator.hasVibrator()) vibrator.cancel();
//                if (call != null) {
//                    call.answer();
//                    call.addCallListener(new SinchCallListener());
//                }
//            });
//            view.findViewById(R.id.incoming_cancel_btn).setOnClickListener(v -> {
//                if (alertDialogCall != null && alertDialogCall.isShowing()) alertDialogCall.dismiss();
//                if (sound != null && sound.isPlaying()) sound.stop();
//                if (vibrator.hasVibrator()) vibrator.cancel();
//                if (call != null) {
//                    call.hangup();
//                    call.addCallListener(new SinchCallListener());
//                }
//                call = null;
//            });
//        } while (incomingCallTime + 10000 > System.currentTimeMillis());
//        if (alertDialogCall != null && alertDialogCall.isShowing()) alertDialogCall.dismiss();
//        if (sound != null && sound.isPlaying()) sound.stop();
//        if (vibrator.hasVibrator()) vibrator.cancel();
//        if (call != null) {
//            call.hangup();
//            call.addCallListener(new SinchCallListener());
//        }
//        call = null;
//    }

    public void btnIncomingAcceptSpeaking(View view) {
        if (sound != null && sound.isPlaying()) sound.stop();
        if (vibrator.hasVibrator()) vibrator.cancel();
        if (call != null) {
            call.answer();
            call.addCallListener(new SinchCallListener());
        }
    }

    public void btnIncomingCancelSpeaking(View view) {
        if (alertDialogCall != null && alertDialogCall.isShowing()) alertDialogCall.dismiss();
        if (sound != null && sound.isPlaying()) sound.stop();
        if (vibrator.hasVibrator()) vibrator.cancel();
        if (call != null) {
            call.hangup();
            call.addCallListener(new SinchCallListener());
        }
        call = null;
    }

    public void btnCancelSpeaking(View view) {
        if (alertDialogCall != null && alertDialogCall.isShowing()) alertDialogCall.dismiss();
        if (call != null) {
            call.hangup();
            call.addCallListener(new SinchCallListener());
        }
        call = null;
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Exit from program with request ~~~~~~~~~~~~~~~~~~~~~~~~~~
    @SuppressLint("SimpleDateFormat")
    @Override
    public void onBackPressed() {
                new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Exit From Chat")
                .setMessage("You want to exit from chat?").setPositiveButton("Yes", (dialog, which) -> {
                    FirebaseUser currentUser = mAuth.getCurrentUser();
                    if (currentUser != null) updateUserStatus("offline");
                    moveTaskToBack(true);
//                    System.exit(0);
                }).setNegativeButton("No", null).show();
    }
}

