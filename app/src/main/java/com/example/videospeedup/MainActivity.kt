package com.example.videospeedup

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig


private const val logTag = "VideoSpeedupLog"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Column {
                    // Step 0: Start with a button click
                    Button(onClick = { requestVideoPermissionRead() }) {
                        Text("Load New Video")
                    }
                    Button(onClick = { doubleAgain() }) {
                        Text("Double the Video Speed")
                    }
                }
            }
        }
    }

    // Step 2: Hopefully they said yes.
    private val requestPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Permission granted, access your video file
                selectVideoIntent()
            } else {
                // Permission denied, explain to the user why it's needed
            }
        }

    // Step 1: Get permissions
    private fun requestVideoPermissionRead() {
        requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_VIDEO)
    }

    // Step 3: Now that we have permission, Select a video
    private fun selectVideoIntent() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "video/*"
        }
        selectVideoProcessor.launch(intent)
    }

    // Step 4: User picked a single video to start with
    private val selectVideoProcessor: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { inputVideoUri ->
                    filesDir.listFiles()?.filter { it.extension == "mp4" }?.forEach {
                        Log.i(logTag, "New Video, clearing out old.  Deleting `${it.absolutePath}`")
                        it.delete()
                    } ?: Log.e(logTag, "primeFirstVideo unable to delete videos.")

                    Log.i(logTag, "primeFirstVideo Reading `$inputVideoUri`")
                    primeFirstVideo(inputVideoUri)
                } ?: Log.e(logTag, "No selectVideoProcessor data?")
            } else {
                Log.e(logTag, "Bad selectVideoProcessor result.resultCode? ${result.resultCode}")
            }
        }

    // Step 5: Act on the original video (clear out old videos)
    private fun primeFirstVideo(inputVideoUri: Uri) {

        val inputPath =
            FFmpegKitConfig.getSafParameterForRead(this.applicationContext, inputVideoUri)
        val outputFile = speedupToFile(1)
        val session =
            FFmpegKit.execute("""-r 60 -i $inputPath -r 60 -c:v mpeg4 -q:v 1 -an ${outputFile.path}""")

        Log.i(logTag, session.allLogsAsString)
        val importedFileSize = humanReadableByteCountBin(outputFile.length())
        Log.i(logTag, "Imported File size: $$importedFileSize")
        Toast.makeText(this, "Imported Video ($importedFileSize)", Toast.LENGTH_SHORT).show()
    }

    private fun doubleAgain() {
        var nextSpeed = 1
        while (speedupToFile(nextSpeed).exists()) {
            nextSpeed *= 2
        }
        val inputPath = speedupToFile(nextSpeed / 2)
        require(inputPath.canRead())
        require(nextSpeed in 1..8192)
        val outputFile = speedupToFile(nextSpeed)
        require(!outputFile.exists())
        // /data/user/0/info.benjaminhill.videospeedup/files/faster_x0001.mp4
        //val outputPath = FFmpegKitConfig.getSafParameterForWrite(this.applicationContext, "doubled_speed_video.mp4")
        val session =
            FFmpegKit.execute("""-r 60 -i $inputPath -vf "tblend=average,framestep=2,setpts=0.5*PTS" -r 60 -c:v mpeg4 -q:v 1 -an ${outputFile.path}""")
        Log.i(logTag, session.allLogsAsString)
        Log.i(logTag, "Double Speed File size: ${outputFile.length()}")
        val title = "Faster Video x$nextSpeed"
        saveAndPublishVideo(this, outputFile, outputFile.name, title)
        Toast.makeText(
            this,
            "Doubled Speed to ${nextSpeed}x (${humanReadableByteCountBin(outputFile.length())})",
            Toast.LENGTH_SHORT
        ).show()
    }


}

