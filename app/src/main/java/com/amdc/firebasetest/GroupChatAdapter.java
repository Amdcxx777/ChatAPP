package com.amdc.firebasetest;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.amdc.firebasetest.GroupChatActivity.currentGroupName;

public class GroupChatAdapter extends RecyclerView.Adapter<GroupChatAdapter.GroupMessageViewHolder> {
    private List<Messages> userMessagesList;
    private FirebaseAuth mAuth;
    private StorageReference storageReference, reference;
    private DatabaseReference GroupNameRef;

    GroupChatAdapter(List<Messages> userMessagesList) {
        this.userMessagesList = userMessagesList;
    }

    static class GroupMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView displayTextMessages, displayNameMessages, displayTimeMessages;
        CircleImageView profileImage;
        GroupMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            displayTextMessages = itemView.findViewById(R.id.group_chat_text_display);
            displayNameMessages = itemView.findViewById(R.id.group_chat_name_display);
            displayTimeMessages = itemView.findViewById(R.id.group_chat_time_display);
            profileImage = itemView.findViewById(R.id.group_message_profile_images);
        }
    }

    @NonNull
    @Override
    public GroupMessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_group_custom_messages, parent, false);
        mAuth = FirebaseAuth.getInstance();
        return new GroupMessageViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull final GroupMessageViewHolder holder, final int position) {
        String messageSenderId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        Messages messages = userMessagesList.get(position);
        String fromUserID = messages.getFrom();
//        String fromMessageType = messages.getType();
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference().child("Users").child(fromUserID);
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild("image")) {
                    String receiverImage = Objects.requireNonNull(dataSnapshot.child("image").getValue()).toString();
                    Picasso.get().load(receiverImage).placeholder(R.drawable.profile_image).into(holder.profileImage);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
        holder.displayNameMessages.setText(messages.getName());
        holder.displayTextMessages.setText(messages.getMessage());
        holder.displayTimeMessages.setText(messages.getTime() + " - " + messages.getDate());
        if (fromUserID.equals(messageSenderId)) {
            holder.displayTextMessages.setBackgroundResource(R.drawable.receiver_messages_layout);
            holder.itemView.setOnClickListener(view -> {
                CharSequence[] options = new CharSequence[] {"Delete message", "Cancel"};
                AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                builder.setTitle("Delete Message").setIcon(R.drawable.file);
                builder.setItems(options, (dialogInterface, i) -> {
                    if (i == 0) {
                        deleteSentMessage(position, holder);
                        Intent intent = new Intent(holder.itemView.getContext(), MainActivity.class);
                        holder.itemView.getContext().startActivity(intent);
                    }
                });
                builder.show();
            });
        } else {
            holder.displayTextMessages.setBackgroundResource(R.drawable.sender_messages_layout);
            holder.itemView.setOnClickListener(view -> {
                Toast.makeText(holder.itemView.getContext(), "You cannot delete this message", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void deleteSentMessage(final int position, final GroupChatAdapter.GroupMessageViewHolder holder) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("Groups").child(currentGroupName)
                .child(userMessagesList.get(position).getFrom())
                .child(userMessagesList.get(position).getMessageID())
                .removeValue().addOnCompleteListener(task -> {
            if(task.isSuccessful()) Toast.makeText(holder.itemView.getContext(),"Deleted Successfully.",Toast.LENGTH_SHORT).show();
            else Toast.makeText(holder.itemView.getContext(),"Error Occurred.",Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return userMessagesList.size();
    }
}
