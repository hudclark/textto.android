/*
 * Copyright (c) 2018. Modulo Apps LLC
 */

package com.moduloapps.textto

import java.util.*
import kotlin.math.roundToInt

/**
 * Created by hudson on 3/23/18.
 */
open class SimpleCache<K, V>(private val maxKeys: Int) {

    private val cache = HashMap<K, V>()

    fun get(key: K) = cache[key]

    open fun put(key: K, value: V) {

        // Are we overfull?
        if (getSize() + 1 > maxKeys) {
            makeSpace()
        }

        cache.put(key, value)
    }

    fun clear () = cache.clear()

    fun getSize () = cache.size

    private fun makeSpace () {
        val keys = cache.keys.toList()
        Collections.shuffle(keys)

        // Remove 30% of keys
        for (i in 0..(keys.size * 0.3).roundToInt()) {
            cache.remove(keys[i])
        }
    }

}
