package com.octopusbeach.textto.model

/**
 * Created by hudson on 9/5/16.
 */
data class User(val _id: String,
                val displayName: String,
                val firstName: String,
                val lastName: String,
                val email: String,
                val image: String?,
                var firebaseId: String?)
