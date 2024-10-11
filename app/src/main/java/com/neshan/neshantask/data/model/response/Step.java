package com.neshan.neshantask.data.model.response;

import com.google.gson.annotations.SerializedName;

public class Step {
    private String name; // نام مرحله
    private String instruction; // دستورالعمل
    private Distance distance; // فاصله
    private Duration duration; // زمان
    @SerializedName("bearing_after")
    private int bearingAfter; // زاویه بعد از مرحله
    @SerializedName("polyline")
    private String encodedPolyline; // خطی که مرحله را نشان می‌دهد

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public Distance getDistance() {
        return distance;
    }

    public void setDistance(Distance distance) {
        this.distance = distance;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public int getBearingAfter() {
        return bearingAfter;
    }

    public void setBearingAfter(int bearingAfter) {
        this.bearingAfter = bearingAfter;
    }

    public String getEncodedPolyline() {
        return encodedPolyline;
    }

    public void setEncodedPolyline(String encodedPolyline) {
        this.encodedPolyline = encodedPolyline;
    }
}
