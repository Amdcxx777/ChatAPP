package com.amdc.firebasetest;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
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

import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;


public class ContactsFragment extends Fragment {
    private RecyclerView myContactsList;
    private DatabaseReference ContactsRef, UsersRef;

    public ContactsFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View contactsView = inflater.inflate(R.layout.fragment_contacts, container, false);

        myContactsList = contactsView.findViewById(R.id.contact_list);
        myContactsList.setLayoutManager(new LinearLayoutManager(getContext()));
        String currentUserID = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        ContactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts").child(currentUserID);
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        return contactsView;
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseRecyclerOptions<Contacts> options = new FirebaseRecyclerOptions.Builder<Contacts>().setQuery(ContactsRef, Contacts.class).build();
        final FirebaseRecyclerAdapter<Contacts, ContactsViewHolder> adapter = new FirebaseRecyclerAdapter<Contacts, ContactsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final ContactsViewHolder holder, int position, @NonNull Contacts model) {
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
                                Picasso.get().load((String) dataSnapshot.child("image").getValue()).resize(120, 120).placeholder(R.drawable.profile_image).into(holder.profileImage);
                            } else {
                                holder.userName.setText((String) dataSnapshot.child("name").getValue());
                                holder.userStatus.setText((String) dataSnapshot.child("status").getValue());
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
            public ContactsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.users_display_layout, viewGroup, false);
                return new ContactsViewHolder(view);
            }
        };
        myContactsList.setAdapter(adapter);
        adapter.startListening();
    }

    private static class ContactsViewHolder extends RecyclerView.ViewHolder {
        TextView userName, userStatus;
        CircleImageView profileImage;
        ImageView onlineIcon;

        ContactsViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.user_profile_name);
            userStatus = itemView.findViewById(R.id.user_status);
            profileImage = itemView.findViewById(R.id.users_profile_image);
            onlineIcon = itemView.findViewById(R.id.user_online_status);
        }
    }
}
