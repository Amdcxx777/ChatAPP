package com.amdc.firebasetest;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.amdc.firebasetest.MainActivity.bell;
import static com.amdc.firebasetest.MainActivity.sound;
import static com.amdc.firebasetest.MainActivity.vibrator;
import static com.amdc.firebasetest.MainActivity.vibro;

public class ChatFragment extends Fragment {
    private RecyclerView chatsList;
    private DatabaseReference RootRef, ChatsRef, UsersRef;
    private String currentUserID = "";
    private int count;
    public ChatFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View privateChatsView = inflater.inflate(R.layout.fragment_chat, container, false);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {currentUserID = currentUser.getUid();}
        RootRef = FirebaseDatabase.getInstance().getReference();
        ChatsRef = FirebaseDatabase.getInstance().getReference().child("Contacts").child(currentUserID);
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        chatsList = privateChatsView.findViewById(R.id.chats_list);
        chatsList.setLayoutManager(new LinearLayoutManager(getContext()));
        return privateChatsView;
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseRecyclerOptions<Contacts> options = new FirebaseRecyclerOptions.Builder<Contacts>().setQuery(ChatsRef, Contacts.class).build();
        FirebaseRecyclerAdapter<Contacts, ChatsViewHolder> adapter = new FirebaseRecyclerAdapter<Contacts, ChatsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final ChatsViewHolder holder, int position, @NonNull Contacts model) {
                final String usersIDs = getRef(position).getKey();
                final String[] retImage = {"default_image"};
                    RootRef.child("Message notifications").child(currentUserID).child(Objects.requireNonNull(usersIDs)).addValueEventListener(new ValueEventListener() { // counter listener
                        @SuppressLint("SetTextI18n") //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                        @Override //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) { //~~~~~~~~~~~
                            if (dataSnapshot.exists()) { //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                                try { count = Integer.parseInt(((String) Objects.requireNonNull(dataSnapshot.child("Counter").getValue()))); // get counter value for visible
                                } catch (Exception ignored) { } //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                                if (count != 0) {
                                    holder.itemView.findViewById(R.id.message_counter).setVisibility(View.VISIBLE); // visibility counter message
                                    holder.messCounter.setText(count + "");
                                    if (bell) sound.start(); // sound when message was received
                                    if (vibro) vibrator.vibrate(200); // vibrator when message was received
                                }
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                        }
                    });
                UsersRef.child(Objects.requireNonNull(usersIDs)).addValueEventListener(new ValueEventListener() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            if (dataSnapshot.hasChild("image")) {
                                retImage[0] = (String) dataSnapshot.child("image").getValue();
                                Picasso.get().load(retImage[0]).into(holder.profileImage);
                            }
                            final String retName = (String) dataSnapshot.child("name").getValue();
                            holder.userName.setText(retName);
                            holder.userStatus.setText("Last Seen: " + "\n" + "Time" + " - " + "Date ");
                            if (dataSnapshot.child("userState").hasChild("state")) {
                                String state = (String) dataSnapshot.child("userState").child("state").getValue();
                                String date = (String) dataSnapshot.child("userState").child("date").getValue();
                                String time = (String) dataSnapshot.child("userState").child("time").getValue();
                                if (state != null) {
                                    if (state.equals("online"))  holder.userStatus.setText("online"); //receive status from base
                                    else if (state.equals("offline"))  holder.userStatus.setText("Last Seen: " + time + " - " + date);
                                }
                            }
                            else { holder.userStatus.setText("offline"); }
                            holder.itemView.setOnClickListener(view -> { // click on item for choice
                                holder.itemView.findViewById(R.id.message_counter).setVisibility(View.INVISIBLE); // invisibility counter message
                                Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                                chatIntent.putExtra("visit_user_id", usersIDs);
                                chatIntent.putExtra("visit_user_name", retName);
                                chatIntent.putExtra("visit_image", retImage[0]);
                                startActivity(chatIntent);
                            });
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });
            }
            @NonNull
            @Override
            public ChatsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.users_display_layout, viewGroup, false);
                return new ChatsViewHolder(view);
            }
        };
        chatsList.setAdapter(adapter);
        adapter.startListening();
    }

    public static class  ChatsViewHolder extends RecyclerView.ViewHolder {
        CircleImageView profileImage;
        TextView userStatus, userName, messCounter;
        ChatsViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.users_profile_image);
            userStatus = itemView.findViewById(R.id.user_status);
            userName = itemView.findViewById(R.id.user_profile_name);
            messCounter = itemView.findViewById(R.id.message_counter);
        }
    }
}
