package com.amdc.firebasetest;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
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
import static com.amdc.firebasetest.MainActivity.userSet;

public class ChatActivity extends AppCompatActivity {
    private String messageReceiverID, messageSenderID, messageReceiverName, messageReceiverImage, saveCurrentTime,
            saveCurrentDate, checker = "", msmID, messageSenderRef, messageReceiverRef, messageCounterOutput;
    private TextView userName, userLastSeen;
    private EditText messageInputText;
    private CircleImageView userImage;
    private DatabaseReference RootRef;
    private ImageButton SendMessageButton, SendFilesButton;
    private final List<Messages> messagesList = new ArrayList<>();
    private ChatAdapter messageAdapter;
    private RecyclerView userMessagesList;
    private ProgressDialog loadingBar;
    private Uri fileUri;
    private int count;
    static boolean keyEnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        messageSenderID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        RootRef = FirebaseDatabase.getInstance().getReference();
        messageReceiverID = (String) Objects.requireNonNull(getIntent().getExtras()).get("visit_user_id");
        messageReceiverName = (String) getIntent().getExtras().get("visit_user_name");
        messageReceiverImage = (String) getIntent().getExtras().get("visit_image");
        InitializeControllers();
        userName.setText(messageReceiverName); // for chat bar
        Picasso.get().load(messageReceiverImage).resize(90, 90).placeholder(R.drawable.profile_image).into(userImage); // for chat bar
        SendMessageButton.setOnClickListener(view -> {
            try { SendMessage();
            } catch (Exception e) { e.printStackTrace(); }
        });
        DisplayLastSeen();
        SendFilesButton.setOnClickListener(view -> {
            CharSequence[] options = new CharSequence[] {"Images", "PDF Files", "Excel Files", "MS Word Files", "Zip Type Files", "Exit"}; // list dialog-menu
            AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
            builder.setTitle("Select File"); // title dialog-menu
            builder.setIcon(R.drawable.send_files); //icon dialog-menu
            builder.setItems(options, (dialogInterface, i) -> {
                if(i == 0) { checker = "image";
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    startActivityForResult(Intent.createChooser(intent,"Select Image"),443); //443
                }
                if(i == 1) { checker = "pdf";
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("application/pdf");
                    startActivityForResult(Intent.createChooser(intent,"Select PDF"),443);
                }
                if(i == 2) { checker = "xls";
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    final String[] mineTypes = {"application/vnd.ms-excel","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"}; // filter xml files
                    intent.setType("*/*");
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, mineTypes);
                    startActivityForResult(Intent.createChooser(intent,"Select Excel Files"),443);
                }
                if(i == 3) { checker = "doc";
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    final String[] mineTypes = {"application/msword","application/vnd.openxmlformats-officedocument.wordprocessingml.document"}; // filter word files
                    intent.setType("*/*");
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, mineTypes);
                    startActivityForResult(Intent.createChooser(intent,"Select Word Files"),443);
                }
                if(i == 4) { checker = "zip";
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("application/zip");
                    startActivityForResult(Intent.createChooser(intent,"Select Zip Files"),443);
                }
            });
            builder.show();
        });
        //~~~~~~~~~~~~~~~~~ read or create new counter messages ~~~~~~~~~~~~~~~~~~~~~~~~~
        RootRef.child("Message notifications").child(messageReceiverID).child(messageSenderID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    count = Integer.parseInt(((String) Objects.requireNonNull(dataSnapshot.child("Counter").getValue())));
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
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ add/delete messages ~~~~~~~~~~~~~~~~~~~~~~~~~
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

    private void checkerViewCrypt(MenuItem item) { // user key used or not used renew activity
        if(item.isChecked()) { item.setChecked(false); keyEnable = false; }
        else { item.setChecked(true); keyEnable = true; }
        Intent chatIntent = new Intent(ChatActivity.this, ChatActivity.class); //renew view item
        chatIntent.putExtra("visit_user_id", messageReceiverID);
        chatIntent.putExtra("visit_user_name", messageReceiverName);
        chatIntent.putExtra("visit_image", messageReceiverImage);
        startActivity(chatIntent);
    }

    @Override
    public void onBackPressed() {
        final String messageCounterInput = "Message notifications/" + messageSenderID + "/" + messageReceiverID; // counter input messages to zero
        final Map<String, String> messageCounter = new HashMap<>(); //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        messageCounter.put("Counter", 0 + ""); //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        final Map<String, Object> messageBodyCounter = new HashMap<>(); //~~~~~~~~~~~~~~~~~~~~~~~~~~
        messageBodyCounter.put(messageCounterInput, messageCounter); //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        RootRef.updateChildren(messageBodyCounter); // update counter messages
        Intent intent = new Intent(ChatActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
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
        SendMessageButton = findViewById(R.id.send_message_btn);
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
        if(requestCode == 443 && resultCode == RESULT_OK && data != null && data.getData() != null) { //443, 438
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
                    messageCounter.put("Counter", (count + 1) + ""); //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ add one for counter messages
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
                    messageCounter.put("Counter", (count + 1) + ""); //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ add one for counter messages
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
            messageCounter.put("Counter", (count + 1) + ""); //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ add one for counter messages
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
}
