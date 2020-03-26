package com.amdc.firebasetest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {
    private String receiverUserID, senderUserID, Current_State;
    private CircleImageView userProfileImage;
    private TextView userProfileName, userProfileStatus;
    private Button SendMessageRequestButton, DeclineMessageRequestButton;
    private DatabaseReference UserRef, ChatRequestRef, ContactsRef, NotificationRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Objects.requireNonNull(getSupportActionBar()).setTitle("Contact");
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        UserRef = FirebaseDatabase.getInstance().getReference().child("Users");
        ChatRequestRef = FirebaseDatabase.getInstance().getReference().child("Chat Requests");
        ContactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");
        NotificationRef = FirebaseDatabase.getInstance().getReference().child("Notifications");
//
//        // from FindFriendsActivity.java
        receiverUserID = getIntent().getExtras().get("visit_user_id").toString(); //received user id from find friends
        senderUserID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

        userProfileImage = findViewById(R.id.visit_profile_image);
        userProfileName = findViewById(R.id.visit_user_name);
        userProfileStatus = findViewById(R.id.visit_profile_status);
        SendMessageRequestButton = findViewById(R.id.send_message_request_button);
        DeclineMessageRequestButton = findViewById(R.id.decline_message_request_button);
        Current_State = "new";
        RetrieveUserInfo();
    }

    private void RetrieveUserInfo() { // received user info
        UserRef.child(receiverUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if ((dataSnapshot.exists())  &&  (dataSnapshot.hasChild("image"))) {
                    String userImage = (String) dataSnapshot.child("image").getValue();
                    Picasso.get().load(userImage).placeholder(R.drawable.profile_image).into(userProfileImage);
                    userProfileName.setText((String) dataSnapshot.child("name").getValue());
                    userProfileStatus.setText((String) dataSnapshot.child("status").getValue());
                    ManageChatRequests();
                } else {
                    userProfileName.setText((String) dataSnapshot.child("name").getValue());
                    userProfileStatus.setText((String) dataSnapshot.child("status").getValue());
                    ManageChatRequests();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    private void ManageChatRequests() {
        ChatRequestRef.child(senderUserID).addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild(receiverUserID)) {
                    String request_type = (String) dataSnapshot.child(receiverUserID).child("request_type").getValue();
                    if(Objects.requireNonNull(request_type).equals("sent")) {
                        Current_State = "request_send";
                        SendMessageRequestButton.setText("Cancel Chat Request");
                    }
                    else if(request_type.equals("received")) {
                        Current_State = "request_received";
                        SendMessageRequestButton.setText("Accept Chat Request");
                        DeclineMessageRequestButton.setVisibility(View.VISIBLE);
                        DeclineMessageRequestButton.setEnabled(true);
                        DeclineMessageRequestButton.setOnClickListener(view -> CancelChatRequest());
                    }
                } else {
                    ContactsRef.child(senderUserID).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.hasChild(receiverUserID)) {
                                Current_State = "friends";
                                SendMessageRequestButton.setText("Remove This Contact");
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) { }
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

        if(!senderUserID.equals(receiverUserID)) {
            SendMessageRequestButton.setOnClickListener(view -> {
                SendMessageRequestButton.setEnabled(false);
                if(Current_State.equals("new")) SendChatRequest();
                if(Current_State.equals("request_send")) CancelChatRequest();
                if(Current_State.equals("request_received"))  AcceptChatRequest();
                    if(Current_State.equals("friends")) RemoveSpecificContact();
            });
        }
    }

    @SuppressLint("SetTextI18n")
    private void RemoveSpecificContact() {
        ContactsRef.child(senderUserID).removeValue().addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                ContactsRef.child(receiverUserID).child(senderUserID).removeValue().addOnCompleteListener(task1 -> {
                    if(task1.isSuccessful()) {
                        SendMessageRequestButton.setEnabled(true);
                        Current_State = "new";
                        SendMessageRequestButton.setText("Send Message");
                        DeclineMessageRequestButton.setVisibility(View.INVISIBLE);
                        DeclineMessageRequestButton.setEnabled(false);
                    }
                });
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void AcceptChatRequest() {
        ContactsRef.child(senderUserID).child(receiverUserID).child("Contacts").setValue("Saved").addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                ChatRequestRef.child(senderUserID).child(receiverUserID).removeValue().addOnCompleteListener(task1 -> {
                    if(task1.isSuccessful()) {
                        ChatRequestRef.child(receiverUserID).child(senderUserID).removeValue().addOnCompleteListener(task11 -> {
                            SendMessageRequestButton.setEnabled(true);
                            Current_State = "friends";
                            SendMessageRequestButton.setText("Remove This Contact");
                            DeclineMessageRequestButton.setVisibility(View.INVISIBLE);
                            DeclineMessageRequestButton.setEnabled(false);
                        });
                    }
                });
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void SendChatRequest() {
        ChatRequestRef.child(senderUserID).child(receiverUserID).child("request_type").setValue("sent").addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ChatRequestRef.child(receiverUserID).child(senderUserID).child("request_type").setValue("received").addOnCompleteListener(task1 -> {
                    if (task.isSuccessful()) {
                        HashMap<String, String> chatNotificationMap = new HashMap<>();
                        chatNotificationMap.put("from", senderUserID);
                        chatNotificationMap.put("type", "request");
                        NotificationRef.child(receiverUserID).push().setValue(chatNotificationMap).addOnCompleteListener(task2 -> {
                            if (task2.isSuccessful()) {
                                SendMessageRequestButton.setEnabled(true);
                                Current_State = "request_send";
                                SendMessageRequestButton.setText("Cancel Chat Request");
                            }
                        });
                    }
                });
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void CancelChatRequest() {
        ChatRequestRef.child(senderUserID).child(receiverUserID).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ChatRequestRef.child(receiverUserID).child(senderUserID).removeValue().addOnCompleteListener(task1 -> {
                    if (task.isSuccessful()) {
                        SendMessageRequestButton.setEnabled(true);
                        Current_State = "new";
                        SendMessageRequestButton.setText("Send Message");
                        DeclineMessageRequestButton.setVisibility(View.INVISIBLE);
                        DeclineMessageRequestButton.setEnabled(false);
                    }
                });
            }
        });
    }
}
