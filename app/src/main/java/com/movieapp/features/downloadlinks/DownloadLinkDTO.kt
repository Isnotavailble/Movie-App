package com.movieapp.features.downloadlinks

import com.google.gson.annotations.SerializedName

/**
 * DownloadLinkDTO represents a downloadable media link extracted from movie or TV show detail endpoints.
 *
 * It maps external hosting servers such as Telegram, MegaUp, Usersdrive, and Yoteshin,
 * providing the target URL, stream quality, resolution, and human-readable file size.
 *
 * @property id Unique identifier for the download link entry.
 * @property serverName The name of the hosting provider (e.g. "Telegram", "Megaup", "Usersdrive", "Yoteshin").
 * @property url Direct web link or Telegram channel/bot destination.
 * @property size File size string (e.g. "1.84 GB", "720 MB").
 * @property quality Video encode quality (e.g. "BluRay", "WEB-DL", "HDTV").
 * @property resolution Display resolution (e.g. "1080p", "720p", "4K").
 * @property viewable Access permission (e.g. "Free", "VIP").
 */
data class DownloadLinkDTO(
    @SerializedName("id")
    val id: Long = 0L,

    @SerializedName("server_name")
    val serverName: String? = null,

    @SerializedName("url")
    val url: String? = null,

    @SerializedName("size")
    val size: String? = null,

    @SerializedName("quality")
    val quality: String? = null,

    @SerializedName("resolution")
    val resolution: String? = null,

    @SerializedName("viewable")
    val viewable: String? = null
) {
    /**
     * Sanitized server name removing trailing newlines or whitespace.
     */
    val cleanServerName: String
        get() = serverName?.trim() ?: "Direct Link"

    /**
     * Checks whether this download link points to Telegram.
     */
    val isTelegram: Boolean
        get() = cleanServerName.contains("telegram", ignoreCase = true) ||
                (url?.contains("t.me", ignoreCase = true) == true)

    /**
     * Checks whether this download link points to Yoteshin / Yoteshin Portal.
     */
    val isYoteshin: Boolean
        get() = cleanServerName.contains("yoteshin", ignoreCase = true) ||
                (url?.contains("yoteshinportal.cc", ignoreCase = true) == true) ||
                (url?.startsWith("yoteshin://", ignoreCase = true) == true)

    /**
     * Human-readable label combining server name, resolution, and file size.
     */
    val displayLabel: String
        get() = buildString {
            append(cleanServerName)
            resolution?.takeIf { it.isNotBlank() }?.let { append(" • $it") }
            size?.takeIf { it.isNotBlank() }?.let { append(" ($it)") }
        }
}
