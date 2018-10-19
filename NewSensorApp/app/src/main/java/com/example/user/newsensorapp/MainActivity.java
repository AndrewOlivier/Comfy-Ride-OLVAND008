package com.example.user.newsensorapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;

import android.view.MenuItem;
import android.widget.Toast;


import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.PolylineOptions;

import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.datatype.Duration;

public class MainActivity extends AppCompatActivity{

    private static final String TAG = "MainActivity";

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;

    private Boolean mLocationPermissionsGranted = true;

    private SensorManager sensorManager;
    private Sensor Accel, Magno, Grav;

    Button buttonStart;
    Button buttonStop;
    Button buttonDuration;
    Button buttonRoad;
    boolean isRunning;

    private float[] gravityValues = null;
    private float[] magneticValues = null;

    private LocationManager locationManager;
    private LatLng prevPosition;
    private boolean isFirstLocation = true;

    private SensorManager sM;
    private Sensor acc, mgyro, mmag, mlight, mpres, mtemp, mhum;
    private int duration;
    private String road_type;
    private String Car;
    private long timestart;
    private long time;
    private double latitude;
    private double longitude;
    private float speed;
    private int anomaly;


    TextView xValue, yValue, zValue, lat, lon, speedlabel;
    FileWriter writer;
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        road_type = "";
        //writing =0;
        timestart = 0;
        anomaly =0;

        xValue = (TextView) findViewById(R.id.xValue);
        yValue = (TextView) findViewById(R.id.yValue);
        zValue = (TextView) findViewById(R.id.zValue);

        lat = (TextView) findViewById(R.id.lat);
        lon = (TextView) findViewById(R.id.lon);
        speedlabel = (TextView) findViewById(R.id.speed);

        buttonStart = (Button)findViewById(R.id.button);
        buttonDuration = (Button)findViewById(R.id.button3);
        buttonRoad = (Button)findViewById(R.id.button2);



        isRunning = false;


        getLocationPermission();

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Accel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        Magno = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        Grav = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        buttonStart.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(duration==0){
                    Toast.makeText(MainActivity.this,"Please Indicate Duration before Recording", Toast.LENGTH_SHORT).show();
                    return true;
                }else {
                    SelectedItem();
                    return true;
                }
            }
        });
        buttonDuration.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                    registerForContextMenu(view);
                    openContextMenu(view);
                return true;
            }
        });


        buttonRoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recording_done();
            }
        });

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        if(v.getId() == R.id.button3){
            menu.setHeaderTitle("Select Recording Duration");
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.activity_main, menu);}

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case(R.id.a):
                item.setChecked(true);
                Log.d(TAG, "a");
                buttonDuration.setText("Duration: 500");
                buttonDuration.setEnabled(false);
                duration = 500;
                break;
            case(R.id.b):
                item.setChecked(true);
                Log.d(TAG, "b");
                buttonDuration.setText("Duration: 50");
                buttonDuration.setEnabled(false);
                duration = 50;
                break;
            case(R.id.c):
                item.setChecked(true);
                Log.d(TAG, "c");
                buttonDuration.setText("Duration: 40");
                buttonDuration.setEnabled(false);
                duration = 40;
                break;
            case(R.id.d):
                item.setChecked(true);
                Log.d(TAG, "d");
                buttonDuration.setText("Duration: 30");
                buttonDuration.setEnabled(false);
                duration = 30;
                break;
            case(R.id.e):
                item.setChecked(true);
                Log.d(TAG, "e");
                buttonDuration.setText("Duration: 20");
                buttonDuration.setEnabled(false);
                duration = 20;
                break;
            case(R.id.f):
                item.setChecked(true);
                Log.d(TAG, "f");
                buttonDuration.setText("Duration: 10");
                buttonDuration.setEnabled(false);
                duration = 10;
                break;
            default:
                return super.onContextItemSelected(item);
        }
        return true;
    }

    public void SelectedItem(){
        buttonStart.setEnabled(false);
        buttonStart.setText("Recording...");
        timestart = System.currentTimeMillis();
        Log.d(TAG, "Writing to " + getStorageDir());
        try {
            writer = new FileWriter(new File(getStorageDir(), "sensors_" + System.currentTimeMillis() + ".csv"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        isRunning = true;

        int TIME = duration*1000; //5000 ms (5 Seconds)

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                recording_done();

            }
        }, TIME);

        }
        public void recording_done(){
            buttonStart.setEnabled(true);
            buttonDuration.setEnabled(true);

            buttonStart.setText("Start");
            buttonDuration.setText("Duration");

            Toast.makeText(MainActivity.this,duration+" Seconds of Data of "+ road_type+ " Road has been saved", Toast.LENGTH_SHORT).show();

            duration = 0;
            road_type="";
            timestart=0;

            isRunning = false;

            try {
                writer.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }



    private String getStorageDir() {
        return this.getExternalFilesDir(null).getAbsolutePath();
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean permissionGranted = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (permissionGranted) {
            Log.d(TAG, "onCreate: Permissions have been granted, starting Main Activity");
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);

        } else {
            Log.d(TAG, "onCreate: Permissions have not been granted, starting Intro Activity");
        }
        sensorManager.registerListener(listener, Accel, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(listener, Grav, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(listener, Magno, SensorManager.SENSOR_DELAY_FASTEST);

    }
    SensorEventListener listener = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            if(isRunning){
                try {
                    if ((gravityValues != null) && (magneticValues != null) && (sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION)) {
                        Log.d("Time Difference", ""+ (System.currentTimeMillis()-time));

                        float[] deviceRelativeAcceleration = new float[4];
                        deviceRelativeAcceleration[0] = sensorEvent.values[0];
                        deviceRelativeAcceleration[1] = sensorEvent.values[1];
                        deviceRelativeAcceleration[2] = sensorEvent.values[2];
                        deviceRelativeAcceleration[3] = 0;

                        // Change the device relative acceleration values to earth relative values
                        // X axis -> East
                        // Y axis -> North Pole
                        // Z axis -> Sky

                        float[] R = new float[16], I = new float[16], earthAcc = new float[16];

                        SensorManager.getRotationMatrix(R, I, gravityValues, magneticValues);

                        float[] inv = new float[16];

                        android.opengl.Matrix.invertM(inv, 0, R, 0);
                        android.opengl.Matrix.multiplyMV(earthAcc, 0, inv, 0, deviceRelativeAcceleration, 0);
                        Log.d("Acceleration", "Values: (" + earthAcc[0] + ", " + earthAcc[1] + ", " + earthAcc[2] + ")");


                        xValue.setText("X: " + earthAcc[0]);
                        yValue.setText("Y: " + earthAcc[1]);
                        zValue.setText("Z: " + earthAcc[2]);

                        writer.write(String.format("%d; %f; %f; %f; %f; %f; %f; %f; %f; %f; %d\n", sensorEvent.timestamp, earthAcc[0], earthAcc[1], earthAcc[2], sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2], latitude, longitude, speed, anomaly));
                        time = System.currentTimeMillis();;
                        anomaly=0;

                    } else if (sensorEvent.sensor.getType() == Sensor.TYPE_GRAVITY) {
                        gravityValues = sensorEvent.values;


                    } else if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                        magneticValues = sensorEvent.values;

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }};

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if(isRunning) {
                LatLng curPosition = new LatLng(location.getLatitude(), location.getLongitude());
                latitude=location.getLatitude();
                longitude = location.getLongitude();

                lat.setText("Latitude: " + location.getLatitude());
                lon.setText("Longitude: " + location.getLongitude());
                speed= ((location.getSpeed()*3600)/1000);
                speedlabel.setText("Speed: " + speed+"Km/h");
            }else{
                LatLng curPosition = new LatLng(location.getLatitude(), location.getLongitude());
            }

        }

        @Override
        public void onProviderDisabled(String provider) { }

        @Override
        public void onProviderEnabled(String provider) { }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };

    private void getLocationPermission(){
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mLocationPermissionsGranted = true;
            }else{
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        }else{
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: called.");
        mLocationPermissionsGranted = false;

        switch(requestCode){
            case LOCATION_PERMISSION_REQUEST_CODE:{
                if(grantResults.length > 0){
                    for(int i = 0; i < grantResults.length; i++){
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            mLocationPermissionsGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
                            return;
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    mLocationPermissionsGranted = true;
                }
            }
        }
    }
}