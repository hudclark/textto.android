/*
 * Copyright (c) 2018. Modulo Apps LLC
 */

package com.moduloapps.textto.model

import com.moduloapps.textto.encryption.EncryptionHelper

/**
 * Created by hudson on 3/23/18.
 */
data class Notification(
        var title: String,
        var subtitle: String?,
        var thumbnail: String,
        var encrypted: Boolean = false) {


    fun encrypt (helper: EncryptionHelper) {
        title = helper.encrypt(title)
        subtitle?.let { subtitle = helper.encrypt(it) }
        thumbnail = helper.encrypt(thumbnail)
        encrypted = true
    }

}