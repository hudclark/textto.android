package com.moduloapps.textto.model

/**
 * Created by hudson on 10/7/17.
 */
data class StatusUpdate(val mms: Map<String, Long>?,
                        val sms: Map<String, Long>?,
                        val scheduledMessages: Array<ScheduledMessage>)