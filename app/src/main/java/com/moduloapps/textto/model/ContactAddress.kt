/*
 * Copyright (c) 2018. Modulo Apps LLC
 */

package com.moduloapps.textto.model

/**
 * Created by hudson on 2/15/18.
 *
 * Contact v2 format:
 * {
 *      name: string,
 *      androidId: number,
 *      image: [string],
 *      addresses: [
 *          type: string,
 *          address: string
 *      ]
 * }
 */
data class ContactAddress(
        val type: String?,
        val address: String
)
