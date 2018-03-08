/*
 * Copyright (c) 2018. Modulo Apps LLC
 */

package com.moduloapps.textto

import com.moduloapps.textto.persistance.Persistence

/**
 * Created by hudson on 3/8/18.
 */
class TestPersistence : Persistence {
    override fun getString(key: String, default: String?): String? {
        return null
    }

    override fun putString(key: String, value: String?) {
    }
}