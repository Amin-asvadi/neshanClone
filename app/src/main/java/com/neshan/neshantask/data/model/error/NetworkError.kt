package com.neshan.neshantask.data.model.error

class NetworkError : GeneralError {
    companion object {
        fun instance() = NetworkError()
    }
}