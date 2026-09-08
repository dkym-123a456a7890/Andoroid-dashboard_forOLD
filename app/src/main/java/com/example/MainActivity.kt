package com.example

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Bundle
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.*
import com.example.ui.AiContentUiState
import com.example.ui.AppUpdateDialog
import com.example.ui.DashboardViewModel
import com.example.ui.PhotoItem
import com.example.ui.QrUtils
import com.example.ui.REGIONS
import com.example.ui.RegionConfig
import com.example.ui.WeatherUiState
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : ComponentActivity() {
    private fun installSecurityProvider() {
        try {
            com.google.android.gms.security.ProviderInstaller.installIfNeeded(this)
            android.util.Log.i("MainActivity", "Security provider installed successfully")
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Failed to install security provider or not available", e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        installSecurityProvider()
        enableEdgeToEdge()
        setImmersiveFullscreen()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0C0F14)
                ) {
                    DashboardApp()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setImmersiveFullscreen()
        hideSoftKeyboard()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            setImmersiveFullscreen()
            hideSoftKeyboard()
        }
    }

    private fun hideSoftKeyboard() {
        try {
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            currentFocus?.let { view ->
                imm?.hideSoftInputFromWindow(view.windowToken, 0)
                view.clearFocus()
            } ?: run {
                imm?.hideSoftInputFromWindow(window.decorView.windowToken, 0)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error hiding keyboard", e)
        }
    }

    @Suppress("DEPRECATION")
    fun setImmersiveFullscreen() {
        try {
            val flags = (
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
            window.decorView.systemUiVisibility = flags

            window.decorView.postDelayed({
                try {
                    window.decorView.systemUiVisibility = flags
                } catch (e: Exception) {}
            }, 300)

            window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
                if ((visibility and android.view.View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    window.decorView.systemUiVisibility = flags
                    window.decorView.postDelayed({
                        try {
                            window.decorView.systemUiVisibility = flags
                        } catch (e: Exception) {}
                    }, 500)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error setting immersive fullscreen", e)
        }
    }
}

// ThemePalette representing the colors of the chosen dashboard theme (Solid & Opaque for maximum performance)
data class ThemePalette(
    val backgroundColor: Color,
    val containerColor: Color,
    val itemBackgroundColor: Color,
    val accentColor: Color,
    val textColor: Color,
    val secondaryTextColor: Color,
    val cardBorderColor: Color,
    val buttonColor: Color
)

fun getThemePalette(index: Int): ThemePalette {
    return when (index) {
        0 -> ThemePalette( // アビス (Default Dark Blue/Obsidian - Solid Opaque)
            backgroundColor = Color(0xFF0D1117),
            containerColor = Color(0xFF161B22),
            itemBackgroundColor = Color(0xFF21262D),
            accentColor = Color(0xFF58A6FF), // Light Blue
            textColor = Color(0xFFF0F6FC),
            secondaryTextColor = Color(0xFF8B949E),
            cardBorderColor = Color(0xFF30363D),
            buttonColor = Color(0xFF2D333B)
        )
        1 -> ThemePalette( // エメラルド (Deep Emerald/Mint - Solid Opaque)
            backgroundColor = Color(0xFF0A1410),
            containerColor = Color(0xFF10241C),
            itemBackgroundColor = Color(0xFF183328),
            accentColor = Color(0xFF2EA043), // Mint Green
            textColor = Color(0xFFF0F6FC),
            secondaryTextColor = Color(0xFFA5D6A7),
            cardBorderColor = Color(0xFF234A3A),
            buttonColor = Color(0xFF1D3F32)
        )
        2 -> ThemePalette( // トワイライト (Cosmic Violet/Magenta - Solid Opaque)
            backgroundColor = Color(0xFF13091F),
            containerColor = Color(0xFF1F1230),
            itemBackgroundColor = Color(0xFF2D1B44),
            accentColor = Color(0xFFBC8CFF), // Light Purple
            textColor = Color(0xFFF0F6FC),
            secondaryTextColor = Color(0xFFE1BEE7),
            cardBorderColor = Color(0xFF442866),
            buttonColor = Color(0xFF372054)
        )
        3 -> ThemePalette( // ルビー (Crimson Dark/Coral - Solid Opaque)
            backgroundColor = Color(0xFF1A0A0E),
            containerColor = Color(0xFF281218),
            itemBackgroundColor = Color(0xFF3B1B23),
            accentColor = Color(0xFFFF7B72), // Coral Red
            textColor = Color(0xFFF0F6FC),
            secondaryTextColor = Color(0xFFEF9A9A),
            cardBorderColor = Color(0xFF592734),
            buttonColor = Color(0xFF48202A)
        )
        4 -> ThemePalette( // カーボン (Pure Slate Black/Amber - Solid Opaque)
            backgroundColor = Color(0xFF121212),
            containerColor = Color(0xFF1E1E1E),
            itemBackgroundColor = Color(0xFF292929),
            accentColor = Color(0xFFFFA657), // Amber Orange
            textColor = Color(0xFFF0F6FC),
            secondaryTextColor = Color(0xFF9E9E9E),
            cardBorderColor = Color(0xFF3D3D3D),
            buttonColor = Color(0xFF333333)
        )
        else -> ThemePalette(
            backgroundColor = Color(0xFF0D1117),
            containerColor = Color(0xFF161B22),
            itemBackgroundColor = Color(0xFF21262D),
            accentColor = Color(0xFF58A6FF),
            textColor = Color(0xFFF0F6FC),
            secondaryTextColor = Color(0xFF8B949E),
            cardBorderColor = Color(0xFF30363D),
            buttonColor = Color(0xFF2D333B)
        )
    }
}

@Composable
fun DashboardApp() {
    val viewModel: DashboardViewModel = viewModel()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Periodically stabilize immersive fullscreen mode on Android 5.1 real devices
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            (context as? MainActivity)?.setImmersiveFullscreen()
        }
    }

    // States from ViewModel
    val currentTime by viewModel.currentTime.collectAsState()
    val currentDateJp by viewModel.currentDateJp.collectAsState()
    val currentEraJp by viewModel.currentEraJp.collectAsState()
    val lastUpdatedTime by viewModel.lastUpdatedTime.collectAsState()
    val weatherState by viewModel.weatherState.collectAsState()
    val aiContentState by viewModel.aiContentState.collectAsState()
    val tasks by viewModel.allTasks.collectAsState()
    val apiKeyWarning by viewModel.apiKeyWarning.collectAsState()
    val selectedRegion by viewModel.selectedRegion.collectAsState()
    val backgroundThemeIndex by viewModel.backgroundThemeIndex.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()

    val installedApps by viewModel.installedApps.collectAsState()
    val isAppsLoading by viewModel.isAppsLoading.collectAsState()
    val launcherColumns by viewModel.launcherColumns.collectAsState()
    val hiddenLauncherPackages by viewModel.hiddenLauncherPackages.collectAsState()
    val launcherFolders by viewModel.launcherFolders.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var isUtilitySheetOpen by remember { mutableStateOf(false) }

    // Close utility if launcher drawer is opened
    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen && isUtilitySheetOpen) {
            isUtilitySheetOpen = false
        }
    }

    // Timer & Alarm States
    val timerTotalSeconds by viewModel.timerTotalSeconds.collectAsState()
    val timerRemainingSeconds by viewModel.timerRemainingSeconds.collectAsState()
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()
    val isTimerFinished by viewModel.isTimerFinished.collectAsState()
    val alarms by viewModel.alarms.collectAsState()
    val activeAlarmTriggered by viewModel.activeAlarmTriggered.collectAsState()

    // Photo Frame States
    val photos by viewModel.photos.collectAsState()
    val isPhotosLoading by viewModel.isPhotosLoading.collectAsState()
    val currentPhotoIndex by viewModel.currentPhotoIndex.collectAsState()
    val isSlideshowPlaying by viewModel.isSlideshowPlaying.collectAsState()
    val slideshowIntervalSeconds by viewModel.slideshowIntervalSeconds.collectAsState()
    val photoScaleMode by viewModel.photoScaleMode.collectAsState()
    val showPhotoClockOverlay by viewModel.showPhotoClockOverlay.collectAsState()
    val isPhotoShuffle by viewModel.isPhotoShuffle.collectAsState()
    val isFullscreenSlideshow by viewModel.isFullscreenSlideshow.collectAsState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addCustomPhotoUris(uris)
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            viewModel.loadDevicePhotos()
        } else {
            viewModel.loadDevicePhotos()
        }
    }

    val updateState by viewModel.updateState.collectAsState()
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showQrDialogText by remember { mutableStateOf<Pair<String, String>?>(null) }

    val palette = remember(backgroundThemeIndex) {
        getThemePalette(backgroundThemeIndex)
    }

    // Left Drawer: App Launcher ONLY
    val launcherWidth = remember(launcherColumns) {
        when (launcherColumns) {
            2 -> 340.dp
            3 -> 440.dp
            4 -> 560.dp
            5 -> 680.dp
            else -> 780.dp
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        scrimColor = Color(0x99000000),
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = palette.containerColor,
                drawerContentColor = palette.textColor,
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp, topStart = 0.dp, bottomStart = 0.dp),
                modifier = Modifier
                    .width(launcherWidth)
                    .fillMaxHeight()
                    .border(1.dp, palette.cardBorderColor, RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp, topStart = 0.dp, bottomStart = 0.dp))
            ) {
                AppLauncherDrawerContent(
                    apps = installedApps,
                    isLoading = isAppsLoading,
                    columns = launcherColumns,
                    palette = palette,
                    hiddenPackages = hiddenLauncherPackages,
                    folders = launcherFolders,
                    onRefreshApps = { viewModel.loadInstalledApps() },
                    onAppClicked = { app ->
                        coroutineScope.launch { drawerState.close() }
                        viewModel.launchApp(context, app.packageName)
                    },
                    onHideApp = { pkg -> viewModel.hideAppFromLauncher(pkg) },
                    onUnhideApp = { pkg -> viewModel.unhideAppFromLauncher(pkg) },
                    onResetHiddenApps = { viewModel.resetHiddenApps() },
                    onRequestUninstall = { pkg -> viewModel.requestUninstallApp(context, pkg) },
                    onCreateFolder = { name, pkgs -> viewModel.createFolder(name, pkgs) },
                    onUpdateFolder = { id, name, pkgs -> viewModel.updateFolder(id, name, pkgs) },
                    onDeleteFolder = { id -> viewModel.deleteFolder(id) },
                    onAddAppToFolder = { folderId, pkg -> viewModel.addAppToFolder(folderId, pkg) },
                    onRemoveAppFromFolder = { folderId, pkg -> viewModel.removeAppFromFolder(folderId, pkg) },
                    onOpenUpdateDialog = {
                        coroutineScope.launch { drawerState.close() }
                        showUpdateDialog = true
                    },
                    onClose = {
                        coroutineScope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.backgroundColor)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        if (dragAmount > 25f) {
                            // Left-to-right swipe: open left app launcher, close utility
                            isUtilitySheetOpen = false
                            coroutineScope.launch { drawerState.open() }
                        } else if (dragAmount < -25f) {
                            // Right-to-left swipe: open right utility menu, close launcher
                            coroutineScope.launch { drawerState.close() }
                            isUtilitySheetOpen = true
                        }
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Panel (30% width) - Digital Clock, Local Weather, Status, Update Time
                Box(
                    modifier = Modifier
                        .weight(0.3f)
                        .fillMaxHeight()
                ) {
                    LeftClockWeatherPanel(
                        currentTime = currentTime,
                        currentDateJp = currentDateJp,
                        currentEraJp = currentEraJp,
                        lastUpdatedTime = lastUpdatedTime,
                        weatherState = weatherState,
                        apiKeyWarning = apiKeyWarning,
                        selectedRegion = selectedRegion,
                        palette = palette,
                        updateState = updateState,
                        onRefresh = { viewModel.refreshDashboardData() },
                        onOpenSettings = { showSettingsDialog = true },
                        onOpenUpdateDialog = { showUpdateDialog = true }
                    )
                }

                // Center Panel (40% width) - Dynamic AI Curated Notebook (News, X Trends, Anniversaries, AI Chat)
                Box(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                ) {
                    CenterCurationPanel(
                        aiContentState = aiContentState,
                        selectedRegion = selectedRegion,
                        chatMessages = chatMessages,
                        isChatLoading = isChatLoading,
                        palette = palette,
                        onSendMessage = { viewModel.sendChatMessage(it) },
                        onClearChat = { viewModel.clearChat() },
                        onItemClicked = { headline, queryText ->
                            showQrDialogText = headline to queryText
                        }
                    )
                }

                // Right Panel (30% width) - Calendar Grid & Chore List
                Box(
                    modifier = Modifier
                        .weight(0.3f)
                        .fillMaxHeight()
                ) {
                    RightDashboardPanel(
                        tasks = tasks,
                        palette = palette,
                        onAddTask = { viewModel.addTask(it) },
                        onToggleTask = { viewModel.toggleTask(it) },
                        onDeleteTask = { viewModel.deleteTask(it) }
                    )
                }
            }

            // Left Edge Pull Hint / Fast Open Tab for App Launcher
            Surface(
                onClick = {
                    isUtilitySheetOpen = false
                    coroutineScope.launch { drawerState.open() }
                },
                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                color = palette.buttonColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .height(84.dp)
                    .width(36.dp)
                    .testTag("app_launcher_edge_handle")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 6.dp, end = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = "アプリ一覧を開く",
                        tint = palette.accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "アプリ一覧を開く",
                        tint = palette.textColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Right Edge Pull Hint / Fast Open Tab for Utility Menu (Timer & Alarm)
            Surface(
                onClick = {
                    coroutineScope.launch { drawerState.close() }
                    isUtilitySheetOpen = true
                },
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                color = palette.buttonColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .height(84.dp)
                    .width(36.dp)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            if (dragAmount < -15f) {
                                coroutineScope.launch { drawerState.close() }
                                isUtilitySheetOpen = true
                            }
                        }
                    }
                    .testTag("utility_menu_edge_handle")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 2.dp, end = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "便利機能メニューを開く",
                        tint = palette.accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "便利機能メニューを開く",
                        tint = palette.textColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Invisible Right Edge Swipe Detector (makes swiping from right screen border effortless)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(32.dp)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            if (dragAmount < -15f) {
                                coroutineScope.launch { drawerState.close() }
                                isUtilitySheetOpen = true
                            }
                        }
                    }
            )

            // Right Swipeable / Slide-over Drawer for Utility Features (Timer & Alarm)
            AnimatedVisibility(
                visible = isUtilitySheetOpen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x99000000))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { isUtilitySheetOpen = false }
                ) {
                    Surface(
                        color = palette.containerColor,
                        shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 0.dp, bottomEnd = 0.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .widthIn(min = 360.dp, max = 480.dp)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { /* Prevent dismissing when clicking content */ }
                    ) {
                        UtilityDrawerContent(
                            timerTotalSeconds = timerTotalSeconds,
                            timerRemainingSeconds = timerRemainingSeconds,
                            isTimerRunning = isTimerRunning,
                            isTimerFinished = isTimerFinished,
                            alarms = alarms,
                            palette = palette,
                            onStartTimer = { viewModel.startTimer() },
                            onPauseTimer = { viewModel.pauseTimer() },
                            onResetTimer = { viewModel.resetTimer() },
                            onClearTimer = { viewModel.clearTimer() },
                            onSetTimerDuration = { viewModel.setTimerDuration(it) },
                            onModifyTimerDigit = { isMin, isTens, delta -> viewModel.modifyTimerDigit(isMin, isTens, delta) },
                            onDismissTimerAlert = { viewModel.dismissTimerAlert() },
                            onAddAlarm = { h, m, label -> viewModel.addAlarm(h, m, label) },
                            onToggleAlarm = { viewModel.toggleAlarm(it) },
                            onDeleteAlarm = { viewModel.deleteAlarm(it) },
                            onClose = { isUtilitySheetOpen = false }
                        )
                    }
                }
            }

            // Top Alert Banner for Active Alarm or Timer Up
            if (activeAlarmTriggered != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFD32F2F),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                        .fillMaxWidth(0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = "Alarm",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "🔔 アラーム: ${activeAlarmTriggered?.label}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "${String.format(Locale.JAPAN, "%02d:%02d", activeAlarmTriggered?.hour ?: 0, activeAlarmTriggered?.minute ?: 0)} の時間です",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                        Button(
                            onClick = { viewModel.dismissActiveAlarm() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("停止", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            } else if (isTimerFinished) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFE65100),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                        .fillMaxWidth(0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Timer",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "⏱ タイマーの時間になりました！",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                        Button(
                            onClick = { viewModel.dismissTimerAlert() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("OK", color = Color(0xFFE65100), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Dialogs (Completely Opaque)
    if (showSettingsDialog) {
        SettingsDialog(
            selectedRegion = selectedRegion,
            backgroundThemeIndex = backgroundThemeIndex,
            launcherColumns = launcherColumns,
            palette = palette,
            updateState = updateState,
            onRegionSelected = { viewModel.changeRegion(it) },
            onBackgroundSelected = { viewModel.changeBackground(it) },
            onLauncherColumnsChanged = { viewModel.setLauncherColumns(it) },
            onPostalCodeSubmitted = { code, callback ->
                coroutineScope.launch {
                    val result = viewModel.setAddressByPostalCode(code)
                    if (result.isSuccess) {
                        callback(true, "住所を設定しました: ${result.getOrNull()}")
                    } else {
                        callback(false, result.exceptionOrNull()?.message ?: "検索に失敗しました")
                    }
                }
            },
            onOpenUpdateDialog = {
                showSettingsDialog = false
                showUpdateDialog = true
            },
            onCheckForUpdates = { viewModel.checkForUpdates() },
            onAutoRefreshIntervalChanged = { viewModel.setAutoRefreshIntervalMinutes(it) },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showUpdateDialog) {
        AppUpdateDialog(
            updateState = updateState,
            palette = palette,
            onCheckForUpdates = { viewModel.checkForUpdates() },
            onStartDownloadAndInstall = { viewModel.startDownloadAndInstall() },
            onToggleSimulatedUpdate = { viewModel.resetOrToggleSimulatedUpdate() },
            onSetAutoCheckUpdates = { viewModel.setAutoCheckUpdates(it) },
            onOpenPlayStore = { viewModel.openPlayStore(context) },
            onDismiss = { showUpdateDialog = false }
        )
    }

    showQrDialogText?.let { (headline, queryText) ->
        QrCodeDialog(
            title = headline,
            queryText = queryText,
            palette = palette,
            onDismiss = { showQrDialogText = null }
        )
    }
}

@Composable
fun LeftClockWeatherPanel(
    currentTime: String,
    currentDateJp: String,
    currentEraJp: String,
    lastUpdatedTime: String,
    weatherState: WeatherUiState,
    apiKeyWarning: Boolean,
    selectedRegion: RegionConfig,
    palette: ThemePalette,
    updateState: AppUpdateState,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenUpdateDialog: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = palette.containerColor),
        modifier = Modifier
            .fillMaxSize()
            .border(1.dp, palette.cardBorderColor, RoundedCornerShape(24.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with Region, Battery, Network Status and Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Region name with Battery and Network directly underneath
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Location",
                            tint = palette.accentColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = selectedRegion.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Battery & Internet indicators directly below the region
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BatteryStatusIndicator()
                        NetworkStatusIndicator(palette = palette)
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(34.dp)
                            .background(palette.buttonColor, CircleShape)
                            .testTag("refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(34.dp)
                            .background(palette.buttonColor, CircleShape)
                            .testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Time & Date Display
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = currentEraJp,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.accentColor,
                    letterSpacing = 1.sp
                )

                Text(
                    text = currentTime.ifEmpty { "--:--:--" },
                    fontSize = 42.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = palette.textColor,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = currentDateJp.ifEmpty { "日付を取得中..." },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.secondaryTextColor,
                    textAlign = TextAlign.Center
                )
            }

            // Weather Widget Console
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.buttonColor, RoundedCornerShape(16.dp))
                    .border(1.dp, palette.cardBorderColor, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (weatherState) {
                    is WeatherUiState.Loading -> {
                        CircularProgressIndicator(
                            color = palette.accentColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Text("天気読み込み中...", fontSize = 12.sp, color = Color.Gray)
                    }
                    is WeatherUiState.Success -> {
                        val current = weatherState.response.current
                        if (current != null) {
                            val code = current.weatherCode
                            val desc = getWeatherDescriptionJp(code)
                            val icon = getWeatherIconJp(code)
                            val iconColor = getWeatherIconColorJp(code)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = desc,
                                    tint = iconColor,
                                    modifier = Modifier.size(46.dp)
                                )

                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(
                                        text = "${current.temperature}°C",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Text(
                                        text = desc,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB0BEC5)
                                    )
                                }
                            }

                            // Extra weather details
                            HorizontalDivider(color = palette.cardBorderColor, thickness = 1.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                WeatherIndicator(
                                    label = "湿度",
                                    value = "${current.humidity?.toInt() ?: "--"}%"
                                )
                                WeatherIndicator(
                                    label = "体感",
                                    value = "${current.apparentTemperature ?: "--"}°C"
                                )
                                WeatherIndicator(
                                    label = "風速",
                                    value = "${current.windSpeed ?: "--"}m/s"
                                )
                            }
                        } else {
                            Text("データなし", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                    is WeatherUiState.Error -> {
                        Text(
                            text = weatherState.message,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Bottom Area: Last Updated Timestamp above Version Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (lastUpdatedTime.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Last Update",
                            tint = palette.accentColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "更新日時: $lastUpdatedTime",
                            fontSize = 11.sp,
                            color = palette.secondaryTextColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Quick Update Check Button
                Surface(
                    onClick = onOpenUpdateDialog,
                    shape = RoundedCornerShape(10.dp),
                    color = if (updateState.status == UpdateCheckStatus.UPDATE_AVAILABLE) palette.accentColor.copy(alpha = 0.2f) else palette.buttonColor,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (updateState.status == UpdateCheckStatus.UPDATE_AVAILABLE) palette.accentColor else palette.cardBorderColor
                    ),
                    modifier = Modifier.testTag("quick_update_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (updateState.status == UpdateCheckStatus.UPDATE_AVAILABLE) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(Color(0xFFFF9800), CircleShape)
                            )
                            Text(
                                text = "v${updateState.currentVersion} • 更新あり (v${updateState.latestVersion})",
                                fontSize = 10.5.sp,
                                color = palette.accentColor,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Update Check",
                                tint = palette.secondaryTextColor,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "v${updateState.currentVersion} • アップデート確認",
                                fontSize = 10.5.sp,
                                color = palette.secondaryTextColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                if (apiKeyWarning) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x22FF5252)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "AI Studioの秘密キーを設定すると、リアルタイムのAI分析が有効になります。",
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF8A80)
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Smart Display Console v1.2",
                        fontSize = 10.sp,
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun NetworkStatusIndicator(palette: ThemePalette) {
    val context = LocalContext.current
    var isConnected by remember { mutableStateOf(true) }
    var isWifi by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        while (true) {
            try {
                if (cm != null) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        val activeNet = cm.activeNetwork
                        val caps = cm.getNetworkCapabilities(activeNet)
                        isConnected = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                    } else {
                        @Suppress("DEPRECATION")
                        val activeInfo = cm.activeNetworkInfo
                        isConnected = activeInfo != null && activeInfo.isConnected
                        @Suppress("DEPRECATION")
                        isWifi = activeInfo?.type == ConnectivityManager.TYPE_WIFI
                    }
                }
            } catch (e: Exception) {
                isConnected = true
            }
            delay(5000)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .background(Color(0xFF232A36), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = if (!isConnected) {
                Icons.Default.WifiOff
            } else if (isWifi) {
                Icons.Default.Wifi
            } else {
                Icons.Default.SignalCellularAlt
            },
            contentDescription = "Internet Status",
            tint = if (!isConnected) Color(0xFFEF5350) else Color(0xFF81C784),
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = if (!isConnected) "オフライン" else if (isWifi) "WiFi" else "LTE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (!isConnected) Color(0xFFEF5350) else Color.White
        )
    }
}

@Composable
fun BatteryStatusIndicator() {
    val context = LocalContext.current
    var batteryPercent by remember { mutableStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.let {
                    val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    if (level != -1 && scale != -1) {
                        batteryPercent = (level * 100 / scale.toFloat()).toInt()
                    }
                    val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .background(Color(0xFF232A36), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = if (isCharging) Icons.Default.BatteryChargingFull else {
                if (batteryPercent <= 20) Icons.Default.BatteryAlert else Icons.Default.BatteryFull
            },
            contentDescription = "Battery Status",
            tint = when {
                isCharging -> Color(0xFF81C784)
                batteryPercent <= 20 -> Color(0xFFEF5350)
                batteryPercent <= 50 -> Color(0xFFFFD54F)
                else -> Color(0xFF69F0AE)
            },
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = "$batteryPercent%",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun WeatherIndicator(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 11.sp, color = Color.Gray)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun CenterCurationPanel(
    aiContentState: AiContentUiState,
    selectedRegion: RegionConfig,
    chatMessages: List<ChatMessage>,
    isChatLoading: Boolean,
    palette: ThemePalette,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit,
    onItemClicked: (String, String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("ニュース", "Xトレンド", "記念日", "AIチャット")

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = palette.containerColor),
        modifier = Modifier
            .fillMaxSize()
            .border(1.dp, palette.cardBorderColor, RoundedCornerShape(24.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Tab Row Setup
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                divider = { HorizontalDivider(color = Color(0x22FFFFFF)) },
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = palette.accentColor,
                        height = 3.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == index) palette.textColor else Color.Gray
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body content based on State
            when (aiContentState) {
                is AiContentUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = palette.accentColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("AIがダッシュボードを編集中...", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
                is AiContentUiState.Success -> {
                    val content = aiContentState.content

                    AnimatedContent(
                        targetState = selectedTab,
                        label = "TabContent"
                    ) { tabIndex ->
                        when (tabIndex) {
                            0 -> NewsTabContent(news = content.news, selectedRegion = selectedRegion, palette = palette, onItemClicked = onItemClicked)
                            1 -> TrendsTabContent(trends = content.trends, selectedRegion = selectedRegion, palette = palette, onItemClicked = onItemClicked)
                            2 -> AnniversariesTabContent(
                                anniversaries = content.anniversaries,
                                famousBirthdays = content.famousBirthdays,
                                palette = palette,
                                onItemClicked = onItemClicked
                            )
                            3 -> NativeAiChatTabContent(
                                chatMessages = chatMessages,
                                isLoading = isChatLoading,
                                selectedRegion = selectedRegion,
                                palette = palette,
                                onSendMessage = onSendMessage,
                                onClearChat = onClearChat
                            )
                        }
                    }
                }
                is AiContentUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = aiContentState.message,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NewsTabContent(
    news: List<NewsItem>,
    selectedRegion: RegionConfig,
    palette: ThemePalette,
    onItemClicked: (String, String) -> Unit
) {
    if (news.isEmpty()) {
        EmptyStateWidget(message = "最新のニュースはありません。")
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(news) { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = palette.itemBackgroundColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorderColor, RoundedCornerShape(12.dp))
                    .clickable {
                        val searchQuery = "${item.category} ${item.title}"
                        onItemClicked(item.title, item.url ?: searchQuery)
                    }
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
                        Surface(
                            color = when (item.category) {
                                "テクノロジー", "IT" -> Color(0xFF0288D1)
                                "スポーツ" -> Color(0xFF2E7D32)
                                "社会" -> Color(0xFFEF6C00)
                                "エンタメ" -> Color(0xFFC2185B)
                                else -> Color(0xFF455A64)
                            },
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = item.category,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "QR Code",
                                tint = palette.accentColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = item.time,
                                fontSize = 11.sp,
                                color = palette.secondaryTextColor
                            )
                        }
                    }

                    Text(
                        text = item.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TrendsTabContent(
    trends: List<TrendItem>,
    selectedRegion: RegionConfig,
    palette: ThemePalette,
    onItemClicked: (String, String) -> Unit
) {
    if (trends.isEmpty()) {
        EmptyStateWidget(message = "急上昇トレンドはありません。")
        return
    }

    val displayTrends = remember(trends) { trends.take(5) }

    // Display Top 5 X Trends with ranking badges
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = "Xトレンド",
                    tint = palette.accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "X（旧Twitter）リアルタイム急上昇（5選）",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.accentColor
                )
            }
        }

        itemsIndexed(displayTrends) { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.itemBackgroundColor, RoundedCornerShape(12.dp))
                    .border(1.dp, palette.cardBorderColor, RoundedCornerShape(12.dp))
                    .clickable {
                        val searchQuery = item.keyword
                        onItemClicked(item.keyword, searchQuery)
                    }
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Trend Rank Badge
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            when (index) {
                                0 -> Color(0xFFFFD700)
                                1 -> Color(0xFFC0C0C0)
                                2 -> Color(0xFFCD7F32)
                                else -> palette.buttonColor
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = if (index < 3) Color.Black else palette.textColor
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.keyword,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = palette.textColor
                        )
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "QR Code",
                            tint = palette.accentColor,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    if (item.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.description,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            color = palette.secondaryTextColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnniversariesTabContent(
    anniversaries: List<String>,
    famousBirthdays: List<String>,
    palette: ThemePalette,
    onItemClicked: (String, String) -> Unit
) {
    if (anniversaries.isEmpty() && famousBirthdays.isEmpty()) {
        EmptyStateWidget(message = "記念日情報はありません。")
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Section 1: 今日は何の日・記念日（5件）
        if (anniversaries.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "記念日",
                        tint = palette.accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "今日は何の日・記念日（5選）",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.accentColor
                    )
                }
            }

            itemsIndexed(anniversaries.take(5)) { index, item ->
                val title = item.substringBefore(":")
                val cleanTitle = if (title == item) item.substringBefore("：") else title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.itemBackgroundColor, RoundedCornerShape(12.dp))
                        .border(1.dp, palette.cardBorderColor, RoundedCornerShape(12.dp))
                        .clickable {
                            onItemClicked(cleanTitle, cleanTitle)
                        }
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(palette.buttonColor, CircleShape)
                            .border(1.dp, palette.cardBorderColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accentColor
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textColor,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "QR Code",
                                tint = palette.accentColor,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section 2: 有名人の誕生日（3件）
        if (famousBirthdays.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cake,
                        contentDescription = "誕生日",
                        tint = Color(0xFFFFB74D),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "今日誕生日の有名人・著名人（3選）",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFFB74D)
                    )
                }
            }

            items(famousBirthdays.take(3)) { item ->
                val name = item.substringBefore("(")
                val cleanName = if (name == item) item.substringBefore(":") else name
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.itemBackgroundColor, RoundedCornerShape(12.dp))
                        .border(1.dp, palette.cardBorderColor, RoundedCornerShape(12.dp))
                        .clickable {
                            onItemClicked(cleanName.trim(), cleanName.trim())
                        }
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(palette.buttonColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Star",
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textColor,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "QR Code",
                                tint = Color(0xFFFFB74D),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Native Jetpack Compose AI Chat interface (strictly NO WebView)
 * Modeled after the modern mobile Gemini app with robust voice conversation (TTS and STT).
 */
@Composable
fun NativeAiChatTabContent(
    chatMessages: List<ChatMessage>,
    isLoading: Boolean,
    selectedRegion: RegionConfig,
    palette: ThemePalette,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var autoSpeakEnabled by remember { mutableStateOf(true) }
    var currentSpeakingText by remember { mutableStateOf<String?>(null) }
    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    // Initialize TextToSpeech engine with robust language setup and UtteranceListener
    DisposableEffect(context) {
        var ttsInstance: TextToSpeech? = null
        ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val langResult = ttsInstance?.setLanguage(Locale.JAPANESE)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    ttsInstance?.setLanguage(Locale.JAPAN)
                }
                ttsInstance?.setPitch(1.0f)
                ttsInstance?.setSpeechRate(1.05f)
                ttsInstance?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        currentSpeakingText = null
                    }
                    override fun onError(utteranceId: String?) {
                        currentSpeakingText = null
                    }
                })
                ttsReady = true
            }
        }
        textToSpeech = ttsInstance
        onDispose {
            ttsInstance?.stop()
            ttsInstance?.shutdown()
        }
    }

    // Function to speak cleaned text out loud
    val speakText: (String) -> Unit = { rawText ->
        val cleanText = rawText
            .replace("*", "")
            .replace("#", "")
            .replace("`", "")
            .replace("- ", "")
            .trim()
        if (cleanText.isNotBlank() && ttsReady && textToSpeech != null) {
            currentSpeakingText = rawText
            val utteranceId = "GEMINI_CHAT_${System.currentTimeMillis()}"
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                textToSpeech?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            } else {
                @Suppress("DEPRECATION")
                val params = HashMap<String, String>()
                params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = utteranceId
                textToSpeech?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params)
            }
        }
    }

    val stopSpeaking = {
        textToSpeech?.stop()
        currentSpeakingText = null
    }

    // Auto-read aloud latest assistant response when received
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
            val latest = chatMessages.last()
            if (!latest.isUser && autoSpeakEnabled) {
                speakText(latest.text)
            }
        }
    }

    // Standard Android Voice Input Launcher (100% compatible on real devices)
    val speechResultLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.firstOrNull()?.trim() ?: ""
            if (spokenText.isNotBlank()) {
                inputText = ""
                onSendMessage(spokenText)
            }
        }
    }

    val launchVoiceRecognizer = {
        try {
            val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ja-JP")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "ja-JP")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Geminiに話しかけてください...")
            }
            speechResultLauncher.launch(speechIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "音声認識機能が見つかりませんでした。キーボードの音声入力もお試しください。", Toast.LENGTH_SHORT).show()
        }
    }

    // Audio recording permission launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchVoiceRecognizer()
        } else {
            Toast.makeText(context, "音声入力を使用するにはマイクへのアクセス権限が必要です", Toast.LENGTH_SHORT).show()
        }
    }

    val handleVoiceButtonClick = {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val hasAudioPermission = context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (hasAudioPermission) {
                launchVoiceRecognizer()
            } else {
                audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            }
        } else {
            launchVoiceRecognizer()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.containerColor, RoundedCornerShape(16.dp))
            .border(1.dp, palette.cardBorderColor, RoundedCornerShape(16.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Modern Gemini App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color(0xFF4E82EE), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Gemini",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Gemini",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.textColor
                        )
                        Surface(
                            color = palette.buttonColor,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "汎用AI対話",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.accentColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Auto TTS toggle button
                IconButton(
                    onClick = {
                        autoSpeakEnabled = !autoSpeakEnabled
                        if (!autoSpeakEnabled) {
                            stopSpeaking()
                        }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (autoSpeakEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "自動読み上げ切り替え",
                        tint = if (autoSpeakEnabled) palette.accentColor else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Clear Chat History button
                IconButton(
                    onClick = {
                        stopSpeaking()
                        onClearChat()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "チャット履歴消去",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = palette.cardBorderColor, thickness = 1.dp)

        // Quick Gemini Prompt Suggestion Chips (Generic AI chat)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val suggestions = listOf(
                "✨ 今日の雑学・豆知識" to "今日知っておくと面白い雑学や楽しい豆知識を教えて",
                "✨ 簡単おすすめ晩ごはん" to "手軽にパパッと作れる美味しい晩ごはんレシピのアイデアを教えて",
                "✨ リフレッシュ方法" to "仕事や勉強の合間にできる効果的なリフレッシュ法やストレッチを教えて"
            )
            suggestions.forEach { (label, prompt) ->
                Surface(
                    color = palette.buttonColor,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .clickable {
                            onSendMessage(prompt)
                        }
                        .border(1.dp, palette.cardBorderColor, RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Chat Message History List (Gemini mobile style)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(chatMessages) { message ->
                    val isCurrentSpeaking = currentSpeakingText == message.text
                    ChatMessageBubble(
                        message = message,
                        palette = palette,
                        isSpeaking = isCurrentSpeaking,
                        onSpeak = {
                            if (isCurrentSpeaking) {
                                stopSpeaking()
                            } else {
                                speakText(message.text)
                            }
                        }
                    )
                }

                if (isLoading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(0xFF4E82EE), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Gemini Thinking",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            CircularProgressIndicator(
                                color = palette.accentColor,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Geminiが回答を生成中...",
                                fontSize = 11.sp,
                                color = palette.secondaryTextColor
                            )
                        }
                    }
                }
            }
        }

        // Modern Pill Input Dock (Gemini mobile style)
        Surface(
            color = palette.buttonColor,
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text("Geminiに何でも質問...", fontSize = 11.sp, color = palette.secondaryTextColor)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = palette.textColor,
                        unfocusedTextColor = palette.textColor,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("ai_chat_input_field"),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                            focusManager.clearFocus()
                        }
                    })
                )

                // Voice Mic Button (Real-device STT)
                IconButton(
                    onClick = handleVoiceButtonClick,
                    modifier = Modifier
                        .size(34.dp)
                        .background(palette.itemBackgroundColor, CircleShape)
                        .border(1.dp, palette.cardBorderColor, CircleShape)
                        .testTag("voice_input_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "音声入力",
                        tint = palette.accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Send Button
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                            focusManager.clearFocus()
                        }
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .background(palette.accentColor, CircleShape)
                        .testTag("ai_chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "送信",
                        tint = Color(0xFF090B0F),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    palette: ThemePalette,
    isSpeaking: Boolean,
    onSpeak: () -> Unit
) {
    val context = LocalContext.current
    val isUser = message.isUser

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color(0xFF4E82EE), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Gemini",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Surface(
                color = if (isUser) palette.accentColor else palette.itemBackgroundColor,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                modifier = Modifier.border(
                    1.dp,
                    if (isUser) palette.accentColor else palette.cardBorderColor,
                    RoundedCornerShape(16.dp)
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = message.text,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (isUser) Color(0xFF090B0F) else palette.textColor
                    )
                }
            }

            // Message actions and timestamp
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 3.dp, start = 2.dp, end = 2.dp)
            ) {
                Text(
                    text = message.timestamp,
                    fontSize = 9.sp,
                    color = Color.Gray
                )

                if (!isUser) {
                    // Play/Stop TTS Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .clickable { onSpeak() }
                            .padding(2.dp)
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.StopCircle else Icons.Default.VolumeUp,
                            contentDescription = if (isSpeaking) "停止" else "読み上げ",
                            tint = if (isSpeaking) Color(0xFFEF5350) else palette.accentColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isSpeaking) "停止" else "読み上げ",
                            fontSize = 9.sp,
                            color = if (isSpeaking) Color(0xFFEF5350) else palette.accentColor
                        )
                    }

                    // Copy to clipboard
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val clip = ClipData.newPlainText("Gemini Response", message.text)
                                clipboard?.setPrimaryClip(clip)
                                Toast.makeText(context, "回答をコピーしました", Toast.LENGTH_SHORT).show()
                            }
                            .padding(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "コピー",
                            tint = Color.Gray,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "コピー",
                            fontSize = 9.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateWidget(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message, color = Color.Gray, fontSize = 13.sp)
    }
}

@Composable
fun RightDashboardPanel(
    tasks: List<Task>,
    palette: ThemePalette,
    onAddTask: (String) -> Unit,
    onToggleTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper Part (Calendar Widget)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            CalendarWidget(palette = palette)
        }

        // Lower Part (Chore List / Quick Tasks Widget)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            TaskChecklistWidget(
                tasks = tasks,
                palette = palette,
                onAddTask = onAddTask,
                onToggleTask = onToggleTask,
                onDeleteTask = onDeleteTask
            )
        }
    }
}

@Composable
fun CalendarWidget(palette: ThemePalette) {
    val cal = Calendar.getInstance()
    val currentDay = cal.get(Calendar.DAY_OF_MONTH)
    val currentMonth = cal.get(Calendar.MONTH) // 0-indexed
    val currentYear = cal.get(Calendar.YEAR)

    val monthNames = listOf(
        "1月", "2月", "3月", "4月", "5月", "6月",
        "7月", "8月", "9月", "10月", "11月", "12月"
    )

    val daysOfWeek = listOf("日", "月", "火", "水", "木", "金", "土")

    val daysList = remember(currentYear, currentMonth) {
        generateCalendarDays(currentYear, currentMonth)
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = palette.containerColor),
        modifier = Modifier
            .fillMaxSize()
            .border(1.dp, palette.cardBorderColor, RoundedCornerShape(24.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Calendar Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${currentYear}年 ${monthNames[currentMonth]}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textColor
                )

                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Calendar",
                    tint = palette.accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Days of Week Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                daysOfWeek.forEach { dayName ->
                    Text(
                        text = dayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (dayName) {
                            "日" -> Color(0xFFEF5350)
                            "土" -> Color(0xFF42A5F5)
                            else -> Color.Gray
                        },
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Calendar Grid Items
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val rows = daysList.chunked(7)
                rows.forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        week.forEach { day ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.2f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (day != null) {
                                    val isToday = (day == currentDay)
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(if (isToday) palette.accentColor else Color.Transparent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = day.toString(),
                                            fontSize = 12.sp,
                                            fontWeight = if (isToday) FontWeight.Black else FontWeight.Bold,
                                            color = if (isToday) Color(0xFF090B0F) else palette.textColor
                                        )
                                    }
                                }
                            }
                        }
                        if (week.size < 7) {
                            for (i in 0 until (7 - week.size)) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun generateCalendarDays(year: Int, month: Int): List<Int?> {
    val calendar = Calendar.getInstance()
    calendar.set(year, month, 1)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    val days = mutableListOf<Int?>()
    for (i in 1 until firstDayOfWeek) {
        days.add(null)
    }
    for (i in 1..daysInMonth) {
        days.add(i)
    }
    return days
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskChecklistWidget(
    tasks: List<Task>,
    palette: ThemePalette,
    onAddTask: (String) -> Unit,
    onToggleTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit
) {
    var newTaskTitle by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = palette.containerColor),
        modifier = Modifier
            .fillMaxSize()
            .border(1.dp, palette.cardBorderColor, RoundedCornerShape(24.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Widget Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "本日のメモ / タスク",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textColor
                )

                Text(
                    text = "${tasks.count { !it.isCompleted }}件未完了",
                    fontSize = 11.sp,
                    color = palette.accentColor
                )
            }

            // Scrollable list
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                if (tasks.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "タスクはありません。追加しましょう！",
                            color = Color.DarkGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(tasks, key = { it.id }) { task ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(palette.itemBackgroundColor, RoundedCornerShape(10.dp))
                                    .border(1.dp, palette.cardBorderColor, RoundedCornerShape(10.dp))
                                    .clickable { onToggleTask(task) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = task.isCompleted,
                                        onCheckedChange = { onToggleTask(task) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = palette.accentColor,
                                            uncheckedColor = palette.secondaryTextColor
                                        ),
                                        modifier = Modifier.size(24.dp)
                                    )

                                    Text(
                                        text = task.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (task.isCompleted) palette.secondaryTextColor else palette.textColor,
                                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteTask(task) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color(0xFFEF5350),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Task Addition Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newTaskTitle,
                    onValueChange = { newTaskTitle = it },
                    placeholder = { Text("メモを入力...", fontSize = 11.sp, color = palette.secondaryTextColor) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.accentColor,
                        unfocusedBorderColor = palette.cardBorderColor,
                        focusedTextColor = palette.textColor,
                        unfocusedTextColor = palette.textColor,
                        focusedContainerColor = palette.buttonColor,
                        unfocusedContainerColor = palette.buttonColor
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("task_input"),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (newTaskTitle.isNotBlank()) {
                            onAddTask(newTaskTitle)
                            newTaskTitle = ""
                            focusManager.clearFocus()
                        }
                    })
                )

                IconButton(
                    onClick = {
                        if (newTaskTitle.isNotBlank()) {
                            onAddTask(newTaskTitle)
                            newTaskTitle = ""
                            focusManager.clearFocus()
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(palette.accentColor, RoundedCornerShape(10.dp))
                        .testTag("add_task_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Task",
                        tint = Color(0xFF090B0F),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// --- Weather Code Mapping Helpers in Japanese ---
private fun getWeatherDescriptionJp(code: Int): String {
    return when (code) {
        0 -> "快晴"
        1 -> "ほぼ快晴"
        2 -> "晴れ後曇り"
        3 -> "曇り"
        45, 48 -> "霧"
        51, 53, 55 -> "霧雨"
        56, 57 -> "凍結霧雨"
        61, 63, 65 -> "雨"
        66, 67 -> "凍える雨"
        71, 73, 75 -> "雪"
        77 -> "粒雪"
        80, 81, 82 -> "にわか雨"
        85, 86 -> "にわか雪"
        95 -> "雷雨"
        96, 99 -> "雹を伴う雷雨"
        else -> "曇り"
    }
}

private fun getWeatherIconJp(code: Int): ImageVector {
    return when (code) {
        0 -> Icons.Default.WbSunny
        1, 2 -> Icons.Default.Cloud
        3 -> Icons.Default.Cloud
        45, 48 -> Icons.Default.Dehaze
        51, 53, 55, 61, 63, 65, 80, 81, 82 -> Icons.Default.Umbrella
        71, 73, 75, 77, 85, 86 -> Icons.Default.AcUnit
        95, 96, 99 -> Icons.Default.FlashOn
        else -> Icons.Default.Cloud
    }
}

private fun getWeatherIconColorJp(code: Int): Color {
    return when (code) {
        0 -> Color(0xFFFFB300)
        1, 2 -> Color(0xFFFFD54F)
        3 -> Color(0xFF90A4AE)
        45, 48 -> Color(0xFFB0BEC5)
        51, 53, 55, 61, 63, 65, 80, 81, 82 -> Color(0xFF4FC3F7)
        71, 73, 75, 77, 85, 86 -> Color(0xFFE0F7FA)
        95, 96, 99 -> Color(0xFFE040FB)
        else -> Color(0xFF90A4AE)
    }
}

/**
 * Settings Dialog: Completely opaque popup background and card.
 * Includes postal code address lookup.
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
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var postalCodeInput by remember { mutableStateOf("") }
    var isSearchingPostalCode by remember { mutableStateOf(false) }
    var postalCodeStatusMessage by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        for (i in 1..4) {
            delay(150 * i.toLong())
            (context as? MainActivity)?.setImmersiveFullscreen()
        }
    }

    // Completely opaque dark backdrop overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable(enabled = true, onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        // Completely opaque popup dialog card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = palette.containerColor),
            modifier = Modifier
                .width(500.dp)
                .wrapContentHeight()
                .border(1.5.dp, palette.cardBorderColor, RoundedCornerShape(24.dp))
                .clickable(enabled = false) { /* Prevent click propagation to overlay dismiss */ }
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(palette.containerColor)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "システム設定",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textColor
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = palette.secondaryTextColor
                        )
                    }
                }

                HorizontalDivider(color = palette.cardBorderColor)

                // App Launcher Columns Setting Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "■ アプリランチャーの横並び数 (現在: ${launcherColumns}列)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accentColor
                    )
                    Text(
                        text = "アプリ一覧メニューで1行に表示するアプリアイコンの列数を設定できます。",
                        fontSize = 11.sp,
                        color = palette.secondaryTextColor
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(2, 3, 4, 5, 6).forEach { cols ->
                            val isSelected = cols == launcherColumns
                            Button(
                                onClick = { onLauncherColumnsChanged(cols) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) palette.accentColor else palette.buttonColor
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${cols}列",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF090B0F) else palette.textColor
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = palette.cardBorderColor)

                // Postal Code Address Setting Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "■ 郵便番号で住所を設定",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accentColor
                    )
                    Text(
                        text = "郵便番号（7桁）を入力して、天気やAIチャットの地域をカスタム設定できます。",
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
                            onValueChange = { postalCodeInput = it },
                            placeholder = { Text("例: 523-0891 または 1000001", fontSize = 11.sp, color = palette.secondaryTextColor) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = palette.accentColor,
                                unfocusedBorderColor = palette.cardBorderColor,
                                focusedTextColor = palette.textColor,
                                unfocusedTextColor = palette.textColor,
                                focusedContainerColor = palette.buttonColor,
                                unfocusedContainerColor = palette.buttonColor
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = {
                                if (postalCodeInput.isNotBlank() && !isSearchingPostalCode) {
                                    isSearchingPostalCode = true
                                    postalCodeStatusMessage = null
                                    focusManager.clearFocus()
                                    onPostalCodeSubmitted(postalCodeInput) { success, msg ->
                                        isSearchingPostalCode = false
                                        postalCodeStatusMessage = success to msg
                                    }
                                }
                            })
                        )

                        Button(
                            onClick = {
                                if (postalCodeInput.isNotBlank() && !isSearchingPostalCode) {
                                    isSearchingPostalCode = true
                                    postalCodeStatusMessage = null
                                    focusManager.clearFocus()
                                    onPostalCodeSubmitted(postalCodeInput) { success, msg ->
                                        isSearchingPostalCode = false
                                        postalCodeStatusMessage = success to msg
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.accentColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(46.dp)
                        ) {
                            if (isSearchingPostalCode) {
                                CircularProgressIndicator(
                                    color = Color.Black,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "検索・適用",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF090B0F)
                                )
                            }
                        }
                    }

                    // Status feedback
                    postalCodeStatusMessage?.let { (success, msg) ->
                        Text(
                            text = (if (success) "✓ " else "⚠ ") + msg,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (success) Color(0xFF81C784) else Color(0xFFEF5350)
                        )
                    }
                }

                HorizontalDivider(color = palette.cardBorderColor)

                // Background Theme Selection
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "■ 背景テーマを選択",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accentColor
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val themes = listOf(
                            Color(0xFF0D1117) to "アビス",
                            Color(0xFF0A1410) to "エメラルド",
                            Color(0xFF13091F) to "トワイライト",
                            Color(0xFF1A0A0E) to "ルビー",
                            Color(0xFF121212) to "カーボン"
                        )
                        themes.forEachIndexed { idx, (color, name) ->
                            val isSelected = idx == backgroundThemeIndex
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onBackgroundSelected(idx) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(color, CircleShape)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) palette.accentColor else palette.cardBorderColor,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = palette.accentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = name,
                                    fontSize = 10.sp,
                                    color = if (isSelected) palette.textColor else palette.secondaryTextColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = palette.cardBorderColor)

                // Kiosk / Launcher Support
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "■ キオスク/ランチャー設定",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accentColor
                    )

                    Text(
                        text = "本アプリはホーム画面（ランチャー）として動作させることができます。標準のランチャーに戻す、またはデフォルトランチャーを切り替えるには、下記のシステム設定を開いて変更してください。",
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = palette.secondaryTextColor
                    )

                    Button(
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
                        colors = ButtonDefaults.buttonColors(containerColor = palette.buttonColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Home",
                                modifier = Modifier.size(16.dp),
                                tint = palette.textColor
                            )
                            Text(
                                text = "システムのホームアプリ設定を開く",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textColor
                            )
                        }
                    }
                }

                HorizontalDivider(color = palette.cardBorderColor)

                // Software Update & Auto-Refresh Settings Section
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
                            text = "■ ソフトウェアアップデート・自動更新",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accentColor
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (updateState.status == UpdateCheckStatus.UPDATE_AVAILABLE) Color(0xFFFF9800).copy(alpha = 0.2f) else palette.buttonColor,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (updateState.status == UpdateCheckStatus.UPDATE_AVAILABLE) Color(0xFFFF9800) else palette.cardBorderColor
                            )
                        ) {
                            Text(
                                text = "Ver ${updateState.currentVersion}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (updateState.status == UpdateCheckStatus.UPDATE_AVAILABLE) Color(0xFFFFB74D) else palette.textColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Update Status Banner
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = palette.buttonColor),
                        border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                val statusText = when (updateState.status) {
                                    UpdateCheckStatus.CHECKING -> "更新を確認中..."
                                    UpdateCheckStatus.UPDATE_AVAILABLE -> "新バージョン v${updateState.latestVersion} が利用可能！"
                                    UpdateCheckStatus.DOWNLOADING -> "ダウンロード中 (${(updateState.downloadProgress * 100).toInt()}%)"
                                    UpdateCheckStatus.READY_TO_INSTALL -> "インストール準備完了"
                                    UpdateCheckStatus.COMPLETED -> "最新バージョンに更新完了！"
                                    UpdateCheckStatus.UP_TO_DATE -> "お使いのアプリは最新です"
                                    else -> "最新の更新プログラムを確認できます"
                                }
                                Text(
                                    text = statusText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (updateState.status == UpdateCheckStatus.UPDATE_AVAILABLE) palette.accentColor else palette.textColor
                                )
                                Text(
                                    text = "新機能、UI改善、バグ修正の確認と適用",
                                    fontSize = 10.sp,
                                    color = palette.secondaryTextColor
                                )
                            }

                            Button(
                                onClick = onOpenUpdateDialog,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (updateState.status == UpdateCheckStatus.UPDATE_AVAILABLE) palette.accentColor else palette.containerColor
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor),
                                modifier = Modifier.testTag("open_update_dialog_button")
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Update Check",
                                        tint = if (updateState.status == UpdateCheckStatus.UPDATE_AVAILABLE) Color.Black else palette.textColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (updateState.status == UpdateCheckStatus.UPDATE_AVAILABLE) "更新する" else "更新確認",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (updateState.status == UpdateCheckStatus.UPDATE_AVAILABLE) Color.Black else palette.textColor
                                    )
                                }
                            }
                        }
                    }

                    // Dashboard Data Auto-Refresh Interval Setting
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "■ ダッシュボード情報の自動更新間隔",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textColor
                        )
                        Text(
                            text = "天気、ニュース、AIノートを自動で再読み込みする頻度を選択します。",
                            fontSize = 10.sp,
                            color = palette.secondaryTextColor
                        )
                        val intervals = listOf(
                            15 to "15分ごと",
                            30 to "30分ごと",
                            60 to "1時間ごと",
                            0 to "手動のみ"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            intervals.forEach { (mins, label) ->
                                val isSelected = updateState.autoRefreshIntervalMinutes == mins
                                Surface(
                                    onClick = { onAutoRefreshIntervalChanged(mins) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) palette.accentColor.copy(alpha = 0.25f) else palette.buttonColor,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) palette.accentColor else palette.cardBorderColor
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) palette.accentColor else palette.textColor,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * QR Code Dialog: Completely opaque popup background and card.
 */
@Composable
fun QrCodeDialog(
    title: String,
    queryText: String,
    palette: ThemePalette,
    onDismiss: () -> Unit
) {
    val searchUrl = remember(queryText) { QrUtils.getSearchUrl(queryText) }
    val qrBitmap = remember(searchUrl) { QrUtils.generateQrCode(searchUrl, 250) }
    val imageBitmap = remember(qrBitmap) { qrBitmap?.asImageBitmap() }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        for (i in 1..4) {
            delay(150 * i.toLong())
            (context as? MainActivity)?.setImmersiveFullscreen()
        }
    }

    // Completely opaque dark backdrop overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable(enabled = true, onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        // Completely opaque popup dialog card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = palette.containerColor),
            modifier = Modifier
                .width(360.dp)
                .wrapContentHeight()
                .border(1.5.dp, palette.cardBorderColor, RoundedCornerShape(24.dp))
                .clickable(enabled = false) { /* Prevent click propagation to overlay dismiss */ }
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(palette.containerColor)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "スマートフォンで開く",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textColor
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = palette.secondaryTextColor
                        )
                    }
                }

                HorizontalDivider(color = palette.cardBorderColor)

                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )

                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = "Search QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = "QRコードの生成に失敗しました",
                            fontSize = 12.sp,
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Text(
                    text = "スマートフォンのカメラアプリなどでQRコードをスキャンすると、詳細情報にアクセスできます。",
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = palette.secondaryTextColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun AppLauncherDrawerContent(
    apps: List<AppLauncherItem>,
    isLoading: Boolean,
    columns: Int = 3,
    palette: ThemePalette,
    hiddenPackages: Set<String> = emptySet(),
    folders: List<LauncherFolder> = emptyList(),
    onRefreshApps: () -> Unit,
    onAppClicked: (AppLauncherItem) -> Unit,
    onHideApp: (String) -> Unit,
    onUnhideApp: (String) -> Unit,
    onResetHiddenApps: () -> Unit,
    onRequestUninstall: (String) -> Unit,
    onCreateFolder: (String, List<String>) -> Unit,
    onUpdateFolder: (String, String, List<String>) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onAddAppToFolder: (String, String) -> Unit,
    onRemoveAppFromFolder: (String, String) -> Unit,
    onOpenUpdateDialog: (() -> Unit)? = null,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.containerColor)
            .padding(16.dp)
    ) {
        // Drawer Header with Title and Close Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(palette.buttonColor, RoundedCornerShape(10.dp))
                        .border(1.dp, palette.cardBorderColor, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = "アプリ一覧",
                        tint = palette.accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "アプリランチャー",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textColor
                    )
                    Text(
                        text = "アプリ一覧・フォルダ整理 (${columns}列表示)",
                        fontSize = 11.sp,
                        color = palette.secondaryTextColor
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (onOpenUpdateDialog != null) {
                    IconButton(
                        onClick = onOpenUpdateDialog,
                        modifier = Modifier
                            .size(32.dp)
                            .background(palette.buttonColor, CircleShape)
                            .border(1.dp, palette.cardBorderColor, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "アップデート確認",
                            tint = palette.accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(32.dp)
                        .background(palette.buttonColor, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "閉じる",
                        tint = palette.textColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        AppsTabContent(
            apps = apps,
            isLoading = isLoading,
            columns = columns,
            palette = palette,
            hiddenPackages = hiddenPackages,
            folders = folders,
            onRefreshApps = onRefreshApps,
            onAppClicked = onAppClicked,
            onHideApp = onHideApp,
            onUnhideApp = onUnhideApp,
            onResetHiddenApps = onResetHiddenApps,
            onRequestUninstall = onRequestUninstall,
            onCreateFolder = onCreateFolder,
            onUpdateFolder = onUpdateFolder,
            onDeleteFolder = onDeleteFolder,
            onAddAppToFolder = onAddAppToFolder,
            onRemoveAppFromFolder = onRemoveAppFromFolder
        )
    }
}

enum class UtilityViewMode {
    HUB, TIMER, ALARM
}

@Composable
fun AlarmClockIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f + h * 0.04f
        val r = w * 0.34f

        // Top Twin Bells
        val bellR = w * 0.14f
        // Left Bell
        drawCircle(
            color = Color(0xFFD32F2F),
            radius = bellR,
            center = Offset(cx - r * 0.72f, cy - r * 0.78f)
        )
        drawCircle(
            color = Color(0xFFFFCDD2),
            radius = bellR * 0.4f,
            center = Offset(cx - r * 0.78f, cy - r * 0.84f)
        )
        // Right Bell
        drawCircle(
            color = Color(0xFFD32F2F),
            radius = bellR,
            center = Offset(cx + r * 0.72f, cy - r * 0.78f)
        )
        drawCircle(
            color = Color(0xFFFFCDD2),
            radius = bellR * 0.4f,
            center = Offset(cx + r * 0.66f, cy - r * 0.84f)
        )

        // Hammer / top handle
        drawCircle(
            color = Color(0xFFB0BEC5),
            radius = w * 0.05f,
            center = Offset(cx, cy - r * 0.95f)
        )

        // Feet (Left & Right)
        drawLine(
            color = Color(0xFFB0BEC5),
            start = Offset(cx - r * 0.6f, cy + r * 0.7f),
            end = Offset(cx - r * 0.85f, cy + r * 1.05f),
            strokeWidth = w * 0.06f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFFB0BEC5),
            start = Offset(cx + r * 0.6f, cy + r * 0.7f),
            end = Offset(cx + r * 0.85f, cy + r * 1.05f),
            strokeWidth = w * 0.06f,
            cap = StrokeCap.Round
        )

        // Outer Alarm Clock Ring
        drawCircle(
            color = Color(0xFFC62828),
            radius = r,
            center = Offset(cx, cy)
        )
        // Inner Face (White)
        drawCircle(
            color = Color(0xFFFFFFFF),
            radius = r * 0.84f,
            center = Offset(cx, cy)
        )

        // 12 Hour Ticks
        for (i in 0 until 12) {
            val angle = (i * 30.0) * Math.PI / 180.0
            val tickStartR = if (i % 3 == 0) r * 0.64f else r * 0.72f
            val tickEndR = r * 0.80f
            val sx = cx + (tickStartR * Math.sin(angle)).toFloat()
            val sy = cy - (tickStartR * Math.cos(angle)).toFloat()
            val ex = cx + (tickEndR * Math.sin(angle)).toFloat()
            val ey = cy - (tickEndR * Math.cos(angle)).toFloat()
            drawLine(
                color = if (i % 3 == 0) Color(0xFF1E1E1E) else Color(0xFF757575),
                start = Offset(sx, sy),
                end = Offset(ex, ey),
                strokeWidth = if (i % 3 == 0) w * 0.035f else w * 0.02f,
                cap = StrokeCap.Round
            )
        }

        // Hour Hand (pointing ~10 o'clock)
        val hourAngle = (-60.0) * Math.PI / 180.0
        val hx = cx + (r * 0.45f * Math.sin(hourAngle)).toFloat()
        val hy = cy - (r * 0.45f * Math.cos(hourAngle)).toFloat()
        drawLine(
            color = Color(0xFF1E1E1E),
            start = Offset(cx, cy),
            end = Offset(hx, hy),
            strokeWidth = w * 0.045f,
            cap = StrokeCap.Round
        )

        // Minute Hand (pointing ~2 o'clock)
        val minAngle = (60.0) * Math.PI / 180.0
        val mx = cx + (r * 0.65f * Math.sin(minAngle)).toFloat()
        val my = cy - (r * 0.65f * Math.cos(minAngle)).toFloat()
        drawLine(
            color = Color(0xFF1E1E1E),
            start = Offset(cx, cy),
            end = Offset(mx, my),
            strokeWidth = w * 0.035f,
            cap = StrokeCap.Round
        )

        // Second Hand (Red)
        val secAngle = (180.0) * Math.PI / 180.0
        val sx = cx + (r * 0.72f * Math.sin(secAngle)).toFloat()
        val sy = cy - (r * 0.72f * Math.cos(secAngle)).toFloat()
        drawLine(
            color = Color(0xFFE53935),
            start = Offset(cx, cy),
            end = Offset(sx, sy),
            strokeWidth = w * 0.02f,
            cap = StrokeCap.Round
        )

        // Center Pin
        drawCircle(
            color = Color(0xFFD32F2F),
            radius = w * 0.04f,
            center = Offset(cx, cy)
        )
    }
}

@Composable
fun TimerDialIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = w * 0.42f

        // Outer Metallic Rim
        drawCircle(
            color = Color(0xFFECEFF1),
            radius = r,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Color(0xFFCFD8DC),
            radius = r * 0.94f,
            center = Offset(cx, cy),
            style = Stroke(width = w * 0.03f)
        )

        // Dial Face (White)
        drawCircle(
            color = Color(0xFFFFFFFF),
            radius = r * 0.88f,
            center = Offset(cx, cy)
        )

        // Active Red Pie Wedge (showing 15 min remaining)
        drawArc(
            color = Color(0xFFE53935),
            startAngle = -90f,
            sweepAngle = 90f,
            useCenter = true,
            size = androidx.compose.ui.geometry.Size(r * 1.56f, r * 1.56f),
            topLeft = Offset(cx - r * 0.78f, cy - r * 0.78f)
        )

        // Dial graduation ticks (every 30 deg for 60 min dial)
        for (i in 0 until 12) {
            val angle = (i * 30.0) * Math.PI / 180.0
            val tickStartR = if (i % 3 == 0) r * 0.68f else r * 0.76f
            val tickEndR = r * 0.84f
            val sx = cx + (tickStartR * Math.sin(angle)).toFloat()
            val sy = cy - (tickStartR * Math.cos(angle)).toFloat()
            val ex = cx + (tickEndR * Math.sin(angle)).toFloat()
            val ey = cy - (tickEndR * Math.cos(angle)).toFloat()
            drawLine(
                color = Color(0xFF37474F),
                start = Offset(sx, sy),
                end = Offset(ex, ey),
                strokeWidth = if (i % 3 == 0) w * 0.03f else w * 0.015f,
                cap = StrokeCap.Round
            )
        }

        // Center Knob (Silver/Grey)
        drawCircle(
            color = Color(0xFF78909C),
            radius = r * 0.38f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Color(0xFFB0BEC5),
            radius = r * 0.32f,
            center = Offset(cx, cy)
        )
        // Center Knob Indicator line
        drawLine(
            color = Color(0xFF263238),
            start = Offset(cx, cy),
            end = Offset(cx, cy - r * 0.30f),
            strokeWidth = w * 0.035f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun UtilityHubView(
    palette: ThemePalette,
    onOpenAlarm: () -> Unit,
    onOpenTimer: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Alarm Feature Circle Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onOpenAlarm() }
            ) {
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFAB47BC), Color(0xFF7B1FA2))
                            ),
                            shape = CircleShape
                        )
                        .border(2.dp, palette.cardBorderColor.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    AlarmClockIllustration(modifier = Modifier.size(72.dp))
                }
                Text(
                    text = "アラーム",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textColor
                )
            }

            // Timer Feature Circle Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onOpenTimer() }
            ) {
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFAB47BC), Color(0xFF7B1FA2))
                            ),
                            shape = CircleShape
                        )
                        .border(2.dp, palette.cardBorderColor.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    TimerDialIllustration(modifier = Modifier.size(72.dp))
                }
                Text(
                    text = "タイマー",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textColor
                )
            }
        }
    }
}

@Composable
fun UtilityDrawerContent(
    timerTotalSeconds: Int,
    timerRemainingSeconds: Int,
    isTimerRunning: Boolean,
    isTimerFinished: Boolean,
    alarms: List<AlarmItem>,
    palette: ThemePalette,
    onStartTimer: () -> Unit,
    onPauseTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onClearTimer: () -> Unit,
    onSetTimerDuration: (Int) -> Unit,
    onModifyTimerDigit: (Boolean, Boolean, Int) -> Unit,
    onDismissTimerAlert: () -> Unit,
    onAddAlarm: (Int, Int, String) -> Unit,
    onToggleAlarm: (String) -> Unit,
    onDeleteAlarm: (String) -> Unit,
    onClose: () -> Unit
) {
    var viewMode by remember { mutableStateOf(UtilityViewMode.HUB) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.containerColor)
            .padding(16.dp)
    ) {
        // Drawer Header with Title and Close / Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (viewMode != UtilityViewMode.HUB) {
                    IconButton(
                        onClick = { viewMode = UtilityViewMode.HUB },
                        modifier = Modifier
                            .size(36.dp)
                            .background(palette.buttonColor, RoundedCornerShape(10.dp))
                            .border(1.dp, palette.cardBorderColor, RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "戻る",
                            tint = palette.accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF7B1FA2).copy(alpha = 0.85f), RoundedCornerShape(10.dp))
                            .border(1.dp, palette.cardBorderColor, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "便利機能",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = when (viewMode) {
                            UtilityViewMode.HUB -> "便利機能メニュー"
                            UtilityViewMode.TIMER -> "タイマー"
                            UtilityViewMode.ALARM -> "アラーム設定"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textColor
                    )
                    Text(
                        text = when (viewMode) {
                            UtilityViewMode.HUB -> "機能アイコンをタップして起動"
                            UtilityViewMode.TIMER -> if (isTimerRunning) "カウントダウン中" else "時間計測・タイマー"
                            UtilityViewMode.ALARM -> "${alarms.count { it.isEnabled }}件のアラームが有効"
                        },
                        fontSize = 11.sp,
                        color = palette.secondaryTextColor
                    )
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(32.dp)
                    .background(palette.buttonColor, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "閉じる",
                    tint = palette.textColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        HorizontalDivider(color = palette.cardBorderColor, thickness = 1.dp)

        Spacer(modifier = Modifier.height(12.dp))

        // Screen Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (viewMode) {
                UtilityViewMode.HUB -> {
                    UtilityHubView(
                        palette = palette,
                        onOpenAlarm = { viewMode = UtilityViewMode.ALARM },
                        onOpenTimer = { viewMode = UtilityViewMode.TIMER }
                    )
                }
                UtilityViewMode.TIMER -> {
                    TimerTabContent(
                        totalSeconds = timerTotalSeconds,
                        remainingSeconds = timerRemainingSeconds,
                        isRunning = isTimerRunning,
                        isFinished = isTimerFinished,
                        palette = palette,
                        onStart = onStartTimer,
                        onPause = onPauseTimer,
                        onReset = onResetTimer,
                        onClear = onClearTimer,
                        onSetDuration = onSetTimerDuration,
                        onModifyDigit = onModifyTimerDigit,
                        onDismissAlert = onDismissTimerAlert
                    )
                }
                UtilityViewMode.ALARM -> {
                    AlarmTabContent(
                        alarms = alarms,
                        palette = palette,
                        onAddAlarm = onAddAlarm,
                        onToggleAlarm = onToggleAlarm,
                        onDeleteAlarm = onDeleteAlarm
                    )
                }
            }
        }
    }
}

// ==========================================
// 1. Timer Tab View & Digit Components
// ==========================================
@Composable
fun HoldToRepeatIconButton(
    onTrigger: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    palette: ThemePalette,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isPressed) palette.accentColor.copy(alpha = 0.3f) else palette.buttonColor,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isPressed) palette.accentColor else palette.cardBorderColor
        ),
        modifier = modifier
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onTrigger()
                        val job = coroutineScope.launch {
                            delay(300)
                            var stepDelay = 120L
                            var count = 0
                            while (isPressed) {
                                onTrigger()
                                count++
                                if (count > 6) stepDelay = 70L
                                if (count > 16) stepDelay = 35L
                                delay(stepDelay)
                            }
                        }
                        tryAwaitRelease()
                        isPressed = false
                        job.cancel()
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
fun TimerDigitColumn(
    digit: Int,
    label: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    canIncrement: Boolean,
    canDecrement: Boolean,
    isFinished: Boolean,
    palette: ThemePalette,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.secondaryTextColor
        )

        HoldToRepeatIconButton(
            onTrigger = onIncrement,
            enabled = canIncrement,
            palette = palette,
            modifier = Modifier
                .width(46.dp)
                .height(34.dp)
        ) {
            Text(
                text = "▲",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = if (canIncrement) palette.accentColor else palette.secondaryTextColor.copy(alpha = 0.25f)
            )
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = palette.backgroundColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor),
            modifier = Modifier.width(46.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = "$digit",
                    fontSize = 38.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = if (isFinished) Color(0xFFFF7043) else palette.textColor
                )
            }
        }

        HoldToRepeatIconButton(
            onTrigger = onDecrement,
            enabled = canDecrement,
            palette = palette,
            modifier = Modifier
                .width(46.dp)
                .height(34.dp)
        ) {
            Text(
                text = "▼",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = if (canDecrement) palette.accentColor else palette.secondaryTextColor.copy(alpha = 0.25f)
            )
        }
    }
}

@Composable
fun TimerTabContent(
    totalSeconds: Int,
    remainingSeconds: Int,
    isRunning: Boolean,
    isFinished: Boolean,
    palette: ThemePalette,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onClear: () -> Unit,
    onSetDuration: (Int) -> Unit,
    onModifyDigit: (isMinutes: Boolean, isTens: Boolean, delta: Int) -> Unit,
    onDismissAlert: () -> Unit
) {
    val scrollState = rememberScrollState()
    val clampedRemaining = remainingSeconds.coerceAtLeast(0)
    val minutes = clampedRemaining / 60
    val seconds = clampedRemaining % 60

    val m10 = (minutes / 10) % 10
    val m1 = minutes % 10
    val s10 = (seconds / 10) % 10
    val s1 = seconds % 10

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isFinished) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFE65100),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "時間になりました！",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                    Button(
                        onClick = onDismissAlert,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("OK", color = Color(0xFFE65100), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Digital Display Card with ▲ / 00:00 / ▼ (Long press supported, tens & ones place separate)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.itemBackgroundColor,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isRunning) palette.accentColor else palette.cardBorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isRunning) "計測中" else "カウントダウンタイマー",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isRunning) palette.accentColor else palette.secondaryTextColor
                )

                // 4-Digit Timer Layout:
                // [分: 10位 1位] : [秒: 10位 1位]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Minutes Group
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "分",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accentColor
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TimerDigitColumn(
                                digit = m10,
                                label = "10位",
                                onIncrement = { onModifyDigit(true, true, 1) },
                                onDecrement = { onModifyDigit(true, true, -1) },
                                canIncrement = m10 < 9,
                                canDecrement = m10 > 0,
                                isFinished = isFinished,
                                palette = palette
                            )

                            TimerDigitColumn(
                                digit = m1,
                                label = "1位",
                                onIncrement = { onModifyDigit(true, false, 1) },
                                onDecrement = { onModifyDigit(true, false, -1) },
                                canIncrement = m1 < 9,
                                canDecrement = m1 > 0,
                                isFinished = isFinished,
                                palette = palette
                            )
                        }
                    }

                    // Separator colon
                    Text(
                        text = ":",
                        fontSize = 36.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        color = palette.textColor,
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .padding(top = 18.dp)
                    )

                    // Seconds Group
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "秒",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accentColor
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TimerDigitColumn(
                                digit = s10,
                                label = "10位",
                                onIncrement = { onModifyDigit(false, true, 1) },
                                onDecrement = { onModifyDigit(false, true, -1) },
                                canIncrement = s10 < 5,
                                canDecrement = s10 > 0,
                                isFinished = isFinished,
                                palette = palette
                            )

                            TimerDigitColumn(
                                digit = s1,
                                label = "1位",
                                onIncrement = { onModifyDigit(false, false, 1) },
                                onDecrement = { onModifyDigit(false, false, -1) },
                                canIncrement = s1 < 9,
                                canDecrement = s1 > 0,
                                isFinished = isFinished,
                                palette = palette
                            )
                        }
                    }
                }

                Text(
                    text = "※ ▲ / ▼ を長押しすると連続で数字が変わります",
                    fontSize = 10.sp,
                    color = palette.secondaryTextColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Control Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (isRunning) onPause() else onStart()
                },
                enabled = isRunning || clampedRemaining > 0,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) Color(0xFFD32F2F) else palette.accentColor,
                    contentColor = if (isRunning) Color.White else Color.Black
                ),
                modifier = Modifier
                    .weight(1.2f)
                    .height(46.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isRunning) "一時停止" else "スタート",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onReset,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.buttonColor,
                    contentColor = palette.textColor
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor),
                modifier = Modifier
                    .weight(0.9f)
                    .height(46.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "リセット",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            OutlinedButton(
                onClick = onClear,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = palette.secondaryTextColor
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor.copy(alpha = 0.7f)),
                modifier = Modifier
                    .weight(0.8f)
                    .height(46.dp)
            ) {
                Text(
                    text = "クリア",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Preset Duration Selectors
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "プリセット設定",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = palette.secondaryTextColor
            )

            val presets = listOf(
                "1分" to 60,
                "3分" to 180,
                "5分" to 300,
                "10分" to 600,
                "15分" to 900,
                "30分" to 1800
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presets.take(3).forEach { (label, secs) ->
                    Surface(
                        onClick = { onSetDuration(secs) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (totalSeconds == secs) palette.buttonColor else palette.itemBackgroundColor,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (totalSeconds == secs) palette.accentColor else palette.cardBorderColor
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (totalSeconds == secs) palette.accentColor else palette.textColor
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presets.drop(3).forEach { (label, secs) ->
                    Surface(
                        onClick = { onSetDuration(secs) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (totalSeconds == secs) palette.buttonColor else palette.itemBackgroundColor,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (totalSeconds == secs) palette.accentColor else palette.cardBorderColor
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (totalSeconds == secs) palette.accentColor else palette.textColor
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. Alarm Tab View
// ==========================================
@Composable
fun AlarmTabContent(
    alarms: List<AlarmItem>,
    palette: ThemePalette,
    onAddAlarm: (Int, Int, String) -> Unit,
    onToggleAlarm: (String) -> Unit,
    onDeleteAlarm: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    var selectedHour by remember { mutableIntStateOf(7) }
    var selectedMinute by remember { mutableIntStateOf(0) }
    var alarmLabel by remember { mutableStateOf("") }
    var showAddPanel by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quick Presets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "登録済みアラーム (${alarms.size}件)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = palette.secondaryTextColor
            )

            Button(
                onClick = { showAddPanel = !showAddPanel },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showAddPanel) palette.buttonColor else palette.accentColor,
                    contentColor = if (showAddPanel) palette.textColor else Color.Black
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Icon(
                    imageVector = if (showAddPanel) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (showAddPanel) "閉じる" else "新規追加",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Add Alarm Form Panel
        if (showAddPanel) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = palette.itemBackgroundColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.accentColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "アラーム時刻の設定",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accentColor
                    )

                    // Hour and Minute Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hour Selector
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = { selectedHour = (selectedHour + 1) % 24 },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "時+1", tint = palette.textColor)
                            }
                            Text(
                                text = String.format(Locale.JAPAN, "%02d", selectedHour),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = palette.textColor
                            )
                            IconButton(
                                onClick = { selectedHour = if (selectedHour > 0) selectedHour - 1 else 23 },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "時-1", tint = palette.textColor)
                            }
                        }

                        Text(
                            text = ":",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = palette.textColor,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        // Minute Selector
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = { selectedMinute = (selectedMinute + 5) % 60 },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "分+5", tint = palette.textColor)
                            }
                            Text(
                                text = String.format(Locale.JAPAN, "%02d", selectedMinute),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = palette.textColor
                            )
                            IconButton(
                                onClick = { selectedMinute = if (selectedMinute >= 5) selectedMinute - 5 else 55 },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "分-5", tint = palette.textColor)
                            }
                        }
                    }

                    // Quick hour buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("06:00" to (6 to 0), "07:00" to (7 to 0), "12:00" to (12 to 0), "21:00" to (21 to 0)).forEach { (label, time) ->
                            OutlinedButton(
                                onClick = {
                                    selectedHour = time.first
                                    selectedMinute = time.second
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(26.dp)
                            ) {
                                Text(label, fontSize = 10.sp, color = palette.textColor)
                            }
                        }
                    }

                    // Label Input
                    OutlinedTextField(
                        value = alarmLabel,
                        onValueChange = { alarmLabel = it },
                        placeholder = { Text("アラーム名 (例: 起床、薬の時間)", fontSize = 11.sp, color = palette.secondaryTextColor) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = palette.buttonColor,
                            unfocusedContainerColor = palette.buttonColor,
                            focusedBorderColor = palette.accentColor,
                            unfocusedBorderColor = palette.cardBorderColor,
                            focusedTextColor = palette.textColor,
                            unfocusedTextColor = palette.textColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            onAddAlarm(selectedHour, selectedMinute, alarmLabel.ifBlank { "アラーム" })
                            alarmLabel = ""
                            showAddPanel = false
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accentColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("この時刻でアラームを登録", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                    }
                }
            }
        }

        // Alarms List
        if (alarms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "設定されているアラームはありません",
                    fontSize = 12.sp,
                    color = palette.secondaryTextColor
                )
            }
        } else {
            alarms.forEach { alarm ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.itemBackgroundColor,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (alarm.isEnabled) palette.cardBorderColor else palette.buttonColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = if (alarm.isEnabled) palette.accentColor else palette.secondaryTextColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = String.format(Locale.JAPAN, "%02d:%02d", alarm.hour, alarm.minute),
                                    fontSize = 22.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (alarm.isEnabled) palette.textColor else palette.secondaryTextColor
                                )
                                Text(
                                    text = alarm.label,
                                    fontSize = 11.sp,
                                    color = palette.secondaryTextColor
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Switch(
                                checked = alarm.isEnabled,
                                onCheckedChange = { onToggleAlarm(alarm.id) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = palette.accentColor,
                                    checkedTrackColor = palette.buttonColor,
                                    uncheckedThumbColor = palette.secondaryTextColor,
                                    uncheckedTrackColor = palette.buttonColor
                                )
                            )

                            IconButton(
                                onClick = { onDeleteAlarm(alarm.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "削除",
                                    tint = palette.secondaryTextColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. Apps Tab View with Delete & Folders Support
// ==========================================
@Composable
fun FolderIconVector(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFFFB300)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val folderPath = Path().apply {
            moveTo(w * 0.08f, h * 0.22f)
            lineTo(w * 0.42f, h * 0.22f)
            lineTo(w * 0.52f, h * 0.35f)
            lineTo(w * 0.92f, h * 0.35f)
            lineTo(w * 0.92f, h * 0.85f)
            lineTo(w * 0.08f, h * 0.85f)
            close()
        }
        drawPath(folderPath, color = color)
    }
}

@Composable
fun AppsTabContent(
    apps: List<AppLauncherItem>,
    isLoading: Boolean,
    columns: Int = 3,
    palette: ThemePalette,
    hiddenPackages: Set<String> = emptySet(),
    folders: List<LauncherFolder> = emptyList(),
    onRefreshApps: () -> Unit,
    onAppClicked: (AppLauncherItem) -> Unit,
    onHideApp: (String) -> Unit,
    onUnhideApp: (String) -> Unit,
    onResetHiddenApps: () -> Unit,
    onRequestUninstall: (String) -> Unit,
    onCreateFolder: (String, List<String>) -> Unit,
    onUpdateFolder: (String, String, List<String>) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onAddAppToFolder: (String, String) -> Unit,
    onRemoveAppFromFolder: (String, String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isEditMode by remember { mutableStateOf(false) }

    // Dialog control states
    var selectedFolderForDetail by remember { mutableStateOf<LauncherFolder?>(null) }
    var folderForEdit by remember { mutableStateOf<LauncherFolder?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showHiddenAppsDialog by remember { mutableStateOf(false) }
    var appForActionMenu by remember { mutableStateOf<AppLauncherItem?>(null) }
    var appToDeleteConfirm by remember { mutableStateOf<AppLauncherItem?>(null) }
    var folderToDeleteConfirm by remember { mutableStateOf<LauncherFolder?>(null) }
    var appToAddToFolder by remember { mutableStateOf<AppLauncherItem?>(null) }

    // Filter out hidden apps
    val visibleApps = remember(apps, hiddenPackages) {
        apps.filter { it.packageName !in hiddenPackages }
    }

    val hiddenApps = remember(apps, hiddenPackages) {
        apps.filter { it.packageName in hiddenPackages }
    }

    val filteredApps = remember(visibleApps, searchQuery) {
        if (searchQuery.isBlank()) {
            visibleApps
        } else {
            val q = searchQuery.trim().lowercase(Locale.JAPANESE)
            visibleApps.filter {
                it.label.lowercase(Locale.JAPANESE).contains(q) ||
                it.packageName.lowercase(Locale.JAPANESE).contains(q)
            }
        }
    }

    val filteredFolders = remember(folders, searchQuery) {
        if (searchQuery.isBlank()) {
            folders
        } else {
            val q = searchQuery.trim().lowercase(Locale.JAPANESE)
            folders.filter {
                it.name.lowercase(Locale.JAPANESE).contains(q)
            }
        }
    }

    // Keep selectedFolderForDetail in sync with updated folder state
    val currentSelectedFolder = remember(selectedFolderForDetail, folders) {
        selectedFolderForDetail?.let { current ->
            folders.find { it.id == current.id }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // --- Top Action & Status Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${visibleApps.size} 件",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.secondaryTextColor
                )

                if (hiddenApps.isNotEmpty()) {
                    Surface(
                        onClick = { showHiddenAppsDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        color = palette.buttonColor,
                        border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "非表示 ${hiddenApps.size}件",
                                fontSize = 10.sp,
                                color = palette.accentColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // New Folder Button
                IconButton(
                    onClick = { showCreateFolderDialog = true },
                    modifier = Modifier
                        .size(30.dp)
                        .background(palette.buttonColor, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "フォルダ作成",
                        tint = palette.accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Edit / Delete Mode Toggle
                IconButton(
                    onClick = { isEditMode = !isEditMode },
                    modifier = Modifier
                        .size(30.dp)
                        .background(
                            if (isEditMode) Color(0xFFE53935) else palette.buttonColor,
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isEditMode) Icons.Default.Check else Icons.Default.Delete,
                        contentDescription = if (isEditMode) "整理終了" else "整理・削除",
                        tint = if (isEditMode) Color.White else palette.textColor,
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Rescan Button
                IconButton(
                    onClick = onRefreshApps,
                    modifier = Modifier
                        .size(30.dp)
                        .background(palette.buttonColor, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "再スキャン",
                        tint = palette.textColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = "アプリやフォルダ名で検索...",
                    fontSize = 12.sp,
                    color = palette.secondaryTextColor
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "検索",
                    tint = palette.accentColor,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "クリア",
                            tint = palette.secondaryTextColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = palette.buttonColor,
                unfocusedContainerColor = palette.buttonColor,
                focusedBorderColor = palette.accentColor,
                unfocusedBorderColor = palette.cardBorderColor,
                focusedTextColor = palette.textColor,
                unfocusedTextColor = palette.textColor
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        )

        // Edit Mode Information Banner
        if (isEditMode) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0x33E53935),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66E53935)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "アプリ・フォルダの右上の✕で削除・非表示",
                            fontSize = 11.sp,
                            color = palette.textColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "完了",
                        fontSize = 11.sp,
                        color = Color(0xFFE53935),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { isEditMode = false }
                            .padding(4.dp)
                    )
                }
            }
        }

        // --- Content Area ---
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = palette.accentColor,
                    strokeWidth = 2.5.dp
                )
            }
        } else if (filteredApps.isEmpty() && filteredFolders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isEmpty()) "アプリが見つかりません" else "一致するアプリやフォルダがありません",
                    fontSize = 13.sp,
                    color = palette.secondaryTextColor
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // --- Folders Section (if any folders exist) ---
                if (filteredFolders.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📁 フォルダ (${filteredFolders.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textColor
                        )
                    }

                    // Folders Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns.coerceIn(2, 6)),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        items(filteredFolders, key = { it.id }) { folder ->
                            FolderCard(
                                folder = folder,
                                allApps = apps,
                                palette = palette,
                                isEditMode = isEditMode,
                                onClick = {
                                    if (isEditMode) {
                                        folderToDeleteConfirm = folder
                                    } else {
                                        selectedFolderForDetail = folder
                                    }
                                },
                                onDeleteClick = {
                                    folderToDeleteConfirm = folder
                                }
                            )
                        }
                    }

                    HorizontalDivider(
                        color = palette.cardBorderColor,
                        thickness = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }

                // --- Apps Grid ---
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns.coerceIn(2, 6)),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clipToBounds()
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        AppLauncherCard(
                            app = app,
                            palette = palette,
                            isEditMode = isEditMode,
                            onClick = {
                                if (isEditMode) {
                                    appToDeleteConfirm = app
                                } else {
                                    onAppClicked(app)
                                }
                            },
                            onLongClick = {
                                appForActionMenu = app
                            },
                            onDeleteClick = {
                                appToDeleteConfirm = app
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "※ タップで起動、長押しでメニュー（フォルダ追加・削除など）",
            fontSize = 10.sp,
            color = palette.secondaryTextColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    // ==========================================
    // Dialogs
    // ==========================================

    // 1. Folder Detail Dialog (Showing apps inside the folder)
    currentSelectedFolder?.let { folder ->
        val folderApps = remember(folder.packageNames, apps, hiddenPackages) {
            folder.packageNames.mapNotNull { pkg ->
                apps.find { it.packageName == pkg && it.packageName !in hiddenPackages }
            }
        }

        Dialog(
            onDismissRequest = { selectedFolderForDetail = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.containerColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.75f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FolderIconVector(modifier = Modifier.size(24.dp))
                            Text(
                                text = folder.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textColor
                            )
                            Text(
                                text = "(${folderApps.size}個)",
                                fontSize = 12.sp,
                                color = palette.secondaryTextColor
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Edit Folder Button
                            IconButton(
                                onClick = {
                                    folderForEdit = folder
                                },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(palette.buttonColor, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "フォルダ名編集・アプリ管理",
                                    tint = palette.textColor,
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            // Delete Folder Button
                            IconButton(
                                onClick = {
                                    folderToDeleteConfirm = folder
                                },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(palette.buttonColor, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "フォルダ削除",
                                    tint = Color(0xFFE53935),
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            // Close Button
                            IconButton(
                                onClick = { selectedFolderForDetail = null },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(palette.buttonColor, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "閉じる",
                                    tint = palette.textColor,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = palette.cardBorderColor,
                        thickness = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )

                    // Apps inside folder
                    if (folderApps.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "フォルダ内にアプリがありません",
                                    fontSize = 13.sp,
                                    color = palette.secondaryTextColor
                                )
                                Button(
                                    onClick = { folderForEdit = folder },
                                    colors = ButtonDefaults.buttonColors(containerColor = palette.accentColor),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "アプリを追加", fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns.coerceIn(2, 5)),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(folderApps, key = { it.packageName }) { app ->
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    AppLauncherCard(
                                        app = app,
                                        palette = palette,
                                        isEditMode = false,
                                        onClick = {
                                            selectedFolderForDetail = null
                                            onAppClicked(app)
                                        },
                                        onLongClick = {
                                            appForActionMenu = app
                                        },
                                        onDeleteClick = {
                                            onRemoveAppFromFolder(folder.id, app.packageName)
                                        }
                                    )

                                    // Remove from folder button
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(2.dp)
                                            .size(20.dp)
                                            .background(palette.buttonColor, CircleShape)
                                            .border(1.dp, palette.cardBorderColor, CircleShape)
                                            .clickable {
                                                onRemoveAppFromFolder(folder.id, app.packageName)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "フォルダから外す",
                                            tint = palette.secondaryTextColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { folderForEdit = folder },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.accentColor),
                            border = androidx.compose.foundation.BorderStroke(1.dp, palette.accentColor)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "アプリを追加・編集", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // 2. Create or Edit Folder Dialog (Select apps to put into folder)
    if (showCreateFolderDialog || folderForEdit != null) {
        val targetFolder = folderForEdit
        val isCreating = targetFolder == null

        var folderNameInput by remember(targetFolder) {
            mutableStateOf(targetFolder?.name ?: "")
        }
        var selectedPackages by remember(targetFolder) {
            mutableStateOf(targetFolder?.packageNames?.toSet() ?: emptySet())
        }
        var appFilterQuery by remember { mutableStateOf("") }

        val appsForPicker = remember(visibleApps, appFilterQuery) {
            if (appFilterQuery.isBlank()) {
                visibleApps
            } else {
                val q = appFilterQuery.trim().lowercase(Locale.JAPANESE)
                visibleApps.filter { it.label.lowercase(Locale.JAPANESE).contains(q) }
            }
        }

        Dialog(
            onDismissRequest = {
                showCreateFolderDialog = false
                folderForEdit = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.containerColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.82f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = if (isCreating) "📁 新規フォルダ作成" else "📁 フォルダの編集",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Folder Name Input
                    OutlinedTextField(
                        value = folderNameInput,
                        onValueChange = { folderNameInput = it },
                        label = { Text("フォルダ名") },
                        placeholder = { Text("例: ツール、SNS、便利アプリ") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = palette.buttonColor,
                            unfocusedContainerColor = palette.buttonColor,
                            focusedBorderColor = palette.accentColor,
                            unfocusedBorderColor = palette.cardBorderColor,
                            focusedTextColor = palette.textColor,
                            unfocusedTextColor = palette.textColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    )

                    Text(
                        text = "フォルダに入れるアプリを選択 (${selectedPackages.size}個選択中):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.secondaryTextColor,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // Quick Search for App Picker
                    OutlinedTextField(
                        value = appFilterQuery,
                        onValueChange = { appFilterQuery = it },
                        placeholder = { Text("アプリを絞り込み...", fontSize = 11.sp) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = palette.buttonColor,
                            unfocusedContainerColor = palette.buttonColor,
                            focusedBorderColor = palette.accentColor,
                            unfocusedBorderColor = palette.cardBorderColor,
                            focusedTextColor = palette.textColor,
                            unfocusedTextColor = palette.textColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    // Apps Checklist
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .border(1.dp, palette.cardBorderColor, RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(appsForPicker, key = { it.packageName }) { app ->
                            val isChecked = app.packageName in selectedPackages
                            Surface(
                                onClick = {
                                    selectedPackages = if (isChecked) {
                                        selectedPackages - app.packageName
                                    } else {
                                        selectedPackages + app.packageName
                                    }
                                },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isChecked) palette.buttonColor else Color.Transparent,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(6.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (app.iconBitmap != null) {
                                                Image(
                                                    bitmap = app.iconBitmap,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Android,
                                                    contentDescription = null,
                                                    tint = palette.accentColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = app.label,
                                            fontSize = 12.sp,
                                            color = palette.textColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            selectedPackages = if (checked) {
                                                selectedPackages + app.packageName
                                            } else {
                                                selectedPackages - app.packageName
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = palette.accentColor)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dialog Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                showCreateFolderDialog = false
                                folderForEdit = null
                            }
                        ) {
                            Text("キャンセル", color = palette.secondaryTextColor)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val name = folderNameInput.ifBlank { if (isCreating) "新規フォルダ" else targetFolder!!.name }
                                if (isCreating) {
                                    onCreateFolder(name, selectedPackages.toList())
                                } else {
                                    onUpdateFolder(targetFolder!!.id, name, selectedPackages.toList())
                                }
                                showCreateFolderDialog = false
                                folderForEdit = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.accentColor),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (isCreating) "作成" else "保存")
                        }
                    }
                }
            }
        }
    }

    // 3. App Long-Press Action Menu
    appForActionMenu?.let { app ->
        Dialog(
            onDismissRequest = { appForActionMenu = null }
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.containerColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // App Header
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (app.iconBitmap != null) {
                            Image(
                                bitmap = app.iconBitmap,
                                contentDescription = app.label,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Android,
                                contentDescription = app.label,
                                tint = palette.accentColor,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Text(
                        text = app.label,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textColor,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = app.packageName,
                        fontSize = 10.sp,
                        color = palette.secondaryTextColor,
                        textAlign = TextAlign.Center
                    )

                    HorizontalDivider(
                        color = palette.cardBorderColor,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // 1. Launch App
                    Surface(
                        onClick = {
                            appForActionMenu = null
                            onAppClicked(app)
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = palette.buttonColor,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = palette.accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "アプリを起動",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = palette.textColor
                            )
                        }
                    }

                    // 2. Add / Move to Folder
                    Surface(
                        onClick = {
                            val target = app
                            appForActionMenu = null
                            appToAddToFolder = target
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = palette.buttonColor,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FolderIconVector(modifier = Modifier.size(18.dp))
                            Text(
                                text = "📁 フォルダに追加 / 移動",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = palette.textColor
                            )
                        }
                    }

                    // 3. Hide / Delete from Launcher
                    Surface(
                        onClick = {
                            val target = app
                            appForActionMenu = null
                            onHideApp(target.packageName)
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = palette.buttonColor,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "ランチャーから削除（非表示）",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFE53935)
                                )
                                Text(
                                    text = "非表示アプリからいつでも復元できます",
                                    fontSize = 10.sp,
                                    color = palette.secondaryTextColor
                                )
                            }
                        }
                    }

                    // 4. Uninstall from Device
                    Surface(
                        onClick = {
                            val target = app
                            appForActionMenu = null
                            onRequestUninstall(target.packageName)
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = palette.buttonColor,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "端末からアンインストール",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFD32F2F)
                            )
                        }
                    }

                    TextButton(
                        onClick = { appForActionMenu = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("キャンセル", color = palette.secondaryTextColor)
                    }
                }
            }
        }
    }

    // 4. Add App to Folder Dialog (Picker)
    appToAddToFolder?.let { app ->
        Dialog(
            onDismissRequest = { appToAddToFolder = null }
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.containerColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "「${app.label}」をフォルダに追加",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textColor
                    )

                    if (folders.isEmpty()) {
                        Text(
                            text = "現在フォルダがありません。新しいフォルダを作成してアプリを追加しますか？",
                            fontSize = 12.sp,
                            color = palette.secondaryTextColor
                        )
                        Button(
                            onClick = {
                                appToAddToFolder = null
                                showCreateFolderDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.accentColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("新規フォルダを作成")
                        }
                    } else {
                        Text(
                            text = "追加先のフォルダを選択してください:",
                            fontSize = 12.sp,
                            color = palette.secondaryTextColor
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(folders, key = { it.id }) { folder ->
                                val isInFolder = app.packageName in folder.packageNames
                                Surface(
                                    onClick = {
                                        if (isInFolder) {
                                            onRemoveAppFromFolder(folder.id, app.packageName)
                                        } else {
                                            onAddAppToFolder(folder.id, app.packageName)
                                        }
                                        appToAddToFolder = null
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isInFolder) palette.buttonColor else palette.itemBackgroundColor,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            FolderIconVector(modifier = Modifier.size(18.dp))
                                            Text(
                                                text = folder.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = palette.textColor
                                            )
                                        }
                                        if (isInFolder) {
                                            Text(
                                                text = "所属中 (タップで解除)",
                                                fontSize = 11.sp,
                                                color = palette.accentColor
                                            )
                                        } else {
                                            Text(
                                                text = "+ 追加",
                                                fontSize = 11.sp,
                                                color = palette.secondaryTextColor
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                appToAddToFolder = null
                                showCreateFolderDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.buttonColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = palette.accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("新しいフォルダを作成して追加", color = palette.accentColor, fontSize = 12.sp)
                        }
                    }

                    TextButton(
                        onClick = { appToAddToFolder = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("キャンセル", color = palette.secondaryTextColor)
                    }
                }
            }
        }
    }

    // 5. Delete App Confirmation Dialog
    appToDeleteConfirm?.let { app ->
        Dialog(
            onDismissRequest = { appToDeleteConfirm = null }
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.containerColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "アプリの削除・非表示",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textColor
                    )

                    Text(
                        text = "「${app.label}」をどうしますか？\n\n・「ランチャーから削除（非表示）」は、ランチャー一覧から隠します（後からいつでも復元可能）。\n・「端末からアンインストール」は、Android本体からアプリを削除します。",
                        fontSize = 12.sp,
                        color = palette.secondaryTextColor,
                        lineHeight = 17.sp
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                onHideApp(app.packageName)
                                appToDeleteConfirm = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ランチャーから削除（非表示）")
                        }

                        OutlinedButton(
                            onClick = {
                                onRequestUninstall(app.packageName)
                                appToDeleteConfirm = null
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD32F2F)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("端末からアンインストール")
                        }

                        TextButton(
                            onClick = { appToDeleteConfirm = null },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("キャンセル", color = palette.secondaryTextColor)
                        }
                    }
                }
            }
        }
    }

    // 6. Delete Folder Confirmation Dialog
    folderToDeleteConfirm?.let { folder ->
        Dialog(
            onDismissRequest = { folderToDeleteConfirm = null }
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.containerColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "フォルダの削除",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textColor
                    )

                    Text(
                        text = "「${folder.name}」フォルダを削除しますか？\n（※ フォルダを削除しても、中のアプリは消えずランチャーに残ります）",
                        fontSize = 12.sp,
                        color = palette.secondaryTextColor,
                        lineHeight = 17.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { folderToDeleteConfirm = null }
                        ) {
                            Text("キャンセル", color = palette.secondaryTextColor)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onDeleteFolder(folder.id)
                                if (selectedFolderForDetail?.id == folder.id) {
                                    selectedFolderForDetail = null
                                }
                                folderToDeleteConfirm = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("フォルダを削除")
                        }
                    }
                }
            }
        }
    }

    // 7. Hidden Apps Management Dialog (Restore hidden apps)
    if (showHiddenAppsDialog) {
        Dialog(
            onDismissRequest = { showHiddenAppsDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.containerColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.72f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "非表示アプリの管理 (${hiddenApps.size}件)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textColor
                        )
                        IconButton(
                            onClick = { showHiddenAppsDialog = false },
                            modifier = Modifier
                                .size(28.dp)
                                .background(palette.buttonColor, CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "閉じる", tint = palette.textColor, modifier = Modifier.size(15.dp))
                        }
                    }

                    HorizontalDivider(
                        color = palette.cardBorderColor,
                        thickness = 1.dp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    if (hiddenApps.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "非表示にしたアプリはありません",
                                fontSize = 13.sp,
                                color = palette.secondaryTextColor
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(hiddenApps, key = { it.packageName }) { app ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = palette.itemBackgroundColor,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(6.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (app.iconBitmap != null) {
                                                    Image(
                                                        bitmap = app.iconBitmap,
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Android,
                                                        contentDescription = null,
                                                        tint = palette.accentColor,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = app.label,
                                                fontSize = 12.sp,
                                                color = palette.textColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Button(
                                            onClick = { onUnhideApp(app.packageName) },
                                            colors = ButtonDefaults.buttonColors(containerColor = palette.accentColor),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text(text = "再表示", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = onResetHiddenApps
                            ) {
                                Text("すべて再表示", color = palette.accentColor, fontSize = 12.sp)
                            }

                            Button(
                                onClick = { showHiddenAppsDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = palette.buttonColor),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("完了", color = palette.textColor, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// Folder Card Composable
// ==========================================
@Composable
fun FolderCard(
    folder: LauncherFolder,
    allApps: List<AppLauncherItem>,
    palette: ThemePalette,
    isEditMode: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val folderApps = remember(folder.packageNames, allApps) {
        folder.packageNames.mapNotNull { pkg ->
            allApps.find { it.packageName == pkg }
        }.take(4)
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            color = palette.itemBackgroundColor,
            border = androidx.compose.foundation.BorderStroke(
                if (isEditMode) 1.5.dp else 1.dp,
                if (isEditMode) Color(0xFFE53935) else palette.cardBorderColor
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 2x2 Mini Icons Preview or Folder Icon
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.buttonColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (folderApps.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(3.dp),
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                for (i in 0 until 2) {
                                    val item = folderApps.getOrNull(i)
                                    if (item?.iconBitmap != null) {
                                        Image(
                                            bitmap = item.iconBitmap,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(13.dp)
                                                .background(
                                                    if (item != null) palette.accentColor.copy(alpha = 0.5f) else Color.Transparent,
                                                    RoundedCornerShape(2.dp)
                                                )
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                for (i in 2 until 4) {
                                    val item = folderApps.getOrNull(i)
                                    if (item?.iconBitmap != null) {
                                        Image(
                                            bitmap = item.iconBitmap,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(13.dp)
                                                .background(
                                                    if (item != null) palette.accentColor.copy(alpha = 0.5f) else Color.Transparent,
                                                    RoundedCornerShape(2.dp)
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        FolderIconVector(modifier = Modifier.size(24.dp))
                    }
                }

                Text(
                    text = folder.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "${folder.packageNames.size}個のアプリ",
                    fontSize = 9.sp,
                    color = palette.secondaryTextColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Red "✕" Badge in Edit Mode to delete folder
        if (isEditMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(20.dp)
                    .background(Color(0xFFE53935), CircleShape)
                    .border(1.dp, Color.White, CircleShape)
                    .clickable { onDeleteClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "フォルダ削除",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

// ==========================================
// App Launcher Card Composable
// ==========================================
@Composable
fun AppLauncherCard(
    app: AppLauncherItem,
    palette: ThemePalette,
    isEditMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = palette.itemBackgroundColor,
            border = androidx.compose.foundation.BorderStroke(
                if (isEditMode) 1.5.dp else 1.dp,
                if (isEditMode) Color(0xFFE53935) else palette.cardBorderColor
            ),
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(app.packageName, isEditMode) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onLongPress = { onLongClick() }
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (app.iconBitmap != null) {
                        Image(
                            bitmap = app.iconBitmap,
                            contentDescription = app.label,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(palette.buttonColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Android,
                                contentDescription = app.label,
                                tint = palette.accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Text(
                    text = app.label,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Red "✕" Badge in Edit Mode to delete/hide app
        if (isEditMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(20.dp)
                    .background(Color(0xFFE53935), CircleShape)
                    .border(1.dp, Color.White, CircleShape)
                    .clickable { onDeleteClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "削除・非表示",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}


