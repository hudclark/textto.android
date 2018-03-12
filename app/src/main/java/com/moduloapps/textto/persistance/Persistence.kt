/*
 * Copyright (c) 2018. Modulo Apps LLC
 */

package com.moduloapps.textto.persistance

/**
 * Created by hudson on 3/8/18.
 */
interface Persistence {

    fun putString(key: String, value: String?)
    fun getString(key: String, default: String?): String?

}