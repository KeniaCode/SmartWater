package com.app.kenia.smartwater;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

/**
 * Created by luisbarrera on 10/3/17.
 */

public class LocationServices extends Service implements LocationListener {

    public static double mLatitude = 0;
    public static double mLongitude = 0;
    LocationManager locationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                mLatitude = location.getLatitude();
                mLongitude = location.getLongitude();
            } else {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (location != null) {
                    mLatitude = location.getLatitude();
                    mLongitude = location.getLongitude();
                } else {
                    mLatitude = 0;
                    mLongitude = 0;
                }
            }
        } else {
            ActivityCompat.requestPermissions((MainActivity) getApplicationContext(), new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        requestLocation();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void requestLocation() {
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (isGPSEnabled || isNetworkEnabled) {
            if (isGPSEnabled) {
                try {
                    //locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER,this,null);
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0.0f, this);
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            } else if (isNetworkEnabled) {
                try {
                    //locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER,this,null);
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0.0f, this);
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        mLatitude = location.getLatitude();
        mLongitude = location.getLongitude();
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        requestLocation();
    }

    @Override
    public void onProviderEnabled(String s) {
        requestLocation();
    }

    @Override
    public void onProviderDisabled(String s) {
        requestLocation();
    }
}
