package com.neshan.neshantask;

import android.annotation.SuppressLint;
import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.neshan.neshantask.core.util.Util;
import com.neshan.neshantask.data.model.enums.RoutingType;
import com.neshan.neshantask.data.model.error.GeneralError;
import com.neshan.neshantask.data.model.error.SimpleError;
import com.neshan.neshantask.data.model.response.AddressDetailResponse;
import com.neshan.neshantask.data.model.response.Route;
import com.neshan.neshantask.data.model.response.RoutingResponse;
import com.neshan.neshantask.data.model.response.Step;

import org.neshan.common.model.LatLng;
import org.neshan.common.utils.PolylineEncoding;

import com.neshan.neshantask.data.network.Result;
import com.neshan.neshantask.data.util.Event;

import java.util.ArrayList;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

@HiltViewModel
public class MainViewModel extends AndroidViewModel {

    private final MainModel mModel;

    private final CompositeDisposable mCompositeDisposable;

    private final MutableLiveData<Event<GeneralError>> mGeneralError;

    private final MutableLiveData<Result<AddressDetailResponse>> mLocationAddressDetail;

    private final MutableLiveData<RoutingResponse> mRoutingDetail;

    private final MutableLiveData<ArrayList<LatLng>> mRoutePoints;

    private LatLng mStartPoint = null;

    private LatLng mEndPoint = null;

    @Inject
    public MainViewModel(@NonNull Application application, MainModel model) {
        super(application);

        mModel = model;
        mCompositeDisposable = new CompositeDisposable();
        mGeneralError = new MutableLiveData<>();
        mLocationAddressDetail = new MutableLiveData<>();
        mRoutingDetail = new MutableLiveData<>();
        mRoutePoints = new MutableLiveData<>();

    }

    public LiveData<Event<GeneralError>> getGeneralErrorLiveData() {
        return mGeneralError;
    }

    public LiveData<Result<AddressDetailResponse>> getLocationAddressDetailLiveData() {
        return mLocationAddressDetail;
    }

    public LiveData<RoutingResponse> getRoutingDetailLiveData() {
        return mRoutingDetail;
    }

    public LiveData<ArrayList<LatLng>> getRoutePoints() {
        return mRoutePoints;
    }

    public LatLng getStartPoint() {
        return mStartPoint;
    }

    public void setStartPoint(LatLng latLng) {
        mStartPoint = latLng;
    }

    public LatLng getEndPoint() {
        return mEndPoint;
    }

    public void setEndPoint(LatLng latLng) {
        mEndPoint = latLng;
    }

    @SuppressLint("CheckResult")
    public void loadAddressForLocation(LatLng latLng) {
        mLocationAddressDetail.postValue(Result.loading());
        mModel.getAddress(latLng.getLatitude(), latLng.getLongitude())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        addressDetailResponse -> {
                            // Success: Post the result to mLocationAddressDetail
                            Result<AddressDetailResponse> successResult = Result.success(addressDetailResponse);
                            mLocationAddressDetail.postValue(successResult);

                            // Check if the result is a success by comparing the status
                            if (successResult.getStatus() == Result.Status.SUCCESS) {
                                // Further actions on success, e.g., logging, UI updates, etc.
                                AddressDetailResponse data = successResult.getData();
                                // Do something with the data
                            }
                        },
                        throwable -> {
                            // Error: Handle the error and post a failure result
                            mLocationAddressDetail.postValue(Result.error(throwable));
                        }
                );
    }

    public void loadDirection(RoutingType routingType) {
        if (mStartPoint == null) {
            SimpleError error = new SimpleError(getApplication().getString(R.string.start_point_not_selected));
            mGeneralError.postValue(new Event<>(error));
        } else if (mEndPoint == null) {
            SimpleError error = new SimpleError(getApplication().getString(R.string.end_point_not_selected));
            mGeneralError.postValue(new Event<>(error));
        } else {
            mModel.getDirection(routingType, mStartPoint, mEndPoint, 0)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<>() {
                        @Override
                        public void onSubscribe(Disposable disposable) {
                            mCompositeDisposable.add(disposable);
                        }

                        @Override
                        public void onSuccess(RoutingResponse response) {
                            if (response.getRoutes() != null) {

                                mRoutingDetail.postValue(response);

                                try {
                                    Route route = response.getRoutes().get(0);

                                    ArrayList<LatLng> decodedStepByStepPath = new ArrayList<>();
                                    for (Step step : route.getLegs().get(0).getSteps()) {
                                        decodedStepByStepPath.addAll(PolylineEncoding.decode(step.getEncodedPolyline()));
                                    }

                                    mRoutePoints.postValue(decodedStepByStepPath);
                                } catch (NullPointerException exception) {
                                    SimpleError error = new SimpleError(getApplication().getString(R.string.routing_failure));
                                    mGeneralError.postValue(new Event<>(error));
                                    exception.printStackTrace();
                                }
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            mGeneralError.postValue(new Event<>(Util.getError(e)));
                        }
                    });
        }
    }

    @Override
    protected void onCleared() {

        // disposes any incomplete request to avoid possible error also unnecessary network usage
        if (!mCompositeDisposable.isDisposed()) {
            mCompositeDisposable.dispose();
        }

        super.onCleared();

    }

}

