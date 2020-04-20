package com.amdc.firebasetest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class GroupChatActivity extends AppCompatActivity {
    private ImageButton SendMessageButton;
    private EditText userMessageInput;
    private GroupChatAdapter groupChatAdapter;
    private RecyclerView userMessagesList;
    private final List<Messages> messagesList = new ArrayList<>();
    private DatabaseReference UsersRef, GroupNameRef;
    static String currentGroupName;
    private String currentUserID;
    private String currentUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        currentGroupName = Objects.requireNonNull(Objects.requireNonNull(getIntent().getExtras()).get("groupName")).toString();
        currentUserID = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        GroupNameRef = FirebaseDatabase.getInstance().getReference().child("Groups").child(currentGroupName);
        InitializeFields();
        GetUserInfo();
        SendMessageButton.setOnClickListener(view -> {
            SaveMessageInfoToDatabase();
            userMessageInput.setText("");
        });
        GroupNameRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Messages messages = dataSnapshot.getValue(Messages.class);
                messagesList.add(messages);
                groupChatAdapter.notifyDataSetChanged();
                userMessagesList.smoothScrollToPosition(Objects.requireNonNull(userMessagesList.getAdapter()).getItemCount());
            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Toast.makeText(GroupChatActivity.this, "Child will be changed", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                GroupChatActivity.this.startActivity(new Intent(GroupChatActivity.this, GroupChatActivity.class).putExtra("groupName" , currentGroupName));
                Toast.makeText(GroupChatActivity.this, currentUserName +": deleted his message", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    private void InitializeFields() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); //full screen
        setSupportActionBar(findViewById(R.id.group_chat_bar_layout)); // my toolbar
        Objects.requireNonNull(getSupportActionBar()).setTitle(Html.fromHtml("<font color='#F3FB00'>" + "Group chat: " + currentGroupName + "</font>"));
        SendMessageButton = findViewById(R.id.send_message_button);
        userMessageInput = findViewById(R.id.input_group_message);
        groupChatAdapter = new GroupChatAdapter(messagesList);
        userMessagesList = findViewById(R.id.private_messages_list_from_users);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        userMessagesList.setLayoutManager(linearLayoutManager);
        userMessagesList.setAdapter(groupChatAdapter);
    }

    private void GetUserInfo() {
        UsersRef.child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) currentUserName = Objects.requireNonNull(dataSnapshot.child("name").getValue()).toString();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    @SuppressLint("SimpleDateFormat")
    private void SaveMessageInfoToDatabase() {
        String message = userMessageInput.getText().toString();
        String messageKEY = GroupNameRef.push().getKey();
        if (TextUtils.isEmpty(message)) Toast.makeText(this, "Please write message first...", Toast.LENGTH_SHORT).show();
        else {
            HashMap<String, Object> groupMessageKey = new HashMap<>();
            GroupNameRef.updateChildren(groupMessageKey);
            DatabaseReference groupMessageKeyRef = GroupNameRef.child(Objects.requireNonNull(messageKEY));
            HashMap<String, Object> messageInfoMap = new HashMap<>();
            messageInfoMap.put("name", currentUserName);
            messageInfoMap.put("from", currentUserID);
            messageInfoMap.put("type", "sms");
            messageInfoMap.put("messageID", messageKEY);
            messageInfoMap.put("message", message);
            messageInfoMap.put("date", new SimpleDateFormat("dd.MM.yyyy").format(Calendar.getInstance().getTime()));
            messageInfoMap.put("time", new SimpleDateFormat("HH:mm").format(Calendar.getInstance().getTime()));
            groupMessageKeyRef.updateChildren(messageInfoMap);
        }
    }
}
