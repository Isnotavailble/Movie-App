package com.movieapp.features.downloadlinks

import android.app.DownloadManager
import android.content.Context
import com.movieapp.data.local.AppDatabase
import com.movieapp.data.local.DownloadDao
import com.movieapp.data.local.DownloadEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Repository responsible for managing download state, syncing real-time DownloadManager progress,
 * and maintaining completed download history.
 */
class DownloadRepository(
    private val context: Context,
    private val downloadDao: DownloadDao = AppDatabase.getInstance(context).downloadDao()
) {
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    val activeDownloads: Flow<List<DownloadEntity>> = downloadDao.getActiveDownloads()
    val completedDownloads: Flow<List<DownloadEntity>> = downloadDao.getCompletedDownloads()
    val allDownloads: Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()

    /**
     * Periodically syncs active download progress from Android's DownloadManager into Room.
     */
    fun startSync(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val activeList = downloadDao.getActiveDownloads().first()
                    if (activeList.isNotEmpty()) {
                        for (item in activeList) {
                            syncDownloadTask(item)
                        }
                    }
                } catch (_: Exception) {}
                delay(1000)
            }
        }
    }

    /**
     * Queries Android's DownloadManager for a specific download ID and updates the Room record.
     */
    suspend fun syncDownloadTask(entity: DownloadEntity) {
        val query = DownloadManager.Query().setFilterById(entity.downloadId)
        try {
            downloadManager.query(query)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val totalBytesIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val localUriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)

                    val downloaded = if (bytesDownloadedIndex != -1) cursor.getLong(bytesDownloadedIndex) else entity.downloadedBytes
                    val total = if (totalBytesIndex != -1) cursor.getLong(totalBytesIndex) else entity.totalBytes
                    val status = if (statusIndex != -1) cursor.getInt(statusIndex) else entity.status
                    val localUri = if (localUriIndex != -1) cursor.getString(localUriIndex) else entity.fileUri
                    var finalStatus = status
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        val file = localUri?.let { uriStr ->
                            try {
                                val parsed = android.net.Uri.parse(uriStr)
                                if (parsed.scheme == "file") java.io.File(parsed.path ?: "")
                                else null
                            } catch (_: Exception) { null }
                        } ?: java.io.File(
                            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                            entity.fileName
                        )

                        if (DownloadManagerHelper.isCorruptHtmlDownload(file)) {
                            // File downloaded is an HTML error/challenge page instead of real video; delete it and fail
                            try { file.delete() } catch (_: Exception) {}
                            finalStatus = DownloadManager.STATUS_FAILED
                        }
                    }

                    val completedAt = if (finalStatus == DownloadManager.STATUS_SUCCESSFUL && entity.completedAt == null) {
                        System.currentTimeMillis()
                    } else {
                        entity.completedAt
                    }

                    downloadDao.updateProgress(
                        id = entity.downloadId,
                        downloadedBytes = downloaded,
                        totalBytes = total,
                        status = finalStatus,
                        completedAt = completedAt,
                        fileUri = localUri
                    )
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Cancels an active download and removes it from the database.
     */
    suspend fun cancelDownload(downloadId: Long) {
        try {
            com.movieapp.features.downloads.MovieDownloadService.cancelDownload(context, downloadId)
        } catch (_: Exception) {}
        try {
            downloadManager.remove(downloadId)
        } catch (_: Exception) {}
        downloadDao.deleteDownload(downloadId)
    }

    /**
     * Deletes a completed download from disk and database.
     */
    fun deleteDownload(entity: DownloadEntity) {
        DownloadManagerHelper.deleteDownloadedFile(context, entity)
    }
}
