package com.amdc.firebasetest;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Html;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.calling.CallListener;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.amdc.firebasetest.Decryption.decryptedSMS;
import static com.amdc.firebasetest.Encryption.encryptedBytes;
import static com.amdc.firebasetest.MainActivity.call;
import static com.amdc.firebasetest.MainActivity.displayOFF;
import static com.amdc.firebasetest.MainActivity.sinchClient;
import static com.amdc.firebasetest.MainActivity.sound;
import static com.amdc.firebasetest.MainActivity.vibrator;

public class GroupChatActivity extends AppCompatActivity {
    private final int RECOGNIZER_VOICE_RESULT = 1;
    private ImageButton sendMessageButton;
    private EditText userMessageInput;
    private GroupChatAdapter groupChatAdapter;
    private RecyclerView userMessagesList;
    private AlertDialog alertDialogCall;
    private TextView incomingUserName;
    private CircleImageView incomingUserImage;
    private final List<Messages> messagesList = new ArrayList<>();
    private DatabaseReference RootRef, UsersRef, GroupNameRef, GroupNameMessageRef;
    private String currentUserID, currentUserName, msmID, snow, adminGroupID, incomingCallUser = "Unknown";
    static String currentGroupName;
    private LinearLayout view;
    final String[] retImage = {"default_image"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        currentGroupName = Objects.requireNonNull(Objects.requireNonNull(getIntent().getExtras()).get("groupName")).toString();
        currentUserID = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        RootRef = FirebaseDatabase.getInstance().getReference();
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        GroupNameRef = FirebaseDatabase.getInstance().getReference().child("Groups").child(currentGroupName);
        GroupNameMessageRef = FirebaseDatabase.getInstance().getReference().child("Groups").child(currentGroupName).child("Mesages");
        initializeFields();
        getUserInfo();

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Voice Incoming Call ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        sinchClient.getCallClient().addCallClientListener((callClient, incomingCall) ->
                RootRef.child("Users").child(incomingCall.getRemoteUserId()).addValueEventListener(new ValueEventListener() {
            @SuppressLint({"SetTextI18n", "InflateParams"})
            @Override // search Name incoming user
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    incomingCallUser = (String) dataSnapshot.child("name").getValue();
                    retImage[0] = (String) dataSnapshot.child("image").getValue();
                }
                view = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_incoming_call, null);
                alertDialogCall = new AlertDialog.Builder(GroupChatActivity.this).setCancelable(false).setView(view).create();
                incomingUserName = view.findViewById(R.id.incoming_user_name);
                incomingUserImage = view.findViewById(R.id.incoming_user_image);
                view.findViewById(R.id.incoming_accept_btn).setVisibility(View.VISIBLE);
                view.findViewById(R.id.incoming_cancel_btn).setVisibility(View.VISIBLE);
                view.findViewById(R.id.speaking_cancel_btn).setVisibility(View.INVISIBLE);
                incomingUserName.setText(incomingCallUser + " is calling");
                try { Picasso.get().load(retImage[0]).resize(90, 90).placeholder(R.drawable.profile_image).into(incomingUserImage); // for chat bar
                } catch (Exception e) { Toast.makeText(GroupChatActivity.this, "Error download User-Image", Toast.LENGTH_SHORT).show(); }
                if (!alertDialogCall.isShowing()) alertDialogCall.show();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        }));
        sinchClient.startListeningOnActiveConnection();

        //~~~~~~~~~~~~~~~~~~~~~~~~~ Button for send messages ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        sendMessageButton.setOnClickListener(view -> { //listener when push shot for send messages
            try { sendMessageToGroupChat(); }
            catch (Exception e) { Toast.makeText(this, "Send message error", Toast.LENGTH_SHORT).show(); }
            userMessageInput.setText("");
        });
        sendMessageButton.setOnLongClickListener(v -> { //listener when push long for voice to text
            Intent speakIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speakIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speakIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            speakIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hi speak Something");
            startActivityForResult(speakIntent, RECOGNIZER_VOICE_RESULT);
            return false;
        });

        //~~~~~~~~~~~~~~~~~~~~~~~~~~ Get ID admin and key group ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        GroupNameRef.child("Settings").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) adminGroupID = Objects.requireNonNull(dataSnapshot.child("admin").getValue()).toString();
                if(dataSnapshot.exists()) snow = Objects.requireNonNull(dataSnapshot.child("key").getValue()).toString();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

        //~~~~~~~~~~~~~~~~~~~~ Create messages list ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        GroupNameMessageRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Messages messages = dataSnapshot.getValue(Messages.class);
                try { new Decryption(Objects.requireNonNull(messages).getMessage(), snow); }
                catch (Exception e) { Toast.makeText(GroupChatActivity.this, "Error decrypt", Toast.LENGTH_SHORT).show(); }
                Objects.requireNonNull(messages).setMessage(decryptedSMS); // set decrypted message
                messagesList.add(messages);
                groupChatAdapter.notifyDataSetChanged();
                userMessagesList.smoothScrollToPosition(Objects.requireNonNull(userMessagesList.getAdapter()).getItemCount());
            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }
            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                msmID = Objects.requireNonNull(dataSnapshot.getValue(Messages.class)).getMessageID();
                for (int i = 0; i < messagesList.size(); i++) {
                    if (msmID.equals(messagesList.get(i).getMessageID())) messagesList.remove(i);
                }
                groupChatAdapter.notifyDataSetChanged();
                userMessagesList.smoothScrollToPosition(Objects.requireNonNull(userMessagesList.getAdapter()).getItemCount());
                Toast.makeText(GroupChatActivity.this, "User " + Objects.requireNonNull(dataSnapshot.getValue(Messages.class)).getName() + " deleted message", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    private void initializeFields() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); //full screen
        setSupportActionBar(findViewById(R.id.group_chat_bar_layout)); // my toolbar
        Objects.requireNonNull(getSupportActionBar()).setTitle(Html.fromHtml("<font color='#F3FB00'>" + "Group chat: " + currentGroupName + "</font>"));
        sendMessageButton = findViewById(R.id.send_message_button);
        userMessageInput = findViewById(R.id.input_group_message);
        groupChatAdapter = new GroupChatAdapter(messagesList);
        userMessagesList = findViewById(R.id.private_messages_list_from_users);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true); //  item into list gravity from end to up
        userMessagesList.setLayoutManager(linearLayoutManager);
        userMessagesList.setAdapter(groupChatAdapter);
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Listener Voice Call ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private class SinchCallListener implements CallListener {
        @Override
        public void onCallProgressing(com.sinch.android.rtc.calling.Call call) {
        }
        @SuppressLint("SetTextI18n")
        @Override
        public void onCallEstablished(com.sinch.android.rtc.calling.Call speakCall) {
            if (sound != null && sound.isPlaying()) sound.stop();
            displayOFF = true;
            view.findViewById(R.id.incoming_accept_btn).setVisibility(View.INVISIBLE);
            view.findViewById(R.id.incoming_cancel_btn).setVisibility(View.INVISIBLE);
            view.findViewById(R.id.speaking_cancel_btn).setVisibility(View.VISIBLE);
            incomingUserName.setText(incomingCallUser + " online");
        }
        @Override
        public void onCallEnded(com.sinch.android.rtc.calling.Call endedCall) { call = endedCall;
            Toast.makeText(getApplicationContext(), "Call Ended", Toast.LENGTH_SHORT).show();
            if (alertDialogCall.isShowing()) alertDialogCall.dismiss();
            if (sound != null && sound.isPlaying()) sound.stop();
            sound = MediaPlayer.create(GroupChatActivity.this, R.raw.telephone_busy);
            sound.start();
            displayOFF = false;
        }
        @Override
        public void onShouldSendPushNotification(com.sinch.android.rtc.calling.Call call, List<PushPair> list) { }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.group_chat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) { // item menu
        super.onOptionsItemSelected(item);
        if(item.getItemId() == R.id.group_admin) viewGroupAdmin();
        if(item.getItemId() == R.id.add_user_to_group) addUserToGroup();
        if(item.getItemId() == R.id.view_group_users) viewUserGroup();
        if(item.getItemId() == R.id.get_out_from_group) RemoveFromGroup();
        if(item.getItemId() == R.id.change_group_name) changeNameGroup();
        if(item.getItemId() == R.id.change_crypt_key) renewSecurityKey();
        if(item.getItemId() == R.id.delete_user) deleteUserFromGroup();
        if(item.getItemId() == R.id.delete_group) deleteGroup();
//        if(item.getItemId() == R.id.chat_inform) {}
        return true;
    }

    private void viewUserGroup() {
        Intent intentUser = new Intent(this, UserListActivity.class);
        intentUser.putExtra("group", currentGroupName);
        intentUser.putExtra("status", "view");
        startActivity(intentUser);
    }

    private void addUserToGroup() {
        if (!currentUserID.equals(adminGroupID)) Toast.makeText(GroupChatActivity.this, "This function is available only to the administrator!!!", Toast.LENGTH_SHORT).show();
        else {
            Intent intentUser = new Intent(this, UserListActivity.class);
            intentUser.putExtra("group", currentGroupName);
            intentUser.putExtra("status", "add");
            startActivity(intentUser);
        }
    }

    private void deleteUserFromGroup() {
        if (!currentUserID.equals(adminGroupID)) Toast.makeText(GroupChatActivity.this, "This function is available only to the administrator!!!", Toast.LENGTH_SHORT).show();
        else {
            Intent intentUser = new Intent(this, UserListActivity.class);
            intentUser.putExtra("group", currentGroupName);
            intentUser.putExtra("status", "delete");
            startActivity(intentUser);
        }
    }

    private void changeNameGroup() {
//        if (!currentUserID.equals(adminGroupID)) Toast.makeText(GroupChatActivity.this, "This function is available only to the administrator!!!", Toast.LENGTH_SHORT).show();
//        else {
//            AlertDialog.Builder builder = new AlertDialog.Builder(GroupChatActivity.this,R.style.AlertDialog).setIcon(android.R.drawable.ic_menu_edit)
//                    .setTitle("Change name this group");
//            final EditText fieldGroupName = new EditText(GroupChatActivity.this); //enter text
//            fieldGroupName.setText(currentGroupName); // preview old key
//            builder.setView(fieldGroupName);
//            builder.setPositiveButton("Change", (dialogInterface, i) -> {
//                final String newGroupName = fieldGroupName.getText().toString();
//                if (TextUtils.isEmpty(newGroupName)) Toast.makeText(GroupChatActivity.this, "This field cannot be empty", Toast.LENGTH_SHORT).show();
//                else {
//                    GroupNameRef.child("Settings").child("key").setValue(newGroupName).addOnCompleteListener(task -> {
//                        if (task.isSuccessful()) {
//                            snow = userKey;
//                            Intent chatIntent = new Intent(GroupChatActivity.this, GroupChatActivity.class); //renew view item
//                            chatIntent.putExtra("groupName" , currentGroupName);
//                            startActivity(chatIntent);
//                        }
//                    });
//                }
//            });
//            builder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.cancel());
//            builder.show();
//        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RECOGNIZER_VOICE_RESULT) { // for voice to text
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                userMessageInput.setText(Objects.requireNonNull(result).get(0));
            }
        }
    }

    private void renewSecurityKey() {
        if (!currentUserID.equals(adminGroupID)) Toast.makeText(GroupChatActivity.this, "This function is available only to the administrator!!!", Toast.LENGTH_SHORT).show();
        else {
            AlertDialog.Builder builder = new AlertDialog.Builder(GroupChatActivity.this,R.style.AlertDialog).setIcon(android.R.drawable.ic_menu_edit)
                    .setTitle("Change personal key for this group").setMessage("This key is valid in the current group chat");
            final EditText groupKeyField = new EditText(GroupChatActivity.this); //enter text
            groupKeyField.setText(snow); // preview old key
            builder.setView(groupKeyField);
            builder.setPositiveButton("Change", (dialogInterface, i) -> {
                final String userKey = groupKeyField.getText().toString();
                if (TextUtils.isEmpty(userKey)) Toast.makeText(GroupChatActivity.this, "Please write key", Toast.LENGTH_SHORT).show();
                if (userKey.length() != 16) Toast.makeText(GroupChatActivity.this, "Incorrect key, key length must be 16 characters.", Toast.LENGTH_SHORT).show();
                else {
                    GroupNameRef.child("Settings").child("key").setValue(userKey).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            snow = userKey;
                            Intent chatIntent = new Intent(GroupChatActivity.this, GroupChatActivity.class); //renew view item
                            chatIntent.putExtra("groupName" , currentGroupName);
                            startActivity(chatIntent);
                        }
                    });
                }
            });
            builder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.cancel());
            builder.show();
        }
    }

    private void viewGroupAdmin() {
                Intent profileIntent = new Intent(GroupChatActivity.this, ProfileActivity.class);
                profileIntent.putExtra("visit_user_id",adminGroupID);
                profileIntent.putExtra("visit_view_admin", "visit_admin");
                startActivity(profileIntent);
    }

    private void getUserInfo() {
        UsersRef.child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) currentUserName = Objects.requireNonNull(dataSnapshot.child("name").getValue()).toString();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    private void RemoveFromGroup() {
        if (!currentUserID.equals(adminGroupID)) {
            new AlertDialog.Builder(GroupChatActivity.this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Get out from group")
                    .setMessage("Are you sure??? Do you really want to leave the group?").setPositiveButton("Exit", (dialog, which) ->
                            GroupNameRef.child("Users").child(currentUserID).removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Intent intent = new Intent(GroupChatActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            })).setNegativeButton("No", null).show();
        } else {
            Toast.makeText(GroupChatActivity.this, "You are the group administrator !!! Before leaving the group, you must appoint one of the group user as the administrator.", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteGroup() {
        if (currentUserID.equals(adminGroupID)) {
            new AlertDialog.Builder(GroupChatActivity.this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("DELETE GROUP!!!")
                    .setMessage("Are you sure??? Do you want to delete group chat and all users?").setPositiveButton("Delete", (dialog, which) -> GroupNameRef.removeValue().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Intent intent = new Intent(GroupChatActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    })).setNegativeButton("No", null).show();
        } else {
            Toast.makeText(GroupChatActivity.this, "   This function is available only to the administrator!!! \n   You do not have permission for this operation!!!", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("SimpleDateFormat")
    private void sendMessageToGroupChat() {
        String message = userMessageInput.getText().toString();
        String messageKEY = GroupNameMessageRef.push().getKey();
        if (TextUtils.isEmpty(message)) Toast.makeText(this, "Please write message first...", Toast.LENGTH_SHORT).show();
        else {
            GroupNameRef.child("Settings").child("key").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    try { new Encryption(message, snow); }
                    catch (Exception e) { Toast.makeText(GroupChatActivity.this, "Error send", Toast.LENGTH_SHORT).show(); }
                    HashMap<String, Object> groupMessageKey = new HashMap<>();
                    GroupNameMessageRef.updateChildren(groupMessageKey);
                    DatabaseReference groupMessageKeyRef = GroupNameMessageRef.child(Objects.requireNonNull(messageKEY));
                    HashMap<String, Object> messageInfoMap = new HashMap<>();
                    messageInfoMap.put("name", currentUserName);
                    messageInfoMap.put("from", currentUserID);
                    messageInfoMap.put("type", "sms");
                    messageInfoMap.put("action", "view");
                    messageInfoMap.put("messageID", messageKEY);
                    messageInfoMap.put("message", Arrays.toString(encryptedBytes));
                    messageInfoMap.put("date", new SimpleDateFormat("dd.MM.yyyy").format(Calendar.getInstance().getTime()));
                    messageInfoMap.put("time", new SimpleDateFormat("HH:mm").format(Calendar.getInstance().getTime()));
                    groupMessageKeyRef.updateChildren(messageInfoMap);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });
        }
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~ Buttons for incoming call ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    public void onClickAcceptIncomingCall(View view) {
        if (sound != null && sound.isPlaying()) sound.stop();
        call.addCallListener(new SinchCallListener());
        if (vibrator.hasVibrator()) vibrator.cancel();
        call.answer();
    }
    public void onClickCancelIncomingCall(View view) {
        if (alertDialogCall.isShowing()) alertDialogCall.dismiss();
        if (sound != null && sound.isPlaying()) sound.stop();
        if (vibrator.hasVibrator()) vibrator.cancel();
        call.hangup();
    }
    public void onClickSpeakingHangUp(View view) {
        call.hangup();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        sinchClient.stopListeningOnActiveConnection(); // Sinch Client STOP Listener This Activity
        Intent intent = new Intent(GroupChatActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
