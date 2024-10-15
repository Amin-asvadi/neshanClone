package com.neshan.neshantask.location;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import com.carto.styles.MarkerStyleBuilder;
import com.carto.utils.BitmapUtils;
import com.google.android.gms.location.LocationRequest;
import com.google.android.material.snackbar.Snackbar;
import com.neshan.neshantask.R;
import com.neshan.neshantask.core.location.BoundLocationManager;
import com.neshan.neshantask.core.location.BoundLocationManager;
import com.neshan.neshantask.core.location.LocationListener;
import com.neshan.neshantask.core.util.Util;
import com.neshan.neshantask.databinding.ActivityChooseLocationBinding;
import org.neshan.common.model.LatLng;
import org.neshan.mapsdk.model.Marker;

import java.util.concurrent.TimeUnit;
public class ChooseLocationActivity extends AppCompatActivity implements LocationListener {

    private static final String TAG = "ChooseLocationActivity";
    public static final String EXTRA_LATITUDE = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";

    private ActivityChooseLocationBinding mBinding;

    // handle location updates
    private BoundLocationManager mLocationManager;

    // a marker for user location to be shown on map
    private Marker mUserLocationMarker;

    private Location mUserLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = ActivityChooseLocationBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        setViewListeners();
        setUpLocationManager();
    }

    @Override
    public void onLastLocation(Location location) {
        onLocationChange(location);
    }

    // handle location change
    @Override
    public void onLocationChange(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        // remove previously added marker from map and add new marker to user location
        if (mUserLocationMarker != null) {
            mBinding.mapview.removeMarker(mUserLocationMarker);
        }
        mUserLocationMarker = createMarker(latLng);
        mBinding.mapview.addMarker(mUserLocationMarker);

        if (mUserLocation == null) {
            focusOnLocation(latLng);
        }

        mUserLocation = location;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.d(TAG, "onRequestPermissionResult");

        switch (requestCode) {
            case BoundLocationManager.REQUEST_CODE_FOREGROUND_PERMISSIONS:
                if (grantResults.length == 0) {
                    Log.d(TAG, "User interaction was cancelled.");
                } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted.
                    mLocationManager.startLocationUpdates();
                } else {
                    // Permission denied.
                    Snackbar.make(mBinding.getRoot(), R.string.permission_denied_explanation, Snackbar.LENGTH_LONG)
                            .setAction(R.string.settings, v -> {
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getApplication().getPackageName(), null);
                                intent.setData(uri);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }).show();
                }
                break;
        }
    }

    private void setViewListeners() {

        mBinding.location.setOnClickListener(v -> {
            if (mUserLocation != null) {
                focusOnLocation(new LatLng(mUserLocation.getLatitude(), mUserLocation.getLongitude()));
            } else {
                mLocationManager.startLocationUpdates();
            }
        });

        mBinding.confirm.setOnClickListener(v -> chooseSelectedPosition());
    }

    private void setUpLocationManager() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(TimeUnit.SECONDS.toMillis(3));
        locationRequest.setFastestInterval(TimeUnit.SECONDS.toMillis(1));
        locationRequest.setMaxWaitTime(1);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mLocationManager = new BoundLocationManager(this, locationRequest, this);
        mLocationManager.startLocationUpdates();
    }

    private Marker createMarker(LatLng latLng) {
        MarkerStyleBuilder markStCr = new MarkerStyleBuilder();
        markStCr.setSize(30f);

        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_marker);
        if (drawable != null) {
            // Creating bitmap from drawable
            markStCr.setBitmap(BitmapUtils.createBitmapFromAndroidBitmap(Util.drawableToBitmap(drawable)));
        }

        return new Marker(latLng, markStCr.buildStyle());
    }

    private void focusOnLocation(LatLng loc) {
        mBinding.mapview.moveCamera(loc, 0.25f);
        mBinding.mapview.setZoom(15f, 0.25f);
    }

    private void chooseSelectedPosition() {
        LatLng latLng = mBinding.mapview.getCameraTargetPosition();
        setResult(RESULT_OK, new Intent().putExtra(EXTRA_LATITUDE, latLng.getLatitude())
                .putExtra(EXTRA_LONGITUDE, latLng.getLongitude()));

        onBackPressed();
    }
}
