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
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class FusedLocationService extends Service {

    public static final String START_ACTION = "START_ACTION";
    public static final String STOP_ACTION = "STOP_ACTION";
    public static final String FILTER_ACTION = "FILTER_ACTION";
    public static final String KEY_LOCATION = "KEY_LOCATION";
    private static String TAG = FusedLocationService.class.getSimpleName();
    private static int LOCATION_INTERVAL = 60000;
    private static int LOCATION_FAST_INTERVAL = 30000;
    private static float LOCATION_DISTANCE = 0f;
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
        intent.setAction(FILTER_ACTION);
        intent.putExtra(KEY_LOCATION, location);
        sendBroadcast(intent);
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(LOCATION_INTERVAL);
        mLocationRequest.setFastestInterval(LOCATION_FAST_INTERVAL);
        mLocationRequest.setSmallestDisplacement(LOCATION_DISTANCE);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void create()
    {
        Log.d(TAG, "create");
        createLocationRequest();
        client = LocationServices.getFusedLocationProviderClient(this);
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
