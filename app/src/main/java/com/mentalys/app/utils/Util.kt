package com.mentalys.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import com.mentalys.app.R
import androidx.exifinterface.media.ExifInterface
import com.google.android.material.snackbar.Snackbar
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** String-resource overload that funnels into the branded toast. */
fun showToast(context: Context, @androidx.annotation.StringRes resId: Int) {
    showToast(context, context.getString(resId))
}

/**
 * Branded Mentalys toast — same look across the whole app (logo + text on a
 * surface card). On Android 12+ the system may still render the standard
 * Toast UI for background apps; that fallback degrades gracefully.
 */
fun showToast(context: Context, text: String) {
    if (text.isBlank()) return

    try {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.view_custom_toast, null, false)
        view.findViewById<ImageView>(R.id.toast_icon).setImageResource(R.drawable.ic_logo)
        view.findViewById<TextView>(R.id.toast_text).text = text

        @Suppress("DEPRECATION")
        Toast(context.applicationContext).apply {
            duration = Toast.LENGTH_SHORT
            this.view = view
            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 120)
            show()
        }
    } catch (_: Exception) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }
}

fun showSnackbar(view: View, message: String) {
    Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
}

//Camera X
private const val MAXIMAL_SIZE = 1000000 //1 MB
private const val FILENAME_FORMAT = "yyyyMMdd_HHmmss"
private val timeStamp: String = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(Date())


fun createCustomTempFile(context: Context): File {
    val filesDir = context.externalCacheDir
    return File.createTempFile(timeStamp, ".jpg", filesDir)
}

fun uriToFile(imageUri: Uri, context: Context): File {
    val myFile = createCustomTempFile(context)
    val inputStream = context.contentResolver.openInputStream(imageUri) as InputStream
    val outputStream = FileOutputStream(myFile)
    val buffer = ByteArray(1024)
    var length: Int
    while (inputStream.read(buffer).also { length = it } > 0) outputStream.write(buffer, 0, length)
    outputStream.close()
    inputStream.close()
    return myFile
}

fun File.reduceFileImage(): File {
    val file = this
    val bitmap = BitmapFactory.decodeFile(file.path).getRotatedBitmap(file)
    var compressQuality = 100
    var streamLength: Int
    do {
        val bmpStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, bmpStream)
        val bmpPicByteArray = bmpStream.toByteArray()
        streamLength = bmpPicByteArray.size
        compressQuality -= 5
    } while (streamLength > MAXIMAL_SIZE)
    bitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, FileOutputStream(file))
    return file
}

fun Bitmap.getRotatedBitmap(file: File): Bitmap {
    val orientation = ExifInterface(file).getAttributeInt(
        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED
    )
    return when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(this, 90F)
        ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(this, 180F)
        ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(this, 270F)
        ExifInterface.ORIENTATION_NORMAL -> this
        else -> this
    }
}

fun rotateImage(source: Bitmap, angle: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(angle)
    return Bitmap.createBitmap(
        source, 0, 0, source.width, source.height, matrix, true
    )
}

fun formatTimestamp(timestamp: String): String {
    val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    isoFormatter.timeZone = TimeZone.getTimeZone("UTC")
    return try {
        val date = isoFormatter.parse(timestamp)
        val outputFormatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        outputFormatter.format(date ?: return "Invalid Date")
    } catch (e: ParseException) {
        "Invalid Date"
    }
}


//AUDIO
object AudioUtils {
    private const val AUDIO_FOLDER = "audio_recordings"

    fun createAudioFile(context: Context, timestamp: Long, extension: String = ".wav"): File? {
        return try {
            val directory = File(context.getExternalFilesDir(null), AUDIO_FOLDER)
            if (!directory.exists()) {
                directory.mkdirs()
            }
            File(directory, "voice_test_$timestamp$extension")
        } catch (e: Exception) {
            Log.e("AudioUtils", "Error creating audio file: ${e.message}", e)
            null
        }
    }

    fun deleteRecording(file: File) {
        try {
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e("AudioUtils", "Error deleting recording: ${e.message}", e)
        }
    }
}

data class AudioRecording(
    val file: File,
    val timestamp: Long,
    val duration: Long
)

fun getErrorMessage(e: Exception): String {
    return when (e) {
        is UnknownHostException -> "No internet connection. Please check your network."
        is SocketTimeoutException -> "The request timed out. Please try again."
        else -> e.message ?: "An unexpected error occurred."
    }
}

fun openAndroidXBrowser(context: Context, url: String) {
    val customTabsIntent = CustomTabsIntent.Builder()
        .setShowTitle(true)
        .setShareState(CustomTabsIntent.SHARE_STATE_ON)
        .build()

    try {
        customTabsIntent.launchUrl(context, Uri.parse(url))
    } catch (e: Exception) {
        showToast(context, "Unable to open the link. Please try again.")
    }
}

fun formatToIDR(amount: Int): String {
    val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
    return format.format(amount)
}
