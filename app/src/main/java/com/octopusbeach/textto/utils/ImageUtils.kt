package com.octopusbeach.textto.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Created by hudson on 10/13/17.
 */

class ImageUtils {

    companion object {

        val TAG = "ImageUtils"

        fun compressImage(inStream: InputStream, compressedSize: Int): ByteArray {
            val bitmap = BitmapFactory.decodeStream(inStream)
            val outStream = ByteArrayOutputStream()
            var compressQuality = 100
            var streamLength = compressedSize

            while (streamLength >= compressedSize && compressQuality > 5) {
                try {
                    outStream.flush()
                    outStream.reset()
                } catch (e: Exception) {
                    Log.e(TAG, "Error compressing image", e)
                }
                Log.d(TAG, "Compressing image to $compressQuality% quality...")
                // Try losses compression first.
                val format = if (compressQuality == 100) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                bitmap.compress(format, compressQuality, outStream)
                streamLength = outStream.size()
                if (compressQuality == 100) {
                    Log.d(TAG, "Original size is ${outStream.size() / 1024}kb")
                }
                compressQuality -= 5
            }

            outStream.close()
            bitmap.recycle()

            Log.d(TAG, "Final size is ${streamLength / 1024}kb")
            return outStream.toByteArray()
        }

        fun compressImage(inStream: InputStream, format: Bitmap.CompressFormat, quality: Int = 70): ByteArray {
            val bitmap = BitmapFactory.decodeStream(inStream)
            val outStream = ByteArrayOutputStream()
            bitmap.compress(format, quality, outStream)
            bitmap.recycle()
            outStream.close()
            return outStream.toByteArray()
        }
    }

}

