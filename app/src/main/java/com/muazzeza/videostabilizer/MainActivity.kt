package com.muazzeza.videostabilizer

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.muazzeza.videostabilizer.ui.theme.VideoStabilizerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VideoStabilizerTheme {
                VideoPickerScreen()
            }
        }
    }
}

@Composable
fun VideoPickerScreen() {
    val context = LocalContext.current
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var videoName by remember { mutableStateOf<String?>(null) }
    var videoDuration by remember { mutableStateOf<String?>(null) }

    // Galeri seçici
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedVideoUri = uri

            // Dosya adı
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                    videoName = it.getString(nameIndex)
                }
            }

            // tüm video süresi hesaplama
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            retriever.release()

            val seconds = (durationMs / 1000) % 60
            val minutes = (durationMs / (1000 * 60)) % 60
            val hours = (durationMs / (1000 * 60 * 60))

            videoDuration = if (hours > 0)
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            else
                String.format("%02d:%02d", minutes, seconds)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(onClick = { videoPickerLauncher.launch("video/*") }) {
            Text(text = "Galeriden Video Seç")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedVideoUri != null) {
            Text(text = "Dosya: ${videoName ?: "Bilinmiyor"}")
            Text(text = "Süre: ${videoDuration ?: "Bilinmiyor"}")

            Spacer(modifier = Modifier.height(16.dp))

            // ExoPlayer ile video oynatma
            val exoPlayer = remember(selectedVideoUri) {
                ExoPlayer.Builder(context).build().apply {
                    val mediaItem = MediaItem.fromUri(selectedVideoUri!!)
                    setMediaItem(mediaItem)
                    prepare()
                    playWhenReady = true
                }
            }

            DisposableEffect(
                AndroidView(factory = {
                    PlayerView(it).apply {
                        player = exoPlayer
                    }
                }, modifier = Modifier.fillMaxWidth().height(250.dp))
            ) {
                onDispose {
                    exoPlayer.release()
                }
            }
        }
    }
}

