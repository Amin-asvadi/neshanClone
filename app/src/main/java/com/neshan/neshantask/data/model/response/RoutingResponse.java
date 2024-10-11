package com.neshan.neshantask.data.model.response;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;

public class RoutingResponse extends NeshanResponse {
    private ArrayList<Route> routes;

    public ArrayList<Route> getRoutes() {
        return routes;
    }

    public void setRoutes(ArrayList<Route> routes) {
        this.routes = routes;
    }
}

