package com.octopusbeach.textto.model


/**
 * Created by hudson on 9/5/16.
 */
data class Message(val androidId: Int,
                   val body: String?,
                   val addresses: List<String>,
                   val type: String,
                   val sender: String,
                   val date: Long,
                   val threadId: Int)

