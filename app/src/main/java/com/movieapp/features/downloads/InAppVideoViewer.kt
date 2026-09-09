package com.movieapp.features.downloads

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.movieapp.data.local.DownloadEntity
import com.movieapp.features.downloadlinks.DownloadManagerHelper
import com.movieapp.theme.NeoBlack
import com.movieapp.theme.NeubrutalismIcons
import com.movieapp.theme.SpideyBlue
import com.movieapp.theme.SpideyRed
import com.movieapp.theme.WebGold
import com.movieapp.theme.WebWhite
import com.movieapp.theme.badgeFontFamily
import com.movieapp.theme.bodyFontFamily
import com.movieapp.theme.buttonFontFamily
import com.movieapp.theme.headerFontFamily
import com.movieapp.theme.neoBorder
import com.movieapp.theme.neoShadow
import com.movieapp.util.t
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

/**
 * Resolved source representation for video playback.
 */
sealed interface VideoSource {
    data class LocalFile(val file: File) : VideoSource
    data class UriSource(val uri: Uri) : VideoSource
    object NotFound : VideoSource
}

/**
 * Resolves the underlying video file or content URI for a given DownloadEntity.
 */
fun resolveVideoSource(download: DownloadEntity): VideoSource {
    val file = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        download.fileName
    )
    if (file.exists() && file.length() > 0L) {
        return VideoSource.LocalFile(file)
    }
    val uriStr = download.fileUri
    if (!uriStr.isNullOrBlank()) {
        return VideoSource.UriSource(Uri.parse(uriStr))
    }
    return VideoSource.NotFound
}

/**
 * Resolves a secure, playable Uri for local downloaded videos using FileProvider,
 * MediaStore, DownloadManager, or file Uri.
 */
fun resolveVideoPlaybackUri(context: Context, download: DownloadEntity): Uri? {
    // 1. Check public Download directory file
    val publicFile = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        download.fileName
    )
    if (publicFile.exists() && publicFile.length() > 0L) {
        return try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                publicFile
            )
        } catch (_: Exception) {
            Uri.fromFile(publicFile)
        }
    }

    // 2. Check app-specific external files dir
    val appFile = File(
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
        download.fileName
    )
    if (appFile.exists() && appFile.length() > 0L) {
        return try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                appFile
            )
        } catch (_: Exception) {
            Uri.fromFile(appFile)
        }
    }

    // 3. DownloadManager content URI
    try {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
        val dmUri = dm?.getUriForDownloadedFile(download.downloadId)
        if (dmUri != null) return dmUri
    } catch (_: Exception) {}

    // 4. Stored fileUri
    val uriStr = download.fileUri
    if (!uriStr.isNullOrBlank()) {
        val parsed = Uri.parse(uriStr)
        if (parsed.scheme == "file") {
            val f = File(parsed.path ?: "")
            if (f.exists() && f.length() > 0L) {
                return try {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        f
                    )
                } catch (_: Exception) {
                    parsed
                }
            }
        }
        return parsed
    }

    return null
}

/**
 * Formats milliseconds into standard playback time representation (hh:mm:ss or mm:ss).
 */
fun formatDurationMs(milliseconds: Long): String {
    if (milliseconds <= 0L) return "00:00"
    val totalSeconds = milliseconds / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

/**
 * In-app video viewer modal adhering strictly to DESIGN.md Neobrutalism rules,
 * WCAG 2.2 AA accessibility standards, and powered by AndroidX Media3 (ExoPlayer)
 * for seamless playback of downloaded MP4 and MKV movies.
 */
@Composable
fun InAppVideoViewerModal(
    download: DownloadEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentView = LocalView.current

    // Localized strings read in composable context
    val closeLabel = t("video_player_close")
    val playLabel = t("video_player_play")
    val pauseLabel = t("video_player_pause")
    val rewindLabel = t("video_player_rewind")
    val forwardLabel = t("video_player_forward")
    val externalDescLabel = t("video_player_external_desc")
    val scrubberLabel = t("video_player_scrubber")
    val tapHintLabel = t("video_player_tap_hint")
    val screenAwakeLabel = t("video_player_screen_awake")
    val errorLabel = t("video_player_error")
    val cancelLabel = t("cancel")

    // Keep screen awake while video viewer is in foreground
    DisposableEffect(currentView) {
        currentView.keepScreenOn = true
        onDispose {
            currentView.keepScreenOn = false
        }
    }

    val mediaUri = remember(download) {
        resolveVideoPlaybackUri(context, download)
    }

    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(mediaUri == null) }
    var controlsVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val exoPlayer = remember(context, mediaUri) {
        if (mediaUri != null) {
            try {
                ExoPlayer.Builder(context).build().apply {
                    val mediaItem = MediaItem.fromUri(mediaUri)
                    setMediaItem(mediaItem)
                    prepare()
                    playWhenReady = true
                }
            } catch (_: Exception) {
                isError = true
                null
            }
        } else null
    }

    DisposableEffect(exoPlayer) {
        if (exoPlayer != null) {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            val dur = exoPlayer.duration
                            if (dur > 0L) totalDurationMs = dur
                            isBuffering = false
                        }
                        Player.STATE_BUFFERING -> {
                            isBuffering = true
                        }
                        Player.STATE_ENDED -> {
                            isPlaying = false
                            currentPositionMs = totalDurationMs
                        }
                        Player.STATE_IDLE -> {}
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    isError = true
                }
            }
            exoPlayer.addListener(listener)
            onDispose {
                exoPlayer.removeListener(listener)
                exoPlayer.release()
            }
        } else {
            onDispose {}
        }
    }

    // Progress updater coroutine
    LaunchedEffect(exoPlayer, isPlaying, isSeeking) {
        while (isPlaying && !isSeeking) {
            exoPlayer?.let { player ->
                currentPositionMs = player.currentPosition.coerceAtLeast(0L)
                val dur = player.duration
                if (dur > 0L) totalDurationMs = dur
            }
            delay(250L)
        }
    }

    // Auto-hide controls after 3.5s of inactivity while playing
    LaunchedEffect(controlsVisible, isPlaying, lastInteractionTime) {
        if (controlsVisible && isPlaying) {
            delay(3500L)
            controlsVisible = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    controlsVisible = !controlsVisible
                    lastInteractionTime = System.currentTimeMillis()
                }
        ) {
            // Media3 PlayerView Video Surface
            if (!isError && exoPlayer != null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                        }
                    },
                    update = { view ->
                        if (view.player != exoPlayer) {
                            view.player = exoPlayer
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Buffering Indicator
            if (isBuffering && !isError) {
                CircularProgressIndicator(
                    color = WebGold,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                )
            }

            // Neobrutalist Controls Overlay
            AnimatedVisibility(
                visible = controlsVisible && !isError,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Top Bar (Close, Title, Format, External Launcher)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 28.dp)
                            .neoShadow(offsetX = 3.dp, offsetY = 3.dp, color = NeoBlack, shape = RoundedCornerShape(12.dp))
                            .background(Color(0xFF121826), RoundedCornerShape(12.dp))
                            .neoBorder(width = 2.dp, color = NeoBlack, shape = RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Close Button (48dp x 48dp touch target)
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    .neoShadow(offsetX = 2.dp, offsetY = 2.dp, color = NeoBlack, shape = RoundedCornerShape(10.dp))
                                    .background(SpideyRed, RoundedCornerShape(10.dp))
                                    .neoBorder(width = 1.5.dp, color = NeoBlack, shape = RoundedCornerShape(10.dp))
                                    .clickable(onClick = onDismiss)
                                    .semantics {
                                        role = Role.Button
                                        contentDescription = closeLabel
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = NeubrutalismIcons.Close,
                                    contentDescription = null,
                                    tint = WebWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Title & Subtitle Info
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = download.title,
                                    fontFamily = headerFontFamily(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = WebWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(SpideyBlue, RoundedCornerShape(4.dp))
                                            .neoBorder(width = 1.dp, color = NeoBlack, shape = RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = download.formattedTotalSize,
                                            fontFamily = badgeFontFamily(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = WebWhite
                                        )
                                    }
                                    Text(
                                        text = download.fileName,
                                        fontFamily = bodyFontFamily(),
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // External App Button (44dp min height, Icon only)
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                                    .neoShadow(offsetX = 2.dp, offsetY = 2.dp, color = NeoBlack, shape = RoundedCornerShape(10.dp))
                                    .background(SpideyBlue, RoundedCornerShape(10.dp))
                                    .neoBorder(width = 1.5.dp, color = NeoBlack, shape = RoundedCornerShape(10.dp))
                                    .clickable {
                                        DownloadManagerHelper.openDownloadedFile(context, download)
                                    }
                                    .semantics {
                                        role = Role.Button
                                        contentDescription = externalDescLabel
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = WebWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Center Action Controls (Rewind 10s, Play/Pause, Forward 10s) - Icons Only
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 24.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            // Rewind 10s (Icon only)
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .neoShadow(offsetX = 2.5.dp, offsetY = 2.5.dp, color = NeoBlack, shape = CircleShape)
                                    .background(Color(0xFF1E293B), CircleShape)
                                    .neoBorder(width = 2.dp, color = NeoBlack, shape = CircleShape)
                                    .clickable {
                                        exoPlayer?.let { player ->
                                            val target = (player.currentPosition - 10_000L).coerceAtLeast(0L)
                                            player.seekTo(target)
                                            currentPositionMs = target
                                        }
                                        lastInteractionTime = System.currentTimeMillis()
                                    }
                                    .semantics {
                                        role = Role.Button
                                        contentDescription = rewindLabel
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay10,
                                    contentDescription = null,
                                    tint = WebWhite,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            // Big Tactile Play/Pause Button (68dp x 68dp)
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .neoShadow(offsetX = 3.5.dp, offsetY = 3.5.dp, color = NeoBlack, shape = CircleShape)
                                    .background(if (isPlaying) SpideyRed else WebGold, CircleShape)
                                    .neoBorder(width = 2.5.dp, color = NeoBlack, shape = CircleShape)
                                    .clickable {
                                        exoPlayer?.let { player ->
                                            if (isPlaying) {
                                                player.pause()
                                            } else {
                                                if (player.playbackState == Player.STATE_ENDED) {
                                                    player.seekTo(0L)
                                                }
                                                player.play()
                                            }
                                        }
                                        lastInteractionTime = System.currentTimeMillis()
                                    }
                                    .semantics {
                                        role = Role.Button
                                        contentDescription = if (isPlaying) pauseLabel else playLabel
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = if (isPlaying) WebWhite else NeoBlack,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            // Forward 10s (Icon only)
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .neoShadow(offsetX = 2.5.dp, offsetY = 2.5.dp, color = NeoBlack, shape = CircleShape)
                                    .background(Color(0xFF1E293B), CircleShape)
                                    .neoBorder(width = 2.dp, color = NeoBlack, shape = CircleShape)
                                    .clickable {
                                        exoPlayer?.let { player ->
                                            val maxPos = if (totalDurationMs > 0L) totalDurationMs else Long.MAX_VALUE
                                            val target = (player.currentPosition + 10_000L).coerceAtMost(maxPos)
                                            player.seekTo(target)
                                            currentPositionMs = target
                                        }
                                        lastInteractionTime = System.currentTimeMillis()
                                    }
                                    .semantics {
                                        role = Role.Button
                                        contentDescription = forwardLabel
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Forward10,
                                    contentDescription = null,
                                    tint = WebWhite,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }

                    // Bottom Bar with Scrubber & Time Display
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 28.dp)
                            .neoShadow(offsetX = 3.dp, offsetY = 3.dp, color = NeoBlack, shape = RoundedCornerShape(12.dp))
                            .background(Color(0xFF121826), RoundedCornerShape(12.dp))
                            .neoBorder(width = 2.dp, color = NeoBlack, shape = RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            // Scrubber Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = formatDurationMs(currentPositionMs),
                                    fontFamily = badgeFontFamily(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = WebWhite,
                                    modifier = Modifier.width(58.dp)
                                )

                                Slider(
                                    value = currentPositionMs.toFloat().coerceIn(0f, totalDurationMs.toFloat().coerceAtLeast(1f)),
                                    onValueChange = { newPos ->
                                        isSeeking = true
                                        currentPositionMs = newPos.toLong()
                                        lastInteractionTime = System.currentTimeMillis()
                                    },
                                    onValueChangeFinished = {
                                        exoPlayer?.seekTo(currentPositionMs)
                                        isSeeking = false
                                        lastInteractionTime = System.currentTimeMillis()
                                    },
                                    valueRange = 0f..totalDurationMs.toFloat().coerceAtLeast(1f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 48.dp)
                                        .semantics {
                                            contentDescription = scrubberLabel
                                        },
                                    colors = SliderDefaults.colors(
                                        thumbColor = WebGold,
                                        activeTrackColor = SpideyBlue,
                                        inactiveTrackColor = Color(0xFF334155)
                                    )
                                )

                                Text(
                                    text = formatDurationMs(totalDurationMs),
                                    fontFamily = badgeFontFamily(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8),
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(58.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Microcopy & Screen-Awake Status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tapHintLabel,
                                    fontFamily = bodyFontFamily(),
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(WebGold, CircleShape)
                                    )
                                    Text(
                                        text = screenAwakeLabel,
                                        fontFamily = badgeFontFamily(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = WebGold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Neobrutalist Error Card Overlay (Icons only)
            if (isError) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                        .neoShadow(offsetX = 4.dp, offsetY = 4.dp, color = NeoBlack, shape = RoundedCornerShape(14.dp))
                        .background(Color(0xFF121826), RoundedCornerShape(14.dp))
                        .neoBorder(width = 2.5.dp, color = NeoBlack, shape = RoundedCornerShape(14.dp))
                        .padding(20.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = NeubrutalismIcons.Info,
                            contentDescription = null,
                            tint = SpideyRed,
                            modifier = Modifier.size(44.dp)
                        )

                        Text(
                            text = errorLabel,
                            fontFamily = bodyFontFamily(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = WebWhite,
                            textAlign = TextAlign.Center
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Close Button (Icon only)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .neoShadow(offsetX = 2.dp, offsetY = 2.dp, color = NeoBlack, shape = RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                                    .neoBorder(width = 1.5.dp, color = NeoBlack, shape = RoundedCornerShape(10.dp))
                                    .clickable(onClick = onDismiss)
                                    .semantics {
                                        role = Role.Button
                                        contentDescription = cancelLabel
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = NeubrutalismIcons.Close,
                                    contentDescription = null,
                                    tint = WebWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // External Player Button (Icon only)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .neoShadow(offsetX = 2.dp, offsetY = 2.dp, color = NeoBlack, shape = RoundedCornerShape(10.dp))
                                    .background(SpideyBlue, RoundedCornerShape(10.dp))
                                    .neoBorder(width = 1.5.dp, color = NeoBlack, shape = RoundedCornerShape(10.dp))
                                    .clickable {
                                        DownloadManagerHelper.openDownloadedFile(context, download)
                                        onDismiss()
                                    }
                                    .semantics {
                                        role = Role.Button
                                        contentDescription = externalDescLabel
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = WebWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
