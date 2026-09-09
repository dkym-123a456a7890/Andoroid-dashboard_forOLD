package com.example.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainActivity
import com.example.ThemePalette
import com.example.data.*
import com.example.getThemePalette
import kotlinx.coroutines.delay

/**
 * Android System Settings-style UI for Smart Dashboard.
 * Groups preferences neatly into Android-like settings categories and cards.
 * Removes redundant preset cities and streamlines postal code lookup.
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

    // Modal Scrim / Backdrop
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xDD000000))
            .clickable(enabled = true, onClick = onDismiss)
            .testTag("settings_screen_backdrop"),
        contentAlignment = Alignment.Center
    ) {
        // Main Settings Sheet / Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = palette.containerColor),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, palette.cardBorderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .clickable(enabled = false) { /* Prevent click through */ }
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.containerColor)
            ) {
                // Top App Bar: Android Settings Style Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = palette.accentColor.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, palette.accentColor.copy(alpha = 0.5f)),
                            modifier = Modifier.size(42.dp)
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
                        Column {
                            Text(
                                text = "設定",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textColor
                            )
                            Text(
                                text = "スマートディスプレイの環境設定",
                                fontSize = 11.5.sp,
                                color = palette.secondaryTextColor
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(palette.itemBackgroundColor)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "閉じる",
                            tint = palette.textColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = palette.cardBorderColor.copy(alpha = 0.6f))

                // Scrollable Preference Groups
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Category 1: Location & Weather
                    SettingsCategorySection(
                        title = "地域と天気",
                        palette = palette
                    ) {
                        // Region & Address (Postal code search)
                        SettingsPreferenceItem(
                            icon = Icons.Default.LocationOn,
                            iconColor = Color(0xFF42A5F5),
                            title = "お住まいの地域・住所",
                            summary = "${selectedRegion.name} (${selectedRegion.englishName}) - 郵便番号から自動設定",
                            palette = palette,
                            onClick = { showPostalCodeModal = true },
                            trailingContent = {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = palette.accentColor.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, palette.accentColor)
                                ) {
                                    Text(
                                        text = "変更",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.accentColor,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        )

                        SettingsItemDivider(palette)

                        // Auto-refresh interval
                        val intervalText = when (updateState.autoRefreshIntervalMinutes) {
                            0 -> "手動更新のみ"
                            15 -> "15分ごと"
                            30 -> "30分ごと"
                            60 -> "1時間ごと"
                            else -> "${updateState.autoRefreshIntervalMinutes}分ごと"
                        }
                        SettingsPreferenceItem(
                            icon = Icons.Default.Sync,
                            iconColor = Color(0xFF26A69A),
                            title = "情報の自動更新間隔",
                            summary = "天気・ニュース・AIノートの更新: $intervalText",
                            palette = palette,
                            onClick = { showIntervalModal = true },
                            trailingContent = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = intervalText,
                                        fontSize = 11.sp,
                                        color = palette.secondaryTextColor
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = null,
                                        tint = palette.secondaryTextColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        )
                    }

                    // Category 2: Display & Appearance
                    SettingsCategorySection(
                        title = "ディスプレイとテーマ",
                        palette = palette
                    ) {
                        // Background Theme
                        val themeName = when (backgroundThemeIndex) {
                            0 -> "アビス (ブルー)"
                            1 -> "エメラルド (グリーン)"
                            2 -> "トワイライト (パープル)"
                            3 -> "ルビー (レッド)"
                            4 -> "カーボン (アンバー)"
                            else -> "カスタム"
                        }
                        SettingsPreferenceItem(
                            icon = Icons.Default.Palette,
                            iconColor = Color(0xFFAB47BC),
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
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(palette.accentColor)
                                            .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = null,
                                        tint = palette.secondaryTextColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        )

                        SettingsItemDivider(palette)

                        // Launcher Columns
                        SettingsPreferenceItem(
                            icon = Icons.Default.GridView,
                            iconColor = Color(0xFFFFA726),
                            title = "アプリランチャーの列数",
                            summary = "1行に ${launcherColumns}個 のアプリアイコンを表示",
                            palette = palette,
                            onClick = { showColumnsModal = true },
                            trailingContent = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${launcherColumns}列",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textColor
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = null,
                                        tint = palette.secondaryTextColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        )
                    }

                    // Category 3: System & Home App
                    SettingsCategorySection(
                        title = "システムと動作",
                        palette = palette
                    ) {
                        // Home Launcher Intent
                        SettingsPreferenceItem(
                            icon = Icons.Default.Home,
                            iconColor = Color(0xFF66BB6A),
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
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )

                        SettingsItemDivider(palette)

                        // Software Update
                        SettingsPreferenceItem(
                            icon = Icons.Default.SystemUpdate,
                            iconColor = Color(0xFF29B6F6),
                            title = "ソフトウェア更新",
                            summary = "現在のバージョン: v${updateState.currentVersion}" +
                                    if (updateState.status == UpdateCheckStatus.UPDATE_AVAILABLE) " (新バージョン v${updateState.latestVersion} あり)" else "",
                            palette = palette,
                            onClick = onOpenUpdateDialog,
                            trailingContent = {
                                Button(
                                    onClick = onOpenUpdateDialog,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (updateState.status == UpdateCheckStatus.UPDATE_AVAILABLE) palette.accentColor else palette.buttonColor,
                                        contentColor = if (updateState.status == UpdateCheckStatus.UPDATE_AVAILABLE) Color.Black else palette.textColor
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        text = if (updateState.status == UpdateCheckStatus.UPDATE_AVAILABLE) "更新する" else "更新確認",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        )
                    }

                    // Category 4: Setup & Factory Reset
                    SettingsCategorySection(
                        title = "セットアップと初期化",
                        palette = palette
                    ) {
                        // Welcome Setup Wizard
                        SettingsPreferenceItem(
                            icon = Icons.Default.AutoFixHigh,
                            iconColor = Color(0xFFFFCA28),
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
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        )

                        SettingsItemDivider(palette)

                        // Factory Reset
                        SettingsPreferenceItem(
                            icon = Icons.Default.DeleteForever,
                            iconColor = Color(0xFFEF5350),
                            title = "すべてのデータを初期化",
                            summary = "保存設定・メモ・タスク・写真を消去して工場出荷時の状態へ",
                            palette = palette,
                            titleColor = Color(0xFFFF6B6B),
                            onClick = { showResetConfirmDialog = true },
                            trailingContent = {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFD32F2F).copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD32F2F))
                                ) {
                                    Text(
                                        text = "リセット",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF5252),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        // Sub-Modal 1: Postal Code Search Modal (No preset city grid!)
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
 * Android Settings Category Section Header & Group Card
 */
@Composable
private fun SettingsCategorySection(
    title: String,
    palette: ThemePalette,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = palette.accentColor,
            modifier = Modifier.padding(start = 6.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = palette.itemBackgroundColor),
            border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor),
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
 * Individual Android Preference Item Row
 */
@Composable
private fun SettingsPreferenceItem(
    icon: ImageVector,
    iconColor: Color,
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
            .defaultMinSize(minHeight = 56.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Android-like colored square icon container
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = iconColor.copy(alpha = 0.18f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = titleColor
                    )
                    Text(
                        text = summary,
                        fontSize = 11.sp,
                        color = palette.secondaryTextColor,
                        lineHeight = 15.sp
                    )
                }
            }

            trailingContent?.let {
                Spacer(modifier = Modifier.width(8.dp))
                it()
            }
        }
    }
}

@Composable
private fun SettingsItemDivider(palette: ThemePalette) {
    HorizontalDivider(
        color = palette.cardBorderColor.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 56.dp, end = 16.dp)
    )
}

/**
 * Postal Code Address Setting Modal
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
            .background(Color(0xBB000000))
            .clickable(enabled = true, onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = palette.containerColor),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, palette.accentColor),
            modifier = Modifier
                .width(440.dp)
                .padding(16.dp)
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = palette.accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "地域・住所の設定",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textColor
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "閉じる", tint = palette.secondaryTextColor)
                    }
                }

                // Current Region Display
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = palette.itemBackgroundColor,
                    border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "現在設定されている地域:",
                            fontSize = 10.5.sp,
                            color = palette.secondaryTextColor
                        )
                        Text(
                            text = "${selectedRegion.name} (${selectedRegion.englishName})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accentColor
                        )
                        Text(
                            text = "緯度: ${String.format("%.2f", selectedRegion.latitude)}, 経度: ${String.format("%.2f", selectedRegion.longitude)}",
                            fontSize = 10.sp,
                            color = palette.secondaryTextColor
                        )
                    }
                }

                Text(
                    text = "郵便番号（7桁・ハイフン任意）を入力して住所を設定します：",
                    fontSize = 11.sp,
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
                            .height(48.dp)
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
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Text("検索", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                statusMessage?.let { (success, msg) ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (success) Color(0xFF2E7D32).copy(alpha = 0.15f) else Color(0xFFC62828).copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (success) Color(0xFF81C784) else Color(0xFFE57373)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = (if (success) "✓ " else "⚠ ") + msg,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (success) Color(0xFF81C784) else Color(0xFFFF8A80),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("閉じる", color = palette.textColor)
                    }
                }
            }
        }
    }
}

/**
 * Theme Selector Modal
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
            .background(Color(0xBB000000))
            .clickable(enabled = true, onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = palette.containerColor),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, palette.cardBorderColor),
            modifier = Modifier
                .width(420.dp)
                .padding(16.dp)
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "背景テーマカラーの選択",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textColor
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    themes.forEach { (index, data) ->
                        val (name, accent) = data
                        val isSelected = currentThemeIndex == index

                        Surface(
                            onClick = { onThemeSelected(index) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) palette.accentColor.copy(alpha = 0.2f) else palette.itemBackgroundColor,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) palette.accentColor else palette.cardBorderColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(accent)
                                    )
                                    Text(
                                        text = name,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) palette.accentColor else palette.textColor
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = palette.accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("閉じる", color = palette.textColor)
                    }
                }
            }
        }
    }
}

/**
 * Launcher Columns Modal
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
            .background(Color(0xBB000000))
            .clickable(enabled = true, onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = palette.containerColor),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, palette.cardBorderColor),
            modifier = Modifier
                .width(380.dp)
                .padding(16.dp)
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "アプリランチャーの列数",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textColor
                )
                Text(
                    text = "アプリ一覧メニューで1行に並べるアイコンの数を選択してください。",
                    fontSize = 11.sp,
                    color = palette.secondaryTextColor
                )

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
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) palette.accentColor else palette.cardBorderColor
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "${cols}列",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("閉じる", color = palette.textColor)
                    }
                }
            }
        }
    }
}

/**
 * Data Refresh Interval Modal
 */
@Composable
private fun RefreshIntervalModal(
    currentInterval: Int,
    palette: ThemePalette,
    onIntervalSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val intervals = listOf(
        15 to "15分ごと (推奨・リアルタイム寄り)",
        30 to "30分ごと (標準的・省電力)",
        60 to "1時間ごと (低頻度)",
        0 to "手動更新のみ (自動では更新しない)"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xBB000000))
            .clickable(enabled = true, onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = palette.containerColor),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, palette.cardBorderColor),
            modifier = Modifier
                .width(420.dp)
                .padding(16.dp)
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "情報の自動更新間隔",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textColor
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    intervals.forEach { (mins, label) ->
                        val isSelected = currentInterval == mins

                        Surface(
                            onClick = { onIntervalSelected(mins) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) palette.accentColor.copy(alpha = 0.2f) else palette.itemBackgroundColor,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) palette.accentColor else palette.cardBorderColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) palette.accentColor else palette.textColor
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = palette.accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("閉じる", color = palette.textColor)
                    }
                }
            }
        }
    }
}

/**
 * Factory Reset Confirm Modal
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
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = palette.containerColor),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF4444)),
            modifier = Modifier
                .width(420.dp)
                .padding(20.dp)
                .clickable(enabled = false) {},
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFF4444).copy(alpha = 0.2f),
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = Color(0xFFFF4444),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Text(
                    text = "アプリを初期化しますか？",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textColor
                )

                Text(
                    text = "すべての保存データ（地域設定、アラーム、メモ、フォルダー分け、フォト設定など）が消去され、初回起動時のようこそセットアップ画面へ戻ります。\n\n※この操作は取り消せません。",
                    fontSize = 12.sp,
                    color = palette.secondaryTextColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.textColor),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "キャンセル", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF4444),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("confirm_factory_reset_button")
                    ) {
                        Text(text = "初期化を実行", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
