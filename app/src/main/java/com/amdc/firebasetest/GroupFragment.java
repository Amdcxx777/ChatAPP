package com.amdc.firebasetest;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class GroupFragment extends Fragment {
    private ArrayAdapter<String> arrayAdapter;
    private ArrayList<String> list_of_groups = new ArrayList<>();
    private DatabaseReference GroupRef;
    private String currentUserID = "";
//    private Set<String> set;

    public GroupFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View groupFragmentView = inflater.inflate(R.layout.fragment_groups, container, false);

        ListView list_view = groupFragmentView.findViewById(R.id.list_view);
        GroupRef = FirebaseDatabase.getInstance().getReference().child("Groups"); // list groups
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {currentUserID = currentUser.getUid();}
        arrayAdapter = new ArrayAdapter<>(Objects.requireNonNull(getContext()),android.R.layout.simple_list_item_1, list_of_groups);
        list_view.setAdapter(arrayAdapter);
        RetrieveAndDisplayGroups();
        list_view.setOnItemClickListener((adapterView, view, position, l) -> {
            String currentGroupName = adapterView.getItemAtPosition(position).toString();
            Intent groupChatIntent = new Intent(getContext(), GroupChatActivity.class);
            groupChatIntent.putExtra("groupName" , currentGroupName);
            startActivity(groupChatIntent);
        });
        return groupFragmentView;
    }

    private void RetrieveAndDisplayGroups() {  // retrieve list off groups from base
        GroupRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Set<String> set = new HashSet<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    final GroupUsers[] groupUsers = new GroupUsers[1];
                    GroupRef.child(Objects.requireNonNull(snapshot.getKey())).child("Users").child(currentUserID).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                groupUsers[0] = dataSnapshot.getValue(GroupUsers.class);
                                if (currentUserID.equals(Objects.requireNonNull(groupUsers[0]).getUserID())) set.add(snapshot.getKey());
                                list_of_groups.clear();
                                list_of_groups.addAll(set);
                                arrayAdapter.notifyDataSetChanged();
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
    }
}
