package com.amdc.firebasetest;

import androidx.appcompat.app.AppCompatActivity;

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

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Objects;

import static android.os.Environment.DIRECTORY_DOWNLOADS;

public class FilesViewerActivity extends AppCompatActivity {
    StorageReference storageReference, reference;
    ProgressBar progressBar;
    WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_files_viewer);

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        String fileName = getIntent().getStringExtra("fileName");
        String fileNameForDownload = getIntent().getStringExtra("fileNameForDownload");
        download(fileNameForDownload, fileName);

    }

    private void download(String fileNameForDownload, String fileName) {
        storageReference = FirebaseStorage.getInstance().getReference();
        reference = storageReference.child("Document Files").child(fileNameForDownload);
        reference.getDownloadUrl().addOnSuccessListener(uri -> {
            DownloadManager downloadManager = (DownloadManager) FilesViewerActivity.this.getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalFilesDir(FilesViewerActivity.this, DIRECTORY_DOWNLOADS, fileName);
            Objects.requireNonNull(downloadManager).enqueue(request);
            lookingFile(uri);
        }).addOnFailureListener(e -> Toast.makeText(this, "Error download", Toast.LENGTH_SHORT).show());
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void lookingFile(Uri uri) {
        progressBar.setVisibility(View.VISIBLE);
        Toast.makeText(FilesViewerActivity.this, uri.toString(), Toast.LENGTH_LONG).show();
        String finalURL = "https://drive.google.com/viewerng/viewer?embedded=true&url=" + uri;
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


//
//
////        String imageUrl =  getIntent().getStringExtra("url");
////        Uri uri = Uri.parse(getIntent().getStringExtra("url"));
////        Toast.makeText(FilesViewerActivity.this, uri.toString(), Toast.LENGTH_LONG).show();
//        String file = getIntent().getStringExtra("url");
////        Uri uri = Uri.parse("https://firebasestorage.googleapis.com/v0/b/testbase-def93.appspot.com/o/Document%20Files%2F-M4_GQJv-IpdmhC17GBm.pdf?alt=media&token=666eefe8-4124-4940-8786-6fd39d866d69");
//        lookingFile(file);
//    }
//
////    private void download() {
//////        storageReference = FirebaseStorage.getInstance().getReference();
//////        reference = storageReference.child("Document Files").child(uri);
////        reference.getDownloadUrl().addOnSuccessListener(uri -> {
////            DownloadManager downloadManager = (DownloadManager) FilesViewerActivity.this.getSystemService(Context.DOWNLOAD_SERVICE);
////            DownloadManager.Request request = new DownloadManager.Request(uri);
////            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
////            request.setDestinationInExternalFilesDir(FilesViewerActivity.this, DIRECTORY_DOWNLOADS, reference.getName());
////            downloadManager.enqueue(request);
////            lookingFile(uri);
////        }).addOnFailureListener(e -> {
////            Toast.makeText(this, "Error download", Toast.LENGTH_SHORT).show();
////        });
////    }
//
//    @SuppressLint("SetJavaScriptEnabled")
//    private void lookingFile(String file) {
////        Uri uri = Uri.parse(file);
//        Toast.makeText(FilesViewerActivity.this, file, Toast.LENGTH_SHORT).show();
//        progressBar.setVisibility(View.VISIBLE);
//        String finalURL = "https://drive.google.com/viewerng/viewer?embedded=true&url=" + file;
////        String finalURL = "https://stackoverflow.com/questions/8017374/how-to-pass-a-uri-to-an-intent";
//        webView.getSettings().setJavaScriptEnabled(true);
//        webView.getSettings().setBuiltInZoomControls(true);
//        webView.setWebChromeClient(new WebChromeClient() {
//            @Override
//            public void onProgressChanged(WebView view, int newProgress) {
//                super.onProgressChanged(view, newProgress);
//                getSupportActionBar().setTitle("Loading...");
//                if (newProgress == 100) { //indicated that loading is complite
//                    progressBar.setVisibility(View.GONE);
//                    getSupportActionBar().setTitle(Html.fromHtml("<font color='#F3FB00'>" + "File Viewer" + "</font>"));
//                }
//            }
//        });
//        webView.loadUrl(finalURL);
//    }
//}


//        webView = findViewById(R.id.webView);
//        progressBar = findViewById(R.id.progressBar);
//        progressBar.setVisibility(View.GONE);
//        String imageUrl = getIntent().getStringExtra("url");
//        progressBar.setVisibility(View.VISIBLE);
//        String finalURL = "https://drive.google.com/viewerng/viewer?embedded=true&url=" + imageUrl;
//        webView.getSettings().setJavaScriptEnabled(true);
//        webView.getSettings().setBuiltInZoomControls(true);
//        webView.setWebChromeClient(new WebChromeClient(){
//            @Override
//            public void onProgressChanged(WebView view, int newProgress) {
//                super.onProgressChanged(view, newProgress);
//                getSupportActionBar().setTitle("Loading...");
//                if (newProgress == 100) { //indicated that loading is complite
//                    progressBar.setVisibility(View.GONE);
//                    getSupportActionBar().setTitle(R.string.app_name);
//                }
//            }
//        });
//        webView.loadUrl(finalURL);
//
//    }



//    @SuppressLint("SetJavaScriptEnabled")
//    void lookingFile(Uri uri){
//        progressBar.setVisibility(View.VISIBLE);
////        Toast.makeText(this, uri, Toast.LENGTH_LONG).show();
//        String finalURL = "https://drive.google.com/viewerng/viewer?embedded=true&url=" + uri;
//        webView.getSettings().setJavaScriptEnabled(true);
//        webView.getSettings().setBuiltInZoomControls(true);
//        webView.setWebChromeClient(new WebChromeClient(){
//            @Override
//            public void onProgressChanged(WebView view, int newProgress) {
//                super.onProgressChanged(view, newProgress);
//                getSupportActionBar().setTitle("Loading...");
//                if (newProgress == 100) { //indicated that loading is complite
//                    progressBar.setVisibility(View.GONE);
//                    getSupportActionBar().setTitle(R.string.app_name);
//                }
//            }
//        });
//        webView.loadUrl(finalURL);
//    }
//}
