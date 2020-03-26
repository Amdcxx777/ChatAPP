package com.amdc.firebasetest;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import java.util.Objects;
import de.hdodenhof.circleimageview.CircleImageView;


public class RequestsFragment extends Fragment {
    private RecyclerView myRequestsList;
    private DatabaseReference ChatRequestsRef, UsersRef, ContactsRef;
    private String currentUserID;
    private Button request_accept_btn;

    public RequestsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View requestsFragmentView = inflater.inflate(R.layout.fragment_requests, container, false);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currentUserID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        ChatRequestsRef = FirebaseDatabase.getInstance().getReference().child("Chat Requests");
        ContactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");
        myRequestsList = requestsFragmentView.findViewById(R.id.chat_requests_list);
        myRequestsList.setLayoutManager(new LinearLayoutManager(getContext()));
        return requestsFragmentView;
    }
    @Override
    public void onStart() {
        super.onStart();
        FirebaseRecyclerOptions<Contacts> options = new FirebaseRecyclerOptions.Builder<Contacts>().setQuery(ChatRequestsRef.child(currentUserID), Contacts.class).build();
        FirebaseRecyclerAdapter<Contacts, RequestsViewHolder> adapter = new FirebaseRecyclerAdapter<Contacts, RequestsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final RequestsViewHolder holder, int position, @NonNull Contacts model) {
                holder.itemView.findViewById(R.id.request_accept_btn).setVisibility(View.VISIBLE);
                holder.itemView.findViewById(R.id.request_cancel_btn).setVisibility(View.VISIBLE);
                final String list_user_id = getRef(position).getKey();
                DatabaseReference getTypeRef = getRef(position).child("request_type").getRef();
                getTypeRef.addValueEventListener(new ValueEventListener() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String type = (String) dataSnapshot.getValue();
                            if (Objects.equals(type, "received")) {
                                assert list_user_id != null;
                                UsersRef.child(list_user_id).addValueEventListener(new ValueEventListener() { //UsersRef.addValueEventListener
                                    @SuppressLint("SetTextI18n")
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.hasChild("image")) {
                                             Picasso.get().load((String) dataSnapshot.child("image").getValue()).into(holder.profileImage);
                                        }
                                        holder.userName.setText((String) dataSnapshot.child("name").getValue());
                                        holder.userStatus.setText("wants to connect with you.");
                                        holder.itemView.setOnClickListener(view -> {
                                            CharSequence[] options1 = new CharSequence[] {"Accept", "Cancel"};
                                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext()); //всплывающее меню
                                            builder.setTitle(dataSnapshot.child("name").getValue()  + "  Chat Request");
                                            builder.setItems(options1, (dialogInterface, i) -> {
                                                if (i == 0) { // if the choice is made to accept
                                                ContactsRef.child(currentUserID).child(list_user_id).child("Contact").setValue("Saved").addOnCompleteListener(task -> {
                                                    if (task.isSuccessful()) {
                                                        ContactsRef.child(list_user_id).child(currentUserID).child("Contact").setValue("Saved").addOnCompleteListener(task1 -> {
                                                            if (task1.isSuccessful()) {
                                                                ChatRequestsRef.child(currentUserID).child(list_user_id).removeValue().addOnCompleteListener(task11 -> {
                                                                    if (task11.isSuccessful()) {
                                                                        ChatRequestsRef.child(list_user_id).child(currentUserID).removeValue().addOnCompleteListener(task111 -> {
                                                                            if (task111.isSuccessful()) {
                                                                                Toast.makeText(getContext(), "New Contact Saved", Toast.LENGTH_SHORT).show();
                                                                            }
                                                                        });
                                                                    }
                                                                });
                                                            }
                                                        });
                                                    }
                                                });
                                            }
                                            if (i == 1) { // if the choice is made to accept
                                                ChatRequestsRef.child(currentUserID).child(list_user_id).removeValue().addOnCompleteListener(task2 -> {
                                                    if (task2.isSuccessful()) {
                                                        ChatRequestsRef.child(list_user_id).child(currentUserID).removeValue().addOnCompleteListener(task12 -> {
                                                            if (task12.isSuccessful()) {
                                                                Toast.makeText(getContext(), "Contact Deleted", Toast.LENGTH_SHORT).show();
                                                            }
                                                        });
                                                    }
                                                });
                                            }
                                            });
                                            builder.show();
                                        });

                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) { }
                                });
                            }
                            else if (Objects.equals(type, "sent")) {
                                request_accept_btn = holder.itemView.findViewById(R.id.request_accept_btn);
                                request_accept_btn.setText("Req Sent");
                                holder.itemView.findViewById(R.id.request_cancel_btn).setVisibility(View.INVISIBLE);
                                UsersRef.child(Objects.requireNonNull(list_user_id)).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.hasChild("image")) {
                                            Picasso.get().load((String) dataSnapshot.child("image").getValue()).into(holder.profileImage);
                                        }
                                        holder.userName.setText((String) dataSnapshot.child("name").getValue());
                                        holder.userStatus.setText("request to " + dataSnapshot.child("name").getValue());
                                        holder.itemView.setOnClickListener(view -> {
                                            CharSequence[] options12 = new CharSequence[] { "Cancel Chat Request"};
                                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext()); //всплывающее меню
                                            builder.setTitle("Already Sent Request"); //заголовок всплывающего меню
                                            builder.setItems(options12, (dialogInterface, i) -> {
                                                if (i == 0) {
                                                    ChatRequestsRef.child(currentUserID).child(list_user_id).removeValue().addOnCompleteListener(task3 -> {
                                                        if (task3.isSuccessful()) {
                                                            ChatRequestsRef.child(list_user_id).child(currentUserID).removeValue().addOnCompleteListener(task13 -> {
                                                                if (task13.isSuccessful()) {
                                                                    Toast.makeText(getContext(), "you have cancelled the chat request.", Toast.LENGTH_SHORT).show();
                                                                }
                                                            });
                                                        }
                                                    });
                                                }
                                            });
                                            builder.show();
                                        });
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
            }
            @NonNull
            @Override
            public RequestsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.users_display_layout, viewGroup, false);
                return new RequestsViewHolder(view);
            }
        };
        myRequestsList.setAdapter(adapter);
        adapter.startListening();
    }

    public static class RequestsViewHolder extends RecyclerView.ViewHolder {
        TextView userName, userStatus;
        CircleImageView profileImage;
        Button AcceptButton, CancelButton;

        RequestsViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.user_profile_name);
            userStatus = itemView.findViewById(R.id.user_status);
            profileImage = itemView.findViewById(R.id.users_profile_image);
            AcceptButton = itemView.findViewById(R.id.request_accept_btn);
            CancelButton = itemView.findViewById(R.id.request_cancel_btn);
        }
    }
}