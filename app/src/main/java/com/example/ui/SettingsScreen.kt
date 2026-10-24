package com.example.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainActivity
import com.example.ThemePalette
import com.example.data.AppUpdateState
import com.example.data.UpdateCheckStatus
import kotlinx.coroutines.delay

/**
 * Material Design 3 (M3) Settings UI for Smart Dashboard.
 *
 * Implements genuine Material You / Material 3 styling:
 * - Expressive rounded containers (RoundedCornerShape 28.dp for Dialogs, 20.dp for Cards)
 * - Tonal icon badges with soft color containers
 * - Inset item dividers and generous touch targets (min 56dp)
 * - M3 standard selection chips, status pills, and radio options
 * - Fully responsive layout optimized for handhelds and smart displays
 */
@Composable
fun SettingsDialog(
    selectedRegion: RegionConfig,
    backgroundThemeIndex: Int,
    launcherColumns: Int,
    palette: ThemePalette,
    updateState: AppUpdateState,
    onRegionSelected: (RegionConfig) -> Unit,
    onBackgroundSelected: (Int) -> Unit,
    onLauncherColumnsChanged: (Int) -> Unit,
    onPostalCodeSubmitted: (String, (Boolean, String) -> Unit) -> Unit,
    onOpenUpdateDialog: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onAutoRefreshIntervalChanged: (Int) -> Unit,
    onResetAllData: () -> Unit,
    onOpenSetupWizard: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showPostalCodeModal by remember { mutableStateOf(false) }
    var showThemeModal by remember { mutableStateOf(false) }
    var showColumnsModal by remember { mutableStateOf(false) }
    var showIntervalModal by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        for (i in 1..4) {
            delay(150 * i.toLong())
            (context as? MainActivity)?.setImmersiveFullscreen()
        }
    }

    // Modal Scrim / Backdrop with smooth darkened scrim
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable(enabled = true, onClick = onDismiss)
            .testTag("settings_screen_backdrop"),
        contentAlignment = Alignment.Center
    ) {
        // Material Design 3 Surface Container Dialog Card (shape 28.dp)
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = palette.containerColor),
            border = BorderStroke(1.dp, palette.cardBorderColor.copy(alpha = 0.7f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.90f)
                .clickable(enabled = false) { /* Prevent click through */ }
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.containerColor)
            ) {
                // Top App Bar: Material Design 3 Header
                Surface(
                    color = palette.containerColor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // M3 Squircle Icon Container
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = palette.accentColor.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, palette.accentColor.copy(alpha = 0.4f)),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = palette.accentColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "設定",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textColor,
                                    letterSpacing = 0.2.sp
                                )
                                Text(
                                    text = "端末とディスプレイの環境設定",
                                    fontSize = 12.sp,
                                    color = palette.secondaryTextColor
                                )
                            }
                        }

                        // M3 Close IconButton
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(palette.itemBackgroundColor)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "閉じる",
                                tint = palette.textColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = palette.cardBorderColor.copy(alpha = 0.4f),
                    thickness = 1.dp
                )

                // Scrollable Content with M3 Preference Groups
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Category 1: Location & Weather
                    M3SettingsCategory(
                        title = "地域と天気",
                        palette = palette
                    ) {
                        // Region & Address
                        M3SettingsPreferenceItem(
                            icon = Icons.Default.LocationOn,
                            iconContainerColor = Color(0xFF1E88E5),
                            title = "お住まいの地域・住所",
                            summary = "${selectedRegion.name} (${selectedRegion.englishName}) - 郵便番号から自動設定",
                            palette = palette,
                            onClick = { showPostalCodeModal = true },
                            trailingContent = {
                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = palette.accentColor.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, palette.accentColor.copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        text = "変更",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.accentColor,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        )

                        M3ItemDivider(palette)

                        // Auto-refresh interval
                        val intervalText = when (updateState.autoRefreshIntervalMinutes) {
                            0 -> "手動更新のみ"
                            15 -> "15分ごと"
                            30 -> "30分ごと"
                            60 -> "1時間ごと"
                            else -> "${updateState.autoRefreshIntervalMinutes}分ごと"
                        }
                        M3SettingsPreferenceItem(
                            icon = Icons.Default.Sync,
                            iconContainerColor = Color(0xFF00897B),
                            title = "情報の自動更新間隔",
                            summary = "天気・ニュース・更新情報の更新: $intervalText",
                            palette = palette,
                            onClick = { showIntervalModal = true },
                            trailingContent = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(100.dp),
                                        color = palette.itemBackgroundColor,
                                        border = BorderStroke(1.dp, palette.cardBorderColor)
                                    ) {
                                        Text(
                                            text = intervalText,
                                            fontSize = 11.5.sp,
                                            color = palette.textColor,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = null,
                                        tint = palette.secondaryTextColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        )
                    }

                    // Category 2: Display & Appearance
                    M3SettingsCategory(
                        title = "ディスプレイと外観",
                        palette = palette
                    ) {
                        val themeName = when (backgroundThemeIndex) {
                            0 -> "アビス (ブルー)"
                            1 -> "エメラルド (グリーン)"
                            2 -> "トワイライト (パープル)"
                            3 -> "ルビー (レッド)"
                            4 -> "カーボン (アンバー)"
                            else -> "カスタム"
                        }
                        M3SettingsPreferenceItem(
                            icon = Icons.Default.Palette,
                            iconContainerColor = Color(0xFF8E24AA),
                            title = "背景テーマカラー",
                            summary = themeName,
                            palette = palette,
                            onClick = { showThemeModal = true },
                            trailingContent = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(palette.accentColor)
                                            .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = null,
                                        tint = palette.secondaryTextColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        )

                        M3ItemDivider(palette)

                        // Launcher Columns
                        M3SettingsPreferenceItem(
                            icon = Icons.Default.GridView,
                            iconContainerColor = Color(0xFFFB8C00),
                            title = "アプリランチャーの列数",
                            summary = "1行あたり ${launcherColumns}個 のアプリアイコンを表示",
                            palette = palette,
                            onClick = { showColumnsModal = true },
                            trailingContent = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(100.dp),
                                        color = palette.itemBackgroundColor,
                                        border = BorderStroke(1.dp, palette.cardBorderColor)
                                    ) {
                                        Text(
                                            text = "${launcherColumns}列",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.accentColor,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = null,
                                        tint = palette.secondaryTextColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        )
                    }

                    // Category 3: System & Home App
                    M3SettingsCategory(
                        title = "システムと動作",
                        palette = palette
                    ) {
                        // Home Launcher Intent
                        M3SettingsPreferenceItem(
                            icon = Icons.Default.Home,
                            iconContainerColor = Color(0xFF43A047),
                            title = "デフォルトのホームアプリ設定",
                            summary = "本アプリを端末の常時ホーム画面として設定",
                            palette = palette,
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    try {
                                        val intent = Intent(Settings.ACTION_SETTINGS).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    } catch (ex: Exception) {}
                                }
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "開く",
                                    tint = palette.accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )

                        M3ItemDivider(palette)

                        // Software Update
                        val hasUpdate = updateState.status == UpdateCheckStatus.UPDATE_AVAILABLE
                        M3SettingsPreferenceItem(
                            icon = Icons.Default.SystemUpdate,
                            iconContainerColor = Color(0xFF039BE5),
                            title = "ソフトウェア更新",
                            summary = "現在のバージョン: v${updateState.currentVersion}" +
                                    if (hasUpdate) " (新バージョン v${updateState.latestVersion} が利用可能)" else "",
                            palette = palette,
                            onClick = onOpenUpdateDialog,
                            trailingContent = {
                                Button(
                                    onClick = onOpenUpdateDialog,
                                    shape = RoundedCornerShape(100.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (hasUpdate) palette.accentColor else palette.buttonColor,
                                        contentColor = if (hasUpdate) Color.Black else palette.textColor
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(
                                        text = if (hasUpdate) "更新する" else "確認",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        )
                    }

                    // Category 4: Setup & Maintenance
                    M3SettingsCategory(
                        title = "セットアップと初期化",
                        palette = palette
                    ) {
                        // Welcome Setup Wizard
                        M3SettingsPreferenceItem(
                            icon = Icons.Default.AutoFixHigh,
                            iconContainerColor = Color(0xFFFDD835),
                            title = "ようこそセットアップ画面",
                            summary = "初回起動時の案内とセットアップ手順を再度実行",
                            palette = palette,
                            onClick = {
                                onOpenSetupWizard()
                                onDismiss()
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = palette.secondaryTextColor,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        )

                        M3ItemDivider(palette)

                        // Factory Reset
                        M3SettingsPreferenceItem(
                            icon = Icons.Default.DeleteForever,
                            iconContainerColor = Color(0xFFE53935),
                            title = "すべてのデータを初期化",
                            summary = "保存設定・メモ・タスク・写真を消去して工場出荷時の状態へ戻す",
                            palette = palette,
                            titleColor = Color(0xFFFF6E6E),
                            onClick = { showResetConfirmDialog = true },
                            trailingContent = {
                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = Color(0xFFD32F2F).copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        text = "リセット",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF6E6E),
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        // Sub-Modal 1: Postal Code Search Modal
        if (showPostalCodeModal) {
            PostalCodeSettingModal(
                selectedRegion = selectedRegion,
                palette = palette,
                onPostalCodeSubmitted = onPostalCodeSubmitted,
                onDismiss = { showPostalCodeModal = false }
            )
        }

        // Sub-Modal 2: Theme Selector Modal
        if (showThemeModal) {
            ThemeSelectorModal(
                currentThemeIndex = backgroundThemeIndex,
                palette = palette,
                onThemeSelected = { idx ->
                    onBackgroundSelected(idx)
                    showThemeModal = false
                },
                onDismiss = { showThemeModal = false }
            )
        }

        // Sub-Modal 3: Launcher Columns Modal
        if (showColumnsModal) {
            LauncherColumnsModal(
                currentColumns = launcherColumns,
                palette = palette,
                onColumnsSelected = { cols ->
                    onLauncherColumnsChanged(cols)
                    showColumnsModal = false
                },
                onDismiss = { showColumnsModal = false }
            )
        }

        // Sub-Modal 4: Refresh Interval Modal
        if (showIntervalModal) {
            RefreshIntervalModal(
                currentInterval = updateState.autoRefreshIntervalMinutes,
                palette = palette,
                onIntervalSelected = { mins ->
                    onAutoRefreshIntervalChanged(mins)
                    showIntervalModal = false
                },
                onDismiss = { showIntervalModal = false }
            )
        }

        // Sub-Modal 5: Factory Reset Confirmation Dialog
        if (showResetConfirmDialog) {
            FactoryResetConfirmModal(
                palette = palette,
                onConfirm = {
                    showResetConfirmDialog = false
                    onResetAllData()
                    onDismiss()
                },
                onDismiss = { showResetConfirmDialog = false }
            )
        }
    }
}

/**
 * Material Design 3 Category Section Container
 */
@Composable
private fun M3SettingsCategory(
    title: String,
    palette: ThemePalette,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = palette.accentColor,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(start = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = palette.itemBackgroundColor),
            border = BorderStroke(1.dp, palette.cardBorderColor.copy(alpha = 0.6f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content
            )
        }
    }
}

/**
 * Material Design 3 Preference Item Row
 */
@Composable
private fun M3SettingsPreferenceItem(
    icon: ImageVector,
    iconContainerColor: Color,
    title: String,
    summary: String,
    palette: ThemePalette,
    titleColor: Color = palette.textColor,
    onClick: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                // M3 Icon Container
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = iconContainerColor.copy(alpha = 0.16f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconContainerColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = summary,
                        fontSize = 11.5.sp,
                        color = palette.secondaryTextColor,
                        lineHeight = 16.sp
                    )
                }
            }

            trailingContent?.let {
                Spacer(modifier = Modifier.width(10.dp))
                it()
            }
        }
    }
}

@Composable
private fun M3ItemDivider(palette: ThemePalette) {
    HorizontalDivider(
        color = palette.cardBorderColor.copy(alpha = 0.35f),
        thickness = 0.8.dp,
        modifier = Modifier.padding(start = 68.dp, end = 18.dp)
    )
}

/**
 * Postal Code Address Setting Modal (Material Design 3)
 */
@Composable
private fun PostalCodeSettingModal(
    selectedRegion: RegionConfig,
    palette: ThemePalette,
    onPostalCodeSubmitted: (String, (Boolean, String) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var postalCodeInput by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable(enabled = true, onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = palette.containerColor),
            border = BorderStroke(1.5.dp, palette.accentColor.copy(alpha = 0.6f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth(0.92f)
                .padding(16.dp)
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with centered icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1E88E5).copy(alpha = 0.16f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFF1E88E5),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "地域・住所の設定",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textColor
                            )
                            Text(
                                text = "郵便番号から地域情報を自動更新",
                                fontSize = 11.sp,
                                color = palette.secondaryTextColor
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(palette.itemBackgroundColor)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "閉じる",
                            tint = palette.textColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Current Region Display Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.itemBackgroundColor,
                    border = BorderStroke(1.dp, palette.cardBorderColor.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = null,
                            tint = palette.accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "現在設定中の地域:",
                                fontSize = 11.sp,
                                color = palette.secondaryTextColor
                            )
                            Text(
                                text = "${selectedRegion.name} (${selectedRegion.englishName})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.accentColor
                            )
                            Text(
                                text = "北緯 ${String.format("%.2f", selectedRegion.latitude)}°, 東経 ${String.format("%.2f", selectedRegion.longitude)}°",
                                fontSize = 10.5.sp,
                                color = palette.secondaryTextColor
                            )
                        }
                    }
                }

                Text(
                    text = "7桁の郵便番号（ハイフン任意）を入力して検索してください：",
                    fontSize = 11.5.sp,
                    color = palette.secondaryTextColor
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = postalCodeInput,
                        onValueChange = { if (it.length <= 8) postalCodeInput = it },
                        placeholder = { Text("例: 100-0001", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(onSearch = {
                            if (postalCodeInput.isNotBlank()) {
                                focusManager.clearFocus()
                                isSearching = true
                                statusMessage = null
                                onPostalCodeSubmitted(postalCodeInput) { success, msg ->
                                    isSearching = false
                                    statusMessage = success to msg
                                }
                            }
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.accentColor,
                            unfocusedBorderColor = palette.cardBorderColor,
                            focusedTextColor = palette.textColor,
                            unfocusedTextColor = palette.textColor
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    )

                    Button(
                        onClick = {
                            if (postalCodeInput.isNotBlank()) {
                                focusManager.clearFocus()
                                isSearching = true
                                statusMessage = null
                                onPostalCodeSubmitted(postalCodeInput) { success, msg ->
                                    isSearching = false
                                    statusMessage = success to msg
                                }
                            }
                        },
                        enabled = !isSearching && postalCodeInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.accentColor,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Text("検索", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                statusMessage?.let { (success, msg) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (success) Color(0xFF2E7D32).copy(alpha = 0.15f) else Color(0xFFC62828).copy(alpha = 0.15f),
                        border = BorderStroke(
                            1.dp,
                            if (success) Color(0xFF81C784) else Color(0xFFE57373)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = (if (success) "✓ " else "⚠ ") + msg,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (success) Color(0xFF81C784) else Color(0xFFFF8A80),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text("閉じる", color = palette.textColor, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/**
 * Material Design 3 Theme Selector Modal
 */
@Composable
private fun ThemeSelectorModal(
    currentThemeIndex: Int,
    palette: ThemePalette,
    onThemeSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val themes = listOf(
        0 to Pair("アビス (深海ダークブルー)", Color(0xFF58A6FF)),
        1 to Pair("エメラルド (ミントグリーン)", Color(0xFF2EA043)),
        2 to Pair("トワイライト (コズミックパープル)", Color(0xFFBC8CFF)),
        3 to Pair("ルビー (クリムゾンレッド)", Color(0xFFFF7B72)),
        4 to Pair("カーボン (アンバーオレンジ)", Color(0xFFFFA657))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable(enabled = true, onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = palette.containerColor),
            border = BorderStroke(1.5.dp, palette.cardBorderColor.copy(alpha = 0.7f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth(0.92f)
                .padding(16.dp)
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF8E24AA).copy(alpha = 0.16f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = Color(0xFFAB47BC),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "背景テーマの選択",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textColor
                        )
                        Text(
                            text = "アクセントカラーと壁紙トーンを選択",
                            fontSize = 11.sp,
                            color = palette.secondaryTextColor
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    themes.forEach { (index, data) ->
                        val (name, accent) = data
                        val isSelected = currentThemeIndex == index

                        Surface(
                            onClick = { onThemeSelected(index) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) palette.accentColor.copy(alpha = 0.18f) else palette.itemBackgroundColor,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) palette.accentColor else palette.cardBorderColor.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(accent)
                                            .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                                    )
                                    Text(
                                        text = name,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) palette.accentColor else palette.textColor
                                    )
                                }
                                if (isSelected) {
                                    Surface(
                                        shape = CircleShape,
                                        color = palette.accentColor,
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Color.Black,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text("閉じる", color = palette.textColor, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/**
 * Material Design 3 Launcher Columns Modal
 */
@Composable
private fun LauncherColumnsModal(
    currentColumns: Int,
    palette: ThemePalette,
    onColumnsSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable(enabled = true, onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = palette.containerColor),
            border = BorderStroke(1.5.dp, palette.cardBorderColor.copy(alpha = 0.7f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(0.92f)
                .padding(16.dp)
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFB8C00).copy(alpha = 0.16f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = null,
                                tint = Color(0xFFFB8C00),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "アプリランチャーの列数",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textColor
                        )
                        Text(
                            text = "画面下部に1行で並べるアイコン数",
                            fontSize = 11.sp,
                            color = palette.secondaryTextColor
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(2, 3, 4, 5, 6).forEach { cols ->
                        val isSelected = cols == currentColumns
                        Button(
                            onClick = { onColumnsSelected(cols) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) palette.accentColor else palette.itemBackgroundColor,
                                contentColor = if (isSelected) Color.Black else palette.textColor
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) palette.accentColor else palette.cardBorderColor.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "$cols",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "列",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text("閉じる", color = palette.textColor, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/**
 * Material Design 3 Data Refresh Interval Modal
 */
@Composable
private fun RefreshIntervalModal(
    currentInterval: Int,
    palette: ThemePalette,
    onIntervalSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val intervals = listOf(
        15 to "15分ごと (推奨・最新情報を保つ)",
        30 to "30分ごと (標準的・省電力)",
        60 to "1時間ごと (低頻度更新)",
        0 to "手動更新のみ (バックグラウンド取得なし)"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable(enabled = true, onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = palette.containerColor),
            border = BorderStroke(1.5.dp, palette.cardBorderColor.copy(alpha = 0.7f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth(0.92f)
                .padding(16.dp)
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF00897B).copy(alpha = 0.16f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = Color(0xFF00897B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "情報の自動更新間隔",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textColor
                        )
                        Text(
                            text = "天気・ニュースの更新頻度を設定",
                            fontSize = 11.sp,
                            color = palette.secondaryTextColor
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    intervals.forEach { (mins, label) ->
                        val isSelected = currentInterval == mins

                        Surface(
                            onClick = { onIntervalSelected(mins) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) palette.accentColor.copy(alpha = 0.18f) else palette.itemBackgroundColor,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) palette.accentColor else palette.cardBorderColor.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 13.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) palette.accentColor else palette.textColor
                                )
                                if (isSelected) {
                                    Surface(
                                        shape = CircleShape,
                                        color = palette.accentColor,
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Color.Black,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text("閉じる", color = palette.textColor, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/**
 * Material Design 3 Factory Reset Confirmation Dialog
 */
@Composable
private fun FactoryResetConfirmModal(
    palette: ThemePalette,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xDD000000))
            .clickable(enabled = true, onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = palette.containerColor),
            border = BorderStroke(2.dp, Color(0xFFE53935)),
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth(0.92f)
                .padding(20.dp)
                .clickable(enabled = false) {},
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFE53935).copy(alpha = 0.16f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Text(
                    text = "アプリを初期化しますか？",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textColor,
                    textAlign = TextAlign.Center
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.itemBackgroundColor,
                    border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "以下のデータがすべて完全に消去されます：",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textColor
                        )
                        Text(
                            text = "• 登録された住所・地域設定\n• 保存されたメモとタスク\n• カスタムフォルダーとフォト設定\n• 各種カスタマイズ",
                            fontSize = 11.sp,
                            color = palette.secondaryTextColor,
                            lineHeight = 16.sp
                        )
                    }
                }

                Text(
                    text = "初期化後は初回起動時の「ようこそセットアップ」画面に戻ります。この操作は取り消せません。",
                    fontSize = 11.5.sp,
                    color = palette.secondaryTextColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(100.dp),
                        border = BorderStroke(1.dp, palette.cardBorderColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.textColor),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text(text = "キャンセル", fontSize = 12.5.sp)
                    }

                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("confirm_factory_reset_button")
                    ) {
                        Text(text = "初期化を実行", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
