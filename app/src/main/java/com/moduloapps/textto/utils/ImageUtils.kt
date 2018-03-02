package com.moduloapps.textto.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.moduloapps.textto.api.ApiService
import com.moduloapps.textto.api.RetryCallback
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
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
                bitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, outStream)
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

        fun compressImage(inStream: InputStream, format: Bitmap.CompressFormat, quality: Int = 65): ByteArray {
            val bitmap = BitmapFactory.decodeStream(inStream)
            val outStream = ByteArrayOutputStream()
            bitmap.compress(format, quality, outStream)
            bitmap.recycle()
            outStream.close()
            return outStream.toByteArray()
        }

        fun compressImage(inputStream: InputStream, contentType: String): ByteArray {
            val bytes: ByteArray = when (contentType) {
                "image/gif" -> {
                    val byteArray = ByteArray(inputStream.available())
                    while (inputStream.read(byteArray) != -1);
                    byteArray
                }
                "image/png" -> compressImage(inputStream, Bitmap.CompressFormat.PNG)
                else -> compressImage(inputStream, Bitmap.CompressFormat.JPEG)
            }
            inputStream.close()
            return bytes
        }

        fun createThumbnail(uri: Uri, context: Context): String? {
            var inStream = context.contentResolver.openInputStream(uri)
            val inOptions = BitmapFactory.Options()
            inOptions.inJustDecodeBounds = true
            BitmapFactory.decodeStream(inStream, null, inOptions)
            inStream.close()

            if ((inOptions.outWidth == -1) || (inOptions.outHeight == -1)) return null

            val scale = Math.min(10.0 / inOptions.outWidth, 10.0 / inOptions.outHeight)

            val width = inOptions.outWidth * scale
            val height = inOptions.outHeight * scale

            inStream = context.contentResolver.openInputStream(uri)

            val thumb =  ThumbnailUtils.extractThumbnail(BitmapFactory.decodeStream(inStream), width.toInt(), height.toInt())
            return imageToBase64(thumb)
        }

        fun imageToBase64(image: Bitmap): String {
            val outStream = ByteArrayOutputStream()
            image.compress(Bitmap.CompressFormat.PNG, 100, outStream)
            val bytes = outStream.toByteArray()
            return Base64.encodeToString(bytes, Base64.NO_WRAP)
        }

        fun byteArrayToBase64(bytes: ByteArray): String {
            return Base64.encodeToString(bytes, Base64.NO_WRAP)
        }

        fun getSampleRatio(ratio: Double): Int {
            val i = Integer.highestOneBit(Math.floor(ratio).toInt())
            return if (i == 0) 1 else i
        }

    }

}

