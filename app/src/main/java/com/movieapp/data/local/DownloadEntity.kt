package com.movieapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing an active or completed download task.
 *
 * @property downloadId Unique identifier matching Android DownloadManager ID or generated task ID.
 * @property title Human-readable movie or episode title.
 * @property movieSlug Associated movie or TV show slug for navigation.
 * @property poster Poster or thumbnail image URL.
 * @property fileName Sanitized file name on disk (e.g. "Hydra_720p.mp4").
 * @property fileUri Local content or file URI once completed.
 * @property totalBytes Total file size in bytes (from DownloadManager or headers).
 * @property downloadedBytes Bytes downloaded so far.
 * @property status DownloadManager status:
 *                  1 = STATUS_PENDING
 *                  2 = STATUS_RUNNING
 *                  4 = STATUS_PAUSED
 *                  8 = STATUS_SUCCESSFUL
 *                  16 = STATUS_FAILED
 * @property createdAt Timestamp when the download was initiated.
 * @property completedAt Timestamp when download finished successfully.
 */
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey
    val downloadId: Long,
    val title: String,
    val movieSlug: String = "",
    val poster: String? = null,
    val fileName: String,
    val fileUri: String? = null,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val status: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
) {
    val progressPercentage: Int
        get() = if (totalBytes > 0L) {
            ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
        } else {
            0
        }

    val formattedTotalSize: String
        get() = formatBytes(totalBytes)

    val formattedDownloadedSize: String
        get() = formatBytes(downloadedBytes)

    val isCompleted: Boolean
        get() = status == 8

    val isActive: Boolean
        get() = status == 1 || status == 2 || status == 4

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0L) return "0 MB"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(java.util.Locale.US, "%.2f GB", gb)
                mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
                else -> String.format(java.util.Locale.US, "%.0f KB", kb)
            }
        }
    }
}
