package com.amdc.firebasetest;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.firebase.storage.StorageReference;
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

//        String imageUrl =  getIntent().getStringExtra("url");
        Uri uri = Uri.parse(getIntent().getStringExtra("url"));
        Toast.makeText(FilesViewerActivity.this, uri.toString(), Toast.LENGTH_LONG).show();
        lookingFile(uri);
    }

//    private void download() {
////        storageReference = FirebaseStorage.getInstance().getReference();
////        reference = storageReference.child("Document Files").child(uri);
//        reference.getDownloadUrl().addOnSuccessListener(uri -> {
//            DownloadManager downloadManager = (DownloadManager) FilesViewerActivity.this.getSystemService(Context.DOWNLOAD_SERVICE);
//            DownloadManager.Request request = new DownloadManager.Request(uri);
//            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
//            request.setDestinationInExternalFilesDir(FilesViewerActivity.this, DIRECTORY_DOWNLOADS, reference.getName());
//            downloadManager.enqueue(request);
//            lookingFile(uri);
//        }).addOnFailureListener(e -> {
//            Toast.makeText(this, "Error download", Toast.LENGTH_SHORT).show();
//        });
//    }

    @SuppressLint("SetJavaScriptEnabled")
    private void lookingFile(Uri uri) {
        progressBar.setVisibility(View.VISIBLE);
        String finalURL = "https://drive.google.com/viewerng/viewer?embedded=true&url=" + uri;
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                getSupportActionBar().setTitle("Loading...");
                if (newProgress == 100) { //indicated that loading is complite
                    progressBar.setVisibility(View.GONE);
                    getSupportActionBar().setTitle(R.string.app_name);
                }
            }
        });
        webView.loadUrl(finalURL);
    }
}


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
