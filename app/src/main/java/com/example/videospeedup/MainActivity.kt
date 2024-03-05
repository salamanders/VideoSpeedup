package com.example.videospeedup

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKitConfig
import java.io.File
import java.io.OutputStream


class MainActivity : VideoProcessingActivity() {
    private var speedIteration: Int = 0
    private lateinit var importedFile: File
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        importedFile = File(filesDir, IMPORTED_FILE_NAME)
    }

    // Step 5: Act on the original video (clear out old videos)
    override suspend fun loadInitialVideo(inputVideoUri: Uri) {
        Log.i(TAG, "loadInitialVideo:START")
        loadNewVideoButtonEnabled.value = false
        modifyVideoButtonEnabled.value = false
        super.loadInitialVideo(inputVideoUri)
        speedIteration = 0
        val safUri = FFmpegKitConfig.getSafParameterForRead(
            this.applicationContext, inputVideoUri
        )
        val ffmpegCommand = """-r 30 -i $safUri -r 30 -c:v mpeg4 -q:v 1 -an ${importedFile.path}"""
        loadNewVideoButtonEnabled.value = false
        modifyVideoButtonEnabled.value = false
        ffmpegAsync(
            ffmpegCommand = ffmpegCommand,
            stepName = "Imported Video"
        ) {
            loadNewVideoButtonEnabled.value = true
            modifyVideoButtonEnabled.value = true
        }
    }

    override suspend fun modifyVideoAndExport() {
        loadNewVideoButtonEnabled.value = false
        modifyVideoButtonEnabled.value = false
        speedIteration++
        val speedMultiplier = 2.pow(speedIteration)
        require(speedMultiplier in 1..8192)


        require(importedFile.canRead())
        val outputFile = File(filesDir, "faster_x%04d.mp4".format(speedMultiplier))
        require(!outputFile.exists())
        val vf = generateSequence { "tblend=average,framestep=2" }.take(speedIteration)
            .joinToString(",") + ",setpts=${1.0 / speedMultiplier}*PTS"
        val ffmpegCommand =
            """-r 30 -i ${importedFile.path} -vf "$vf" -r 30 -c:v mpeg4 -q:v 1 -an ${outputFile.path}"""
        ffmpegAsync(
            ffmpegCommand = ffmpegCommand,
            stepName = "Doubled Speed to ${speedMultiplier}x"
        ) {
            saveAndPublishVideo(
                videoFile = outputFile,
                fileName = outputFile.name,
                title = "Faster Video x$speedMultiplier"
            )
            val nextSpeedMultiplier = 2.pow(speedIteration + 1)
            buttonText.value =
                buttonText.value.replace(Regex("""\d+"""), nextSpeedMultiplier.toString())
            loadNewVideoButtonEnabled.value = true
            modifyVideoButtonEnabled.value = true
        }
    }

    private fun saveAndPublishVideo(videoFile: File, fileName: String, title: String) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.TITLE, title)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
            // Mark as pending until the file is completely written
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        // Get the content resolver and generate a new URI for the video
        val contentResolver: ContentResolver = this.contentResolver
        val videoUri: Uri? =
            contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)

        // val manualUri = MediaStore.Video.Media.getContentUri( MediaStore.VOLUME_EXTERNAL_PRIMARY )

        videoUri?.let { uri ->
            contentResolver.openOutputStream(uri)?.use { outputStream: OutputStream ->
                // Copy the actual video data
                videoFile.inputStream().copyTo(outputStream)
            }

            contentValues.clear()
            contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
            contentResolver.update(uri, contentValues, null, null)
        }
    }

    companion object {
        private const val TAG = "vspeed"
        private const val IMPORTED_FILE_NAME = "video_original.mp4"
    }
}
