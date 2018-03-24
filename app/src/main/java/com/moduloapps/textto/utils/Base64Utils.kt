/*
 * Copyright (c) 2018. Modulo Apps LLC
 */

package com.moduloapps.textto.utils

import android.util.Base64
import java.nio.charset.Charset

/**
 * Created by hudson on 3/24/18.
 */
fun ByteArray.toBase64() = String(Base64.encode(this, Base64.NO_WRAP), Charset.forName("UTF-8"))

fun String.fromBase64() = Base64.decode(this, Base64.NO_WRAP)
