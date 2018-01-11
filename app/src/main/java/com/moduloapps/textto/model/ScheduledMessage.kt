package com.moduloapps.textto.model

/**
 * Created by hudson on 9/18/16.
 */
data class ScheduledMessage(val _id: String,
                            val addresses: Array<String>,
                            var sent: Boolean?,
                            val body: String?,
                            val fileUrl: String?,
                            val retries: Int?)
