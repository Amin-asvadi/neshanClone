package com.neshan.neshantask.data.network;

import com.neshan.neshantask.data.model.response.AddressDetailResponse;

/**
 * A generic class that holds a value with its loading status.
 *
 * Result is usually created by the Repository classes where they return
 * `LiveData<Result<T>>` to pass back the latest data to the UI with its fetch status.
 */
public class Result<T> {

    private final Status status;
    private final T data;
    private final Throwable error;

    private Result(Status status, T data, Throwable error) {
        this.status = status;
        this.data = data;
        this.error = error;
    }

    public Status getStatus() {
        return status;
    }

    public T getData() {
        return data;
    }

    public Throwable getError() {
        return error;
    }

    public enum Status {
        SUCCESS, ERROR, LOADING
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(Status.SUCCESS, data, null);
    }

    public static <T> Result<T> success() {
        return new Result<>(Status.SUCCESS, null, null);
    }

    public static <T> Result<T> error(Throwable error) {
        return new Result<>(Status.ERROR, null, error);
    }

    public static <T> Result<T> loading() {
        return new Result<>(Status.LOADING, null, null);
    }
}
