package com.neshan.neshantask.core.location;

import android.location.Location;

public interface LocationListener {

    void onLastLocation(Location location);

    void onLocationChange(Location location);
}