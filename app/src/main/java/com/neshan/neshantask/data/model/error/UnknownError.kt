package com.neshan.neshantask.data.model.error

class UnknownError : GeneralError {
    companion object {
        fun instance() = UnknownError()
    }
}