package com.neshan.neshantask.navigation;

import static com.neshan.neshantask.core.location.BoundLocationManager.REQUEST_CODE_FOREGROUND_PERMISSIONS;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.lifecycle.ViewModelProvider;

import com.carto.graphics.Color;
import com.carto.styles.LineStyle;
import com.carto.styles.LineStyleBuilder;
import com.carto.styles.MarkerStyleBuilder;
import com.carto.utils.BitmapUtils;
import com.google.android.gms.location.LocationRequest;
import com.google.android.material.snackbar.Snackbar;
import com.neshan.neshantask.R;
import com.neshan.neshantask.core.location.BoundLocationManager;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import com.neshan.neshantask.core.location.LocationListener;
import com.neshan.neshantask.core.snackbar.SnackBar;
import com.neshan.neshantask.core.snackbar.SnackBarType;
import com.neshan.neshantask.core.util.Util;
import com.neshan.neshantask.data.model.error.GeneralError;
import com.neshan.neshantask.data.model.error.SimpleError;
import com.neshan.neshantask.data.util.EventObserver;
import com.neshan.neshantask.databinding.ActivityNavigationBinding;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.core.Observable;
import org.neshan.common.model.LatLng;
import org.neshan.mapsdk.model.Marker;
import org.neshan.mapsdk.model.Polyline;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

@AndroidEntryPoint
public class NavigationActivity extends AppCompatActivity implements LocationListener {

    private static final String TAG = "NavigationActivity";
    private static final long LOCATION_UPDATE_INTERVAL = 3000L; // 3 seconds
    private static final long LOCATION_UPDATE_FASTEST_INTERVAL = 1000L; // 1 second

    public static final String EXTRA_START_POINT = "start_point";
    public static final String EXTRA_END_POINT = "end_point";

    private ActivityNavigationBinding mBinding;
    private NavigationViewModel mViewModel;

    // handle location updates
    private BoundLocationManager mLocationManager;

    // a marker for user location to be shown on map
    private Marker mUserLocationMarker;

    // poly line for showing progress path on map
    private Polyline mProgressPathPolyLine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = ActivityNavigationBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        mViewModel = new ViewModelProvider(this).get(NavigationViewModel.class);
        mBinding.setVm(mViewModel);

        initMap();
        setViewListeners();

        // observe ViewModel live data objects changes
        observeViewModelChange(mViewModel);
        setUpLocationManager();
        loadNavigationData();
    }

    @Override
    public void onLastLocation(Location location) {
        onLocationChange(location);
    }

    /**
     * handle location change
     */
    @Override
    public void onLocationChange(Location location) {
        mViewModel.updateUserLocation(location);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.d(TAG, "onRequestPermissionResult");

        switch (requestCode) {
            case REQUEST_CODE_FOREGROUND_PERMISSIONS:
                if (grantResults.length == 0) {
                    // If user interaction was interrupted, the permission request
                    // is cancelled and you receive empty arrays.
                    Log.d(TAG, "User interaction was cancelled.");
                } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted.
                    mLocationManager.startLocationUpdates();
                } else {
                    // Permission denied.
                    Snackbar.make(
                            mBinding.getRoot(),
                            R.string.permission_denied_explanation,
                            Snackbar.LENGTH_LONG
                    )
                            .setAction(R.string.settings, v -> {
                                // Build intent that displays the App settings screen.
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

    private void initMap() {
        // change camera angle
        mBinding.mapview.setTilt(40f, 0.25f);
    }

    private void setViewListeners() {
        mBinding.stop.setOnClickListener(v -> onBackPressed());
    }

    private void loadNavigationData() {
        com.neshan.neshantask.data.model.LatLng startPoint =
                getIntent().getParcelableExtra(EXTRA_START_POINT);
        com.neshan.neshantask.data.model.LatLng endPoint =
                getIntent().getParcelableExtra(EXTRA_END_POINT);

        if (startPoint != null && endPoint != null) {
            mViewModel.startNavigation(
                    new LatLng(startPoint.getLatitude(), startPoint.getLongitude()),
                    new LatLng(endPoint.getLatitude(), endPoint.getLongitude())
            );
        } else {
            Util.showError(mBinding.getRoot(), new SimpleError(getString(R.string.navigation_failure)));
            finish();
        }
    }

    private void observeViewModelChange(NavigationViewModel viewModel) {
        viewModel.progressPoints.observe(this, progressPoints -> updatePathOnMap(progressPoints));

        viewModel.markerPosition.observe(this, markerPosition -> updateLocationMarker(markerPosition));

        viewModel.reachedDestination.observe(this, reachedDestination -> {
            if (reachedDestination) {
                SnackBar.make(
                        mBinding.getRoot(),
                        getString(R.string.reached_destination),
                        SnackBarType.NORMAL,
                        null
                ).show();
                // Delay for 3 seconds and then call onBackPressed
                Observable.timer(3, TimeUnit.SECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(aLong -> onBackPressed());
            }
        });

        viewModel.generalError.observe(this, new EventObserver<GeneralError>(error -> {
            // This does not need a return statement.
            Util.showError(mBinding.getRoot(), error);
        }));
    }

    /**
     * creates a PolyLine by routing points for showing path on map
     */
    private void updatePathOnMap(ArrayList<LatLng> routePoints) {
        if (routePoints.size() >= 2) {
            // create new poly line by routing points and update path on map
            if (mProgressPathPolyLine != null) {
                mBinding.mapview.removePolyline(mProgressPathPolyLine);
            }
            mProgressPathPolyLine = new Polyline(routePoints, getLineStyle(R.color.colorPrimaryDim75));
            mBinding.mapview.addPolyline(mProgressPathPolyLine);

            // calculate first route angle with north axis
            // and set camera rotation to always show upward
            LatLng startPoint = routePoints.get(0);
            LatLng endPoint = routePoints.get(1);
            LatLng bearingEndPoint = routePoints.size() > 2 ? routePoints.get(2) : endPoint;
            float angle = (float) Util.angleWithNorthAxis(startPoint, bearingEndPoint);
            mBinding.mapview.setBearing(angle, 0.7f);

            focusOnLocation(startPoint);
        }
    }

    private LineStyle getLineStyle(int colorResource) {
        LineStyleBuilder lineStCr = new LineStyleBuilder();
        lineStCr.setColor(new Color(ContextCompat.getColor(this, colorResource)));
        lineStCr.setWidth(10f);
        lineStCr.setStretchFactor(0f);
        return lineStCr.buildStyle();
    }

    /**
     * config and start location manager to track user location
     */
    private void setUpLocationManager() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(LOCATION_UPDATE_INTERVAL);
        locationRequest.setFastestInterval(LOCATION_UPDATE_FASTEST_INTERVAL);
        locationRequest.setMaxWaitTime(1);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mLocationManager = new BoundLocationManager(this, locationRequest, this);
        mLocationManager.startLocationUpdates();
    }

    private void updateLocationMarker(LatLng latLng) {
        if (mUserLocationMarker != null) {
            mBinding.mapview.removeMarker(mUserLocationMarker);
        }
        mUserLocationMarker = createMarker(latLng);
        mBinding.mapview.addMarker(mUserLocationMarker);
    }

    private Marker createMarker(LatLng latLng) {
        MarkerStyleBuilder markStCr = new MarkerStyleBuilder();
        markStCr.setSize(30f);

        // Create the marker bitmap from the drawable
        var drawable = ContextCompat.getDrawable(this, R.drawable.ic_marker);
        if (drawable != null) {
            var markerBitmap = BitmapUtils.createBitmapFromAndroidBitmap(Util.drawableToBitmap(drawable));
            markStCr.setBitmap(markerBitmap);
        }

        return new Marker(latLng, markStCr.buildStyle());
    }

    private void focusOnLocation(LatLng loc) {
        mBinding.mapview.moveCamera(loc, 0.5f);
        if (mBinding.mapview.getZoom() != 17f) {
            mBinding.mapview.setZoom(17f, 0.5f);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mLocationManager.stopLocationUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mViewModel.onCleared();
    }
}
