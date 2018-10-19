package com.example.user.prototypeapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import android.graphics.Color;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import android.media.ToneGenerator;
import android.media.AudioManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "PrototypeApplication";
    private GoogleMap mMap;

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;

    private Boolean mLocationPermissionsGranted = true;

    private float[] gravityValues = null;
    private float[] magneticValues = null;

    private static final float speedCheck = 1.5f;
    private static final float RoughThreshold= 4.903325f;
    private static final float AnomalyThreshold = 6.6f;

    private LocationManager locationManager;
    private LatLng prevPosition;
    private LatLng curPosition;
    private boolean isFirstLocation = true;
    private int quality;

    private SensorManager sensorManager;
    private Sensor sensorLinAccel, Magno, Grav;
    private boolean Rough = false;
    private boolean Anomaly = false;
    private float zaxis = 0;

    private String roadData;
    FileWriter writer;
    private ImageView Pause;
    private FirebaseFirestore db;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        getLocationPermission();

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);


        Pause = (ImageView) findViewById(R.id.Pause);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorLinAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        Magno = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        Grav = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        db =  FirebaseFirestore.getInstance();


        Pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MapsActivity.this,"Road Surface Condition Data Saved", Toast.LENGTH_LONG).show();
                try {
                    writer.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                Intent intent =new Intent(MapsActivity.this,MainActivity.class);
                startActivity(intent);

            }
        });

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
        } else {
            Log.d(TAG, "onCreate: Permissions have not been granted, starting Intro Activity");
        }
        sensorManager.registerListener(listener, sensorLinAccel, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(listener, Grav, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(listener, Magno, SensorManager.SENSOR_DELAY_FASTEST);

        MainActivity m = new MainActivity();
        try {
            writer = new FileWriter(new File(getStorageDir(),  m.getMyString()+"_"+m.getMyString2()+".csv"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(locationListener); //
        sensorManager.unregisterListener(listener); //
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        boolean permissionGranted = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (permissionGranted) {
            Log.d(TAG, "onCreate: Permissions have been granted, starting Main Activity");
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);

        } else {
            Log.d(TAG, "onCreate: Permissions have not been granted, starting Intro Activity");
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }


    SensorEventListener listener = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {

                if ((gravityValues != null) && (magneticValues != null) && (sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION)) {

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
                    //Log.d("Acceleration", "Values: (" + earthAcc[0] + ", " + earthAcc[1] + ", " + earthAcc[2] + ")");

                    float z = earthAcc[2];
                    if (z > RoughThreshold) {
                        Rough = true;
                        if (z > AnomalyThreshold)
                            Anomaly = true;
                            zaxis =z;

                    }

                } else if (sensorEvent.sensor.getType() == Sensor.TYPE_GRAVITY) {
                    gravityValues = sensorEvent.values;


                } else if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    magneticValues = sensorEvent.values;

                }
        }
    };

    private LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            curPosition = new LatLng(location.getLatitude() , location.getLongitude());
            if (isFirstLocation) {
                mMap.moveCamera(CameraUpdateFactory.zoomTo(18));
                isFirstLocation = false;
                Anomaly = false;
                Rough = false;
            }
            else {
                if (location.getSpeed() > speedCheck) {

                    if (Anomaly) {
                        quality = 2;
                        ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
                        toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                        mMap.addPolyline(new PolylineOptions().add(prevPosition, curPosition).color(Color.RED));
                        mMap.addMarker(new MarkerOptions().position(curPosition).title("Anomaly").snippet("Z-Axis Acceleration: "+zaxis).icon(BitmapDescriptorFactory.fromResource(R.drawable.marker)).alpha(0));
                    }
                    else if (Rough) {
                        quality = 1;
                        mMap.addPolyline(new PolylineOptions().add(prevPosition, curPosition).color(Color.YELLOW));
                    }
                    else {
                        quality = 0;
                        mMap.addPolyline(new PolylineOptions().add(prevPosition, curPosition).color(Color.GREEN));
                    }
                    roadData = String.format("%1$f %2$f %3$f %4$f %5$d\n", prevPosition.latitude, prevPosition.longitude,curPosition.latitude, curPosition.longitude, quality);
                    write();
                }
            }
            mMap.animateCamera(CameraUpdateFactory.newLatLng(curPosition));
            prevPosition = curPosition;
            Anomaly = false;
            Rough = false;
        }

        @Override
        public void onProviderDisabled(String provider) { }

        @Override
        public void onProviderEnabled(String provider) { }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };

    void write() {
        try {
            writer.write(roadData);

            Map<String, Object> RoadQualityData = new HashMap<>();
            RoadQualityData.put("Previous Latitude", prevPosition.latitude);
            RoadQualityData.put("Previous Longitude", prevPosition.longitude);
            RoadQualityData.put("Current Latitude", curPosition.latitude);
            RoadQualityData.put("Current Longitude", curPosition.longitude);
            RoadQualityData.put("Z-axis Acceleration", zaxis);
            RoadQualityData.put("Road Quality", quality);

// Add a new document with a generated ID
            db.collection("RoadQualityData")
                    .add(RoadQualityData)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error adding document", e);
                        }
                    });





        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
    private String getStorageDir() {
        return this.getExternalFilesDir(null).getAbsolutePath();
    }

}
