package com.neshan.neshantask.data.model.response

abstract class NeshanResponse {
    val status: String = ""
    val code: Int? = null
    val message: String? = null

    fun isSuccessFull() = status.equals("OK", true)

}
