package com.movieapp.features.downloadlinks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.movieapp.theme.NeubrutalismIcons
import com.movieapp.theme.badgeFontFamily
import com.movieapp.theme.bodyFontFamily
import com.movieapp.theme.buttonFontFamily
import com.movieapp.theme.headerFontFamily
import com.movieapp.theme.neoBorder
import com.movieapp.theme.neoColors
import com.movieapp.theme.neoShadow
import com.movieapp.util.t

/**
 * Fallback dialog presented when in-app stream resolution is cancelled or times out.
 * Gives the user two explicit choices:
 * 1. Open in Browser to download via standard browser session.
 * 2. Copy Link to clipboard for external downloaders (1DM, ADM, etc.).
 */
@Composable
fun DownloadFallbackDialog(
    link: DownloadLinkDTO,
    onOpenInBrowser: () -> Unit,
    onCopyLink: () -> Unit,
    onOpenYoteshin: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val neoColors = MaterialTheme.neoColors
    val isYoteshinInstalled = remember(link) {
        if (link.isYoteshin) DownloadManagerHelper.isYoteshinDriveInstalled(context) else false
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .neoShadow(offsetX = 4.dp, offsetY = 4.dp, color = neoColors.shadow, shape = RoundedCornerShape(16.dp))
                .background(neoColors.surface, RoundedCornerShape(16.dp))
                .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header with Title and Close 'X'
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (link.isYoteshin) t("yoteshin_signin_title") else "Download Options",
                        fontFamily = headerFontFamily(),
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = neoColors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(neoColors.surfaceMuted, RoundedCornerShape(8.dp))
                            .neoBorder(width = 1.dp, color = neoColors.border, shape = RoundedCornerShape(8.dp))
                            .clickable { onDismiss() }
                            .semantics { role = Role.Button },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = NeubrutalismIcons.Close,
                            contentDescription = "Close",
                            tint = neoColors.textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (link.isYoteshin) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(neoColors.secondary.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .neoBorder(width = 1.5.dp, color = neoColors.border, shape = RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(neoColors.secondary, RoundedCornerShape(4.dp))
                                        .neoBorder(width = 1.dp, color = neoColors.border, shape = RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "INFO",
                                        fontFamily = buttonFontFamily(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = neoColors.onSecondary
                                    )
                                }
                                Text(
                                    text = t("yoteshin_signin_title"),
                                    fontFamily = headerFontFamily(),
                                    fontSize = 12.5.sp,
                                    lineHeight = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = neoColors.textPrimary
                                )
                            }
                            Text(
                                text = t("yoteshin_signin_desc"),
                                fontFamily = bodyFontFamily(),
                                fontSize = 11.5.sp,
                                lineHeight = 17.sp,
                                color = neoColors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = t("yoteshin_app_hint"),
                                fontFamily = bodyFontFamily(),
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = neoColors.textSecondary
                            )
                        }
                    }
                } else {
                    Text(
                        text = "In-app stream could not be completed directly. Choose how you would like to download:",
                        fontFamily = bodyFontFamily(),
                        fontSize = 13.sp,
                        color = neoColors.textSecondary,
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Link Info Badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(neoColors.surfaceMuted, RoundedCornerShape(8.dp))
                        .neoBorder(width = 1.dp, color = neoColors.border, shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = link.cleanServerName,
                        fontFamily = buttonFontFamily(),
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = neoColors.textPrimary
                    )
                    link.resolution?.takeIf { it.isNotBlank() }?.let { res ->
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "($res)",
                            fontFamily = badgeFontFamily(),
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = neoColors.textSecondary
                        )
                    }
                    link.size?.takeIf { it.isNotBlank() }?.let { size ->
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "• $size",
                            fontFamily = badgeFontFamily(),
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = neoColors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Primary Choice for Yoteshin: Open in Yoteshin Drive App or Get App
                if (link.isYoteshin && onOpenYoteshin != null) {
                    Button(
                        onClick = onOpenYoteshin,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = neoColors.tertiary,
                            contentColor = neoColors.textPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .neoShadow(offsetX = 2.dp, offsetY = 2.dp, color = neoColors.shadow, shape = RoundedCornerShape(10.dp))
                            .neoBorder(width = 1.5.dp, color = neoColors.border, shape = RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = NeubrutalismIcons.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isYoteshinInstalled) t("yoteshin_open_app") else t("yoteshin_get_app"),
                                fontFamily = buttonFontFamily(),
                                fontSize = 14.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Choice: Open in Browser
                Button(
                    onClick = onOpenInBrowser,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = neoColors.primary,
                        contentColor = neoColors.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .neoShadow(offsetX = 2.dp, offsetY = 2.dp, color = neoColors.shadow, shape = RoundedCornerShape(10.dp))
                        .neoBorder(width = 1.5.dp, color = neoColors.border, shape = RoundedCornerShape(10.dp))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = NeubrutalismIcons.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (link.isYoteshin) t("yoteshin_open_browser_signin") else t("open_in_browser_action"),
                            fontFamily = buttonFontFamily(),
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Choice 2: Copy Link for 1DM / ADM
                Button(
                    onClick = onCopyLink,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = neoColors.secondary,
                        contentColor = neoColors.onSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .neoShadow(offsetX = 2.dp, offsetY = 2.dp, color = neoColors.shadow, shape = RoundedCornerShape(10.dp))
                        .neoBorder(width = 1.5.dp, color = neoColors.border, shape = RoundedCornerShape(10.dp))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = NeubrutalismIcons.Copy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Copy Link for 1DM / ADM",
                            fontFamily = buttonFontFamily(),
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
