package com.neshan.neshantask;

import static com.neshan.neshantask.core.location.BoundLocationManager.REQUEST_CODE_FOREGROUND_PERMISSIONS;
import static com.neshan.neshantask.location.ChooseLocationActivity.EXTRA_LATITUDE;
import static com.neshan.neshantask.location.ChooseLocationActivity.EXTRA_LONGITUDE;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.carto.core.ScreenBounds;
import com.carto.core.ScreenPos;
import com.carto.graphics.Bitmap;
import com.carto.graphics.Color;
import com.carto.styles.LineStyle;
import com.carto.styles.LineStyleBuilder;
import com.carto.styles.MarkerStyleBuilder;
import com.carto.utils.BitmapUtils;
import com.google.android.gms.location.LocationRequest;
import com.google.android.material.snackbar.Snackbar;
import com.neshan.neshantask.core.location.BoundLocationManager;
import com.neshan.neshantask.core.location.LocationListener;

import com.neshan.neshantask.core.util.Util;
import com.neshan.neshantask.data.network.Result;
import com.neshan.neshantask.databinding.ActivityMainBinding;

import org.neshan.common.model.LatLng;
import org.neshan.common.model.LatLngBounds;
import com.neshan.neshantask.data.util.EventObserver;
import com.neshan.neshantask.location.ChooseLocationActivity;

import org.neshan.mapsdk.model.Marker;
import org.neshan.mapsdk.model.Polyline;
import org.neshan.mapsdk.style.NeshanMapStyle;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity implements LocationListener {
    private static final String TAG = "MainActivity";

    private ActivityMainBinding mBinding;

    private MainViewModel mViewModel;
    // save current map style
    // handle location updates
    private BoundLocationManager mLocationManager;

    // a marker for user location to be shown on map
    private Marker mUserLocationMarker;

    // a marker for selected location to be shown on map
    private Marker mDestinationMarker;

    // poly line for the path from start point to end point on map
    private Polyline mRoutingPathPolyLine;
    private final ActivityResultLauncher<Intent> mStartChooseLocationForResult = this.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {

        // check if location selected
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            // handle selected location
            Bundle extras = result.getData().getExtras();
            double latitude = extras.getDouble(EXTRA_LATITUDE);
            double longitude = extras.getDouble(EXTRA_LONGITUDE);
            onDestinationSelected(new LatLng(latitude, longitude));
        }

    });
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        // observe ViewModel live data objects changes
        observeViewModelChange(mViewModel);

        setViewListeners();

        setUpLocationManager();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.d(TAG, "onRequestPermissionResult");

        if (requestCode == REQUEST_CODE_FOREGROUND_PERMISSIONS) {
            if (grantResults.length == 0) {
                Log.d(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted.
                mLocationManager.startLocationUpdates();
            } else {
                // Permission denied.
                // TODO : show custom snack bar
                Snackbar.make(
                        mBinding.getRoot(),
                        R.string.permission_rationale,
                        Snackbar.LENGTH_LONG
                ).setAction(R.string.settings, view -> {
                    // Build intent that displays the App settings screen.
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.fromParts(
                            "package", getApplication().getPackageName(), null
                    ));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }).show();
            }
        }

    }
    @Override
    public void onLastLocation(@NonNull Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        onStartPointSelected(latLng, true);
    }

    @Override
    public void onLocationChange(@NonNull Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        onStartPointSelected(latLng, false);
    }

    private void observeViewModelChange(MainViewModel viewModel) {

        viewModel.getLocationAddressDetailLiveData().observe(this, result -> {

            if (result.getStatus() == Result.Status.SUCCESS && result.getData() != null) {
                    // Hide loading view
                    mBinding.loading.setVisibility(View.GONE);

                    // Show location detail bottom sheet
                    LocationDetailBottomSheet bottomSheet = new LocationDetailBottomSheet();
                    bottomSheet.show(getSupportFragmentManager(), "LocationDetail");
                    bottomSheet.setOnDismissListener(dialogInterface -> clearMapObjects());

            } else if (result.getStatus() == Result.Status.LOADING) {
                // Show loading view
                mBinding.loading.setVisibility(View.VISIBLE);

            } else if (result.getStatus() == Result.Status.ERROR) {
                // Hide loading view
                mBinding.loading.setVisibility(View.GONE);
                // Handle error (e.g., show error message or clear map objects)
                clearMapObjects();
            }
        });

        viewModel.getRoutePoints().observe(this, this::showPathOnMap);

        viewModel.getGeneralErrorLiveData().observe(this, new EventObserver<>(error -> {
            Util.showError(mBinding.getRoot(), error);
        }));
    }
    private void setViewListeners() {

        mBinding.location.setOnClickListener(view -> {
            if (mViewModel.getStartPoint() != null) {
                focusOnLocation(mViewModel.getStartPoint());
            } else {
                mLocationManager.startLocationUpdates();
            }
        });

        mBinding.chooseLocation.setOnClickListener(view -> {
            mStartChooseLocationForResult.launch(new Intent(this, ChooseLocationActivity.class));
        });

    }
    private void clearMapObjects() {
        // if user closed address detail then remove location marker from map
        if (mDestinationMarker != null) {
            mBinding.mapview.removeMarker(mDestinationMarker);
            mViewModel.setEndPoint(null);
        }
        // if user closed address detail then remove drawn path from map
        if (mRoutingPathPolyLine != null) {
            mBinding.mapview.removePolyline(mRoutingPathPolyLine);
        }

        focusOnLocation(mViewModel.getStartPoint());
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


    private void onStartPointSelected(LatLng latLng, boolean isCachedLocation) {

        // remove previously added marker from map and add new marker to user location
        if (mUserLocationMarker != null) {
            mBinding.mapview.removeMarker(mUserLocationMarker);
        }
        if (isCachedLocation) {
            mUserLocationMarker = createMarker(latLng, R.drawable.ic_marker_off);
        } else {
            mUserLocationMarker = createMarker(latLng, R.drawable.ic_marker);
        }
        mBinding.mapview.addMarker(mUserLocationMarker);

        if (mViewModel.getStartPoint() == null) {
            focusOnLocation(latLng);
        }

        mViewModel.setStartPoint(latLng);

    }
    private void onDestinationSelected(LatLng latLng) {

        // remove previously added marker from map and add new marker to selected location
        if (mDestinationMarker != null) {
            mBinding.mapview.removeMarker(mDestinationMarker);
        }
        mDestinationMarker = createMarker(latLng, R.drawable.pin);
        mBinding.mapview.addMarker(mDestinationMarker);

        focusOnLocation(latLng);

        // load address detail for selected location
        mViewModel.loadAddressForLocation(latLng);
        mViewModel.setEndPoint(latLng);

    }

    private Marker createMarker(LatLng latLng, int iconResource) {

        MarkerStyleBuilder markStCr = new MarkerStyleBuilder();
        markStCr.setSize(30f);

        Drawable drawable = ContextCompat.getDrawable(this, iconResource);
        if (drawable != null) {
            Bitmap markerBitmap = BitmapUtils.createBitmapFromAndroidBitmap(Util.drawableToBitmap(drawable));
            markStCr.setBitmap(markerBitmap);
        }

        return new Marker(latLng, markStCr.buildStyle());

    }

    private void focusOnLocation(LatLng latLng) {

        if (latLng != null) {
            mBinding.mapview.moveCamera(latLng, 0.25f);
            mBinding.mapview.setZoom(15f, 0.25f);
        }

    }

    private void showPathOnMap(ArrayList<LatLng> routePoints) {

        if (mRoutingPathPolyLine != null) {
            mBinding.mapview.removePolyline(mRoutingPathPolyLine);
        }
        mRoutingPathPolyLine = new Polyline(routePoints, getLineStyle());
        mBinding.mapview.addPolyline(mRoutingPathPolyLine);

        // setup map camera to show whole path
        LatLngBounds latLngBounds = new LatLngBounds(mViewModel.getStartPoint(), mViewModel.getEndPoint());
        int mapWidth = Math.min(mBinding.mapview.getWidth(), mBinding.mapview.getHeight());
        ScreenBounds screenBounds = new ScreenBounds(
                new ScreenPos(0, 0),
//                new ScreenPos(mBinding.mapview.getWidth(), mBinding.mapview.getHeight())
                new ScreenPos(mapWidth, mapWidth)
        );
        mBinding.mapview.moveToCameraBounds(latLngBounds, screenBounds, true, 0.5f);
    }

    private LineStyle getLineStyle() {
        LineStyleBuilder lineStCr = new LineStyleBuilder();
        Color color = new Color(ContextCompat.getColor(this, R.color.colorPrimaryDim75));
        lineStCr.setColor(color);
        lineStCr.setWidth(10f);
        lineStCr.setStretchFactor(0f);
        return lineStCr.buildStyle();
    }

    @Override
    protected void onDestroy() {

        // cancel location updates and stop service
        mLocationManager.stopLocationUpdates();

        super.onDestroy();

    }
}