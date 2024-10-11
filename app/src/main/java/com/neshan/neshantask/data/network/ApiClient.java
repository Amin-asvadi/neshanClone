package com.neshan.neshantask.data.network;

import io.reactivex.rxjava3.core.Single;
import com.neshan.neshantask.data.model.response.AddressDetailResponse;
import com.neshan.neshantask.data.model.response.RoutingResponse;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * The API interface for all required server APIs
 */
public interface ApiClient {

    /**
     * Loads address detail for a specific location by latitude and longitude.
     * 
     * @param lat latitude for desired location
     * @param lng longitude for desired location
     * @return a Single emitting AddressDetailResponse
     */
    @GET("v4/reverse")
    Single<AddressDetailResponse> getAddress(
            @Query("lat") double lat,
            @Query("lng") double lng
    );

    /**
     * Gets routes from start point to end point.
     * 
     * @param type routing type, one of [RoutingType] values
     * @param startPoint start point coordinates formatted as "latitude,longitude"
     * @param endPoint end point coordinates formatted as "latitude,longitude"
     * @param bearing a value between 0 and 360
     * @return a Single emitting RoutingResponse
     */
    @GET("v4/direction/no-traffic")
    Single<RoutingResponse> getDirection(
            @Query("type") String type,
            @Query("origin") String startPoint,
            @Query("destination") String endPoint,
            @Query("bearing") int bearing
    );
}
