package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ThemePalette
import com.example.data.AppReleaseNote
import com.example.data.AppUpdateState
import com.example.data.UpdateCheckStatus
import java.util.Locale

/**
 * App Update & Software Maintenance Dialog.
 * Direct integration with GitHub Releases to download and install APKs.
 */
@Composable
fun AppUpdateDialog(
    updateState: AppUpdateState,
    palette: ThemePalette,
    onCheckForUpdates: () -> Unit,
    onStartDownloadAndInstall: () -> Unit,
    onTriggerInstallApk: () -> Unit,
    onOpenGitHubReleases: () -> Unit,
    onSetAutoCheckUpdates: (Boolean) -> Unit,
    onOpenPlayStore: () -> Unit,
    onDismiss: () -> Unit
) {

    // Completely opaque backdrop
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable(enabled = true, onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = palette.containerColor),
            modifier = Modifier
                .width(560.dp)
                .heightIn(max = 640.dp)
                .border(1.5.dp, palette.cardBorderColor, RoundedCornerShape(24.dp))
                .clickable(enabled = false) { /* Prevent click propagation */ }
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(palette.containerColor)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(palette.buttonColor, CircleShape)
                                .border(1.dp, palette.cardBorderColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Update",
                                tint = palette.accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "ソフトウェアアップデート",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textColor
                            )
                            Text(
                                text = "現在の端末バージョン: v${updateState.currentVersion}",
                                fontSize = 11.sp,
                                color = palette.secondaryTextColor
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "閉じる",
                            tint = palette.secondaryTextColor
                        )
                    }
                }

                HorizontalDivider(color = palette.cardBorderColor)

                // GitHub Repository Source Bar
                // Repository Info Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = palette.buttonColor),
                    border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF24292E),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF444D56))
                                ) {
                                    Text(
                                        text = "GitHub Releases",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "配信元リポジトリ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.textColor
                                )
                            }
                            Text(
                                text = "github.com/${updateState.githubRepo}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.accentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = onOpenGitHubReleases,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = "GitHubを開く",
                                tint = palette.textColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "GitHubを開く",
                                fontSize = 11.sp,
                                color = palette.textColor
                            )
                        }
                    }
                }

                // Current Status Card & GitHub APK Download Controls
                UpdateStatusSection(
                    updateState = updateState,
                    palette = palette,
                    onCheckForUpdates = onCheckForUpdates,
                    onStartDownloadAndInstall = onStartDownloadAndInstall,
                    onTriggerInstallApk = onTriggerInstallApk,
                    onOpenGitHubReleases = onOpenGitHubReleases,
                    onOpenPlayStore = onOpenPlayStore,
                    onDismiss = onDismiss
                )

                HorizontalDivider(color = palette.cardBorderColor)

                // Release Notes & Changelog Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "■ 更新内容・リリースノート",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accentColor
                        )
                        if (updateState.releaseTitle.isNotBlank()) {
                            Text(
                                text = updateState.releaseTitle,
                                fontSize = 10.sp,
                                color = palette.secondaryTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (updateState.releaseNotes.isNotEmpty()) {
                        updateState.releaseNotes.forEach { note ->
                            ReleaseNoteCard(note = note, palette = palette)
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = palette.buttonColor,
                            border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (updateState.status == UpdateCheckStatus.CHECKING) "リリースノートを確認中..."
                                else "リリースノートはありません。「GitHubで再確認」を押すと最新のリリースノートを取得します。",
                                fontSize = 11.sp,
                                color = palette.secondaryTextColor,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = palette.cardBorderColor)

                // Preferences & Auto-Check Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.buttonColor, RoundedCornerShape(12.dp))
                        .border(1.dp, palette.cardBorderColor, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "起動時にGitHubの更新を自動確認する",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textColor
                        )
                        Text(
                            text = "新しいAPKリリースが公開された場合に通知・案内します",
                            fontSize = 10.sp,
                            color = palette.secondaryTextColor
                        )
                    }
                    Switch(
                        checked = updateState.autoCheckEnabled,
                        onCheckedChange = onSetAutoCheckUpdates,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = palette.accentColor,
                            checkedTrackColor = palette.accentColor.copy(alpha = 0.4f)
                        )
                    )
                }

                // Footer Close Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.buttonColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "閉じる",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textColor
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdateStatusSection(
    updateState: AppUpdateState,
    palette: ThemePalette,
    onCheckForUpdates: () -> Unit,
    onStartDownloadAndInstall: () -> Unit,
    onTriggerInstallApk: () -> Unit,
    onOpenGitHubReleases: () -> Unit,
    onOpenPlayStore: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = palette.buttonColor),
        border = androidx.compose.foundation.BorderStroke(
            width = if (updateState.status == UpdateCheckStatus.UPDATE_AVAILABLE) 1.5.dp else 1.dp,
            color = if (updateState.status == UpdateCheckStatus.UPDATE_AVAILABLE) palette.accentColor else palette.cardBorderColor
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Version summary row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = palette.containerColor,
                    border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor)
                ) {
                    Text(
                        text = "インストール中: v${updateState.currentVersion}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.textColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (updateState.status == UpdateCheckStatus.UPDATE_AVAILABLE) palette.accentColor.copy(alpha = 0.2f) else palette.containerColor,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (updateState.status == UpdateCheckStatus.UPDATE_AVAILABLE) palette.accentColor else palette.cardBorderColor
                    )
                ) {
                    Text(
                        text = "GitHub最新: v${updateState.latestVersion}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (updateState.status == UpdateCheckStatus.UPDATE_AVAILABLE) palette.accentColor else palette.textColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Error notice banner (if any)
            if (!updateState.errorMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFD32F2F).copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD32F2F).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Notice",
                            tint = Color(0xFFFF7043),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = updateState.errorMessage,
                            fontSize = 10.5.sp,
                            color = Color(0xFFFFCCBC),
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            // Status details & Actions
            when (updateState.status) {
                UpdateCheckStatus.CHECKING -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = palette.accentColor,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "GitHub Releasesの最新バージョンを確認中...",
                            fontSize = 12.sp,
                            color = palette.textColor
                        )
                    }
                }

                UpdateCheckStatus.DOWNLOADING -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "GitHubからAPKをダウンロード中...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.accentColor
                            )
                            Text(
                                text = "${(updateState.downloadProgress * 100).toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textColor
                            )
                        }

                        LinearProgressIndicator(
                            progress = { updateState.downloadProgress },
                            color = palette.accentColor,
                            trackColor = palette.cardBorderColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        )

                        Text(
                            text = updateState.downloadSpeedText,
                            fontSize = 10.sp,
                            color = palette.secondaryTextColor
                        )
                    }
                }

                UpdateCheckStatus.READY_TO_INSTALL -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Ready",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "APKのダウンロードが完了しました！",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }

                        Text(
                            text = updateState.downloadSpeedText,
                            fontSize = 10.sp,
                            color = palette.secondaryTextColor
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onTriggerInstallApk,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Install",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "今すぐインストール",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = onOpenGitHubReleases,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(0.8f)
                            ) {
                                Text(
                                    text = "GitHubで確認",
                                    fontSize = 11.sp,
                                    color = palette.textColor
                                )
                            }
                        }
                    }
                }

                UpdateCheckStatus.COMPLETED -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Complete",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "アップデート処理が完了しました！",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                        Text(
                            text = "現在バージョン: v${updateState.currentVersion} が稼働しています。",
                            fontSize = 11.sp,
                            color = palette.secondaryTextColor
                        )

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "完了",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                UpdateCheckStatus.UPDATE_AVAILABLE -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Update Available",
                                tint = palette.accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "新しいGitHubリリース v${updateState.latestVersion} が利用可能！",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.accentColor
                            )
                        }

                        // Display detected APK details
                        if (updateState.apkFileName.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = palette.containerColor,
                                border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "APK",
                                            tint = palette.accentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = updateState.apkFileName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = palette.textColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (updateState.apkFileSize > 0) {
                                        Text(
                                            text = formatApkSize(updateState.apkFileSize),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.secondaryTextColor
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onStartDownloadAndInstall,
                                colors = ButtonDefaults.buttonColors(containerColor = palette.accentColor),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("start_update_download_button")
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Download APK",
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "GitHubからAPKを更新",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = onOpenGitHubReleases,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(0.7f)
                            ) {
                                Text(
                                    text = "Releases",
                                    fontSize = 11.sp,
                                    color = palette.textColor
                                )
                            }
                        }
                    }
                }

                else -> { // UP_TO_DATE, IDLE, ERROR
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (updateState.status == UpdateCheckStatus.ERROR) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Error",
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "アップデート確認エラー",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF5252)
                                )
                            }
                            Text(
                                text = updateState.errorMessage ?: "更新情報の取得に失敗しました",
                                fontSize = 11.sp,
                                color = palette.secondaryTextColor
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Up to date",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (updateState.errorMessage != null) "利用可能なアップデートはありません" else "お使いのアプリは最新です",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textColor
                                )
                            }

                            val description = when {
                                updateState.errorMessage != null -> updateState.errorMessage
                                updateState.latestVersion.isNotBlank() && updateState.latestVersion == updateState.currentVersion ->
                                    "現在インストール中のバージョン (v${updateState.currentVersion}) は最新リリースと一致しています。新しい更新はありません。"
                                updateState.status == UpdateCheckStatus.IDLE ->
                                    "現在のバージョン (v${updateState.currentVersion}) のアップデートを確認できます。"
                                else ->
                                    "現在利用可能な新しいアップデートはありません (最新バージョン: v${updateState.currentVersion})。"
                            }

                            Text(
                                text = description,
                                fontSize = 11.sp,
                                color = palette.secondaryTextColor
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onCheckForUpdates,
                                colors = ButtonDefaults.buttonColors(containerColor = palette.containerColor),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Check again",
                                        tint = palette.textColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "GitHubで再確認",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textColor
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = onOpenGitHubReleases,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Releasesページ",
                                    fontSize = 11.sp,
                                    color = palette.textColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatApkSize(bytes: Long): String {
    if (bytes <= 0) return ""
    val mb = bytes / (1024.0 * 1024.0)
    return String.format(Locale.JAPAN, "%.1f MB", mb)
}

@Composable
private fun ReleaseNoteCard(
    note: AppReleaseNote,
    palette: ThemePalette
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (note.isMajor) palette.buttonColor else palette.containerColor
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (note.isMajor) 1.5.dp else 1.dp,
            color = if (note.isMajor) palette.accentColor.copy(alpha = 0.5f) else palette.cardBorderColor
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Version ${note.version}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (note.isMajor) palette.accentColor else palette.textColor
                    )
                    if (note.isMajor) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = palette.accentColor
                        ) {
                            Text(
                                text = "最新",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = note.releaseDate,
                    fontSize = 10.sp,
                    color = palette.secondaryTextColor
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.padding(start = 4.dp)
            ) {
                note.highlights.forEach { highlight ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "•",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accentColor
                        )
                        Text(
                            text = highlight,
                            fontSize = 10.5.sp,
                            color = palette.textColor,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}
