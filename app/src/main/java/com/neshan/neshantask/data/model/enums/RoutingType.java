package com.neshan.neshantask.data.model.enums;

public enum RoutingType {
    CAR("car"),
    MOTORCYCLE("motorcycle");

    private final String value;

    RoutingType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
