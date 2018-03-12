package com.moduloapps.textto.model

import android.text.TextUtils
import com.moduloapps.textto.encryption.EncryptionHelper

/**
 * Created by hudson on 9/18/16.
 */
data class ScheduledMessage(val _id: String,
                            val addresses: Array<String>,
                            var sent: Boolean?,
                            var body: String?,
                            val fileUrl: String?,
                            val retries: Int?,
                            val encrypted: Boolean) {


    fun decrypt(encryptionHelper: EncryptionHelper) {
        if (!TextUtils.isEmpty(body)) {
            this.body = encryptionHelper.decrypt(body!!)
        }
    }

}
