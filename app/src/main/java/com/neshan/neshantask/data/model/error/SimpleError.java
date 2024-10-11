package com.neshan.neshantask.data.model.error;


public class SimpleError implements GeneralError {
    private final String errorMessage;

    // Constructor to initialize errorMessage
    public SimpleError(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    // Getter method for errorMessage
    public String getErrorMessage() {
        return errorMessage;
    }
}