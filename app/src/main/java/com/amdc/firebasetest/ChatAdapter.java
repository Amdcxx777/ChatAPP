package com.amdc.firebasetest;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.ClipboardManager;
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

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {
    private List<Messages> userMessagesList;
    private String receiverImage;
    private StorageReference storageReference, reference;

    ChatAdapter(List<Messages> userMessagesList) {
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
        return new MessageViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull final MessageViewHolder messageViewHolder, final int position) {
        Context context = messageViewHolder.itemView.getContext();
        String messageSenderId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        Messages messages = userMessagesList.get(position);
        String fromUserID = messages.getFrom();
        String fromMessageType = messages.getType();
        DatabaseReference usersFromRef = FirebaseDatabase.getInstance().getReference().child("Users").child(fromUserID);
        usersFromRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild("image")) {
                    receiverImage = Objects.requireNonNull(dataSnapshot.child("image").getValue()).toString();
                    Picasso.get().load(receiverImage).resize(90, 90).placeholder(R.drawable.profile_image).into(messageViewHolder.receiverProfileImage);
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
        String imagePDF = "https://firebasestorage.googleapis.com/v0/b/testbase-def93.appspot.com/o/Image%20Files%2Ficon_pdf.png?alt=media&token=3deeefdf-472c-4fc8-ba70-dee1f444e54e";
        String imageXML = "https://firebasestorage.googleapis.com/v0/b/testbase-def93.appspot.com/o/Image%20Files%2Ficon_excel.png?alt=media&token=50cb92eb-acb0-4b63-8fcc-8237d7cea5b7";
        String imageDOC = "https://firebasestorage.googleapis.com/v0/b/testbase-def93.appspot.com/o/Image%20Files%2Ficon_world.png?alt=media&token=01a2ea3f-7df4-4b7c-86a4-ff9aded94a40";
        String imageZIP = "https://firebasestorage.googleapis.com/v0/b/testbase-def93.appspot.com/o/Image%20Files%2Ficon_zip.png?alt=media&token=d109ece1-1bef-4c12-9f6f-c987e8d368ab";
        switch (fromMessageType) {
            case "text":
                if (fromUserID.equals(messageSenderId)) {
                    messageViewHolder.senderMessageText.setVisibility(View.VISIBLE);
                    messageViewHolder.senderMessageTime.setVisibility(View.VISIBLE);

                    messageViewHolder.senderMessageText.setBackgroundResource(R.drawable.receiver_messages_layout); // receiver_messages_layout
                    messageViewHolder.senderMessageText.setText(messages.getMessage());
                    messageViewHolder.senderMessageTime.setText(messages.getTime() + " - " + messages.getDate());
                } else {
                    messageViewHolder.receiverProfileImage.setVisibility(View.VISIBLE);
                    messageViewHolder.receiverMessageText.setVisibility(View.VISIBLE);
                    messageViewHolder.receiverMessageTime.setVisibility(View.VISIBLE);
                    messageViewHolder.receiverMessageText.setBackgroundResource(R.drawable.sender_messages_layout); // sender_messages_layout
                    messageViewHolder.receiverMessageText.setText(messages.getMessage());
                    messageViewHolder.receiverMessageTime.setText(messages.getTime() + " - " + messages.getDate());
                } break;
            case "image":
                if (fromUserID.equals(messageSenderId)) { setVisibleForSender(messageViewHolder);
                    Picasso.get().load(messages.getMessage()).resize(120, 120).into(messageViewHolder.messageSenderPicture);
                    messageViewHolder.senderMessageTimeImage.setText(messages.getTime() + " - " + messages.getDate());
                } else { setVisibleForReceiver(messageViewHolder);
                    Picasso.get().load(messages.getMessage()).resize(120, 120).into(messageViewHolder.messageReceiverPicture);
                    messageViewHolder.receiverMessageTimeImage.setText(messages.getTime() + " - " + messages.getDate());
                } break;
            case "pdf":
                if (fromUserID.equals(messageSenderId)) { setVisibleForSender(messageViewHolder);
                    Picasso.get().load(imagePDF).resize(120, 120).into(messageViewHolder.messageSenderPicture);
                    messageViewHolder.senderMessageTimeImage.setText(messages.getTime() + " - " + messages.getDate());
                } else { setVisibleForReceiver(messageViewHolder);
                    Picasso.get().load(imagePDF).resize(120, 120).into(messageViewHolder.messageReceiverPicture);
                    messageViewHolder.receiverMessageTimeImage.setText(messages.getTime() + " - " + messages.getDate());
                } break;
            case "xls":
            case "xlsx":
                if (fromUserID.equals(messageSenderId)) { setVisibleForSender(messageViewHolder);
                    Picasso.get().load(imageXML).resize(120, 120).into(messageViewHolder.messageSenderPicture);
                    messageViewHolder.senderMessageTimeImage.setText(messages.getTime() + " - " + messages.getDate());
                } else { setVisibleForReceiver(messageViewHolder);
                    Picasso.get().load(imageXML).resize(120, 120).into(messageViewHolder.messageReceiverPicture);
                    messageViewHolder.receiverMessageTimeImage.setText(messages.getTime() + " - " + messages.getDate());
                } break;
            case "doc":
            case "docx":
                if (fromUserID.equals(messageSenderId)) { setVisibleForSender(messageViewHolder);
                    Picasso.get().load(imageDOC).resize(120, 120).into(messageViewHolder.messageSenderPicture);
                    messageViewHolder.senderMessageTimeImage.setText(messages.getTime() + " - " + messages.getDate());
                } else { setVisibleForReceiver(messageViewHolder);
                    Picasso.get().load(imageDOC).resize(120, 120).into(messageViewHolder.messageReceiverPicture);
                    messageViewHolder.receiverMessageTimeImage.setText(messages.getTime() + " - " + messages.getDate());
                } break;
            case "zip":
                if (fromUserID.equals(messageSenderId)) { setVisibleForSender(messageViewHolder);
                    Picasso.get().load(imageZIP).resize(120, 120).into(messageViewHolder.messageSenderPicture);
                    messageViewHolder.senderMessageTimeImage.setText(messages.getTime() + " - " + messages.getDate());
                } else { setVisibleForReceiver(messageViewHolder);
                    Picasso.get().load(imageZIP).resize(120, 120).into(messageViewHolder.messageReceiverPicture);
                    messageViewHolder.receiverMessageTimeImage.setText(messages.getTime() + " - " + messages.getDate());
                } break;
        }
        if (fromUserID.equals(messageSenderId)) { // alert dialog for sender user
            messageViewHolder.itemView.setOnClickListener(view -> {
                switch (userMessagesList.get(position).getType()) {
                    case "pdf":
                    case "zip":
                    case "xls":
                    case "xlsx":
                    case "doc":
                    case "docx": {
                        CharSequence[] options = new CharSequence[] {"Download and View this Document", "Download this Document", "Delete from Everywhere", "Delete only from Me", "Cancel"};
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle("Download/Delete Message").setIcon(R.drawable.file);
                        builder.setItems(options, (dialogInterface, i) -> {
                            if (i == 0) {
                                String fileNameForDownload = userMessagesList.get(position).getMessageID() + "." + userMessagesList.get(position).getType(); //get name file from storage
                                String fileName = Uri.parse(userMessagesList.get(position).getName()).toString(); //get name file from user into firebase (getLastPathSegment())
                                Intent intent = new Intent(context, ViewerFilesActivity.class);
                                intent.putExtra("fileNameForDownload", fileNameForDownload);
                                intent.putExtra("fileName", fileName);
                                context.startActivity(intent);
                            } else if (i == 1) { // download file
                                String fileNameForDownload = userMessagesList.get(position).getMessageID() + "." + userMessagesList.get(position).getType(); //get name file from storage
                                storageReference = FirebaseStorage.getInstance().getReference();
                                reference = storageReference.child("Document Files").child(fileNameForDownload);
                                reference.getDownloadUrl().addOnSuccessListener(uri -> {
                                    String fileName = Uri.parse(userMessagesList.get(position).getName()).toString(); //get name file from user into firebase (getLastPathSegment())
                                    DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                                    DownloadManager.Request request = new DownloadManager.Request(uri);
                                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                                    request.setDestinationInExternalFilesDir(context, DIRECTORY_DOWNLOADS, fileName); //reference.getName()
                                    Objects.requireNonNull(downloadManager).enqueue(request);
                                }).addOnFailureListener(e -> Toast.makeText(context, "Error download", Toast.LENGTH_SHORT).show());
                            } if (i == 2) { deleteMessageForEveryOne(position, messageViewHolder);
                            } if (i == 3) { deleteSentMessage(position, messageViewHolder);
                            }
                        });
                        builder.show();
                        break;
                    }
                    case "text": { // sender messages for text
                        CharSequence[] options = new CharSequence[]{"Copy text message", "Delete from Everywhere", "Delete only from Me", "Cancel"};
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle("Copy/Delete Message").setIcon(R.drawable.delete);
                        builder.setItems(options, (dialogInterface, i) -> {
                            if (i == 0) {
                                ((ClipboardManager) Objects.requireNonNull(context.getSystemService(Context.CLIPBOARD_SERVICE))).setText(messages.getMessage());
                                Toast.makeText(context,"Copied to clipboard",Toast.LENGTH_SHORT).show();
                            } if (i == 1) { deleteMessageForEveryOne(position, messageViewHolder);
                            } if (i == 2) { deleteSentMessage(position, messageViewHolder); }
                        });
                        builder.show();
                        break;
                    }
                    case "image": { // sender messages for image
                        CharSequence[] options = new CharSequence[]{"View this Image", "Delete from Everywhere", "Delete only from Me", "Cancel"};
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle("View/Delete Message").setIcon(R.drawable.file);
                        builder.setItems(options, (dialogInterface, i) -> {
                            if (i == 0) {
                                Intent intent = new Intent(context, ViewerImagesActivity.class);
                                intent.putExtra("url", userMessagesList.get(position).getMessage());
                                context.startActivity(intent);
                            } if (i == 1) { deleteMessageForEveryOne(position, messageViewHolder);
                            } if (i == 2) { deleteSentMessage(position, messageViewHolder); }
                        });
                        builder.show();
                        break;
                    }
                }
            });
        } else { // alert dialog for receiver user
            messageViewHolder.itemView.setOnClickListener(view -> {
                switch (userMessagesList.get(position).getType()) {
                    case "pdf":
                    case "zip":
                    case "xls":
                    case "xlsx":
                    case "doc":
                    case "docx": {
                        CharSequence[] options = new CharSequence[]{"Download and View this Document", "Download this Document", "Delete only from Me", "Cancel"};
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle("Download/Delete Message").setIcon(R.drawable.file);
                        builder.setItems(options, (dialogInterface, i) -> {
                            if (i == 0) {
                                String fileNameForDownload = userMessagesList.get(position).getMessageID() + "." + userMessagesList.get(position).getType(); //get name file from storage
                                String fileName = Uri.parse(userMessagesList.get(position).getName()).toString(); //get name file from user into firebase (getLastPathSegment())
                                Intent intent = new Intent(context, ViewerFilesActivity.class);
                                intent.putExtra("fileNameForDownload", fileNameForDownload);
                                intent.putExtra("fileName", fileName);
                                context.startActivity(intent);
                            } if (i == 1) {
                                String fileNameForDownload = userMessagesList.get(position).getMessageID() + "." + userMessagesList.get(position).getType();
                                storageReference = FirebaseStorage.getInstance().getReference();
                                reference = storageReference.child("Document Files").child(fileNameForDownload);
                                reference.getDownloadUrl().addOnSuccessListener(uri -> {
                                    String fileName = Uri.parse(userMessagesList.get(position).getName()).toString(); //get name file from firebase (getLastPathSegment())
                                    DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                                    DownloadManager.Request request = new DownloadManager.Request(uri);
                                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                                    request.setDestinationInExternalFilesDir(context, DIRECTORY_DOWNLOADS, fileName); //reference.getName()
                                    Objects.requireNonNull(downloadManager).enqueue(request);
                                }).addOnFailureListener(e -> Toast.makeText(context, "Error download", Toast.LENGTH_SHORT).show());
                            } if (i == 2) { deleteReceiverMessage(position, messageViewHolder); }
                        });
                        builder.show();
                        break;
                    }
                    case "text": {
                        CharSequence[] options = new CharSequence[]{"Copy text message", "Delete from  Me", "Cancel"};
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle("Copy/Delete Message").setIcon(R.drawable.delete);
                        builder.setItems(options, (dialogInterface, i) -> {
                            if (i == 0) {
                                ((ClipboardManager) Objects.requireNonNull(context.getSystemService(Context.CLIPBOARD_SERVICE))).setText(messages.getMessage());
                                Toast.makeText(context,"Copied to clipboard",Toast.LENGTH_SHORT).show();
                            } if (i == 1) { deleteReceiverMessage(position, messageViewHolder); }
                        });
                        builder.show();
                        break;
                    }
                    case "image": {
                        CharSequence[] options = new CharSequence[]{"View this Image", "Download this Image", "Delete from Me", "Cancel"};
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle("View/Delete Message").setIcon(R.drawable.file);
                        builder.setItems(options, (dialogInterface, i) -> {
                            if (i == 0) {
                                Intent intent = new Intent(context, ViewerImagesActivity.class);
                                intent.putExtra("url", userMessagesList.get(position).getMessage());
                                context.startActivity(intent);
                            } if (i == 1) {
                                String fileNameForDownload = userMessagesList.get(position).getMessageID() + ".jpg";
                                storageReference = FirebaseStorage.getInstance().getReference();
                                reference = storageReference.child("Image Files").child(fileNameForDownload);
                                reference.getDownloadUrl().addOnSuccessListener(uri -> {
                                    String fileName = Uri.parse(userMessagesList.get(position).getName()).toString(); //get name file from firebase (getLastPathSegment())
                                    DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                                    DownloadManager.Request request = new DownloadManager.Request(uri);
                                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                                    request.setDestinationInExternalFilesDir(context, DIRECTORY_DOWNLOADS, fileName); //reference.getName()
                                    Objects.requireNonNull(downloadManager).enqueue(request);
                                }).addOnFailureListener(e -> Toast.makeText(context, "Error download", Toast.LENGTH_SHORT).show());
                            } if (i == 2) {
                                deleteReceiverMessage(position, messageViewHolder);
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
        rootRef.child("Messages").child(userMessagesList.get(position).getFrom())
                .child(userMessagesList.get(position).getTo())
                .child(userMessagesList.get(position).getMessageID())
                .removeValue().addOnCompleteListener(task -> {
            if(task.isSuccessful()) Toast.makeText(holder.itemView.getContext(),"Deleted Successfully.",Toast.LENGTH_SHORT).show();
            else Toast.makeText(holder.itemView.getContext(),"Error Occurred.",Toast.LENGTH_SHORT).show();
        });
    }

    private void deleteReceiverMessage(final int position, final MessageViewHolder holder) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("Messages").child(userMessagesList.get(position).getTo())
                .child(userMessagesList.get(position).getFrom())
                .child(userMessagesList.get(position).getMessageID())
                .removeValue().addOnCompleteListener(task -> {
            if(task.isSuccessful()) Toast.makeText(holder.itemView.getContext(), "Deleted Successfully.", Toast.LENGTH_SHORT).show();
            else Toast.makeText(holder.itemView.getContext(),"Error Occurred.",Toast.LENGTH_SHORT).show();
        });
    }

    private void deleteMessageForEveryOne(final int position, final MessageViewHolder holder) {
        final DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        final String type = userMessagesList.get(position).getType();
        final String messageID = userMessagesList.get(position).getMessageID();
        rootRef.child("Messages").child(userMessagesList.get(position).getTo())
                .child(userMessagesList.get(position).getFrom())
                .child(userMessagesList.get(position).getMessageID())
                .removeValue().addOnCompleteListener(task -> { // delete sms with name file from sender messages
            if(task.isSuccessful()) {
                rootRef.child("Messages").child(userMessagesList.get(position).getFrom())
                        .child(userMessagesList.get(position).getTo())
                        .child(userMessagesList.get(position).getMessageID())
                        .removeValue().addOnCompleteListener(task1 -> { //delete sms with name file from receive messages
                    if(task1.isSuccessful()) {
                        storageReference = FirebaseStorage.getInstance().getReference();
                        if (type.equals("text")) {
                            Toast.makeText(holder.itemView.getContext(), "Message Deleted Successfully.", Toast.LENGTH_SHORT).show();
                        } else {
                            if (type.equals("image")) { //for image
                                String fileNameForDownload = messageID + ".jpg"; //name file form storage
                                storageReference.child("Image Files").child(fileNameForDownload).delete().addOnCompleteListener(task2 -> {
                                    if (task2.isSuccessful())
                                        Toast.makeText(holder.itemView.getContext(), "Deleted Successfully.", Toast.LENGTH_SHORT).show();
                                });
                            } else { //for files
                                String fileNameForDownload = messageID + "." + type; //name file form storage
                                storageReference.child("Document Files").child(fileNameForDownload).delete().addOnCompleteListener(task2 -> { //delete file from storage
                                    if (task2.isSuccessful())
                                        Toast.makeText(holder.itemView.getContext(), "Deleted Successfully.", Toast.LENGTH_SHORT).show();
                                });
                            }
                        }
                    }
                });
            } else  Toast.makeText(holder.itemView.getContext(),"Error Occurred.",Toast.LENGTH_SHORT).show();
        });
    }
}
