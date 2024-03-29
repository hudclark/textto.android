package com.moduloapps.textto.model

/**
 * Created by hudson on 12/10/16.
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
 *
 */
data class Contact(val androidId: Int,
                   val name: String?,
                   val image: String?,
                   val addresses: MutableList<ContactAddress>
)
