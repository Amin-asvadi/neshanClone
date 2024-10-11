package com.neshan.neshantask.core.location

import android.location.Location

interface LocationListener {

    fun onLastLocation(location: Location)

    fun onLocationChange(location: Location)

}