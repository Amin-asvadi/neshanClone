package com.neshan.neshantask.data.model.error;

public class NetworkError implements GeneralError{

    // Private constructor to prevent instantiation from outside
    private NetworkError() {
        super(); // Call to the superclass constructor (GeneralError)
    }

    // Static method to provide an instance of NetworkError
    public static NetworkError instance() {
        return new NetworkError();
    }
}