package com.amdc.firebasetest;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Objects;

import static android.os.Environment.DIRECTORY_DOWNLOADS;

public class ViewerFilesActivity extends AppCompatActivity {
    StorageReference storageReference, reference;
    ProgressBar progressBar;
    WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewer_files);

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); //full screen
        setSupportActionBar(findViewById(R.id.activity_file_toolbar)); // my toolbar
        Objects.requireNonNull(getSupportActionBar()).setTitle(Html.fromHtml("<font color='#F3FB00'>" + "File viewer" + "</font>"));
        String fileName = getIntent().getStringExtra("fileName");
        String fileNameForDownload = getIntent().getStringExtra("fileNameForDownload");
        download(fileNameForDownload, fileName);

    }

    private void download(String fileNameForDownload, String fileName) {
        storageReference = FirebaseStorage.getInstance().getReference();
        reference = storageReference.child("Document Files").child(fileNameForDownload);
        reference.getDownloadUrl().addOnSuccessListener(uri -> {
            DownloadManager downloadManager = (DownloadManager) ViewerFilesActivity.this.getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalFilesDir(ViewerFilesActivity.this, DIRECTORY_DOWNLOADS, fileName);
            Objects.requireNonNull(downloadManager).enqueue(request);
            lookingFile(uri);
        }).addOnFailureListener(e -> Toast.makeText(this, "Error download", Toast.LENGTH_SHORT).show());
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void lookingFile(Uri uri) {
        progressBar.setVisibility(View.VISIBLE);
        String finalURL = "https://drive.google.com/viewerng/viewer?embedded=true&url=" + uri; //https://drive.google.com/viewerng/viewer?embedded=true&url=  // https://docs.google.com/gview?key=YOUR_API_KEY&embedded=true&url=
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                Objects.requireNonNull(getSupportActionBar()).setTitle("Loading...");
                if (newProgress == 100) { //indicated that loading is complite
                    progressBar.setVisibility(View.GONE);
                    getSupportActionBar().setTitle(Html.fromHtml("<font color='#F3FB00'>" + "File Viewer" + "</font>"));
                }
            }
        });
        webView.loadUrl(finalURL);
    }
}