package com.example.g.blankspace;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CameraActivity extends Activity implements SurfaceHolder.Callback, SensorEventListener {

    public static final String EXTRA_CAMERA_DATA = "camera_data";

    private static final String KEY_IS_CAPTURING = "is_capturing";

    private static int counter;

    private DatabaseReference database;

    private Camera mCamera;
    private ImageView mCameraImage;
    private ImageView mCameraTriangle;
    private TextView mInstruction;
    private SurfaceView mCameraPreview;
    private boolean mIsCapturing;

    public SensorManager sManager;

    float Rot[] = null; //for gravity rotational data
    //don't use R because android uses that for other stuff
    float I[] = null; //for magnetic rotational data
    float accels[] = new float[3];
    float mags[] = new float[3];
    float[] values = new float[3];

    float azimuth;
    float pitch;
    float roll;

    float azimuthData;
    float pitchData;
    float rollData;

    private double lensHeight;
    private double pitchBottom;
    private double pitchTop;
    private double azimuthLeft;
    private double azimuthRight;

    private double objectDistance;
    private double objectHeight;
    private double objectWidth;
    private double objectDepth;
    private double objectVolume;


    private String type;
    private String itemID;
    private String flightID;
    private boolean stackable;
    private boolean tiltable;


    private static final String CUBOID_TYPE = "Cuboid";
    private static final String CYLINDRICAL_TYPE = "Cylindrical";

    private OnClickListener mDoneButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {

            blink(v);
            CameraActivity.counter++;
            azimuthData = azimuth;
            pitchData = pitch;
            rollData = roll;
            Log.d("Counter", String.valueOf(CameraActivity.counter));
            Log.d("Data ", String.valueOf(azimuthData) + " " + String.valueOf(pitchData) + " " + String.valueOf(rollData));
            switch (counter) {
                case 1:
                    pitchBottom = pitch;
                    mInstruction.setText("Align with top of object");

                    break;
                case 2:
                    pitchTop = pitch;
                    mInstruction.setText("Point at bottom left of object");

                    break;
                case 3:
                    azimuthLeft = azimuth;
                    mInstruction.setText("Point at bottom right of object");

                    break;
                case 4:
                    azimuthRight = azimuth;
                    MeasureDimension(lensHeight, pitchBottom, pitchTop, azimuthLeft, azimuthRight);
//                    System.out.println(String.format("%.2f", objectHeight));
                    Log.d("Result ", String.valueOf(objectDistance) + " " + String.format("%.2f", objectHeight) + " " + String.format("%.2f", objectWidth));
                    AlertDialog.Builder builder;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        builder = new AlertDialog.Builder(CameraActivity.this, android.R.style.Theme_Material_Dialog_Alert);
                    } else {
                        builder = new AlertDialog.Builder(CameraActivity.this);
                    }

                    if (type.equals(CUBOID_TYPE)) {
                        mInstruction.setText("Align with bottom of object");
                        CameraActivity.counter = 0;

                        if (objectDepth == 0) {
                            builder.setTitle("Action")
                                    .setMessage("Move either to the left/right side of object")
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            // continue with delete
                                        }
                                    })
                                    .show();
                        } else {
                            objectVolume = objectDepth * objectHeight * objectWidth;
                            builder.setTitle("Result")
                                    .setMessage("Height: " + String.format("%.2f", objectHeight * 100) + "cm \nWidth: " + String.format("%.2f", objectWidth * 100) + "cm \nDepth: " + String.format("%.2f", objectDepth * 100) + "cm \nVolume: " + String.format("%.2f", objectVolume * 1000000) + "cm3")
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            launchIntent();
                                        }
                                    })
                                    .show();

                            System.out.println(flightID);
                            System.out.println(itemID);
                            database.child(flightID).child(itemID).child("objectType").setValue("cuboid");
                            database.child(flightID).child(itemID).child("tiltable").setValue(tiltable);
                            database.child(flightID).child(itemID).child("stackable").setValue(stackable);
                            database.child(flightID).child(itemID).child("dimensions").setValue(String.format("%.2f",objectHeight*100)+"*"+ String.format("%.2f",objectWidth*100) +"*"+ String.format("%.2f",objectDepth*100));
                        }
                        break;
                    } else if (type.equals(CYLINDRICAL_TYPE)) {
                        objectVolume = objectHeight * Math.pow((objectWidth / 2), 2) * Math.PI;
                        builder.setTitle("Result")
                                .setMessage("Height: " + String.format("%.2f", objectHeight * 100) + "cm \nWidth: " + String.format("%.2f", objectWidth * 100) + "cm \nVolume: " + String.format("%.2f", objectVolume * 1000000) + "cm3")
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        launchIntent();
                                    }
                                })
                                .show();
                        database.child(flightID).child(itemID).child("objectType").setValue("cylindrical");
                        database.child(flightID).child(itemID).child("tiltable").setValue(tiltable);
                        database.child(flightID).child(itemID).child("stackable").setValue(stackable);
                        database.child(flightID).child(itemID).child("dimensions").setValue(String.format("%.2f",objectHeight*100)+","+ String.format("%.2f",objectWidth*100));



                    }
                default:
                    mInstruction.setText("");
                    break;
            }

        }
    };

    private void launchIntent() {
        Intent it = new Intent(CameraActivity.this, MainActivity.class);
        counter = 0;
        startActivity(it);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        database = FirebaseDatabase.getInstance().getReference();

        type = getIntent().getExtras().getString("Type");
        itemID = getIntent().getExtras().getString("itemID");
        flightID = getIntent().getExtras().getString("flightID");
        stackable = getIntent().getExtras().getBoolean("stackable");
        tiltable = getIntent().getExtras().getBoolean("tiltable");

        lensHeight = 1.61;
        if (getIntent().getExtras().getString("LensHeight") != null) {
            lensHeight = Double.parseDouble(getIntent().getExtras().getString("LensHeight"));
        }
        System.out.println(lensHeight);
        objectWidth = 0;
        sManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        setContentView(R.layout.activity_camera);
        mCameraTriangle = (ImageView) findViewById(R.id.arrow_drop_up);
        mCameraImage = (ImageView) findViewById(R.id.camera_image_view);
        mCameraImage.setVisibility(View.INVISIBLE);
        mInstruction = (TextView) findViewById(R.id.instruction);
        mInstruction.setText("Align with bottom of object");

        mCameraPreview = (SurfaceView) findViewById(R.id.preview_view);
        final SurfaceHolder surfaceHolder = mCameraPreview.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        final Button nextButton = (Button) findViewById(R.id.next_button);
        nextButton.setOnClickListener(mDoneButtonClickListener);
        mIsCapturing = true;
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putBoolean(KEY_IS_CAPTURING, mIsCapturing);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sManager.registerListener(this, sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        sManager.registerListener(this, sManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);

        if (mCamera == null) {
            try {
                mCamera = Camera.open();
                mCamera.setPreviewDisplay(mCameraPreview.getHolder());
                if (mIsCapturing) {
                    mCamera.setDisplayOrientation(90);
                    mCamera.startPreview();
                }
            } catch (Exception e) {
                Toast.makeText(CameraActivity.this, "Unable to open camera.", Toast.LENGTH_LONG)
                        .show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mCamera != null) {
            try {
                mCamera.setPreviewDisplay(holder);
                if (mIsCapturing) {
                    mCamera.startPreview();
                }
            } catch (IOException e) {
                Toast.makeText(CameraActivity.this, "Unable to start camera preview.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    public void blink(View view) {
        SurfaceView image = (SurfaceView) findViewById(R.id.preview_view);
        Animation animation1 =
                AnimationUtils.loadAnimation(getApplicationContext(),
                        R.anim.blink);
        image.startAnimation(animation1);
    }

    public void fade(View view) {
        SurfaceView image = (SurfaceView) findViewById(R.id.preview_view);
        Animation animation1 =
                AnimationUtils.loadAnimation(getApplicationContext(),
                        R.anim.fade);
        image.startAnimation(animation1);
    }


    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_MAGNETIC_FIELD:
                mags = event.values.clone();
                break;
            case Sensor.TYPE_ACCELEROMETER:
                accels = event.values.clone();
                break;
        }

        if (mags != null && accels != null) {
            Rot = new float[9];
            I = new float[9];
            SensorManager.getRotationMatrix(Rot, I, accels, mags);
            // Correct if screen is in Landscape

            float[] outR = new float[9];
            SensorManager.remapCoordinateSystem(Rot, SensorManager.AXIS_X, SensorManager.AXIS_Z, outR);
            SensorManager.getOrientation(outR, values);

            azimuth = values[0] * 57.2957795f; //looks like we don't need this one
            pitch = values[1] * 57.2957795f;
            roll = values[2] * 57.2957795f;
            mags = null; //retrigger the loop when things are repopulated
            accels = null; ////retrigger the loop when things are repopulated
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public double radianFromDeg(double deg) {
        return (deg / 180) * Math.PI;
    }

    public double getOppFromTan(double angle, double adj) {
        return adj * Math.tan(radianFromDeg(angle));
    }

    public void MeasureDimension(double lensHeight, double pitchBottom, double pitchTop, double azimuthLeft, double azimuthRight) {
        this.lensHeight = lensHeight;
        this.pitchBottom = pitchBottom;
        this.pitchTop = pitchTop;
        this.azimuthLeft = azimuthLeft;
        this.azimuthRight = azimuthRight;

        this.objectDistance = getOppFromTan(90 - pitchBottom, lensHeight);
        this.objectHeight = lensHeight - getOppFromTan(pitchTop, objectDistance);
        //this.objectDistance = lensHeight * Math.tan(((90 - pitchBottom)/180) * Math.PI);
        //this.objectHeight = lensHeight - (objectDistance * Math.tan((pitchTop / 180) * Math.PI));

        System.out.println("Lens Height: " + lensHeight);
        System.out.println(pitchBottom);
        System.out.println(pitchTop);
        System.out.println(azimuthLeft);
        System.out.println(azimuthRight);

        double a = (azimuthRight - azimuthLeft);
        if (a > 180) {
            a -= 360;
        } else if (a < -180) {
            a += 360;
        }
        if (this.objectWidth == 0) {
            this.objectWidth = 2 * getOppFromTan(a / 2, objectDistance);
        } else {
            this.objectDepth = 2 * getOppFromTan(a / 2, objectDistance);
        }
        System.out.println(this.objectWidth);
        System.out.println(this.objectHeight);
        System.out.println(this.objectDepth);
    }


}

