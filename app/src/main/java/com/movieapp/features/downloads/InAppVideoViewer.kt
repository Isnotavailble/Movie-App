package com.movieapp.features.downloads

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.VideoView
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
 * Telegram-style in-app video viewer modal adhering strictly to DESIGN.md Neobrutalism rules,
 * WCAG 2.2 AA accessibility standards, and the Ponytail principle (0 external video libraries).
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
    val externalLabel = t("video_player_external")
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

    val videoSource = remember(download) {
        resolveVideoSource(download)
    }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(videoSource is VideoSource.NotFound) }
    var controlsVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    // Progress updater coroutine
    LaunchedEffect(isPlaying, isSeeking) {
        while (isPlaying && !isSeeking) {
            videoViewRef?.let { vv ->
                currentPositionMs = vv.currentPosition.toLong().coerceAtLeast(0L)
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

    DisposableEffect(Unit) {
        onDispose {
            videoViewRef?.stopPlayback()
            videoViewRef = null
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
            // Android SDK Native VideoView
            if (!isError) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            videoViewRef = this
                            setOnPreparedListener { mp ->
                                totalDurationMs = mp.duration.toLong().coerceAtLeast(0L)
                                start()
                                isPlaying = true
                            }
                            setOnCompletionListener {
                                isPlaying = false
                                currentPositionMs = totalDurationMs
                            }
                            setOnErrorListener { _, _, _ ->
                                isError = true
                                true
                            }
                            when (videoSource) {
                                is VideoSource.LocalFile -> setVideoPath(videoSource.file.absolutePath)
                                is VideoSource.UriSource -> setVideoURI(videoSource.uri)
                                VideoSource.NotFound -> {
                                    isError = true
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
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

                            // External App Button (48dp min height)
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    .neoShadow(offsetX = 2.dp, offsetY = 2.dp, color = NeoBlack, shape = RoundedCornerShape(10.dp))
                                    .background(SpideyBlue, RoundedCornerShape(10.dp))
                                    .neoBorder(width = 1.5.dp, color = NeoBlack, shape = RoundedCornerShape(10.dp))
                                    .clickable {
                                        DownloadManagerHelper.openDownloadedFile(context, download)
                                    }
                                    .semantics {
                                        role = Role.Button
                                        contentDescription = externalDescLabel
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = null,
                                        tint = WebWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = externalLabel,
                                        fontFamily = buttonFontFamily(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = WebWhite
                                    )
                                }
                            }
                        }
                    }

                    // Center Action Controls (Rewind 10s, Play/Pause, Forward 10s)
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 24.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            // Rewind 10s
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .neoShadow(offsetX = 2.5.dp, offsetY = 2.5.dp, color = NeoBlack, shape = CircleShape)
                                    .background(Color(0xFF1E293B), CircleShape)
                                    .neoBorder(width = 2.dp, color = NeoBlack, shape = CircleShape)
                                    .clickable {
                                        videoViewRef?.let { vv ->
                                            val target = (vv.currentPosition - 10_000).coerceAtLeast(0)
                                            vv.seekTo(target)
                                            currentPositionMs = target.toLong()
                                        }
                                        lastInteractionTime = System.currentTimeMillis()
                                    }
                                    .semantics {
                                        role = Role.Button
                                        contentDescription = rewindLabel
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Replay10,
                                        contentDescription = null,
                                        tint = WebWhite,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = "-10s",
                                        fontFamily = badgeFontFamily(),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 9.sp,
                                        color = WebGold
                                    )
                                }
                            }

                            // Big Tactile Play/Pause Button (68dp x 68dp)
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .neoShadow(offsetX = 3.5.dp, offsetY = 3.5.dp, color = NeoBlack, shape = CircleShape)
                                    .background(if (isPlaying) SpideyRed else WebGold, CircleShape)
                                    .neoBorder(width = 2.5.dp, color = NeoBlack, shape = CircleShape)
                                    .clickable {
                                        videoViewRef?.let { vv ->
                                            if (isPlaying) {
                                                vv.pause()
                                                isPlaying = false
                                            } else {
                                                vv.start()
                                                isPlaying = true
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

                            // Forward 10s
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .neoShadow(offsetX = 2.5.dp, offsetY = 2.5.dp, color = NeoBlack, shape = CircleShape)
                                    .background(Color(0xFF1E293B), CircleShape)
                                    .neoBorder(width = 2.dp, color = NeoBlack, shape = CircleShape)
                                    .clickable {
                                        videoViewRef?.let { vv ->
                                            val target = (vv.currentPosition + 10_000).coerceAtMost(totalDurationMs.toInt())
                                            vv.seekTo(target)
                                            currentPositionMs = target.toLong()
                                        }
                                        lastInteractionTime = System.currentTimeMillis()
                                    }
                                    .semantics {
                                        role = Role.Button
                                        contentDescription = forwardLabel
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Forward10,
                                        contentDescription = null,
                                        tint = WebWhite,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = "+10s",
                                        fontFamily = badgeFontFamily(),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 9.sp,
                                        color = WebGold
                                    )
                                }
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
                                        videoViewRef?.seekTo(currentPositionMs.toInt())
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

            // Neobrutalist Error Card Overlay
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
                            // Close Button
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
                                        contentDescription = closeLabel
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cancelLabel,
                                    fontFamily = buttonFontFamily(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = WebWhite
                                )
                            }

                            // External Player Button
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
                                Text(
                                    text = externalLabel,
                                    fontFamily = buttonFontFamily(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = WebWhite
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
