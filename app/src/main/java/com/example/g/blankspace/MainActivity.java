package com.example.g.blankspace;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int TAKE_PICTURE_REQUEST_B = 100;

    private ImageView mCameraImageView;
    private EditText flightNumber;
    private EditText itemNumber;
    private EditText numberOfPieces;
    private EditText lensHeight;
    private CheckBox stackable;
    private CheckBox tiltable;

    private String flightData;
    private String lensHeightData;
    private String itemData;
    private String piecesData;
    private boolean stackableData;
    private boolean tiltableData;

    private OnClickListener mCaptureImageButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            startImageCapture();
        }
    };

    private OnClickListener mMeasureCylindricalButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            startCylindricalMeasure();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        flightNumber = (EditText)findViewById(R.id.flight_id);
        itemNumber = (EditText)findViewById(R.id.item_id);
        lensHeight = (EditText)findViewById(R.id.lens_height);
        numberOfPieces = (EditText)findViewById(R.id.number_pieces);
        stackable = (CheckBox)findViewById(R.id.stackable);
        tiltable = (CheckBox)findViewById(R.id.tiltable);
        tiltableData = false;
        stackableData = false;

//        mCameraImageView = (ImageView) findViewById(R.id.camera_image_view);
//        mCameraImageView.setRotation(90);

        findViewById(R.id.measure_image_button).setOnClickListener(mCaptureImageButtonClickListener);
        findViewById(R.id.measure_cylindrical_button).setOnClickListener(mMeasureCylindricalButtonClickListener);

    }

    private void startImageCapture() {
        flightData = flightNumber.getText().toString();
        itemData = itemNumber.getText().toString();
        piecesData = numberOfPieces.getText().toString();
        stackableData = stackable.isChecked();
        tiltableData = tiltable.isChecked();
        System.out.println(flightData);
        System.out.println(tiltableData);
        Intent it = new Intent(MainActivity.this, CameraActivity.class);
        it.putExtra("Type", "Cuboid");
        it.putExtra("itemID", itemData);
        it.putExtra("flightID", flightData);
        it.putExtra("stackable", stackableData);
        it.putExtra("tiltable", tiltableData);
        it.putExtra("LensHeight", lensHeight.getText().toString());
        startActivity(it);
    }
    private void startCylindricalMeasure() {
        flightData = flightNumber.getText().toString();
        itemData = itemNumber.getText().toString();
        piecesData = numberOfPieces.getText().toString();

        stackableData = stackable.isChecked();
        tiltableData = tiltable.isChecked();
        System.out.println(flightData);
        System.out.println(tiltableData);
        Intent it = new Intent(MainActivity.this, CameraActivity.class);
        it.putExtra("Type", "Cylindrical");
        it.putExtra("itemID", itemData);
        it.putExtra("flightID", flightData);
        it.putExtra("stackable", stackableData);
        it.putExtra("tiltable", tiltableData);
        it.putExtra("LensHeight", lensHeight.getText().toString());

        startActivity(it);
    }
}