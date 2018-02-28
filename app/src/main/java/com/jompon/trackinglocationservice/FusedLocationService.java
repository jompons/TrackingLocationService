/*
 * Copyright (C) 2018 jompons.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jompon.trackinglocationservice;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class FusedLocationService extends Service {

    public static final String START_ACTION = "START_ACTION";
    public static final String STOP_ACTION = "STOP_ACTION";
    public static final String LOCATION_SERVICE_FILTER = "LOCATION_SERVICE_FILTER";
    public static final String KEY_RESOLVABLE_API_EXCEPTION = "KEY_RESOLVABLE_API_EXCEPTION";
    public static final String KEY_LOCATION = "KEY_LOCATION";
    private static String TAG = FusedLocationService.class.getSimpleName();
    private static int LOCATION_INTERVAL = 60000;
    private static int LOCATION_FAST_INTERVAL = 30000;
    private static float LOCATION_DISTANCE = 0f;
    private SettingsClient mSettingsClient;
    private LocationSettingsRequest mLocationSettingsRequest;
    private FusedLocationProviderClient client;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private ServiceHandler mServiceHandler;
    private Looper mServiceLooper;
    private Handler handler;

    private final class ServiceHandler extends Handler {

        private ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            Log.d(TAG, "handleMessage");
            if( msg.obj == null )   return;
            if( msg.obj.equals(START_ACTION) ){
                Log.d(TAG, "ACTION = "+START_ACTION);
                destroy();
                create();
            }else if( msg.obj.equals(STOP_ACTION) ){
                Log.d(TAG, "ACTION = "+STOP_ACTION);
                destroy();
                stopSelf(msg.arg1);
            }
            Log.d(TAG, "handleMessaged");
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d(TAG, "onStartCommand");
//        super.onStartCommand(intent, flags, startId);

        try{
            Message msg = mServiceHandler.obtainMessage();
            msg.arg1 = startId;
            msg.obj = intent.getAction();
            mServiceHandler.sendMessage(msg);
        }catch (Exception e){
            Log.d(TAG, e.getMessage()+"");
        }
        return START_STICKY;
    }

    @Override
    public void onCreate()
    {
        Log.d(TAG, "onCreate");
        handler = new Handler(Looper.getMainLooper());
        HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

//        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public void onDestroy()
    {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        destroy();
    }

    public void onLocationChanged(Location location) {
        // New location has now been determined
        String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());
        Log.d(TAG, msg);

        Intent intent = new Intent();
        intent.setAction(LOCATION_SERVICE_FILTER);
        intent.putExtra(KEY_LOCATION, location);
        sendBroadcast(intent);
    }

    private void initInstant( )
    {
        mSettingsClient = LocationServices.getSettingsClient(this);
        client = LocationServices.getFusedLocationProviderClient(this);
    }

    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(LOCATION_INTERVAL);
        mLocationRequest.setFastestInterval(LOCATION_FAST_INTERVAL);
        mLocationRequest.setSmallestDisplacement(LOCATION_DISTANCE);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void createLocationCallback() {
        mLocationCallback = new LocationCallback(){

            @Override
            public void onLocationResult(final LocationResult locationResult) {

                onLocationChanged(locationResult.getLastLocation());
                Log.d(TAG, "onLocationResult location: "+locationResult.getLastLocation());
            }

            @Override
            public void onLocationAvailability(LocationAvailability locationAvailability) {
                super.onLocationAvailability(locationAvailability);
            }
        };
    }

    private void create()
    {
        Log.d(TAG, "create");
        initInstant();
        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {

                        startLocationUpdates();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");

                                Intent intent = new Intent();
                                intent.setAction(LOCATION_SERVICE_FILTER);
                                intent.putExtra(KEY_RESOLVABLE_API_EXCEPTION, e);
                                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " + "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);
                                Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
                                break;
                        }
                        stopSelf();
                    }
                });
    }

    private void startLocationUpdates( )
    {
        if( ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ) {
            client.getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // GPS location can be null if GPS is switched off
                            if (location != null) {
                                onLocationChanged(location);
                                Log.d(TAG, "onSuccess location");
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                            Toast.makeText(FusedLocationService.this, "Error trying to get last GPS location", Toast.LENGTH_LONG).show();
                        }
                    });
            client.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper());
        }else{
            stopSelf();
            Toast.makeText(this, "Please allow location permission", Toast.LENGTH_LONG).show();
        }
    }

    private void destroy()
    {
        if( client != null && mLocationCallback != null )
        {
            client.removeLocationUpdates(mLocationCallback);
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {

        Intent restartServiceTask = new Intent(getApplicationContext(), this.getClass());
        restartServiceTask.setAction(START_ACTION);
        restartServiceTask.setPackage(getPackageName());
        PendingIntent restartPendingIntent = PendingIntent.getService(getApplicationContext(), 1, restartServiceTask, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager myAlarmService = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        myAlarmService.set(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 1000,
                restartPendingIntent);

        super.onTaskRemoved(rootIntent);
    }
}
