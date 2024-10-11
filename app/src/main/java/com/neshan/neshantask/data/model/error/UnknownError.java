package com.neshan.neshantask.data.model.error;

public class UnknownError implements GeneralError{
    // Private constructor to prevent direct instantiation
    private UnknownError() {
    }

    // Static method to get an instance of UnknownError
    public static UnknownError instance() {
        return new UnknownError();
    }
}