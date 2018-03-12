package com.moduloapps.textto.model

import android.text.TextUtils
import com.moduloapps.textto.encryption.EncryptionHelper

/**
 * Created by hudson on 8/6/17.
 */
data class MmsPart(val androidId: Int,
                   var data: String,
                   val contentType: String,
                   val messageId: Int?,
                   val imageUrl: String?,
                   var thumbnail: String?) {

    fun encrypt(encryptionHelper: EncryptionHelper) {
        if (!TextUtils.isEmpty(data)) {
            data = encryptionHelper.encrypt(data)
        }

        if (!TextUtils.isEmpty(thumbnail)) {
            thumbnail = encryptionHelper.encrypt(thumbnail!!)
        }
    }

}