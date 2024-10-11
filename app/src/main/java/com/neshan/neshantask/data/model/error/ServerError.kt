package com.neshan.neshantask.data.model.error

import com.neshan.neshantask.data.model.error.GeneralError

class ServerError(val statusCode: Int, val errorList: List<String>? = null) : GeneralError
