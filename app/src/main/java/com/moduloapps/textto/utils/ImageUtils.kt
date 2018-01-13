package com.moduloapps.textto.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

        fun compressImage(inStream: InputStream, format: Bitmap.CompressFormat, quality: Int = 65): ByteArray {
            val bitmap = BitmapFactory.decodeStream(inStream)
            val outStream = ByteArrayOutputStream()
            bitmap.compress(format, quality, outStream)
            bitmap.recycle()
            outStream.close()
            return outStream.toByteArray()
        }

        fun createThumbnail(uri: Uri, context: Context): String? {
            var inStream = context.contentResolver.openInputStream(uri)
            val inOptions = BitmapFactory.Options()
            inOptions.inJustDecodeBounds = true
            BitmapFactory.decodeStream(inStream, null, inOptions)
            inStream.close()

            if ((inOptions.outWidth == -1) || (inOptions.outHeight == -1)) return null

            val originalSize = if (inOptions.outHeight > inOptions.outWidth) inOptions.outHeight
                               else inOptions.outWidth
            val ratio = (originalSize / 50).toDouble()

            val outOptions = BitmapFactory.Options()
            outOptions.inSampleSize = getSampleRatio(ratio)

            inStream = context.contentResolver.openInputStream(uri)

            val bitmap = BitmapFactory.decodeStream(inStream, null, outOptions)
            inStream.close()
            val imageString = imageToBase64(bitmap)
            bitmap.recycle()

            return imageString
        }

        fun uploadImage(inputStream: InputStream, contentType: String, imageUrl: String, apiService: ApiService) {
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

            val body = RequestBody.create(MediaType.parse(contentType), bytes)
            apiService.uploadImage(imageUrl, body).enqueue(object: RetryCallback<Void>(10, 1000) {
                override fun onResponse(call: Call<Void>?, response: Response<Void>?) {
                    Log.d(TAG, "Uploaded mms image")
                }

                override fun onFailed(t: Throwable) {
                    // TODO could put into a queue to run when we have internet again
                    Log.d(TAG, "Failed to upload mms image", t)
                }
            })
        }

        fun imageToBase64(image: Bitmap): String {
            val outStream = ByteArrayOutputStream()
            image.compress(Bitmap.CompressFormat.PNG, 100, outStream)
            val bytes = outStream.toByteArray()
            return Base64.encodeToString(bytes, Base64.NO_WRAP)
        }

        fun getSampleRatio(ratio: Double): Int {
            val i = Integer.highestOneBit(Math.floor(ratio).toInt())
            return if (i == 0) 1 else i
        }

    }

}

