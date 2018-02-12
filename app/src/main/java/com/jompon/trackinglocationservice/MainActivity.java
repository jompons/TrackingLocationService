package com.jompon.trackinglocationservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private Button btnStart;
    private Button btnStop;
    private TextView txtLocation;
    private LocationReceiver locationReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btnStart = (Button) findViewById(R.id.btn_start);
        btnStop = (Button) findViewById(R.id.btn_stop);
        txtLocation = (TextView) findViewById(R.id.txt_location);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if( locationReceiver == null ){
            locationReceiver = new LocationReceiver();
            registerReceiver(locationReceiver, new IntentFilter(FusedLocationService.FILTER_ACTION));
            Log.d(TAG, "Receiver registered");
        }
    }

    @Override
    protected void onPause() {
        if( locationReceiver != null ){
            unregisterReceiver(locationReceiver);
            locationReceiver = null;
            Log.d(TAG, "Receiver unregistered");
        }
        super.onPause();
    }

    public void startLocation(View view)
    {
        Intent intent = new Intent(this, FusedLocationService.class);
        intent.setAction(FusedLocationService.START_ACTION);
        startService(intent);
    }

    public void stopLocation(View view)
    {
        Intent intent = new Intent(this, FusedLocationService.class);
        intent.setAction(FusedLocationService.STOP_ACTION);
        stopService(intent);
    }

    public void clear(View view)
    {
        txtLocation.setText("CLEAR");
    }

    public class LocationReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if( intent.getAction() == null )        return;
            if( intent.getAction().equals(FusedLocationService.FILTER_ACTION) ){
                Location location = intent.getParcelableExtra(FusedLocationService.KEY_LOCATION);
                txtLocation.setText(location.toString());
            }
        }
    }
}
