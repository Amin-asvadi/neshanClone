package com.neshan.neshantask.data.model.error;

import java.util.ArrayList;
import java.util.List;

public class ServerError implements GeneralError {
    private int statusCode;
    private List<String> errorList;

    // Constructor
    public ServerError(int statusCode, List<String> errorList) {
        this.statusCode = statusCode;
        this.errorList = errorList;
    }

    // Getter methods
    public int getStatusCode() {
        return statusCode;
    }

    public List<String> getErrorList() {
        return errorList;
    }

    // Static method to create ServerError from HTTP status code
    public static ServerError fromCode(int statusCode) {
        // You can customize the error messages based on the status code
        List<String> errorMessages = new ArrayList<>();
        switch (statusCode) {
            case 400:
                errorMessages.add("Bad Request");
                break;
            case 401:
                errorMessages.add("Unauthorized");
                break;
            case 403:
                errorMessages.add("Forbidden");
                break;
            case 404:
                errorMessages.add("Not Found");
                break;
            case 500:
                errorMessages.add("Internal Server Error");
                break;
            default:
                errorMessages.add("Unknown Error");
                break;
        }
        return new ServerError(statusCode, errorMessages);
    }
}
