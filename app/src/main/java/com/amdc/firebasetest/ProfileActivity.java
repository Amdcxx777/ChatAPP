package com.amdc.firebasetest;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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
    private Button btnSendRequest, btnCancelRequest;
    private DatabaseReference UserRef, ChatRequestRef, ContactsRef, NotificationRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); //full screen
        setSupportActionBar(findViewById(R.id.activity_profile_toolbar)); // my toolbar
        Objects.requireNonNull(getSupportActionBar()).setTitle(Html.fromHtml("<font color='#F3FB00'>" + "Contact:" + "</font>"));
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        UserRef = FirebaseDatabase.getInstance().getReference().child("Users");
        ChatRequestRef = FirebaseDatabase.getInstance().getReference().child("Chat Requests");
        ContactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");
        NotificationRef = FirebaseDatabase.getInstance().getReference().child("Notifications");
        receiverUserID = (String) Objects.requireNonNull(getIntent().getExtras()).get("visit_user_id"); //received user id from find friends
        senderUserID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        userProfileImage = findViewById(R.id.visit_profile_image);
        userProfileName = findViewById(R.id.visit_user_name);
        userProfileStatus = findViewById(R.id.visit_profile_status);
        btnSendRequest = findViewById(R.id.send_message_request_button);
        btnCancelRequest = findViewById(R.id.decline_message_request_button);
        Current_State = "new";
        RetrieveUserInfo();
    }

    private void RetrieveUserInfo() { // received user info
        UserRef.child(receiverUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if ((dataSnapshot.exists())  &&  (dataSnapshot.hasChild("image"))) {
                    String userImage = (String) dataSnapshot.child("image").getValue();
                    Picasso.get().load(userImage).resize(300, 300).placeholder(R.drawable.profile_image).into(userProfileImage);
                }
                userProfileName.setText((String) dataSnapshot.child("name").getValue());
                userProfileStatus.setText((String) dataSnapshot.child("status").getValue());
                ManageChatRequests();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
        ContactsRef.child(senderUserID).child(receiverUserID).addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Current_State = "friends";
                    btnSendRequest.setText("Remove This Contact");
                } else {
                    Current_State = "new";
                    btnSendRequest.setText("Send Message");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void ManageChatRequests() {
        if(!senderUserID.equals(receiverUserID)) { // if not my account
            btnSendRequest.setEnabled(true);
            btnSendRequest.setVisibility(View.VISIBLE); // button is visible if not main account
            btnSendRequest.setOnClickListener(view -> {
                if(Current_State.equals("new")) SendChatRequest();
                if(Current_State.equals("request_send")) CancelChatRequest();
                if(Current_State.equals("request_received")) AcceptChatRequest();
                if(Current_State.equals("friends")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialog); // alert dialog
                    builder.setTitle("Are you sure? All messages with it will be deleted!");
                    builder.setPositiveButton("Remove contact", (dialogInterface, i) -> RemoveSpecificContact());
                    builder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.cancel());
                    builder.show();
                }
            });

            ChatRequestRef.child(senderUserID).addValueEventListener(new ValueEventListener() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.hasChild(receiverUserID)) {
                        final String request_type = (String) dataSnapshot.child(receiverUserID).child("request_type").getValue();
                        if (Objects.requireNonNull(request_type).equals("sent")) { //for sender
                            Current_State = "request_send";
                            btnSendRequest.setText("Cancel Chat Request");
                        } else if(request_type.equals("received")) { // for recipient
                            Current_State = "request_received";
                            btnSendRequest.setText("Accept Chat Request");
                            btnCancelRequest.setVisibility(View.VISIBLE);
                            btnCancelRequest.setEnabled(true);
                            btnCancelRequest.setOnClickListener(view -> NotificationRef.child(senderUserID).removeValue().addOnCompleteListener(task -> {
                                if (task.isSuccessful()) CancelChatRequest();
                            }));
                        } else {
                            ContactsRef.child(receiverUserID).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.hasChild(receiverUserID)) {
                                        Current_State = "friends";
                                        btnSendRequest.setText("Remove this Contact");
                                    }
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) { }
                            });
                        }
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) { }
            });
        } else { // button invisible if my account
            btnSendRequest.setEnabled(false);
            btnSendRequest.setVisibility(View.INVISIBLE);
        }
    }

    @SuppressLint({"SetTextI18n", "ResourceAsColor"})
    private void RemoveSpecificContact() {
        ContactsRef.child(senderUserID).child(receiverUserID).removeValue().addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                ContactsRef.child(receiverUserID).child(senderUserID).removeValue().addOnCompleteListener(task1 -> {
                    if(task1.isSuccessful()) {
                        Current_State = "new";
                        btnSendRequest.setText("Send Message");
                        btnCancelRequest.setVisibility(View.INVISIBLE);
                        btnCancelRequest.setEnabled(false);
                        Toast.makeText(this, "Contact has been deleted", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @SuppressLint({"SetTextI18n", "ResourceAsColor"})
    private void AcceptChatRequest() {
        ContactsRef.child(senderUserID).child(receiverUserID).child("Contacts").setValue("Saved").addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                ContactsRef.child(receiverUserID).child(senderUserID).child("Contacts").setValue("Saved").addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        ChatRequestRef.child(senderUserID).child(receiverUserID).removeValue().addOnCompleteListener(task2 -> {
                            if(task2.isSuccessful()) {
                                ChatRequestRef.child(receiverUserID).child(senderUserID).removeValue().addOnCompleteListener(task3 -> {
                                    if (task3.isSuccessful()) {
                                        NotificationRef.child(senderUserID).removeValue().addOnCompleteListener(task4 -> {
                                            if (task4.isSuccessful()) {
                                                Current_State = "friends";
                                                btnSendRequest.setText("Remove This Contact");
                                                btnCancelRequest.setVisibility(View.INVISIBLE);
                                                btnCancelRequest.setEnabled(false);
                                                Toast.makeText(this, "New Contact Saved", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    @SuppressLint({"SetTextI18n", "ResourceAsColor"})
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
                                Current_State = "request_send";
                                btnSendRequest.setText("Cancel Chat Request");
                                Toast.makeText(this, "Request has been sent", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });
    }

    @SuppressLint({"SetTextI18n", "ResourceAsColor"})
    private void CancelChatRequest() {
        ChatRequestRef.child(senderUserID).child(receiverUserID).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ChatRequestRef.child(receiverUserID).child(senderUserID).removeValue().addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        NotificationRef.child(receiverUserID).removeValue().addOnCompleteListener(task2 -> {
                            if (task2.isSuccessful()) {
                                Current_State = "new";
                                btnSendRequest.setText("Send Message");
                                btnCancelRequest.setVisibility(View.INVISIBLE);
                                btnCancelRequest.setEnabled(false);
                                Toast.makeText(this, "Request has been canceled", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });
    }
}
