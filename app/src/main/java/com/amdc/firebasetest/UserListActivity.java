package com.amdc.firebasetest;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserListActivity extends AppCompatActivity {
    private DatabaseReference ContactsRef, UsersRef, currentUserGroup;
    private FirebaseRecyclerOptions<Contacts> options;
    private RecyclerView myContactsList;
    private String status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        myContactsList = findViewById(R.id.user_list);
        myContactsList.setLayoutManager(new LinearLayoutManager(UserListActivity.this));
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); //full screen
        setSupportActionBar(findViewById(R.id.user_toolbar)); // my toolbar
        Objects.requireNonNull(getSupportActionBar()).setTitle(Html.fromHtml("<font color='#F3FB00'>" + "Add new contacts to the group" + "</font>"));
        String groupName = (String) Objects.requireNonNull(getIntent().getExtras()).get("group");
        status = (String) Objects.requireNonNull(getIntent().getExtras()).get("status");
        String currentUserID = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        ContactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts").child(currentUserID);
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        currentUserGroup = FirebaseDatabase.getInstance().getReference().child("Groups").child(Objects.requireNonNull(groupName)).child("Users");
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (status.equals("add"))  options = new FirebaseRecyclerOptions.Builder<Contacts>().setQuery(ContactsRef, Contacts.class).build();
        if (status.equals("view") || status.equals("delete")) options = new FirebaseRecyclerOptions.Builder<Contacts>().setQuery(currentUserGroup, Contacts.class).build();
        final FirebaseRecyclerAdapter<Contacts, userViewHolder> adapter = new FirebaseRecyclerAdapter<Contacts, userViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final userViewHolder holder, int position, @NonNull Contacts model) {
                final String userIDs = getRef(position).getKey();
                UsersRef.child(Objects.requireNonNull(userIDs)).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            if (dataSnapshot.child("userState").hasChild("state")) {
                                if (Objects.equals(dataSnapshot.child("userState").child("state").getValue(), "online"))  holder.onlineIcon.setVisibility(View.VISIBLE);
                                else holder.onlineIcon.setVisibility(View.INVISIBLE);
                            }
                            else holder.onlineIcon.setVisibility(View.INVISIBLE);
                            if (dataSnapshot.hasChild("image")) {
                                holder.userName.setText((String) dataSnapshot.child("name").getValue());
                                holder.userStatus.setText((String) dataSnapshot.child("status").getValue());
                                Picasso.get().load((String) dataSnapshot.child("image").getValue()).resize(90, 90).placeholder(R.drawable.profile_image).into(holder.profileImage);
                                if (status.equals("add")) holder.itemView.setOnClickListener(v -> addUserToGroup(userIDs));
                                if (status.equals("delete")) holder.itemView.setOnClickListener(v -> deleteUserFromGroup(userIDs));
                            } else {
                                holder.userName.setText((String) dataSnapshot.child("name").getValue());
                                holder.userStatus.setText((String) dataSnapshot.child("status").getValue());
                                if (status.equals("add")) holder.itemView.setOnClickListener(v -> addUserToGroup(userIDs));
                                if (status.equals("delete")) holder.itemView.setOnClickListener(v -> deleteUserFromGroup(userIDs));
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });
            }
            @NonNull
            @Override
            public userViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.users_display_layout, viewGroup, false);
                return new userViewHolder(view);
            }
        };
        myContactsList.setAdapter(adapter);
        adapter.startListening();
    }

    @SuppressLint("SimpleDateFormat")
    public void addUserToGroup(String userID) {
        new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_menu_add).setTitle("Add New User")
                .setMessage("Add this contact to a group?").setPositiveButton("Yes", (dialog, which) -> {
            HashMap<String, Object> userGrMap = new HashMap<>();
            userGrMap.put("userID", userID);
            userGrMap.put("status", "user");
            userGrMap.put("time", new SimpleDateFormat("HH:mm").format(Calendar.getInstance().getTime()));
            userGrMap.put("date", new SimpleDateFormat("dd.MMM.yyyy", Locale.US).format(Calendar.getInstance().getTime()));
            currentUserGroup.child(userID).setValue(userGrMap).addOnCompleteListener(task -> {
                if (task.isSuccessful()) Toast.makeText(this, "User added Successfully", Toast.LENGTH_SHORT).show();
            });
        }).setNegativeButton("No", null).show();
    }

    public void deleteUserFromGroup(String userID) {
        new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_menu_add).setTitle("Delete User")
                .setMessage("Remove this contact from the group?").setPositiveButton("Yes", (dialog, which) -> {
            currentUserGroup.child(userID).removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) Toast.makeText(this, "User deleted Successfully", Toast.LENGTH_SHORT).show();
            });
        }).setNegativeButton("No", null).show();
    }

    public static class userViewHolder extends RecyclerView.ViewHolder {
        TextView userName,userStatus;
        CircleImageView profileImage;
        ImageView onlineIcon;
        userViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.user_profile_name);
            userStatus = itemView.findViewById(R.id.user_status);
            profileImage = itemView.findViewById(R.id.users_profile_image);
            onlineIcon = itemView.findViewById(R.id.user_online_status);
        }
    }
}
