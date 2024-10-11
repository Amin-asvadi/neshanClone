package com.neshan.neshantask.navigation;

import static java.lang.Math.sqrt;

import android.animation.ValueAnimator;
import android.app.Application;
import android.location.Location;
import android.util.Log;

import androidx.databinding.ObservableField;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.neshan.neshantask.core.util.Util;
import com.neshan.neshantask.data.model.enums.RoutingType;
import com.neshan.neshantask.data.model.error.GeneralError;
import com.neshan.neshantask.data.model.response.Leg;
import com.neshan.neshantask.data.model.response.Route;
import com.neshan.neshantask.data.model.response.RoutingResponse;
import com.neshan.neshantask.data.model.response.Step;
import com.neshan.neshantask.data.util.Event;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import org.neshan.common.model.LatLng;
import org.neshan.common.utils.PolylineEncoding;

import java.util.ArrayList;

import javax.inject.Inject;

@HiltViewModel
public class NavigationViewModel extends AndroidViewModel {

    private static final int MAX_DISTANCE_TOLERANCE_IN_METERS = 50;
    private static final int MAX_BACKWARD_MOVEMENT_TOLERANCE_IN_METERS = 2;
    private static final float DEFAULT_AVERAGE_SPEED_FOR_CAR = 200f;

    public final ObservableField<String> duration = new ObservableField<>();
    public final ObservableField<String> distance = new ObservableField<>();

    private final CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    private LatLng mStartPoint;
    private LatLng mEndPoint;

    private final NavigationModel mModel; // تعریف متغیر mModel

    private final MutableLiveData<Event<GeneralError>> _generalError = new MutableLiveData<>();
    public LiveData<Event<GeneralError>> generalError = _generalError;

    // remained points for routing
    private final MutableLiveData<ArrayList<LatLng>> _progressPoints = new MutableLiveData<>();
    public LiveData<ArrayList<LatLng>> progressPoints = _progressPoints;

    private final MutableLiveData<Boolean> _reachedDestination = new MutableLiveData<>();
    public LiveData<Boolean> reachedDestination = _reachedDestination;

    private final MutableLiveData<LatLng> _markerPosition = new MutableLiveData<>();
    public LiveData<LatLng> markerPosition = _markerPosition;

    private ArrayList<LatLng> mRoutingPoints;
    private Location mUserLocation;
    private final SpeedCalculator mSpeedCalculator = new SpeedCalculator(DEFAULT_AVERAGE_SPEED_FOR_CAR);
    private boolean mLoadingDirection = false;
    private int mLastReachedPointIndex = 0;
    private LatLng mLastStartingPoint;
    private ValueAnimator mMarkerAnimator;

    @Inject
    public NavigationViewModel(Application application, NavigationModel mModel) {
        super(application);
        this.mModel = mModel;
    }
    public void startNavigation(LatLng startPoint, LatLng endPoint) {
        mStartPoint = startPoint;
        mEndPoint = endPoint;
        loadDirection(mStartPoint, mEndPoint, RoutingType.CAR, 0);
    }

    @Override
    protected void onCleared() {
        // disposes any incomplete request to avoid possible error also unnecessary network usage
        if (!mCompositeDisposable.isDisposed()) {
            mCompositeDisposable.dispose();
        }
        cancelMarkerAnimation();
        super.onCleared();
    }

    /**
     * set user location and start updating movement speed and calculate
     * passed points
     */
    public void updateUserLocation(Location location) {
        mUserLocation = location;
        mSpeedCalculator.update(new LatLng(location.getLatitude(), location.getLongitude()));

        // if loading direction -> avoid updating progress
        if (mRoutingPoints != null && !mRoutingPoints.isEmpty() && !mLoadingDirection) {
            calculateUserProgress(mRoutingPoints);
        }
    }

    /**
     * try to load direction detail from server
     */
    private void loadDirection(LatLng startPoint, LatLng endPoint, RoutingType routingType, int bearing) {
        if (!mLoadingDirection) {
            mLoadingDirection = true;
            mModel.getDirection(routingType, startPoint, endPoint, bearing)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<RoutingResponse>() {
                        @Override
                        public void onSubscribe(Disposable disposable) {
                            mCompositeDisposable.add(disposable);
                        }

                        @Override
                        public void onSuccess(RoutingResponse response) {
                            mLoadingDirection = false; // بارگذاری مسیر پایان یافت
                            if (response != null && response.getRoutes() != null && !response.getRoutes().isEmpty()) {
                                mRoutingPoints = new ArrayList<>(); // لیست نقاط مسیر را ایجاد کنید
                                try {
                                    Route route = response.getRoutes().get(0); // گرفتن اولین Route

                                    if (route.getLegs() != null && !route.getLegs().isEmpty()) {
                                        Leg leg = route.getLegs().get(0); // گرفتن اولین Leg

                                        for (Step step : leg.getSteps()) {
                                            // اضافه کردن نقاط خطی مرحله به لیست
                                            mRoutingPoints.addAll(PolylineEncoding.decode(step.getEncodedPolyline()));
                                        }

                                        if (mRoutingPoints.size() >= 2) {
                                            _progressPoints.postValue(mRoutingPoints); // ارسال نقاط به UI
                                            _markerPosition.postValue(mRoutingPoints.get(0)); // تنظیم موقعیت نشانگر
                                        }

                                        // تنظیم فاصله و زمان
                                        distance.set(leg.getDistance().getText());
                                        duration.set(leg.getDuration().getText());
                                    } else {
                                        Log.e("RoutingError", "No legs found in the route."); // خطا: هیچ Legی پیدا نشد
                                    }
                                } catch (Exception e) { // Catch کلی برای جلوگیری از NullPointerException
                                    Log.e("RoutingError", "Failed to parse routing details: " + e.getMessage());
                                }
                            } else {
                                Log.e("RoutingError", "No routes found in the response."); // خطا: هیچ مسیری پیدا نشد
                            }
                        }


                        @Override
                        public void onError(Throwable e) {
                            mLoadingDirection = false;
                            GeneralError generalError = Util.getError(e);
                            _generalError.postValue(new Event<>(generalError));
                        }
                    });
        }
    }

    /**
     * calculate remained routing points
     */
    private void calculateUserProgress(ArrayList<LatLng> points) {
        LatLng currentPoint = mRoutingPoints != null ? mRoutingPoints.get(mLastReachedPointIndex) : null;
        LatLng nextPoint = mRoutingPoints != null && mLastReachedPointIndex + 1 < mRoutingPoints.size() ? mRoutingPoints.get(mLastReachedPointIndex + 1) : null;

        if (currentPoint != null && nextPoint != null && mUserLocation != null) {
            LatLng userPoint = new LatLng(mUserLocation.getLatitude(), mUserLocation.getLongitude());

            float[] currentToNextDistance = Util.distanceFrom(currentPoint, nextPoint);
            float[] currentToUserDistance = Util.distanceFrom(currentPoint, userPoint);
            float[] nextToUserDistance = Util.distanceFrom(nextPoint, userPoint);

            // check if user moved backward
            boolean isUserMovedBackward = nextToUserDistance[0] > currentToNextDistance[0]
                    && nextToUserDistance[0] > currentToUserDistance[0]
                    && nextToUserDistance[0] - currentToNextDistance[0] > MAX_BACKWARD_MOVEMENT_TOLERANCE_IN_METERS;

            // check if user has gone far from route
            boolean isUserFarFromRoute = false;
            try {
                float s = (currentToNextDistance[0] + currentToUserDistance[0] + nextToUserDistance[0]) / 2;
                float area = (float) sqrt(s * (s - currentToNextDistance[0]) * (s - currentToUserDistance[0]) * (s - nextToUserDistance[0]));
                float userToRouteDistance = 2 * area / currentToNextDistance[0];

                isUserFarFromRoute = userToRouteDistance > MAX_DISTANCE_TOLERANCE_IN_METERS;
            } catch (Exception e) {
                e.printStackTrace();
            }

            // if user has gone far from route or moved backward -> request direction again
            if (isUserFarFromRoute || isUserMovedBackward) {
                mLastReachedPointIndex = 0;
                // cancel marker animation
                cancelMarkerAnimation();

                // try to recalculate path
                LatLng startPoint = new LatLng(mUserLocation.getLatitude(), mUserLocation.getLongitude());
                loadDirection(startPoint, mEndPoint, RoutingType.CAR, (int) mUserLocation.getBearing());
            } else if (currentToUserDistance[0] >= currentToNextDistance[0] || (currentToNextDistance[0] - currentToUserDistance[0]) < 1) {
                // user reached next point -> update progress
                mLastReachedPointIndex++;

                // get all points after closest point as remained routing points
                ArrayList<LatLng> remainedPoints = new ArrayList<>(mRoutingPoints.subList(mLastReachedPointIndex, points.size()));

                // if no points remained -> reached destination
                if (remainedPoints.size() <= 1) {
                    _reachedDestination.postValue(true);
                } else {
                    LatLng startingPoint = remainedPoints.get(0);

                    // check if start point is new
                    if (mLastStartingPoint == null || !Util.equalsTo(mLastStartingPoint, startingPoint)) {
                        mLastStartingPoint = startingPoint;
                        _progressPoints.postValue(new ArrayList<>(remainedPoints));

                        // Start animating marker
                        startMarkerAnimation(startingPoint, remainedPoints.get(1));
                    }
                }
            }
        }
    }

    /**
     * animates marker position from start point to end point
     */
    private void startMarkerAnimation(LatLng start, LatLng end) {
        if (Util.equalsTo(start, end)) {
            return;
        }

        // cancel marker animation if already running
        cancelMarkerAnimation();

        // animate marker from start point to end point in calculated duration (animationDuration)
        float duration = 1000 * mSpeedCalculator.getDuration(start, end);
        mMarkerAnimator = ValueAnimator.ofFloat(0f, 1f);
        mMarkerAnimator.setDuration((long) duration);
        mMarkerAnimator.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();
            double lat = start.getLatitude() + (end.getLatitude() - start.getLatitude()) * fraction;
            double lng = start.getLongitude() + (end.getLongitude() - start.getLongitude()) * fraction;
            _markerPosition.postValue(new LatLng(lat, lng));
        });
        mMarkerAnimator.start();
    }

    /**
     * cancels marker animation
     */
    private void cancelMarkerAnimation() {
        if (mMarkerAnimator != null) {
            mMarkerAnimator.cancel();
            mMarkerAnimator = null;
        }
    }
    public class SpeedCalculator {
        private int mIndex = 0;
        private final float[] mRecords;
        private long mLastTime = 0;
        private LatLng mLastLocation = null;

        public SpeedCalculator(float defaultSpeed) {
            mRecords = new float[]{defaultSpeed, defaultSpeed, defaultSpeed, defaultSpeed, defaultSpeed};
        }

        public float getAverageSpeedRatio() {
            float sum = 0;
            for (float record : mRecords) {
                sum += record;
            }
            return sum / mRecords.length;
        }

        public void update(LatLng latLng) {
            long newTime = System.currentTimeMillis();

            if (mLastLocation != null) {
                long duration = newTime - mLastTime;

                if (mLastLocation != null) {
                    float[] distanceArray = Util.distanceFrom(mLastLocation, latLng);
                    Float distance = distanceArray[0];

                    if (distance != null && distance > 0 && duration > 0) {
                        float speed = (float) duration / distance;
                        mRecords[mIndex % mRecords.length] = speed;
                        mIndex++;
                    }
                }
            }

            mLastLocation = latLng;
            mLastTime = newTime;
        }

        // محاسبه مدت زمان حرکت از start به end بر اساس سرعت متوسط
        public float getDuration(LatLng start, LatLng end) {
            float[] distanceArray = Util.distanceFrom(start, end);
            float distance = distanceArray[0];

            if (distance > 0) {
                float averageSpeed = getAverageSpeedRatio();
                return distance / averageSpeed; // زمان به ثانیه
            }
            return 0; // اگر فاصله 0 باشد
        }
    }
}

