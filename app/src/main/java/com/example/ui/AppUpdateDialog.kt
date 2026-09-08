package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ThemePalette
import com.example.data.AppReleaseNote
import com.example.data.AppUpdateState
import com.example.data.UpdateCheckStatus

/**
 * App Update & Software Maintenance Dialog.
 * Provides update checking, downloading animation, release notes, and auto-update configurations.
 */
@Composable
fun AppUpdateDialog(
    updateState: AppUpdateState,
    palette: ThemePalette,
    onCheckForUpdates: () -> Unit,
    onStartDownloadAndInstall: () -> Unit,
    onToggleSimulatedUpdate: () -> Unit,
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
                .width(540.dp)
                .heightIn(max = 620.dp)
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
                                .size(38.dp)
                                .background(palette.buttonColor, CircleShape)
                                .border(1.dp, palette.cardBorderColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Update",
                                tint = palette.accentColor,
                                modifier = Modifier.size(20.dp)
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
                                text = "現在のバージョン: v${updateState.currentVersion}",
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

                // Current Status Card
                UpdateStatusSection(
                    updateState = updateState,
                    palette = palette,
                    onCheckForUpdates = onCheckForUpdates,
                    onStartDownloadAndInstall = onStartDownloadAndInstall,
                    onToggleSimulatedUpdate = onToggleSimulatedUpdate,
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
                    Text(
                        text = "■ 更新内容・リリースノート",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accentColor
                    )

                    updateState.releaseNotes.forEach { note ->
                        ReleaseNoteCard(note = note, palette = palette)
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
                            text = "起動時に更新を自動確認する",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textColor
                        )
                        Text(
                            text = "新しいアップデートが利用可能な場合に通知します",
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
    onToggleSimulatedUpdate: () -> Unit,
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
                        text = "インストール済み: v${updateState.currentVersion}",
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
                        text = "最新配信版: v${updateState.latestVersion}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (updateState.status == UpdateCheckStatus.UPDATE_AVAILABLE) palette.accentColor else palette.textColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Status details
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
                            text = "最新のバージョン情報を照会中...",
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
                                text = "アップデートをダウンロード中...",
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = palette.accentColor,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "パッケージを検証中...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textColor
                            )
                            Text(
                                text = updateState.downloadSpeedText,
                                fontSize = 10.sp,
                                color = palette.secondaryTextColor
                            )
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
                                text = "最新バージョンへの更新が完了しました！",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                        Text(
                            text = "現在バージョン: v${updateState.currentVersion} が正常に稼働しています。",
                            fontSize = 11.sp,
                            color = palette.secondaryTextColor
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "完了",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            OutlinedButton(
                                onClick = onToggleSimulatedUpdate,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "再テスト (v2.7.0)",
                                    fontSize = 11.sp,
                                    color = palette.textColor
                                )
                            }
                        }
                    }
                }

                UpdateCheckStatus.UPDATE_AVAILABLE -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                text = "新しい更新プログラムが見つかりました！",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.accentColor
                            )
                        }

                        Text(
                            text = "バージョン v${updateState.latestVersion} では、新機能の追加や安定性向上が含まれています。（ファイル容量: 約 18.5 MB）",
                            fontSize = 11.sp,
                            color = palette.textColor,
                            lineHeight = 15.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onStartDownloadAndInstall,
                                colors = ButtonDefaults.buttonColors(containerColor = palette.accentColor),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("start_update_download_button")
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Download",
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "今すぐダウンロードして更新",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = onOpenPlayStore,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(0.8f)
                            ) {
                                Text(
                                    text = "ストアで確認",
                                    fontSize = 11.sp,
                                    color = palette.textColor
                                )
                            }
                        }
                    }
                }

                else -> { // UP_TO_DATE or IDLE
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                                text = "お使いのシステムは最新です",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textColor
                            )
                        }

                        Text(
                            text = "新機能や不具合修正、セキュリティアップデートがすべて適用されています。",
                            fontSize = 11.sp,
                            color = palette.secondaryTextColor
                        )

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
                                        text = "更新を再確認",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textColor
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = onToggleSimulatedUpdate,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "更新をシミュレート",
                                    fontSize = 11.sp,
                                    color = palette.secondaryTextColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
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
