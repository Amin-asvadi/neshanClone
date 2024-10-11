package com.neshan.neshantask.core.location;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.*;
import com.neshan.neshantask.R;

import java.util.concurrent.TimeUnit;

public class ForegroundLocationService extends Service {

    public static final String ACTION_FOREGROUND_LOCATION_BROADCAST = "org.neshan.action.FOREGROUND_LOCATION_BROADCAST";
    public static final String EXTRA_LOCATION = "location";
    public static final String EXTRA_LAST_LOCATION = "last_location";

    private static final String TAG = "LocationService";
    private static final String EXTRA_CANCEL_LOCATION_TRACKING_FROM = "cancel_location_tracking";
    private static final int NOTIFICATION_ID = 2001;

    private boolean mConfigurationChange = false;
    private boolean mServiceRunningInForeground = false;

    private final IBinder mLocalBinder = new LocalBinder();
    private NotificationManager mNotificationManager;

    private FusedLocationProviderClient mFusedLocationProviderClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private Location currentLocation;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                currentLocation = locationResult.getLastLocation();
                Log.d(TAG, "latitude: " + currentLocation.getLatitude() + " longitude: " + currentLocation.getLongitude());

                Intent intent = new Intent(ACTION_FOREGROUND_LOCATION_BROADCAST);
                intent.putExtra(EXTRA_LOCATION, currentLocation);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()");

        boolean cancelLocationTrackingFromNotification = intent.getBooleanExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM, false);
        if (cancelLocationTrackingFromNotification) {
            unsubscribeToLocationUpdates();
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()");

        stopForeground(true);
        mServiceRunningInForeground = false;
        mConfigurationChange = false;

        return mLocalBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "onRebind()");

        stopForeground(true);
        mServiceRunningInForeground = false;
        mConfigurationChange = false;

        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind()");

        if (!mConfigurationChange) {
            Log.d(TAG, "Start foreground service");
            try {
                startForeground(NOTIFICATION_ID, generateNotification());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            mServiceRunningInForeground = true;
        }

        return true;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mConfigurationChange = true;
    }

    public void setLocationRequest(LocationRequest request) {
        mLocationRequest = request;
    }

    public LocationRequest getLocationRequest() {
        return mLocationRequest;
    }

    public void subscribeToLocationUpdates() {
        Log.d(TAG, "subscribeToLocationUpdates()");

        startService(new Intent(getApplicationContext(), ForegroundLocationService.class));

        try {
            if (mLocationRequest == null) {
                mLocationRequest = getDefaultLocationRequest();
            }

            mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper());
            mFusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    Intent intent = new Intent(ACTION_FOREGROUND_LOCATION_BROADCAST);
                    intent.putExtra(EXTRA_LAST_LOCATION, location);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                }
            });
        } catch (SecurityException unlikely) {
            Log.e(TAG, "Lost location permissions. Couldn't remove updates.", unlikely);
        }
    }

    public void unsubscribeToLocationUpdates() {
        Log.d(TAG, "unsubscribeToLocationUpdates()");

        try {
            mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Location Callback removed.");
                    stopSelf();
                } else {
                    Log.d(TAG, "Failed to remove Location Callback.");
                }
            });
        } catch (SecurityException unlikely) {
            Log.e(TAG, "Lost location permissions. Couldn't remove updates.", unlikely);
        }
    }

    private LocationRequest getDefaultLocationRequest() {
        return LocationRequest.create().setInterval(TimeUnit.SECONDS.toMillis(60))
                .setFastestInterval(TimeUnit.SECONDS.toMillis(30))
                .setMaxWaitTime(TimeUnit.MINUTES.toMillis(2))
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private Notification generateNotification() throws ClassNotFoundException {
        Log.d(TAG, "generateNotification()");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(
                    getString(R.string.notification_channel_id),
                    getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            mNotificationManager.createNotificationChannel(notificationChannel);
        }

        Intent launchActivityIntent = new Intent(this, Class.forName(getString(R.string.main_activity_class_name)));

        Intent cancelIntent = new Intent(this, ForegroundLocationService.class);
        cancelIntent.putExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM, true);

        PendingIntent servicePendingIntent = PendingIntent.getService(this, 0, cancelIntent, PendingIntent.FLAG_IMMUTABLE);
        PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 0, launchActivityIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationCompatBuilder = new NotificationCompat.Builder(getApplicationContext(), getString(R.string.notification_channel_id));

        RemoteViews notificationLayout = new RemoteViews(getPackageName(), R.layout.layout_notification);
        notificationLayout.setOnClickPendingIntent(R.id.exit, servicePendingIntent);
        notificationLayout.setOnClickPendingIntent(R.id.container, activityPendingIntent);

        return notificationCompatBuilder.setSmallIcon(R.drawable.ic_app_logo)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setOngoing(true)
                .setCustomContentView(notificationLayout)
                .setSilent(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();
    }

    public class LocalBinder extends Binder {
        public ForegroundLocationService getService() {
            return ForegroundLocationService.this;
        }
    }
}
