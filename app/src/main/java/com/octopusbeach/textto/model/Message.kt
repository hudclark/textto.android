package com.octopusbeach.textto.model


/**
 * Created by hudson on 9/5/16.
 */
data class Message(val androidId: Int?,
                   val body: String,
                   val address: String,
                   val status: String,
                   val date: Long,
                   var name: String,
                   val threadId: Int)

