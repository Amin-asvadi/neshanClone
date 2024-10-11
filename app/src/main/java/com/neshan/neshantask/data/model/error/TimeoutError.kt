package com.neshan.neshantask.data.model.error


class TimeoutError : GeneralError {
    companion object {
        fun instance() = TimeoutError()
    }
}
