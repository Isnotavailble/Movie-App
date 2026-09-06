package com.movieapp.features.downloads

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.movieapp.MainActivity
import com.movieapp.data.local.AppDatabase
import com.movieapp.data.local.DownloadEntity
import com.movieapp.features.downloadlinks.WebViewDownloadSniffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * In-app foreground service that reliably downloads full video files using OkHttp.
 * Preserves Cloudflare Turnstile session cookies and Chromium User-Agent headers.
 * Performs preflight validation to prevent corrupt HTML page downloads.
 */
class MovieDownloadService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<Long, Job>()

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1L)
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Movie"
                val url = intent.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
                val cookies = intent.getStringExtra(EXTRA_COOKIES)
                val userAgent = intent.getStringExtra(EXTRA_USER_AGENT) ?: WebViewDownloadSniffer.CHROME_USER_AGENT
                val referer = intent.getStringExtra(EXTRA_REFERER)
                val movieSlug = intent.getStringExtra(EXTRA_MOVIE_SLUG) ?: ""
                val poster = intent.getStringExtra(EXTRA_POSTER)

                if (downloadId != -1L && !activeJobs.containsKey(downloadId)) {
                    startDownloadTask(
                        downloadId = downloadId,
                        title = title,
                        directUrl = url,
                        cookies = cookies,
                        userAgent = userAgent,
                        referer = referer,
                        movieSlug = movieSlug,
                        poster = poster
                    )
                }
            }
            ACTION_CANCEL -> {
                val downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1L)
                if (downloadId != -1L) {
                    cancelDownloadTask(downloadId)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startDownloadTask(
        downloadId: Long,
        title: String,
        directUrl: String,
        cookies: String?,
        userAgent: String,
        referer: String?,
        movieSlug: String,
        poster: String?
    ) {
        val initialNotification = buildProgressNotification(
            downloadId = downloadId,
            title = title,
            progress = 0,
            downloadedBytes = 0L,
            totalBytes = 0L
        )

        // Start as foreground service
        val notifId = (downloadId % Int.MAX_VALUE).toInt()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                notifId,
                initialNotification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                } else 0
            )
        } else {
            startForeground(notifId, initialNotification)
        }

        val job = serviceScope.launch {
            val downloadDao = AppDatabase.getInstance(applicationContext).downloadDao()

            // Derive target file name
            val rawName = directUrl.substringAfterLast("/").substringBefore("?")
            val extension = when {
                rawName.endsWith(".mkv", ignoreCase = true) -> ".mkv"
                rawName.endsWith(".webm", ignoreCase = true) -> ".webm"
                else -> ".mp4"
            }
            val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9._ -]"), "_")
            val fileName = "$sanitizedTitle$extension"

            // Target folder: Public Download directory
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val targetFile = File(downloadsDir, fileName)
            val tempFile = File(downloadsDir, "$fileName.tmp")

            try {
                // Initialize/persist Room database record
                val initialEntity = DownloadEntity(
                    downloadId = downloadId,
                    title = title,
                    movieSlug = movieSlug,
                    poster = poster,
                    fileName = fileName,
                    status = STATUS_RUNNING,
                    createdAt = System.currentTimeMillis()
                )
                downloadDao.insertOrUpdate(initialEntity)

                val requestBuilder = Request.Builder()
                    .url(directUrl)
                    .header("User-Agent", userAgent)

                cookies?.takeIf { it.isNotBlank() }?.let { requestBuilder.header("Cookie", it) }
                referer?.takeIf { it.isNotBlank() }?.let { requestBuilder.header("Referer", it) }

                val request = requestBuilder.build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("HTTP ${response.code}: ${response.message}")
                    }

                    val body = response.body ?: throw IllegalStateException("Empty response body")
                    val contentType = response.header("Content-Type")?.lowercase() ?: ""
                    val totalBytes = body.contentLength()

                    // PREFLIGHT CHECK: Reject corrupt HTML error or challenge pages!
                    if (contentType.contains("text/html") || contentType.contains("application/json")) {
                        throw IllegalStateException("Received HTML error page instead of video stream")
                    }

                    if (totalBytes in 1..2_000_000L && !contentType.startsWith("video/")) {
                        throw IllegalStateException("File size too small for full movie ($totalBytes bytes)")
                    }

                    downloadDao.updateProgress(
                        id = downloadId,
                        downloadedBytes = 0L,
                        totalBytes = if (totalBytes > 0) totalBytes else 0L,
                        status = STATUS_RUNNING,
                        completedAt = null,
                        fileUri = Uri.fromFile(targetFile).toString()
                    )

                    body.byteStream().use { input ->
                        FileOutputStream(tempFile).use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var bytesRead: Int
                            var downloadedBytes = 0L
                            var lastUpdateMs = System.currentTimeMillis()

                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                downloadedBytes += bytesRead

                                val now = System.currentTimeMillis()
                                if (now - lastUpdateMs > 1000L) {
                                    lastUpdateMs = now
                                    val progress = if (totalBytes > 0) {
                                        ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                                    } else 0

                                    // Update notification
                                    notificationManager.notify(
                                        notifId,
                                        buildProgressNotification(
                                            downloadId = downloadId,
                                            title = title,
                                            progress = progress,
                                            downloadedBytes = downloadedBytes,
                                            totalBytes = totalBytes
                                        )
                                    )

                                    // Update database
                                    downloadDao.updateProgress(
                                        id = downloadId,
                                        downloadedBytes = downloadedBytes,
                                        totalBytes = if (totalBytes > 0) totalBytes else downloadedBytes,
                                        status = STATUS_RUNNING,
                                        completedAt = null,
                                        fileUri = Uri.fromFile(targetFile).toString()
                                    )
                                }
                            }
                        }
                    }

                    // Move temp file to final video file
                    if (tempFile.exists()) {
                        if (targetFile.exists()) targetFile.delete()
                        tempFile.renameTo(targetFile)
                    }

                    // Register with Android MediaStore
                    MediaScannerConnection.scanFile(
                        applicationContext,
                        arrayOf(targetFile.absolutePath),
                        arrayOf(if (extension == ".mkv") "video/x-matroska" else "video/mp4"),
                        null
                    )

                    // Final Room database update
                    val finalSize = targetFile.length()
                    downloadDao.updateProgress(
                        id = downloadId,
                        downloadedBytes = finalSize,
                        totalBytes = finalSize,
                        status = STATUS_SUCCESSFUL,
                        completedAt = System.currentTimeMillis(),
                        fileUri = Uri.fromFile(targetFile).toString()
                    )

                    // Show Completed Notification
                    notificationManager.notify(
                        notifId,
                        buildCompletedNotification(downloadId = downloadId, title = title, targetFile = targetFile)
                    )
                }
            } catch (e: Exception) {
                if (tempFile.exists()) tempFile.delete()
                downloadDao.updateProgress(
                    id = downloadId,
                    downloadedBytes = 0L,
                    totalBytes = 0L,
                    status = STATUS_FAILED,
                    completedAt = null,
                    fileUri = null
                )
                notificationManager.notify(
                    notifId,
                    buildFailedNotification(title = title, errorMessage = e.localizedMessage ?: "Download failed")
                )
            } finally {
                activeJobs.remove(downloadId)
                if (activeJobs.isEmpty()) {
                    stopForeground(STOP_FOREGROUND_DETACH)
                }
            }
        }

        activeJobs[downloadId] = job
    }

    private fun cancelDownloadTask(downloadId: Long) {
        val job = activeJobs.remove(downloadId)
        job?.cancel()

        val notifId = (downloadId % Int.MAX_VALUE).toInt()
        notificationManager.cancel(notifId)

        serviceScope.launch {
            val downloadDao = AppDatabase.getInstance(applicationContext).downloadDao()
            downloadDao.deleteDownload(downloadId)
        }

        if (activeJobs.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun buildProgressNotification(
        downloadId: Long,
        title: String,
        progress: Int,
        downloadedBytes: Long,
        totalBytes: Long
    ): android.app.Notification {
        val cancelIntent = Intent(this, MovieDownloadService::class.java).apply {
            action = ACTION_CANCEL
            putExtra(EXTRA_DOWNLOAD_ID, downloadId)
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            (downloadId % Int.MAX_VALUE).toInt(),
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val appIntent = Intent(this, MainActivity::class.java)
        val appPendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val sizeText = if (totalBytes > 0) {
            "${DownloadEntity.formatBytes(downloadedBytes)} / ${DownloadEntity.formatBytes(totalBytes)} ($progress%)"
        } else {
            "${DownloadEntity.formatBytes(downloadedBytes)} downloaded"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Downloading: $title")
            .setContentText(sizeText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, totalBytes <= 0)
            .setOngoing(true)
            .setContentIntent(appPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun buildCompletedNotification(
        downloadId: Long,
        title: String,
        targetFile: File
    ): android.app.Notification {
        val playIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.fromFile(targetFile), "video/*")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val playPendingIntent = PendingIntent.getActivity(
            this,
            (downloadId % Int.MAX_VALUE).toInt(),
            playIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Download Completed")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentIntent(playPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    private fun buildFailedNotification(title: String, errorMessage: String): android.app.Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Download Failed: $title")
            .setContentText(errorMessage)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Movie Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of in-app movie downloads"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val CHANNEL_ID = "movie_downloads_channel"
        const val ACTION_START = "com.movieapp.action.START_DOWNLOAD"
        const val ACTION_CANCEL = "com.movieapp.action.CANCEL_DOWNLOAD"

        const val EXTRA_DOWNLOAD_ID = "extra_download_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_URL = "extra_url"
        const val EXTRA_COOKIES = "extra_cookies"
        const val EXTRA_USER_AGENT = "extra_user_agent"
        const val EXTRA_REFERER = "extra_referer"
        const val EXTRA_MOVIE_SLUG = "extra_movie_slug"
        const val EXTRA_POSTER = "extra_poster"

        const val STATUS_RUNNING = 2
        const val STATUS_SUCCESSFUL = 8
        const val STATUS_FAILED = 16

        fun startDownload(
            context: Context,
            downloadId: Long,
            title: String,
            directUrl: String,
            cookies: String? = null,
            userAgent: String? = null,
            referer: String? = null,
            movieSlug: String = "",
            poster: String? = null
        ) {
            val intent = Intent(context, MovieDownloadService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_URL, directUrl)
                putExtra(EXTRA_COOKIES, cookies)
                putExtra(EXTRA_USER_AGENT, userAgent)
                putExtra(EXTRA_REFERER, referer)
                putExtra(EXTRA_MOVIE_SLUG, movieSlug)
                putExtra(EXTRA_POSTER, poster)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun cancelDownload(context: Context, downloadId: Long) {
            val intent = Intent(context, MovieDownloadService::class.java).apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
            context.startService(intent)
        }
    }
}
