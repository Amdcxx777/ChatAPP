package com.amdc.firebasetest;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Html;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.github.chrisbanes.photoview.PhotoView;
import com.squareup.picasso.Picasso;

import java.util.Objects;

public class ImageViewerActivity extends AppCompatActivity {

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); //full screen
        Objects.requireNonNull(getSupportActionBar()).setTitle(Html.fromHtml("<font color='#F3FB00'>" + "Image Viewer" + "</font>"));

        PhotoView imageView = findViewById(R.id.image_viewer);
        String imageUrl = getIntent().getStringExtra("url");
        Picasso.get().load(imageUrl).into(imageView);
    }
}
