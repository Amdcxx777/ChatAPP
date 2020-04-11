package com.amdc.firebasetest;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Objects;
import de.hdodenhof.circleimageview.CircleImageView;

import static android.os.Environment.DIRECTORY_DOWNLOADS;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private List<Messages> userMessagesList;
    private FirebaseAuth mAuth;
    private StorageReference storageReference, reference;

    MessageAdapter(List<Messages> userMessagesList) {
        this.userMessagesList = userMessagesList;
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView senderMessageText, receiverMessageText, senderMessageTime, receiverMessageTime, senderMessageTimeImage, receiverMessageTimeImage;
        CircleImageView receiverProfileImage;
        ImageView messageSenderPicture, messageReceiverPicture;
        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            senderMessageText = itemView.findViewById(R.id.sender_message_text);
            senderMessageTime = itemView.findViewById(R.id.sender_message_time);
            senderMessageTimeImage = itemView.findViewById(R.id.sender_message_time_image);
            receiverMessageText = itemView.findViewById(R.id.receiver_message_text);
            receiverMessageTime = itemView.findViewById(R.id.receiver_message_time);
            receiverMessageTimeImage = itemView.findViewById(R.id.receiver_message_time_image);
            receiverProfileImage = itemView.findViewById(R.id.message_profile_image);
            messageReceiverPicture = itemView.findViewById(R.id.message_receiver_image_view);
            messageSenderPicture = itemView.findViewById(R.id.message_sender_image_view);
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.custom_messages_layout, viewGroup, false);
        mAuth = FirebaseAuth.getInstance();
        return new MessageViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull final MessageViewHolder messageViewHolder, final int position) { //final ?
        String messageSenderId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        Messages messages = userMessagesList.get(position);
        String fromUserID = messages.getFrom();
        String fromMessageType = messages.getType();
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference().child("Users").child(fromUserID);
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild("image")) {
                    String receiverImage = Objects.requireNonNull(dataSnapshot.child("image").getValue()).toString();
                    Picasso.get().load(receiverImage).placeholder(R.drawable.profile_image).into(messageViewHolder.receiverProfileImage);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

        messageViewHolder.receiverMessageText.setVisibility(View.GONE);
        messageViewHolder.receiverProfileImage.setVisibility(View.GONE);
        messageViewHolder.senderMessageText.setVisibility(View.GONE);
        messageViewHolder.messageSenderPicture.setVisibility(View.GONE);
        messageViewHolder.senderMessageTime.setVisibility(View.GONE);
        messageViewHolder.receiverMessageTime.setVisibility(View.GONE);
        messageViewHolder.senderMessageTimeImage.setVisibility(View.GONE);
        messageViewHolder.receiverMessageTimeImage.setVisibility(View.GONE);
        messageViewHolder.messageReceiverPicture.setVisibility(View.GONE);
        switch (fromMessageType) {
            case "text":
                if (fromUserID.equals(messageSenderId)) {
                    messageViewHolder.senderMessageText.setVisibility(View.VISIBLE);
                    messageViewHolder.senderMessageTime.setVisibility(View.VISIBLE);

                    messageViewHolder.senderMessageText.setBackgroundResource(R.drawable.sender_messages_layout);
                    messageViewHolder.senderMessageText.setText(messages.getMessage());
                    messageViewHolder.senderMessageTime.setText(messages.getTime() + " - " + messages.getDate());
                } else {
                    messageViewHolder.receiverProfileImage.setVisibility(View.VISIBLE);
                    messageViewHolder.receiverMessageText.setVisibility(View.VISIBLE);
                    messageViewHolder.receiverMessageTime.setVisibility(View.VISIBLE);

                    messageViewHolder.receiverMessageText.setBackgroundResource(R.drawable.receiver_messages_layout);
                    messageViewHolder.receiverMessageText.setText(messages.getMessage());
                    messageViewHolder.receiverMessageTime.setText(messages.getTime() + " - " + messages.getDate());
                }
                break;
            case "image":
                if (fromUserID.equals(messageSenderId)) {
                    setVisibleForSender(messageViewHolder);
                    Picasso.get().load(messages.getMessage()).into(messageViewHolder.messageSenderPicture);
                    messageViewHolder.senderMessageTimeImage.setText(messages.getTime() + " - " + messages.getDate());
                } else {
                    setVisibleForReceiver(messageViewHolder);
                    Picasso.get().load(messages.getMessage()).into(messageViewHolder.messageReceiverPicture);
                    messageViewHolder.receiverMessageTimeImage.setText(messages.getTime() + " - " + messages.getDate());
                }
                break;
            case "pdf":
                if (fromUserID.equals(messageSenderId)) {
                    setVisibleForSender(messageViewHolder);
                    Picasso.get().load("https://firebasestorage.googleapis.com/v0/b/testbase-def93.appspot.com/o/Image%20Files%2Ficon_pdf.png?alt=media&token=3deeefdf-472c-4fc8-ba70-dee1f444e54e").into(messageViewHolder.messageSenderPicture);
                    messageViewHolder.senderMessageTimeImage.setText(messages.getTime() + " - " + messages.getDate());
                } else {
                    setVisibleForReceiver(messageViewHolder);
                    Picasso.get().load("https://firebasestorage.googleapis.com/v0/b/testbase-def93.appspot.com/o/Image%20Files%2Ficon_pdf.png?alt=media&token=3deeefdf-472c-4fc8-ba70-dee1f444e54e").into(messageViewHolder.messageReceiverPicture);
                    messageViewHolder.receiverMessageTimeImage.setText(messages.getTime() + " - " + messages.getDate());
                }
                break;
            case "xls":
            case "xlsx":
                if (fromUserID.equals(messageSenderId)) {
                    setVisibleForSender(messageViewHolder);
                    Picasso.get().load("https://firebasestorage.googleapis.com/v0/b/testbase-def93.appspot.com/o/Image%20Files%2Ficon_excel.png?alt=media&token=50cb92eb-acb0-4b63-8fcc-8237d7cea5b7").into(messageViewHolder.messageSenderPicture);
                    messageViewHolder.senderMessageTimeImage.setText(messages.getTime() + " - " + messages.getDate());
                } else {
                    setVisibleForReceiver(messageViewHolder);
                    Picasso.get().load("https://firebasestorage.googleapis.com/v0/b/testbase-def93.appspot.com/o/Image%20Files%2Ficon_excel.png?alt=media&token=50cb92eb-acb0-4b63-8fcc-8237d7cea5b7").into(messageViewHolder.messageReceiverPicture);
                    messageViewHolder.receiverMessageTimeImage.setText(messages.getTime() + " - " + messages.getDate());
                }
                break;
            case "doc":
            case "docx":
                if (fromUserID.equals(messageSenderId)) {
                    setVisibleForSender(messageViewHolder);
                    Picasso.get().load("https://firebasestorage.googleapis.com/v0/b/testbase-def93.appspot.com/o/Image%20Files%2Ficon_world.png?alt=media&token=01a2ea3f-7df4-4b7c-86a4-ff9aded94a40").into(messageViewHolder.messageSenderPicture);
                    messageViewHolder.senderMessageTimeImage.setText(messages.getTime() + " - " + messages.getDate());
                } else {
                    setVisibleForReceiver(messageViewHolder);
                    Picasso.get().load("https://firebasestorage.googleapis.com/v0/b/testbase-def93.appspot.com/o/Image%20Files%2Ficon_world.png?alt=media&token=01a2ea3f-7df4-4b7c-86a4-ff9aded94a40").into(messageViewHolder.messageReceiverPicture);
                    messageViewHolder.receiverMessageTimeImage.setText(messages.getTime() + " - " + messages.getDate());
                }
                break;
        }
        if (fromUserID.equals(messageSenderId)) { // alert dialog for sender messages
            messageViewHolder.itemView.setOnClickListener(view -> {
                switch (userMessagesList.get(position).getType()) {
                    case "pdf":
                    case "xls":
                    case "xlsx":
                    case "doc":
                    case "docx": {
                        CharSequence[] options = new CharSequence[] {"Download and View this Document", "Download this Document", "Delete From Everywhere", "Delete Only From Me", "Cancel"};
                        AlertDialog.Builder builder = new AlertDialog.Builder(messageViewHolder.itemView.getContext());
                        builder.setTitle("Download/Delete Message").setIcon(R.drawable.file);
                        builder.setItems(options, (dialogInterface, i) -> {
                            if (i == 0) {
                                String fileNameForDownload = userMessagesList.get(position).getMessageID() + "." + userMessagesList.get(position).getType(); //get name file from storage
                                String fileName = Uri.parse(userMessagesList.get(position).getName()).getLastPathSegment(); //get name file from user into firebase
                                Intent intent = new Intent(messageViewHolder.itemView.getContext(), FilesViewerActivity.class);
                                intent.putExtra("fileNameForDownload", fileNameForDownload);
                                intent.putExtra("fileName", fileName);
                                messageViewHolder.itemView.getContext().startActivity(intent);
                            } else if (i == 1) { // download file
                                String fileNameForDownload = userMessagesList.get(position).getMessageID() + "." + userMessagesList.get(position).getType(); //get name file from storage
                                storageReference = FirebaseStorage.getInstance().getReference();
                                reference = storageReference.child("Document Files").child(fileNameForDownload);
                                reference.getDownloadUrl().addOnSuccessListener(uri -> {
                                    String fileName = Uri.parse(userMessagesList.get(position).getName()).getLastPathSegment(); //get name file from user into firebase
                                    DownloadManager downloadManager = (DownloadManager) messageViewHolder.itemView.getContext().getSystemService(Context.DOWNLOAD_SERVICE);
                                    DownloadManager.Request request = new DownloadManager.Request(uri);
                                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                                    request.setDestinationInExternalFilesDir(messageViewHolder.itemView.getContext(), DIRECTORY_DOWNLOADS, fileName); //reference.getName()
                                    Objects.requireNonNull(downloadManager).enqueue(request);
                                }).addOnFailureListener(e -> Toast.makeText(messageViewHolder.itemView.getContext(), "Error download", Toast.LENGTH_SHORT).show());
                            } else if (i == 2) {
                                deleteMessageForEveryOne(position, messageViewHolder);
                                Intent intent = new Intent(messageViewHolder.itemView.getContext(), MainActivity.class);
                                messageViewHolder.itemView.getContext().startActivity(intent);
                            } else if (i == 3) {
                                deleteSentMessage(position, messageViewHolder);
                                Intent intent = new Intent(messageViewHolder.itemView.getContext(), MainActivity.class);
                                messageViewHolder.itemView.getContext().startActivity(intent);
                            }
                        });
                        builder.show();
                        break;
                    }
                    case "text": { // sender messages for text
                        CharSequence[] options = new CharSequence[]{"Delete From Everywhere", "Delete Only From Me", "Cancel"};
                        AlertDialog.Builder builder = new AlertDialog.Builder(messageViewHolder.itemView.getContext());
                        builder.setTitle("Delete Message").setIcon(R.drawable.delete);
                        builder.setItems(options, (dialogInterface, i) -> {
                            if (i == 0) {
                                deleteMessageForEveryOne(position, messageViewHolder);
                                Intent intent = new Intent(messageViewHolder.itemView.getContext(), MainActivity.class);
                                messageViewHolder.itemView.getContext().startActivity(intent);
                            } else if (i == 1) {
                                deleteSentMessage(position, messageViewHolder);
                                Intent intent = new Intent(messageViewHolder.itemView.getContext(), MainActivity.class);
                                messageViewHolder.itemView.getContext().startActivity(intent);
                            }
                        });
                        builder.show();
                        break;
                    }
                    case "image": { // sender messages for image
                        CharSequence[] options = new CharSequence[]{"View This Image", "Delete From Everywhere", "Delete Only From Me", "Cancel"};
                        AlertDialog.Builder builder = new AlertDialog.Builder(messageViewHolder.itemView.getContext());
                        builder.setTitle("View/Delete Message").setIcon(R.drawable.file);
                        builder.setItems(options, (dialogInterface, i) -> {
                            if (i == 0) {
                                Intent intent = new Intent(messageViewHolder.itemView.getContext(), ImageViewerActivity.class);
                                intent.putExtra("url", userMessagesList.get(position).getMessage());
                                messageViewHolder.itemView.getContext().startActivity(intent);
                            } else if (i == 1) {
                                deleteMessageForEveryOne(position, messageViewHolder);
                                Intent intent = new Intent(messageViewHolder.itemView.getContext(), MainActivity.class);
                                messageViewHolder.itemView.getContext().startActivity(intent);
                            } else if (i == 2) {
                                deleteSentMessage(position, messageViewHolder);
                                Intent intent = new Intent(messageViewHolder.itemView.getContext(), MainActivity.class);
                                messageViewHolder.itemView.getContext().startActivity(intent);
                            }
                        });
                        builder.show();
                        break;
                    }
                }
            });
        } else { // alert dialog for receiver messages
            messageViewHolder.itemView.setOnClickListener(view -> {
                switch (userMessagesList.get(position).getType()) {
                    case "pdf":
                    case "xls":
                    case "xlsx":
                    case "doc":
                    case "docx": {
                        CharSequence[] options = new CharSequence[]{"Download and View this Document", "Download this Document", "Delete only From Me", "Cancel"};
                        AlertDialog.Builder builder = new AlertDialog.Builder(messageViewHolder.itemView.getContext());
                        builder.setTitle("Download/Delete Message").setIcon(R.drawable.file);
                        builder.setItems(options, (dialogInterface, i) -> {
                            if (i == 0) {
                                String fileNameForDownload = userMessagesList.get(position).getMessageID() + "." + userMessagesList.get(position).getType(); //get name file from storage
                                String fileName = Uri.parse(userMessagesList.get(position).getName()).getLastPathSegment(); //get name file from user into firebase
                                Intent intent = new Intent(messageViewHolder.itemView.getContext(), FilesViewerActivity.class);
                                intent.putExtra("fileNameForDownload", fileNameForDownload);
                                intent.putExtra("fileName", fileName);
                                messageViewHolder.itemView.getContext().startActivity(intent);
                            }
                            if (i == 1) {
                                String fileNameForDownload = userMessagesList.get(position).getMessageID() + "." + userMessagesList.get(position).getType();
                                storageReference = FirebaseStorage.getInstance().getReference();
                                reference = storageReference.child("Document Files").child(fileNameForDownload);
                                reference.getDownloadUrl().addOnSuccessListener(uri -> {
                                    String fileName = Uri.parse(userMessagesList.get(position).getName()).getLastPathSegment(); //get name file from firebase
                                    DownloadManager downloadManager = (DownloadManager) messageViewHolder.itemView.getContext().getSystemService(Context.DOWNLOAD_SERVICE);
                                    DownloadManager.Request request = new DownloadManager.Request(uri);
                                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                                    request.setDestinationInExternalFilesDir(messageViewHolder.itemView.getContext(), DIRECTORY_DOWNLOADS, fileName); //reference.getName()
                                    Objects.requireNonNull(downloadManager).enqueue(request);
                                }).addOnFailureListener(e -> Toast.makeText(messageViewHolder.itemView.getContext(), "Error download", Toast.LENGTH_SHORT).show());
                            } else if (i == 2) {
                                deleteReceiverMessage(position, messageViewHolder);
                                Intent intent = new Intent(messageViewHolder.itemView.getContext(), MainActivity.class);
                                messageViewHolder.itemView.getContext().startActivity(intent);
                            }
                        });
                        builder.show();
                        break;
                    }
                    case "text": {
                        CharSequence[] options = new CharSequence[]{"Delete From  Me", "Cancel"};
                        AlertDialog.Builder builder = new AlertDialog.Builder(messageViewHolder.itemView.getContext());
                        builder.setTitle("Delete Message").setIcon(R.drawable.delete);
                        builder.setItems(options, (dialogInterface, i) -> {
                            if (i == 0) {
                                deleteReceiverMessage(position, messageViewHolder);
                                Intent intent = new Intent(messageViewHolder.itemView.getContext(), MainActivity.class);
                                messageViewHolder.itemView.getContext().startActivity(intent);
                            }
                        });
                        builder.show();
                        break;
                    }
                    case "image": {
                        CharSequence[] options = new CharSequence[]{"View this Image", "Delete From Me", "Cancel"};
                        AlertDialog.Builder builder = new AlertDialog.Builder(messageViewHolder.itemView.getContext());
                        builder.setTitle("View/Delete Message").setIcon(R.drawable.file);
                        builder.setItems(options, (dialogInterface, i) -> {
                            if (i == 0) {
                                Intent intent = new Intent(messageViewHolder.itemView.getContext(), ImageViewerActivity.class);
                                intent.putExtra("url", userMessagesList.get(position).getMessage());
                                messageViewHolder.itemView.getContext().startActivity(intent);
                            } else if (i == 1) {
                                deleteReceiverMessage(position, messageViewHolder);
                                Intent intent = new Intent(messageViewHolder.itemView.getContext(), MainActivity.class);
                                messageViewHolder.itemView.getContext().startActivity(intent);
                            }
                        });
                        builder.show();
                        break;
                    }
                }
            });
        }
    }

    private void setVisibleForSender(MessageViewHolder messageViewHolder) {
        messageViewHolder.messageSenderPicture.setVisibility(View.VISIBLE);
        messageViewHolder.senderMessageTimeImage.setVisibility(View.VISIBLE);
    }
    private void setVisibleForReceiver(MessageViewHolder messageViewHolder) {
        messageViewHolder.receiverProfileImage.setVisibility(View.VISIBLE);
        messageViewHolder.messageReceiverPicture.setVisibility(View.VISIBLE);
        messageViewHolder.receiverMessageTimeImage.setVisibility(View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return userMessagesList.size();
    }

    private void deleteSentMessage(final int position, final MessageViewHolder holder) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("Messages")
                .child(userMessagesList.get(position).getFrom())
                .child(userMessagesList.get(position).getTo())
                .child(userMessagesList.get(position).getMessageID())
                .removeValue().addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                Toast.makeText(holder.itemView.getContext(),"Deleted Successfully.",Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(holder.itemView.getContext(),"Error Occurred.",Toast.LENGTH_SHORT).show();
            }
        });    }

    private void deleteReceiverMessage(final int position, final MessageViewHolder holder) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("Messages")
                .child(userMessagesList.get(position).getTo())
                .child(userMessagesList.get(position).getFrom())
                .child(userMessagesList.get(position).getMessageID())
                .removeValue().addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                Toast.makeText(holder.itemView.getContext(), "Deleted Successfully.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(holder.itemView.getContext(),"Error Occurred.",Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteMessageForEveryOne(final int position, final MessageViewHolder holder) {
        final DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("Messages")
                .child(userMessagesList.get(position).getTo())
                .child(userMessagesList.get(position).getFrom())
                .child(userMessagesList.get(position).getMessageID())
                .removeValue().addOnCompleteListener(task -> { // delete sms with name file from sender messages
            if(task.isSuccessful()) {
                rootRef.child("Messages")
                        .child(userMessagesList.get(position).getFrom())
                        .child(userMessagesList.get(position).getTo())
                        .child(userMessagesList.get(position).getMessageID())
                        .removeValue().addOnCompleteListener(task1 -> { //delete sms with name file from receive messages
                    if(task1.isSuccessful()) {
                        String fileNameForDownload = userMessagesList.get(position).getMessageID() + "." + userMessagesList.get(position).getType(); //name file form storage
                        storageReference = FirebaseStorage.getInstance().getReference();
                        storageReference.child("Document Files").child(fileNameForDownload)
                                .delete().addOnCompleteListener(task2 -> { //delete file from storage
                        if (task2.isSuccessful()) {
                            Toast.makeText(holder.itemView.getContext(),"Deleted Successfully.",Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            } else {
                Toast.makeText(holder.itemView.getContext(),"Error Occurred.",Toast.LENGTH_SHORT).show();
            }
        });
    }
}
