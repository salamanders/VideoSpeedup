package com.example.videospeedup

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.OutputStream

private val speedupRe = """faster_x(\d+).mp4""".toRegex()
fun getFileSpeedup(videoFile: File):Int = speedupRe.find(videoFile.name)!!.groupValues[1].toInt()

fun ContextWrapper.speedupToFile(speedup:Int):File = File(filesDir, "faster_x%04d.mp4".format(speedup))

fun humanReadableByteCountBin(bytes: Long) = when {
    bytes == Long.MIN_VALUE || bytes < 0 -> "N/A"
    bytes < 1024L -> "$bytes B"
    bytes <= 0xfffccccccccccccL shr 40 -> "%.1f KiB".format(bytes.toDouble() / (0x1 shl 10))
    bytes <= 0xfffccccccccccccL shr 30 -> "%.1f MiB".format(bytes.toDouble() / (0x1 shl 20))
    bytes <= 0xfffccccccccccccL shr 20 -> "%.1f GiB".format(bytes.toDouble() / (0x1 shl 30))
    bytes <= 0xfffccccccccccccL shr 10 -> "%.1f TiB".format(bytes.toDouble() / (0x1 shl 40))
    bytes <= 0xfffccccccccccccL -> "%.1f PiB".format((bytes shr 10).toDouble() / (0x1 shl 40))
    else -> "%.1f EiB".format((bytes shr 20).toDouble() / (0x1 shl 40))
}

fun saveAndPublishVideo(context: Context, videoFile: File, fileName: String, title:String) {
    val contentValues = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Video.Media.TITLE, title)
        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
        put(MediaStore.Video.Media.IS_PENDING, 1) // Mark as pending until the file is completely written
    }

    // Get the content resolver and generate a new URI for the video
    val contentResolver: ContentResolver = context.contentResolver
    val videoUri: Uri? = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)

    // val manualUri = MediaStore.Video.Media.getContentUri( MediaStore.VOLUME_EXTERNAL_PRIMARY )

    videoUri?.let { uri ->
        contentResolver.openOutputStream(uri)?.use { outputStream: OutputStream ->
            videoFile.inputStream().copyTo(outputStream)  // Copy the actual video data
        }

        contentValues.clear()
        contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
        contentResolver.update(uri, contentValues, null, null)
    }
}