package com.neshan.neshantask.data.model.response;

import com.google.gson.annotations.SerializedName;

public class AddressDetailResponse extends NeshanResponse {
    private String neighbourhood;
    private String state;
    private String city;
    @SerializedName("route_name")
    private String routeName;
    @SerializedName("formatted_address")
    private String address;

    public String getNeighbourhood() {
        return neighbourhood;
    }

    public void setNeighbourhood(String neighbourhood) {
        this.neighbourhood = neighbourhood;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getRouteName() {
        return routeName;
    }

    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
