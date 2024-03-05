package com.example.videospeedup

import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFmpegSession
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

abstract class VideoProcessingActivity : VideoPermissionActivity() {
    private val mutex = Mutex()

    /**
     * Run a FFMpeg Command async, but only one at a time.
     */
    suspend fun ffmpegAsync(ffmpegCommand: String, stepName: String, callback: () -> Unit) {
        actionInProgress.value = true
        mutex.withLock {
            Log.d(TAG, "actionInProgress:true")
            FFmpegKitConfig.enableFFmpegSessionCompleteCallback { session: FFmpegSession ->
                Log.i(TAG, "modifyVideoAndExport:CompleteCallback $stepName")
                Log.d(TAG, session.toString())
                this.runOnUiThread {
                    Log.i(TAG, stepName)
                    Toast.makeText(this, stepName, Toast.LENGTH_SHORT).show()
                }
                actionInProgress.value = false
                Log.d(TAG, "actionInProgress:false")
                callback()
            }
            Log.i(TAG, "running FFmpegKit.executeAsync(`$ffmpegCommand`)")
            FFmpegKit.executeAsync(ffmpegCommand, { session ->
                Log.d(
                    TAG,
                    "FFmpeg process exited with state ${session.state} and rc ${session.returnCode}. ${session.failStackTrace}",
                )
            }, { _ ->
                // Log.d(logTag, "executeAsync log: $log")
            }) { statistics ->
                Log.d(TAG, "modifyVideoAndExport.executeAsync statistics: $statistics")
            }
        }
    }

    override suspend fun loadInitialVideo(inputVideoUri: Uri) {
        deleteAllOldVideoFiles()
    }

    private fun deleteAllOldVideoFiles() {
        filesDir.listFiles()?.filter { it.extension == "mp4" }?.forEach {
            Log.i(TAG, "New Video, clearing out old.  Deleting `${it.absolutePath}`")
            it.delete()
        } ?: Log.e(TAG, "deleteAllOldVideoFiles unable to delete videos.")
    }

    companion object {
        private const val TAG = "vspeed"
    }
}