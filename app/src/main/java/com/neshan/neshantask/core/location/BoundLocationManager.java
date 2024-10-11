package com.neshan.neshantask.core.location;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.tasks.Task;

public class BoundLocationManager implements DefaultLifecycleObserver {

    private static final String TAG = "BoundLocationManager";

    public static final int REQUEST_CODE_LOCATION_SETTING = 1001;
    public static final int REQUEST_CODE_FOREGROUND_PERMISSIONS = 1002;

    private boolean mForegroundLocationServiceBound = false;

    // Provides location updates for while-in-use feature.
    private ForegroundLocationService mForegroundLocationService = null;

    // Listens for location broadcasts from ForegroundLocationService.
    private final ForegroundBroadcastReceiver mForegroundBroadcastReceiver;

    // Monitors connection to the while-in-use service.
    private final ServiceConnection mForegroundServiceConnection;

    private final Task<LocationSettingsResponse> mLocationSettingTask;

    private final AppCompatActivity mContext;
    private final LocationRequest mLocationRequest;

    public BoundLocationManager(AppCompatActivity context, LocationRequest locationRequest, LocationListener callback) {
        this.mContext = context;
        this.mLocationRequest = locationRequest;
        this.mForegroundBroadcastReceiver = new ForegroundBroadcastReceiver(callback);
        this.mForegroundServiceConnection = getServiceConnection();
        this.mLocationSettingTask = getLocationSetting();
        context.getLifecycle().addObserver(this);
    }

    @Override
    public void onStart(LifecycleOwner owner) {
        Intent serviceIntent = new Intent(mContext, ForegroundLocationService.class);
        mContext.bindService(serviceIntent, mForegroundServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onResume(LifecycleOwner owner) {
        LocalBroadcastManager.getInstance(mContext).registerReceiver(
            mForegroundBroadcastReceiver,
            new IntentFilter(ForegroundLocationService.ACTION_FOREGROUND_LOCATION_BROADCAST)
        );
    }

    @Override
    public void onPause(LifecycleOwner owner) {
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mForegroundBroadcastReceiver);
    }

    @Override
    public void onStop(LifecycleOwner owner) {
        if (mForegroundLocationServiceBound) {
            mContext.unbindService(mForegroundServiceConnection);
        }
    }

    public void startLocationUpdates() {
        if (mForegroundLocationService != null) {
            if (foregroundPermissionApproved()) {
                mForegroundLocationService.subscribeToLocationUpdates();
                checkLocationAvailability();
            } else {
                Log.d(TAG, "Request foreground permission");
                ActivityCompat.requestPermissions(
                        mContext,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_CODE_FOREGROUND_PERMISSIONS
                );
            }
        }else {
            Log.d("","");
        }

    }

    public void stopLocationUpdates() {
        mForegroundLocationService.unsubscribeToLocationUpdates();
    }

    private boolean foregroundPermissionApproved() {
        return PackageManager.PERMISSION_GRANTED ==
                ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private ServiceConnection getServiceConnection() {
        return new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                ForegroundLocationService.LocalBinder binder = (ForegroundLocationService.LocalBinder) service;
                mForegroundLocationService = binder.getService();
                if (mLocationRequest != null) {
                    mForegroundLocationService.setLocationRequest(mLocationRequest);
                }
                mForegroundLocationServiceBound = true;
                startLocationUpdates();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mForegroundLocationService = null;
                mForegroundLocationServiceBound = false;
            }
        };
    }

    private Task<LocationSettingsResponse> getLocationSetting() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        if (mForegroundLocationService != null && mForegroundLocationService.getLocationRequest() != null) {
            builder.addLocationRequest(mForegroundLocationService.getLocationRequest());
        }
        return LocationServices.getSettingsClient(mContext).checkLocationSettings(builder.build());
    }

    private void checkLocationAvailability() {
        mLocationSettingTask.addOnSuccessListener(response -> {
            Log.d(TAG, "All location settings are satisfied");
        }).addOnFailureListener(exception -> {
            if (exception instanceof ResolvableApiException) {
                Log.e(TAG, "Location settings are not satisfied");
                try {
                    ((ResolvableApiException) exception).startResolutionForResult(
                        mContext, REQUEST_CODE_LOCATION_SETTING);
                } catch (IntentSender.SendIntentException e) {
                    // Ignore the error.
                }
            }
        });
    }

    private class ForegroundBroadcastReceiver extends BroadcastReceiver {
        private final LocationListener locationListener;

        ForegroundBroadcastReceiver(LocationListener locationListener) {
            this.locationListener = locationListener;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(ForegroundLocationService.EXTRA_LOCATION)) {
                Location location = intent.getParcelableExtra(ForegroundLocationService.EXTRA_LOCATION);
                if (location != null) {
                    locationListener.onLocationChange(location);
                }
            } else if (intent.hasExtra(ForegroundLocationService.EXTRA_LAST_LOCATION)) {
                Location location = intent.getParcelableExtra(ForegroundLocationService.EXTRA_LAST_LOCATION);
                if (location != null) {
                    locationListener.onLastLocation(location);
                }
            }
        }
    }
}
