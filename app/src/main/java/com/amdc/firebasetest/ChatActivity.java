package com.amdc.firebasetest;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
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
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.amdc.firebasetest.Decryption.decryptedSMS;
import static com.amdc.firebasetest.Encryption.encryptedBytes;
import static com.amdc.firebasetest.MainActivity.call;
import static com.amdc.firebasetest.MainActivity.displayOFF;
import static com.amdc.firebasetest.MainActivity.sinchClient;
import static com.amdc.firebasetest.MainActivity.sound;
import static com.amdc.firebasetest.MainActivity.userSet;
import static com.amdc.firebasetest.MainActivity.vibrator;

public class ChatActivity extends AppCompatActivity {
    private String messageReceiverID, messageSenderID, messageReceiverName, messageReceiverImage, saveCurrentTime,
            saveCurrentDate, checker = "", msmID, messageSenderRef, messageReceiverRef, messageCounterOutput, incomingCallUser = "Unknown";
    private final int RECOGNIZER_FILE_RESULT = 443, RECOGNIZER_VOICE_RESULT = 1;
    private TextView userName, userLastSeen, incomingUserName;
    private EditText messageInputText;
    private CircleImageView userImage, incomingUserImage;
    private LinearLayout view;
    private DatabaseReference RootRef;
    private Button btnCall;
    private ImageButton sendMessageButton, SendFilesButton;
    private final List<Messages> messagesList = new ArrayList<>();
    private ChatAdapter messageAdapter;
    private RecyclerView userMessagesList;
    private ProgressDialog loadingBar;
    private AlertDialog alertDialogCall;
    private Uri fileUri;
    private int counterMessages;
    static boolean keyEnable, btnCalling = false;
    final String[] retImage = {"default_image"};

    @SuppressLint({"SetTextI18n", "InflateParams"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        messageSenderID = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        RootRef = FirebaseDatabase.getInstance().getReference();
        messageReceiverID = (String) Objects.requireNonNull(getIntent().getExtras()).get("visit_user_id");
        messageReceiverName = (String) getIntent().getExtras().get("visit_user_name");
        messageReceiverImage = (String) getIntent().getExtras().get("visit_image");
        InitializeControllers();
        userName.setText(messageReceiverName); // for chat bar
        try { Picasso.get().load(messageReceiverImage).resize(47, 47).placeholder(R.drawable.profile_image).into(userImage); // for chat bar
        } catch (Exception e) { Toast.makeText(this, "Error download User-Image", Toast.LENGTH_SHORT).show(); }
        DisplayLastSeen();

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Voice Incoming Call ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        sinchClient.getCallClient().addCallClientListener((callClient, incomingCall) -> {
            call = incomingCall;
            RootRef.child("Users").child(incomingCall.getRemoteUserId()).addValueEventListener(new ValueEventListener() {
                @Override // search Name incoming user
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        incomingCallUser = (String) dataSnapshot.child("name").getValue();
                        if (dataSnapshot.hasChild("image")) retImage[0] = (String) dataSnapshot.child("image").getValue();
                    }
                    if (call == incomingCall) {
                        view = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_incoming_call, null);
                        view.findViewById(R.id.incoming_accept_btn).setVisibility(View.VISIBLE);
                        view.findViewById(R.id.incoming_cancel_btn).setVisibility(View.VISIBLE);
                        alertDialogCall = new AlertDialog.Builder(ChatActivity.this).setCancelable(false).setView(view).create();
                        incomingUserName = view.findViewById(R.id.incoming_user_name);
                        incomingUserName.setText(incomingCallUser + " is calling");
                        incomingUserName.setTextColor(Color.BLUE);
                        incomingUserImage = view.findViewById(R.id.incoming_user_image);
                        try { Picasso.get().load(retImage[0]).resize(90, 90).placeholder(R.drawable.profile_image).into(incomingUserImage); // for chat bar
                        } catch (Exception e) { Toast.makeText(ChatActivity.this, "Error download User-Image", Toast.LENGTH_SHORT).show(); }
//                        view.findViewById(R.id.incoming_accept_btn).setOnClickListener(v -> {
//                            if (sound != null && sound.isPlaying()) sound.stop();
//                            if (vibrator.hasVibrator()) vibrator.cancel();
//                            call.answer();
//                            call.addCallListener(new SinchCallListener());
//                        });
//                        view.findViewById(R.id.incoming_cancel_btn).setOnClickListener(v -> {
//                            if (alertDialogCall.isShowing()) alertDialogCall.dismiss();
//                            if (sound != null && sound.isPlaying()) sound.stop();
//                            if (vibrator.hasVibrator()) vibrator.cancel();
//                            call.hangup();
//                            call.addCallListener(new SinchCallListener());
//                            call = null;
//                        });
                        if (alertDialogCall != null && !alertDialogCall.isShowing()) alertDialogCall.show();
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) { }
            });
        });
        sinchClient.startListeningOnActiveConnection();

        //~~~~~~~~~~~~~~~~~~~~ Button for send messages ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        sendMessageButton.setOnClickListener(view -> { //listener when push shot for send messages
            try { SendMessage(); } catch (Exception e) { e.printStackTrace(); }
        });
        sendMessageButton.setOnLongClickListener(v -> { //listener when push long for voice to text
            Intent speakIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speakIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speakIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            speakIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hi Speak Something");
            startActivityForResult(speakIntent, RECOGNIZER_VOICE_RESULT);
            return false;
        });

        //~~~~~~~~~~~~~~~~~~~~ Button for Voice Calling ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        btnCall.setOnClickListener(v -> { // Voice Caller
            if (call == null) {
                btnCalling = true;
                RootRef.child("Users").child(messageReceiverID).addValueEventListener(new ValueEventListener() {
                    @SuppressLint({"InflateParams", "SetTextI18n"})
                    @Override // search Name incoming user
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            incomingCallUser = (String) dataSnapshot.child("name").getValue();
                            if (dataSnapshot.hasChild("image")) retImage[0] = (String) dataSnapshot.child("image").getValue();
                        }
                        if (btnCalling) {
                            view = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_incoming_call, null);
                            alertDialogCall = new AlertDialog.Builder(ChatActivity.this).setCancelable(false).setView(view).create();
                            incomingUserName = view.findViewById(R.id.incoming_user_name);
                            incomingUserImage = view.findViewById(R.id.incoming_user_image);
                            view.findViewById(R.id.speaking_cancel_btn).setVisibility(View.VISIBLE);
                            incomingUserName.setText("Call to " + incomingCallUser);
                            incomingUserName.setTextColor(Color.GREEN);
                            try { Picasso.get().load(retImage[0]).resize(90, 90).placeholder(R.drawable.profile_image).into(incomingUserImage); // for chat bar
                            } catch (Exception e) { Toast.makeText(ChatActivity.this, "Error download User-Image", Toast.LENGTH_SHORT).show(); }
                            if (!alertDialogCall.isShowing()) alertDialogCall.show();
                            call = sinchClient.getCallClient().callUser(messageReceiverID);
                            call.addCallListener(new SinchCallListener());
//                            view.findViewById(R.id.speaking_cancel_btn).setOnClickListener(v1 -> {
//                                if (alertDialogCall.isShowing()) alertDialogCall.dismiss();
//                                if (call != null) {
//                                    call.hangup();
//                                    call.addCallListener(new SinchCallListener());
//                                }
//                                call = null;
//                                btnCalling = false;
//                            });
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) { }
                });
            }
        });

        //~~~~~~~~~~~~~~~~~~~~~~~~~ Button Send File ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        SendFilesButton.setOnClickListener(view -> {
            CharSequence[] options = new CharSequence[] {"Images", "PDF Files", "Excel Files", "MS Word Files", "Zip Type Files", "Exit"}; // list dialog-menu
            AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
            builder.setTitle("Select File"); // title dialog-menu
            builder.setIcon(R.drawable.send_files); //icon dialog-menu
            builder.setItems(options, (dialogInterface, i) -> {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                if(i == 0) { checker = "image";
                    intent.setType("image/*");
                    startActivityForResult(Intent.createChooser(intent,"Select Image"),RECOGNIZER_FILE_RESULT);
                }
                if(i == 1) { checker = "pdf";
                    intent.setType("application/pdf");
                    startActivityForResult(Intent.createChooser(intent,"Select PDF"),RECOGNIZER_FILE_RESULT);
                }
                if(i == 2) { checker = "xls";
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    final String[] mineTypes = {"application/vnd.ms-excel","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"}; // filter xml files
                    intent.setType("*/*");
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, mineTypes);
                    startActivityForResult(Intent.createChooser(intent,"Select Excel Files"),RECOGNIZER_FILE_RESULT);
                }
                if(i == 3) { checker = "doc";
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    final String[] mineTypes = {"application/msword","application/vnd.openxmlformats-officedocument.wordprocessingml.document"}; // filter word files
                    intent.setType("*/*");
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, mineTypes);
                    startActivityForResult(Intent.createChooser(intent,"Select Word Files"),RECOGNIZER_FILE_RESULT);
                }
                if(i == 4) { checker = "zip";
                    intent.setType("application/zip");
                    startActivityForResult(Intent.createChooser(intent,"Select Zip Files"),RECOGNIZER_FILE_RESULT);
                }
            });
            builder.show();
        });
        //~~~~~~~~~~~~~~~~~ read or create new counter messages ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        RootRef.child("Message notifications").child(messageReceiverID).child(messageSenderID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    counterMessages = Integer.parseInt(((String) Objects.requireNonNull(dataSnapshot.child("Counter").getValue())));
                } else {
                    String messageCounterRef = "Message notifications/" + messageReceiverID + "/" + messageSenderID;
                    Map<String, String> messageCounter = new HashMap<>();
                    messageCounter.put("Counter", 0 + "");
                    Map<String, Object> messageBodyDetails = new HashMap<>();
                    messageBodyDetails.put(messageCounterRef, messageCounter);
                    RootRef.updateChildren(messageBodyDetails);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

        //~~~~~~~~~~~~~~~~~~~~ add/delete messages from firebase into messagesList ~~~~~~~~~~~~~~~~~
        RootRef.child("Messages").child(messageSenderID).child(messageReceiverID).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s) {
                Messages messages = dataSnapshot.getValue(Messages.class);
                if (!keyEnable && Objects.requireNonNull(messages).getType().equals("text")) { // security key flag
                    try { new Decryption(Objects.requireNonNull(messages).getMessage(), userSet); }
                    catch (Exception e) { Toast.makeText(ChatActivity.this, "Error decrypt", Toast.LENGTH_SHORT).show(); }
                    Objects.requireNonNull(messages).setMessage(decryptedSMS); // set decrypted message
                }
                messagesList.add(messages);
                messageAdapter.notifyDataSetChanged();
                userMessagesList.smoothScrollToPosition(Objects.requireNonNull(userMessagesList.getAdapter()).getItemCount());
            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, String s) { }
            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) { //when message was deleted
                msmID = Objects.requireNonNull(dataSnapshot.getValue(Messages.class)).getMessageID(); // ID deleted message
                for (int i = 0; i < messagesList.size(); i++) { //search deleted message from message list
                    if (msmID.equals(messagesList.get(i).getMessageID())) messagesList.remove(i);
                }
                    messageAdapter.notifyDataSetChanged();
                    userMessagesList.smoothScrollToPosition(Objects.requireNonNull(userMessagesList.getAdapter()).getItemCount()); // scroll to end
            }
            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, String s) { }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Listener Voice Call ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private class SinchCallListener implements CallListener {
        @Override
        public void onCallProgressing(com.sinch.android.rtc.calling.Call call) {
            Toast.makeText(getApplicationContext(), "Ringing...", Toast.LENGTH_SHORT).show();
            sound = MediaPlayer.create(ChatActivity.this, R.raw.beep);
            sound.setLooping(true);
            sound.start();
        }
        @SuppressLint({"SetTextI18n", "InflateParams"})
        @Override
        public void onCallEstablished(com.sinch.android.rtc.calling.Call speakCall) { btnCalling = false;
            if (sound != null && sound.isPlaying()) sound.stop();
            displayOFF = true;
            if (alertDialogCall.isShowing()) alertDialogCall.dismiss();
            view = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_incoming_call, null);
            alertDialogCall = new AlertDialog.Builder(ChatActivity.this).setCancelable(false).setView(view).create();
            incomingUserName = view.findViewById(R.id.incoming_user_name);
            incomingUserImage = view.findViewById(R.id.incoming_user_image);
            view.findViewById(R.id.speaking_cancel_btn).setVisibility(View.VISIBLE);
            incomingUserName.setText(incomingCallUser + " online");
            incomingUserName.setTextColor(Color.RED);
            try { Picasso.get().load(retImage[0]).resize(90, 90).placeholder(R.drawable.profile_image).into(incomingUserImage); // for chat bar
            } catch (Exception e) { Toast.makeText(ChatActivity.this, "Error download User-Image", Toast.LENGTH_SHORT).show(); }
            call = speakCall;
//            view.findViewById(R.id.speaking_cancel_btn).setOnClickListener(v -> {
//                if (alertDialogCall.isShowing()) alertDialogCall.dismiss();
//                btnCalling = false;
//                call = speakCall;
//                call.hangup();
//                call = null;
//            });
            if (!alertDialogCall.isShowing()) alertDialogCall.show();
        }
        @Override
        public void onCallEnded(com.sinch.android.rtc.calling.Call endedCall) { btnCalling = false;
            Toast.makeText(getApplicationContext(), "Call Ended", Toast.LENGTH_SHORT).show();
            if (alertDialogCall != null && alertDialogCall.isShowing()) alertDialogCall.dismiss();
            if (sound != null && sound.isPlaying()) sound.stop();
            call = endedCall;
            call.hangup();
            call = null;
            sound = MediaPlayer.create(ChatActivity.this, R.raw.telephone_busy);
            sound.start();
            displayOFF = false;
        }
        @Override
        public void onShouldSendPushNotification(com.sinch.android.rtc.calling.Call call, List<PushPair> list) { }
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Option Menu ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        if (keyEnable) menu.findItem(R.id.view_without_security_key).setChecked(true); // change checker
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) { // item menu
        super.onOptionsItemSelected(item);
        if(item.getItemId() == R.id.view_without_security_key) checkerViewCrypt(item); // view crypt
        if(item.getItemId() == R.id.create_new_security_key) createNewSecurityKey(); // new key
//        if (item.getItemId() == R.id.chat_settings_option);
        return true;
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~ Create New Security Key ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @SuppressLint("SetTextI18n")
    private void createNewSecurityKey() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.AlertDialog).setIcon(android.R.drawable.ic_menu_edit)
                .setTitle("Create new personal key").setMessage("This key is valid in the current chat, the recipient must enter the same key before receiving messages.");
        final EditText groupNameField = new EditText(ChatActivity.this);
        groupNameField.setText(userSet);
        builder.setView(groupNameField);
        builder.setPositiveButton("Create", (dialogInterface, i) -> {
            String userKey = groupNameField.getText().toString();
            if (TextUtils.isEmpty(userKey)) Toast.makeText(this, "Please write key", Toast.LENGTH_SHORT).show();
            if (userKey.length() != 16) Toast.makeText(this, "Incorrect key, key length must be 16 characters.", Toast.LENGTH_SHORT).show();
            else {
                userSet = userKey;
                Intent chatIntent = new Intent(ChatActivity.this, ChatActivity.class); //renew view item
                chatIntent.putExtra("visit_user_id", messageReceiverID);
                chatIntent.putExtra("visit_user_name", messageReceiverName);
                chatIntent.putExtra("visit_image", messageReceiverImage);
                startActivity(chatIntent);
            }
        });
        builder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.cancel());
        builder.show();
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~ Check View With New Key ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private void checkerViewCrypt(MenuItem item) { // user key used or not used renew activity
        if(item.isChecked()) { item.setChecked(false); keyEnable = false; }
        else { item.setChecked(true); keyEnable = true; }
        Intent chatIntent = new Intent(ChatActivity.this, ChatActivity.class); //renew view item
        chatIntent.putExtra("visit_user_id", messageReceiverID);
        chatIntent.putExtra("visit_user_name", messageReceiverName);
        chatIntent.putExtra("visit_image", messageReceiverImage);
        startActivity(chatIntent);
    }

    @SuppressLint({"RestrictedApi", "SimpleDateFormat"})
    private void InitializeControllers() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); //full screen
        setSupportActionBar(findViewById(R.id.chat_toolbar)); // my toolbar
        Objects.requireNonNull(getSupportActionBar()).setDefaultDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams")
        View actionBarView = Objects.requireNonNull(layoutInflater).inflate(R.layout.custom_chat_bar,null);
        getSupportActionBar().setCustomView(actionBarView);
        loadingBar = new ProgressDialog(this);
        userName = findViewById(R.id.custom_profile_name);
        userImage = findViewById(R.id.custom_profile_image);
        userLastSeen = findViewById(R.id.custom_user_last_seen);
        btnCall = findViewById(R.id.btn_outgoing_calling);
        sendMessageButton = findViewById(R.id.send_message_btn);
        SendFilesButton = findViewById(R.id.send_files_btn);
        messageInputText = findViewById(R.id.input_message);
        messageAdapter = new ChatAdapter(messagesList);
        userMessagesList = findViewById(R.id.private_messages_list_of_users);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true); //  item into list gravity from end to up
        userMessagesList.setLayoutManager(linearLayoutManager);
        userMessagesList.setAdapter(messageAdapter);
        messageSenderRef = "Messages/" + messageSenderID + "/" + messageReceiverID;
        messageReceiverRef = "Messages/" + messageReceiverID + "/" + messageSenderID;
        messageCounterOutput = "Message notifications/" + messageReceiverID + "/" + messageSenderID; // counter messages
        saveCurrentDate = new SimpleDateFormat("dd.MMM.yyyy", Locale.US).format(Calendar.getInstance().getTime());
        saveCurrentTime = new SimpleDateFormat("HH:mm").format(Calendar.getInstance().getTime());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RECOGNIZER_VOICE_RESULT) { // for voice to text
            if (resultCode == RESULT_OK && data != null) { //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                messageInputText.setText(Objects.requireNonNull(result).get(0));
            }
        }
        if(requestCode == RECOGNIZER_FILE_RESULT && resultCode == RESULT_OK && data != null && data.getData() != null) { //443, 438
            loadingBar.setTitle("Sending File");
            loadingBar.setMessage("Please wait, sending...");
            loadingBar.setCanceledOnTouchOutside(false);
            loadingBar.show();
            fileUri = data.getData();
            if(!checker.equals("image")) { //  choice file not image for send
                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Document Files");
                DatabaseReference userMessageKeyRef = RootRef.child("Messages").child(messageSenderID).child(messageReceiverID).push();
                final String messagePushID = userMessageKeyRef.getKey();
                final StorageReference filePath = storageReference.child(messagePushID + "." + checker);
                filePath.putFile(fileUri).addOnSuccessListener(taskSnapshot -> filePath.getDownloadUrl().addOnSuccessListener(uri -> {
                    Map<String, Object> messageImageBody = new HashMap<>();
                    messageImageBody.put("message", uri.getPath());
                    messageImageBody.put("name", fileUri.getLastPathSegment()); // toString
                    messageImageBody.put("type", checker);
                    messageImageBody.put("from", messageSenderID);
                    messageImageBody.put("to", messageReceiverID);
                    messageImageBody.put("messageID", messagePushID);
                    messageImageBody.put("time", saveCurrentTime);
                    messageImageBody.put("date", saveCurrentDate);
                    Map<String, Object> messageBodyDetail = new HashMap<>();
                    messageBodyDetail.put(messageSenderRef + "/" + messagePushID, messageImageBody);
                    messageBodyDetail.put(messageReceiverRef + "/" + messagePushID, messageImageBody);
                    RootRef.updateChildren(messageBodyDetail);
                    loadingBar.dismiss();
                }).addOnFailureListener(e -> {
                    loadingBar.dismiss();
                    Toast.makeText(ChatActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                })).addOnProgressListener(taskSnapshot -> {
                    double p = (100.0 * taskSnapshot.getBytesTransferred())/taskSnapshot.getTotalByteCount();
                    loadingBar.setMessage((int) p + "% Uploading...");
                }).addOnCompleteListener(task -> {
                    Map<String, String> messageCounter = new HashMap<>(); //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ add one for counter messages
                    messageCounter.put("Counter", (counterMessages + 1) + ""); //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ add one for counter messages
                    Map<String, Object> messageBodyCounter = new HashMap<>(); //~~~~~~~~~~~~~~~~~~~~~~~~~~~~ add one for counter messages
                    messageBodyCounter.put(messageCounterOutput, messageCounter); //~~~~~~~~~~~~~~~~~~~~~~~~~~~ add one for counter messages
                    RootRef.updateChildren(messageBodyCounter); // ~~~~~~~~ update counter messages ~~~~~~~~ add one for counter messages
                });
            } else { //  choice file image for send
                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Image Files");
                DatabaseReference userMessageKeyRef = RootRef.child("Messages").child(messageSenderID).child(messageReceiverID).push();
                final String messagePushID = userMessageKeyRef.getKey();
                final StorageReference filePath = storageReference.child(messagePushID + "." + "jpg");
                filePath.putFile(fileUri).addOnSuccessListener(taskSnapshot -> filePath.getDownloadUrl().addOnSuccessListener(uri -> {
                    Map<String, String> messageTextBody = new HashMap<>();
                    messageTextBody.put("message", uri.toString());
                    messageTextBody.put("name", fileUri.toString());
                    messageTextBody.put("type", checker);
                    messageTextBody.put("from", messageSenderID);
                    messageTextBody.put("to", messageReceiverID);
                    messageTextBody.put("messageID", messagePushID);
                    messageTextBody.put("time", saveCurrentTime);
                    messageTextBody.put("date", saveCurrentDate);
                    Map<String, Object> messageBodyDetails = new HashMap<>();
                    messageBodyDetails.put(messageSenderRef + "/" + messagePushID, messageTextBody);
                    messageBodyDetails.put(messageReceiverRef + "/" + messagePushID, messageTextBody);
                    RootRef.updateChildren(messageBodyDetails).addOnCompleteListener(task1 -> {
                        if (!task1.isSuccessful()) Toast.makeText(ChatActivity.this, "Send Image Error", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                        messageInputText.setText("");
                    });
                })).addOnProgressListener(taskSnapshot -> {
                    double p = (100.0 * taskSnapshot.getBytesTransferred())/taskSnapshot.getTotalByteCount();
                    loadingBar.setMessage((int) p + "% Uploading...");
                }).addOnCompleteListener(task -> {
                    Map<String, String> messageCounter = new HashMap<>(); //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ add one for counter messages
                    messageCounter.put("Counter", (counterMessages + 1) + ""); //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ add one for counter messages
                    Map<String, Object> messageBodyCounter = new HashMap<>(); //~~~~~~~~~~~~~~~~~~~~~~~~~~~~ add one for counter messages
                    messageBodyCounter.put(messageCounterOutput, messageCounter); //~~~~~~~~~~~~~~~~~~~~~~~~~~~ add one for counter messages
                    RootRef.updateChildren(messageBodyCounter); // ~~~~~~~~ update counter messages ~~~~~~~~ add one for counter messages
                });
            }
        }
    }

    private void DisplayLastSeen() { // status for chat bar
        RootRef.child("Users").child(messageReceiverID).addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child("userState").hasChild("state")) {
                    String state = (String) dataSnapshot.child("userState").child("state").getValue();
                    String date = (String) dataSnapshot.child("userState").child("date").getValue();
                    String time = (String) dataSnapshot.child("userState").child("time").getValue();
                    assert state != null;
                    if (state.equals("online"))  userLastSeen.setText("online");
                    else if (state.equals("offline"))  userLastSeen.setText("Last Seen: " + time + " - " + date);
                } else userLastSeen.setText("offline");
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    private void SendMessage() throws Exception {
        String messageText = messageInputText.getText().toString();
        if (TextUtils.isEmpty(messageText)) Toast.makeText(this, "first write your message...", Toast.LENGTH_SHORT).show();
        else { new Encryption(messageText, userSet);
            Map<String, String> messageCounter = new HashMap<>(); //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ add one for counter messages
            messageCounter.put("Counter", (counterMessages + 1) + ""); //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ add one for counter messages
            Map<String, Object> messageBodyCounter = new HashMap<>(); //~~~~~~~~~~~~~~~~~~~~~~~~~~~~ add one for counter messages
            messageBodyCounter.put(messageCounterOutput, messageCounter); //~~~~~~~~~~~~~~~~~~~~~~~~~~~ add one for counter messages
            RootRef.updateChildren(messageBodyCounter); // ~~~~~~~~ update counter messages ~~~~~~~~ add one for counter messages
            DatabaseReference userMessageKeyRef = RootRef.child("Messages").child(messageSenderID).child(messageReceiverID).push();
            String messagePushID = userMessageKeyRef.getKey();
            Map<String, String> messageTextBody = new HashMap<>();
            messageTextBody.put("message", Arrays.toString(encryptedBytes)); // crypt text for send to firebase
            messageTextBody.put("type", "text");
            messageTextBody.put("from", messageSenderID);
            messageTextBody.put("to", messageReceiverID);
            messageTextBody.put("messageID", messagePushID);
            messageTextBody.put("time", saveCurrentTime);
            messageTextBody.put("date", saveCurrentDate);
            Map<String, Object> messageBodyDetails = new HashMap<>();
            messageBodyDetails.put(messageSenderRef + "/" + messagePushID, messageTextBody);
            messageBodyDetails.put(messageReceiverRef + "/" + messagePushID, messageTextBody);
            RootRef.updateChildren(messageBodyDetails).addOnCompleteListener(task -> {
                if (!task.isSuccessful()) Toast.makeText(ChatActivity.this, "Send Message Error", Toast.LENGTH_SHORT).show();
                messageInputText.setText("");
            });
        }
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~ Buttons Speaking Call ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
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
        btnCalling = false;
        call = null;
    }

    public void btnCancelSpeaking(View view) {
        if (alertDialogCall != null && alertDialogCall.isShowing()) alertDialogCall.dismiss();
        if (call != null) {
            call.hangup();
            call.addCallListener(new SinchCallListener());
        }
        btnCalling = false;
        call = null;
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~ Press Button Back ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @Override
    public void onBackPressed() {
        final String messageCounterInput = "Message notifications/" + messageSenderID + "/" + messageReceiverID; // counter input messages to zero
        final Map<String, String> messageCounter = new HashMap<>(); //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        messageCounter.put("Counter", 0 + ""); //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        final Map<String, Object> messageBodyCounter = new HashMap<>(); //~~~~~~~~~~~~~~~~~~~~~~~~~~
        messageBodyCounter.put(messageCounterInput, messageCounter); //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        RootRef.updateChildren(messageBodyCounter); // update counter messages
        sinchClient.stopListeningOnActiveConnection(); //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~Sinch Client STOP Listener This
        Intent intent = new Intent(ChatActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
