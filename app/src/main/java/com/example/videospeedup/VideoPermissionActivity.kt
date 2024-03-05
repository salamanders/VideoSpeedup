package com.example.videospeedup

import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

abstract class VideoPermissionActivity : AppCompatActivity() {

    lateinit var loadNewVideoButtonEnabled: MutableState<Boolean>
    lateinit var modifyVideoButtonEnabled: MutableState<Boolean>
    lateinit var actionInProgress: MutableState<Boolean>
    lateinit var buttonText: MutableState<String>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate")
        setContent {
            loadNewVideoButtonEnabled = remember { mutableStateOf(true) }
            modifyVideoButtonEnabled = remember { mutableStateOf(false) }
            buttonText = remember { mutableStateOf("Export 2x Speed Video") }
            actionInProgress = remember { mutableStateOf(false) }
            MaterialTheme {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Step 0: Start with a button click
                    Button(
                        onClick = {
                            Log.i(TAG, "CLICKED: \"Load New Video\"")
                            requestVideoPermissionRead()
                        },
                        enabled = loadNewVideoButtonEnabled.value,
                    ) {
                        Text("Load New Video")
                    }
                    Button(
                        onClick = {
                            Log.i(TAG, "CLICKED: \"Export Double Speed Video\"")
                            lifecycleScope.launch {
                                modifyVideoAndExport()
                            }
                        },
                        enabled = modifyVideoButtonEnabled.value,
                    ) {
                        Text(buttonText.value)
                    }
                    if (actionInProgress.value) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    // Step 2: Hopefully they said yes.
    private val requestPermissionLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { isGrantedMap ->
            if (isGrantedMap.values.all { it }) {
                // Permission granted, access your video file
                selectVideoIntent()
            } else {
                isGrantedMap.entries.filter { !it.value }.map { it.key }.forEach { grantName ->
                    // Permission denied, explain to the user why it's needed
                    Log.w(
                        TAG,
                        "requestPermissionLauncher registerForActivityResult isGranted $grantName false"
                    )
                    Toast.makeText(
                        this, "Access to read videos is required.", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    // Step 1: Get permissions

    private fun requestVideoPermissionRead() {
        val hasPermission =
            ContextCompat.checkSelfPermission(this, READ_MEDIA_VIDEO)
        Log.i(TAG, "Previous permission: $hasPermission")

        @SuppressLint("ObsoleteSdkInt")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            requestPermissionLauncher.launch(
                arrayOf(
                    READ_MEDIA_VIDEO,
                    READ_MEDIA_VISUAL_USER_SELECTED
                )
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(arrayOf(READ_MEDIA_VIDEO))
        } else {
            requestPermissionLauncher.launch(arrayOf(READ_EXTERNAL_STORAGE))
        }
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
                    Log.i(TAG, "selectVideoProcessor Reading `$inputVideoUri`")
                    lifecycleScope.launch {
                        loadInitialVideo(inputVideoUri)
                    }
                } ?: Log.e(TAG, "No selectVideoProcessor data?")
            } else {
                Log.e(TAG, "Bad selectVideoProcessor result.resultCode? ${result.resultCode}")
            }
        }


    // Step 5: Act on the original video (clear out old videos)
    abstract suspend fun loadInitialVideo(inputVideoUri: Uri)

    abstract suspend fun modifyVideoAndExport()

    companion object {
        private const val TAG = "vspeed"
    }
}