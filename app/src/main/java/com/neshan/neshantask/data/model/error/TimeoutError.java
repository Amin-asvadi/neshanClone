package com.neshan.neshantask.data.model.error;

public class TimeoutError implements GeneralError {

    // Private constructor to prevent instantiation from outside
    private TimeoutError() {
        super(); // Call to the superclass constructor (GeneralError)
    }

    // Static method to provide an instance of TimeoutError
    public static TimeoutError instance() {
        return new TimeoutError();
    }
}