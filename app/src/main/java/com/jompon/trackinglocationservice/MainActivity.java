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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.api.ResolvableApiException;

import java.io.Serializable;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_CHECK_SETTINGS = 0x1;
    private Button btnStart;
    private Button btnStop;
    private TextView txtLocation;
    private LocationReceiver locationReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        bindingView();
    }

    private void bindingView( )
    {
        btnStart = (Button) findViewById(R.id.btn_start);
        btnStop = (Button) findViewById(R.id.btn_stop);
        txtLocation = (TextView) findViewById(R.id.txt_location);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if( locationReceiver == null ){
            locationReceiver = new LocationReceiver();
            LocalBroadcastManager.getInstance(this).registerReceiver(locationReceiver, new IntentFilter(FusedLocationService.LOCATION_SERVICE_FILTER));
            Log.d(TAG, "Receiver registered");
        }
    }

    @Override
    protected void onPause() {
        if( locationReceiver != null ){
            LocalBroadcastManager.getInstance(this).unregisterReceiver(locationReceiver);
            locationReceiver = null;
            Log.d(TAG, "Receiver unregistered");
        }
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case RESULT_OK:
                        Log.i(TAG, "User agreed to make required location settings changes.");
                        startLocation(btnStart);
                        break;
                    case RESULT_CANCELED:
                        Log.i(TAG, "User chose not to make required location settings changes.");
                        break;
                }
                break;
        }
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
            if( intent.getAction().equals(FusedLocationService.LOCATION_SERVICE_FILTER) ){
                Serializable serializable = intent.getSerializableExtra(FusedLocationService.KEY_RESOLVABLE_API_EXCEPTION);
                if( serializable != null ){
                    Log.d(TAG, "serializable != null");
                    try {
                        // Show the dialog by calling startResolutionForResult(), and check the
                        // result in onActivityResult().
                        ResolvableApiException rae = (ResolvableApiException) serializable;
                        rae.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sie) {
                        Log.i(TAG, "PendingIntent unable to execute request.");
                    }
                }else{
                    Location location = intent.getParcelableExtra(FusedLocationService.KEY_LOCATION);
                    txtLocation.setText(location.toString());
                }
            }
        }
    }
}
