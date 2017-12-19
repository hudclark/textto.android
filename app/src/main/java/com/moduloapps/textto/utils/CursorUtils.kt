package com.moduloapps.textto.utils

import android.database.Cursor
import android.util.Log
import com.crashlytics.android.Crashlytics

/**
 * Created by hudson on 12/18/17.
 */

inline fun Cursor.withFirst(fn: (Cursor) -> Unit) {
    if (moveToFirst()) {
        fn(this)
    }
}

inline fun <T> Cursor.find(fn: (Cursor) -> T?): T? {
    if (moveToFirst()) {
        do {
            val result = fn(this)
            if (result != null) return result
        } while (moveToNext())
    }
    return null
}

inline fun Cursor.forEach(fn: (Cursor) -> Unit) {
    if (moveToFirst()) {
        do {
            fn(this)
        } while (moveToNext())
    }
}

inline fun Cursor.tryForEach(fn: (Cursor) -> Unit) {
    if (moveToFirst()) {
        do {
            try {
                fn(this)
            } catch (e: Exception) {
                Log.e("CursorUtils", e.toString())
                Crashlytics.logException(e)
            }
        } while (moveToNext())
    }
}

