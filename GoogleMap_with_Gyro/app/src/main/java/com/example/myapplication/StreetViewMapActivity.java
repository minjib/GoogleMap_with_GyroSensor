package com.example.myapplication;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.StreetViewPanoramaFragment;
import com.google.android.gms.maps.StreetViewPanoramaOptions;
import com.google.android.gms.maps.StreetViewPanoramaView;
import com.google.android.gms.maps.SupportStreetViewPanoramaFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.StreetViewPanoramaCamera;
import com.google.android.gms.maps.model.StreetViewPanoramaLocation;
import com.google.android.gms.maps.model.StreetViewPanoramaOrientation;
import com.google.android.gms.maps.model.StreetViewSource;

import java.util.Timer;
import java.util.TimerTask;

public class StreetViewMapActivity extends AppCompatActivity
        implements OnStreetViewPanoramaReadyCallback {

    private StreetViewPanorama mStreetViewPanorama;
    private boolean secondLocation = false;
    private StreetViewPanoramaCamera camera;
    private SensorManager mySenserManager;
    private SensorEventListener gyroListener;
    private Sensor myGyroscope;

    private double gyroX=0;
    private double gyroY=0;
    private double gyroZ=0;


    private double roll;
    private double pitch;
    private double yaw;

    private double timestamp = 0.0;
    private double dt;
    private Handler handler = new Handler();

    private double rad_to_dgr = 180 /Math.PI;
    private static final float NS2S = 1.0f/1000000000.0f;

    @Override
    public void onResume(){
        super.onResume();
        mySenserManager.registerListener(gyroListener, myGyroscope, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onPause(){
        super.onPause();
        mySenserManager.unregisterListener(gyroListener);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.e("LOG", "onDestroy()");
        mySenserManager.unregisterListener(gyroListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_street_view_map);

        mySenserManager =(SensorManager) getSystemService(Context.SENSOR_SERVICE);
        myGyroscope = mySenserManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        SupportStreetViewPanoramaFragment streetViewFragment =
                (SupportStreetViewPanoramaFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.googleMapStreetView);
        streetViewFragment.getStreetViewPanoramaAsync(this);
        gyroListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                gyroX = event.values[0];
                gyroY = event.values[1];
                gyroZ = event.values[2];

                dt = (event.timestamp - timestamp)*NS2S;
                timestamp = event.timestamp;

                if (dt-timestamp*NS2S!=0) {
                    pitch = pitch + gyroY*dt;
                    roll = roll + gyroX*dt;
                    yaw = yaw+gyroZ*dt;
/*
                    Log.e("LOG", "GYROSCOPE           [X]:" + String.format("%.4f", event.values[0])
                            + "           [Y]:" + String.format("%.4f", event.values[1])
                            + "           [Z]:" + String.format("%.4f", event.values[2])
                            + "           [Pitch]: " + String.format("%.1f", pitch*rad_to_dgr)
                            + "           [Roll]: " + String.format("%.1f", roll*rad_to_dgr)
                            + "           [Yaw]: " + String.format("%.1f", yaw*rad_to_dgr)
                            + "           [dt]: " + String.format("%.4f", dt));*/
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
    }
    private static final int PAN_BY = 30;
    @Override
    public void onStreetViewPanoramaReady(StreetViewPanorama streetViewPanorama) {
        mStreetViewPanorama = streetViewPanorama;
        if (secondLocation) {
            streetViewPanorama.setPosition(new LatLng(51.52887, -0.1726073), StreetViewSource.OUTDOOR);
        } else {
            streetViewPanorama.setPosition(new LatLng(51.52887, -0.1726073));
        }
        streetViewPanorama.setStreetNamesEnabled(true);
        streetViewPanorama.setPanningGesturesEnabled(true);
        streetViewPanorama.setZoomGesturesEnabled(true);
        streetViewPanorama.setUserNavigationEnabled(true);
        camera = new StreetViewPanoramaCamera.Builder()
                .zoom(mStreetViewPanorama.getPanoramaCamera().zoom)
                .tilt(mStreetViewPanorama.getPanoramaCamera().tilt)
                .bearing(mStreetViewPanorama.getPanoramaCamera().bearing)
                .build();

        StreetViewPanoramaCamera previous = mStreetViewPanorama.getPanoramaCamera();
        float tilt = mStreetViewPanorama.getPanoramaCamera().tilt + 30;
        tilt = (tilt > 90) ? 90 : tilt;

        camera = new StreetViewPanoramaCamera.Builder(previous)
                .tilt(tilt)
                .build();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                long duration = 1000;
                float tilt = mStreetViewPanorama.getPanoramaCamera().tilt + (float)(gyroX*rad_to_dgr);
                tilt = (tilt > 90) ? 90 : tilt;
                tilt = (tilt < -90) ? -90 : tilt;
                StreetViewPanoramaCamera camera =
                        new StreetViewPanoramaCamera.Builder()
                                .zoom(mStreetViewPanorama.getPanoramaCamera().zoom)
                                .tilt(tilt)
                                .bearing(mStreetViewPanorama.getPanoramaCamera().bearing  - (float)(gyroY*rad_to_dgr))
                                .build();
                mStreetViewPanorama.animateTo(camera, duration);
                handler.postDelayed(this, 100);
            }
        },1000);

    //mStreetViewPanorama.setOnStreetViewPanoramaCameraChangeListener(streetViewPanoramaCameraChangeListener);
;

        /*new Handler().postDelayed(new Runnable() {// 1 초 후에 실행
            @Override public void run() {
                // 실행할 동작 코딩 mHandler.sendEmptyMessage(0);	// 실행이 끝난후 알림
            } }, 1000);*/
        //streetViewPanorama.setOnStreetViewPanoramaChangeListener(panoramaChangeListener);
        //mStreetViewPanorama.animateTo(camera, duration);
    }

    private  StreetViewPanorama.OnStreetViewPanoramaCameraChangeListener streetViewPanoramaCameraChangeListener = new StreetViewPanorama.OnStreetViewPanoramaCameraChangeListener() {
        @Override
        public void onStreetViewPanoramaCameraChange(StreetViewPanoramaCamera streetViewPanoramaCamera) {
            long duration = 1000;
            float tilt = mStreetViewPanorama.getPanoramaCamera().tilt + (float)(roll*rad_to_dgr);
            tilt = (tilt > 90) ? 90 : tilt;
            tilt = (tilt < -90) ? -90 : tilt;
            StreetViewPanoramaCamera camera =
                    new StreetViewPanoramaCamera.Builder()
                            .zoom(mStreetViewPanorama.getPanoramaCamera().zoom)
                            .tilt(tilt)
                            .bearing(mStreetViewPanorama.getPanoramaCamera().bearing - (float)(yaw*rad_to_dgr))
                            .build();

        }
    };

    private StreetViewPanorama.OnStreetViewPanoramaClickListener streetViewPanoramaClickListener = new StreetViewPanorama.OnStreetViewPanoramaClickListener() {
        @Override
        public void onStreetViewPanoramaClick(StreetViewPanoramaOrientation streetViewPanoramaOrientation) {
            Point point = mStreetViewPanorama.orientationToPoint(streetViewPanoramaOrientation);
            Log.e("LOG", "start");
            if (point != null) {
                Log.e("LOG", "notnull");
                mStreetViewPanorama.animateTo(
                        new StreetViewPanoramaCamera.Builder()
                                .orientation(streetViewPanoramaOrientation)
                                .zoom(mStreetViewPanorama.getPanoramaCamera().zoom)
                                .bearing(mStreetViewPanorama.getPanoramaCamera().bearing - 60)
                                .build(), 1000);
            }
        }
    };

    private StreetViewPanorama.OnStreetViewPanoramaChangeListener panoramaChangeListener =
            new StreetViewPanorama.OnStreetViewPanoramaChangeListener() {
                @Override
                public void onStreetViewPanoramaChange(
                        StreetViewPanoramaLocation streetViewPanoramaLocation) {


                    Toast.makeText(getApplicationContext(), "Lat: " + streetViewPanoramaLocation.position.latitude + " Lng: " + streetViewPanoramaLocation.position.longitude, Toast.LENGTH_SHORT).show();

                }
            };



}

