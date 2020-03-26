package com.amdc.firebasetest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {
    private String messageReceiverID, messageSenderID, messageReceiverName, messageReceiverImage;
    private TextView userName, userLastSeen;
    private CircleImageView userImage;

//    private Toolbar ChatToolBar;
    private FirebaseAuth mAuth;
    private DatabaseReference RootRef;
    private ImageButton SendMessageButton, SendFilesButton;
    private EditText MessageInputText;
    private final List<Messages> messagesList = new ArrayList<>();
    private LinearLayoutManager linearLayoutManager;
    private MessageAdapter messageAdapter;
    private RecyclerView userMessagesList;
    private ProgressDialog loadingBar;
    private String saveCurrentTime, saveCurrentDate;
    private String checker = "", myUrl = "";
    private StorageTask uploadTask;
    private Uri fileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null)  messageSenderID = mAuth.getCurrentUser().getUid();
        RootRef = FirebaseDatabase.getInstance().getReference();
        messageReceiverID = Objects.requireNonNull(Objects.requireNonNull(getIntent().getExtras()).get("visit_user_id")).toString();
        messageReceiverName = Objects.requireNonNull(getIntent().getExtras().get("visit_user_name")).toString();
        messageReceiverImage = Objects.requireNonNull(getIntent().getExtras().get("visit_image")).toString();
        InitializeControllers();
        userName.setText(messageReceiverName);
        Picasso.get().load(messageReceiverImage).placeholder(R.drawable.profile_image).into(userImage);
        SendMessageButton.setOnClickListener(view -> SendMessage());
        DisplayLastSeen();
        SendFilesButton.setOnClickListener(view -> {
            CharSequence[] options = new CharSequence[] {"Images", "PDF Files", "MS Word Files"};
            AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
            builder.setTitle("Select File");
            builder.setIcon(R.drawable.send_files);
            builder.setItems(options, (dialogInterface, i) -> {
                if(i == 0) {
                    checker = "image";
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    startActivityForResult(Intent.createChooser(intent,"Select Image"),443); //443
                }
                if(i == 1) {
                    checker = "pdf";
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    intent.setType("application/pdf");
                    startActivityForResult(Intent.createChooser(intent,"Select PDF"),443);
                }
                if(i == 2) {
                    checker = "docx";
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    intent.setType("application/msword");
                    startActivityForResult(Intent.createChooser(intent,"Select MSWORD FILE"),443);
                }
            });
            builder.show();
        });
        RootRef.child("Messages").child(messageSenderID).child(messageReceiverID).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s) {
                Messages messages = dataSnapshot.getValue(Messages.class);
                messagesList.add(messages);
                messageAdapter.notifyDataSetChanged();
                userMessagesList.smoothScrollToPosition(Objects.requireNonNull(userMessagesList.getAdapter()).getItemCount());
            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, String s) { }
            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) { }
            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, String s) { }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    @SuppressLint("RestrictedApi")
    private void InitializeControllers() {
//        ChatToolBar = findViewById(R.id.chat_toolbar);
//        setSupportActionBar(ChatToolBar);
//        ActionBar actionBar = getSupportActionBar();
//        assert actionBar != null;
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
        MessageInputText = findViewById(R.id.input_message);
        messageAdapter = new MessageAdapter(messagesList);
        userMessagesList = findViewById(R.id.private_messages_list_of_users);
        linearLayoutManager = new LinearLayoutManager(this);
        userMessagesList.setLayoutManager(linearLayoutManager);
        userMessagesList.setAdapter(messageAdapter);

        Calendar calendar = Calendar.getInstance();
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat currentDate = new SimpleDateFormat("dd/MMM/yyyy", Locale.US);
        saveCurrentDate = currentDate.format(calendar.getTime());
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat currentTime = new SimpleDateFormat("hh:mm");
        saveCurrentTime = currentTime.format(calendar.getTime());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 443 && resultCode == RESULT_OK && data != null && data.getData() != null) { //443, 438
            loadingBar.setTitle("Sending File");
            loadingBar.setMessage("Please wait, we are sending...");
            loadingBar.setCanceledOnTouchOutside(false);
            loadingBar.show();
            fileUri = data.getData();
            if(!checker.equals("image")) { // if choice file not image
                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Document Files");
                final String messageSenderRef = "Messages/" + messageSenderID + "/" + messageReceiverID;
                final String messageReceiverRef = "Messages/" + messageReceiverID + "/" + messageSenderID;
                DatabaseReference userMessageKeyRef = RootRef.child("Messages").child(messageSenderID).child(messageReceiverID).push();
                final String messagePushID = userMessageKeyRef.getKey();
                final StorageReference filePath = storageReference.child(messagePushID + "." + checker);
                filePath.putFile(fileUri).addOnSuccessListener(taskSnapshot ->
                        filePath.getDownloadUrl().addOnSuccessListener(uri -> {
                    Map<String, Object> messageImageBody = new HashMap<>();
                    messageImageBody.put("message", uri.getPath());
                    messageImageBody.put("name",fileUri.getLastPathSegment());
                    messageImageBody.put("type", checker);
                    messageImageBody.put("from", messageSenderID);
                    messageImageBody.put("to", messageReceiverID);
                    messageImageBody.put("messageID", messagePushID);
                    messageImageBody.put("time", saveCurrentTime);
                    messageImageBody.put("date", saveCurrentDate);

                    Map<String, Object> messageBodyDetail = new HashMap<>();
                    messageBodyDetail.put(messageSenderRef+ "/" + messagePushID, messageImageBody);
                    messageBodyDetail.put(messageReceiverRef+ "/" + messagePushID, messageImageBody);

                    RootRef.updateChildren(messageBodyDetail);
                    loadingBar.dismiss();
                }).addOnFailureListener(e -> {
                    loadingBar.dismiss();
                    Toast.makeText(ChatActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                })).addOnProgressListener(taskSnapshot -> {
                    double p = (100.0 * taskSnapshot.getBytesTransferred())/taskSnapshot.getTotalByteCount();
                    loadingBar.setMessage((int) p + "% Uploading...");
                });
//                filePath.putFile(fileUri).addOnCompleteListener(task -> {
//                    if(task.isSuccessful()) {
//
//                        Map<String, Object> messageTextBody = new HashMap<>();
//                        messageTextBody.put("message", task.getResult().getMetadata().getReference().getDownloadUrl().toString());  //task.getResult().getStorage().getDownloadUrl().toString()); //myUrl); //
//                        messageTextBody.put("name",fileUri.getLastPathSegment());
//
////                        if(checker.equals("pdf"))  messageTextBody.put("type", checker);
////                        else { messageTextBody.put("type", checker);
////                        }
//                        messageTextBody.put("type", checker);
//                        messageTextBody.put("from", messageSenderID);
//                        messageTextBody.put("to", messageReceiverID);
//                        messageTextBody.put("messageID", messagePushID);
//                        messageTextBody.put("time", saveCurrentTime);
//                        messageTextBody.put("date", saveCurrentDate);
//
//                        Map<String, Object> messageBodyDetails = new HashMap<>();
//                        messageBodyDetails.put(messageSenderRef + "/" + messagePushID, messageTextBody);
//                        messageBodyDetails.put( messageReceiverRef + "/" + messagePushID, messageTextBody);
//
//                        RootRef.updateChildren(messageBodyDetails);
//                        loadingBar.dismiss();
//                    }
//                }).addOnFailureListener(e -> {
//                    loadingBar.dismiss();
//                    Toast.makeText(ChatActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
//                }).addOnProgressListener(taskSnapshot -> {
//                    double p = (100.0 * taskSnapshot.getBytesTransferred())/taskSnapshot.getTotalByteCount();
//                    loadingBar.setMessage((int) p + "% Uploading...");
//                });
            }
            else if(checker.equals("image")) {
                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Image Files");
                final String messageSenderRef = "Messages/" + messageSenderID + "/" + messageReceiverID;
                final String messageReceiverRef = "Messages/" + messageReceiverID + "/" + messageSenderID;
                DatabaseReference userMessageKeyRef = RootRef.child("Messages").child(messageSenderID).child(messageReceiverID).push();
                final String messagePushID = userMessageKeyRef.getKey();
                final StorageReference filePath = storageReference.child(messagePushID + "." + "jpg");

                uploadTask = filePath.putFile(fileUri);
                uploadTask.continueWithTask(task -> {
                    if(!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return filePath.getDownloadUrl();
                }).addOnCompleteListener((OnCompleteListener<Uri>) task -> {
                    if (task.isSuccessful()) {
                        Uri downloadUrl = task.getResult();
                        myUrl = Objects.requireNonNull(downloadUrl).toString();
                        Map<String, String> messageTextBody = new HashMap<>();
                        messageTextBody.put("message", myUrl);
                        messageTextBody.put("name", fileUri.getLastPathSegment());
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
                            if (task1.isSuccessful()) {
                                loadingBar.dismiss(); // ?
                                Toast.makeText(ChatActivity.this, "Message Sent Successfully...", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_SHORT).show();
                            }
                            loadingBar.dismiss();
                            MessageInputText.setText("");
                        });
                    }
                });
            } else {
                loadingBar.dismiss();
                Toast.makeText(this,"Nothing selected, Error..",Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void DisplayLastSeen() {
        RootRef.child("Users").child(messageReceiverID).addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child("userState").hasChild("state")) {
                    String state = Objects.requireNonNull(dataSnapshot.child("userState").child("state").getValue()).toString();
                    String date = Objects.requireNonNull(dataSnapshot.child("userState").child("date").getValue()).toString();
                    String time = Objects.requireNonNull(dataSnapshot.child("userState").child("time").getValue()).toString();
                    if (state.equals("online")) {
                        userLastSeen.setText("online");
                    }
                    else if (state.equals("offline")) {
                        userLastSeen.setText("Last Seen: " + date + " " + time);
                    }
                } else {
                    userLastSeen.setText("offline");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }
   // it is not activity!!!!!!!!!!!!!!!!
//    @Override
//    protected void onStart() {
//        super.onStart();
//        RootRef.child("Messages").child(messageSenderID).child(messageReceiverID).addChildEventListener(new ChildEventListener() {
//            @Override
//            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
//                Messages messages = dataSnapshot.getValue(Messages.class);
//                messagesList.add(messages);
//                messageAdapter.notifyDataSetChanged();
//                userMessagesList.smoothScrollToPosition(Objects.requireNonNull(userMessagesList.getAdapter()).getItemCount());
//            }
//            @Override
//            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }
//            @Override
//            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) { }
//            @Override
//            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) { }
//        });
//    }

    private void SendMessage() {
        String messageText = MessageInputText.getText().toString();
        if (TextUtils.isEmpty(messageText)) {
            Toast.makeText(this, "first write your message...", Toast.LENGTH_SHORT).show();
        } else {
            String messageSenderRef = "Messages/" + messageSenderID + "/" + messageReceiverID;
            String messageReceiverRef = "Messages/" + messageReceiverID + "/" + messageSenderID;
            DatabaseReference userMessageKeyRef = RootRef.child("Messages").child(messageSenderID).child(messageReceiverID).push();

            String messagePushID = userMessageKeyRef.getKey();
            Map<String, String> messageTextBody = new HashMap<>();
            messageTextBody.put("message", messageText);
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
                if (task.isSuccessful()) {
                    Toast.makeText(ChatActivity.this, "Message Sent Successfully...", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_SHORT).show();
                }
                MessageInputText.setText("");
            });
        }
    }
}
