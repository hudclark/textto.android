package com.moduloapps.textto.model

import android.text.TextUtils
import com.moduloapps.textto.encryption.EncryptionHelper


/**
 * Created by hudson on 9/5/16.
 */
data class Message(val androidId: Int,
                   var body: String?,
                   val addresses: List<String>,
                   val type: String,
                   val sender: String,
                   val date: Long,
                   val threadId: Int,
                   var encrypted: Boolean) {


    fun encrypt(encryptionHelper: EncryptionHelper) {
        if (!TextUtils.isEmpty(body)) {
            body = encryptionHelper.encrypt(body!!)
        }
        encrypted = true
    }


}

