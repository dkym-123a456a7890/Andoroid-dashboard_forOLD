package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ThemePalette
import com.example.getThemePalette

/**
 * Welcome & Initial Setup Wizard Screen
 * Displayed on first launch or after factory reset.
 */
@Composable
fun WelcomeSetupScreen(
    currentThemeIndex: Int,
    selectedRegion: RegionConfig,
    onThemeSelected: (Int) -> Unit,
    onRegionSelected: (RegionConfig) -> Unit,
    onPostalCodeSubmitted: (String, (Boolean, String) -> Unit) -> Unit,
    onCompleteSetup: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val palette = remember(currentThemeIndex) { getThemePalette(currentThemeIndex) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.backgroundColor)
            .testTag("welcome_setup_screen"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = palette.containerColor),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, palette.cardBorderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier
                .widthIn(max = 700.dp)
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.90f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header: Step Indicator
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = palette.accentColor.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, palette.accentColor)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Dashboard,
                                    contentDescription = "App Icon",
                                    tint = palette.accentColor,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .padding(6.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Smart Dashboard",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textColor
                                )
                                Text(
                                    text = "初期セットアップウィザード",
                                    fontSize = 11.sp,
                                    color = palette.secondaryTextColor
                                )
                            }
                        }

                        // Step badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = palette.itemBackgroundColor,
                            border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor)
                        ) {
                            Text(
                                text = "Step ${currentStep + 1} / 4",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.accentColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Step Progress Dots & Labels
                    StepProgressBar(
                        currentStep = currentStep,
                        totalSteps = 4,
                        palette = palette,
                        onStepClicked = { step ->
                            // Can only navigate to already passed or current step
                            if (step <= currentStep) currentStep = step
                        }
                    )

                    HorizontalDivider(color = palette.cardBorderColor.copy(alpha = 0.6f))
                }

                // Middle: Step Dynamic Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                            } else {
                                (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                            }
                        },
                        label = "SetupStepTransition"
                    ) { step ->
                        when (step) {
                            0 -> StepWelcome(palette = palette)
                            1 -> StepRegionSelection(
                                selectedRegion = selectedRegion,
                                palette = palette,
                                onRegionSelected = onRegionSelected,
                                onPostalCodeSubmitted = onPostalCodeSubmitted
                            )
                            2 -> StepThemeSelection(
                                currentThemeIndex = currentThemeIndex,
                                palette = palette,
                                onThemeSelected = onThemeSelected
                            )
                            3 -> StepAllSet(
                                selectedRegion = selectedRegion,
                                themeIndex = currentThemeIndex,
                                palette = palette
                            )
                        }
                    }
                }

                // Bottom: Navigation Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.textColor),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("setup_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "戻る",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "戻る", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        // Spacer to keep next button aligned to end
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    // Next / Start Dashboard button
                    Button(
                        onClick = {
                            if (currentStep < 3) {
                                currentStep++
                            } else {
                                onCompleteSetup()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.accentColor,
                            contentColor = Color.Black
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                        modifier = Modifier.testTag("setup_next_button")
                    ) {
                        Text(
                            text = if (currentStep < 3) "次へ進む" else "ダッシュボードを開始する",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (currentStep < 3) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Check,
                            contentDescription = "Next",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Visual Progress Bar for Wizard Steps
 */
@Composable
private fun StepProgressBar(
    currentStep: Int,
    totalSteps: Int,
    palette: ThemePalette,
    onStepClicked: (Int) -> Unit
) {
    val stepTitles = listOf("ようこそ", "地域設定", "テーマ選択", "完了")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalSteps) {
            val isCompleted = i < currentStep
            val isCurrent = i == currentStep

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = i <= currentStep) { onStepClicked(i) }
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = when {
                        isCurrent -> palette.accentColor
                        isCompleted -> palette.accentColor.copy(alpha = 0.4f)
                        else -> palette.itemBackgroundColor
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isCurrent || isCompleted) palette.accentColor else palette.cardBorderColor
                    ),
                    modifier = Modifier.size(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(12.dp)
                            )
                        } else {
                            Text(
                                text = "${i + 1}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) Color.Black else palette.secondaryTextColor
                            )
                        }
                    }
                }

                Text(
                    text = stepTitles.getOrElse(i) { "Step ${i + 1}" },
                    fontSize = 11.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) palette.accentColor else if (isCompleted) palette.textColor else palette.secondaryTextColor
                )
            }

            if (i < totalSteps - 1) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .padding(horizontal = 4.dp)
                        .background(
                            if (i < currentStep) palette.accentColor.copy(alpha = 0.6f) else palette.cardBorderColor
                        )
                )
            }
        }
    }
}

/**
 * Step 1: Welcome & Overview
 */
@Composable
private fun StepWelcome(palette: ThemePalette) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Hero badge
        Surface(
            shape = CircleShape,
            color = palette.accentColor.copy(alpha = 0.15f),
            border = androidx.compose.foundation.BorderStroke(2.dp, palette.accentColor.copy(alpha = 0.4f)),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = "Welcome Icon",
                    tint = palette.accentColor,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "スマートダッシュボードへようこそ！",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = "使わなくなった端末を、スタイリッシュな据え置きスマートディスプレイに再活用しましょう。",
                fontSize = 12.sp,
                color = palette.secondaryTextColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Features Grid Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = palette.itemBackgroundColor),
            border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "■ 搭載されている主な機能",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.accentColor
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FeatureItem(
                        icon = Icons.Default.Schedule,
                        title = "大画面時計 & アラーム",
                        description = "秒針・日付・タイマー・ストップウォッチ完備",
                        palette = palette,
                        modifier = Modifier.weight(1f)
                    )
                    FeatureItem(
                        icon = Icons.Default.Cloud,
                        title = "地域詳細天気",
                        description = "気温・降水確率・時間別予報を常時表示",
                        palette = palette,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FeatureItem(
                        icon = Icons.Default.Newspaper,
                        title = "ニュース & トレンド",
                        description = "最新ヘッドラインや急上昇トピックを配信",
                        palette = palette,
                        modifier = Modifier.weight(1f)
                    )
                    FeatureItem(
                        icon = Icons.Default.PhotoLibrary,
                        title = "フォト & ランチャー",
                        description = "思い出のスライドショーやお気に入りアプリ起動",
                        palette = palette,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Tip Box
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = palette.buttonColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "Tip",
                    tint = palette.accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "設定は後からいつでも変更・初期化できます。まずは簡単にお住まいの地域を選んでみましょう！",
                    fontSize = 11.sp,
                    color = palette.secondaryTextColor
                )
            }
        }
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    title: String,
    description: String,
    palette: ThemePalette,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = palette.containerColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = palette.accentColor.copy(alpha = 0.2f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = palette.accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textColor
                )
                Text(
                    text = description,
                    fontSize = 9.5.sp,
                    color = palette.secondaryTextColor,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

/**
 * Step 2: Region & Weather Setting
 */
@Composable
private fun StepRegionSelection(
    selectedRegion: RegionConfig,
    palette: ThemePalette,
    onRegionSelected: (RegionConfig) -> Unit,
    onPostalCodeSubmitted: (String, (Boolean, String) -> Unit) -> Unit
) {
    var postalCodeInput by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResultStatus by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "お住まいの地域を設定",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textColor
            )
            Text(
                text = "天気予報や気温、地域ニュースなどを正確に表示するために使用します。",
                fontSize = 11.5.sp,
                color = palette.secondaryTextColor
            )
        }

        // Current Selected Region Display
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = palette.accentColor.copy(alpha = 0.15f),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, palette.accentColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Selected Location",
                        tint = palette.accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "現在選択中: ${selectedRegion.name}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textColor
                        )
                        Text(
                            text = "${selectedRegion.englishName} (緯度: ${String.format("%.2f", selectedRegion.latitude)}, 経度: ${String.format("%.2f", selectedRegion.longitude)})",
                            fontSize = 10.sp,
                            color = palette.secondaryTextColor
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = palette.accentColor,
                    contentColor = Color.Black
                ) {
                    Text(
                        text = "設定済",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // Postal Code Search Option
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = palette.itemBackgroundColor),
            border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "郵便番号から自動設定 (7桁・ハイフン任意)",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textColor
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
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (postalCodeInput.isNotBlank()) {
                                    focusManager.clearFocus()
                                    isSearching = true
                                    searchResultStatus = null
                                    onPostalCodeSubmitted(postalCodeInput) { success, msg ->
                                        isSearching = false
                                        searchResultStatus = Pair(success, msg)
                                    }
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.accentColor,
                            unfocusedBorderColor = palette.cardBorderColor,
                            focusedTextColor = palette.textColor,
                            unfocusedTextColor = palette.textColor
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    )

                    Button(
                        onClick = {
                            if (postalCodeInput.isNotBlank()) {
                                focusManager.clearFocus()
                                isSearching = true
                                searchResultStatus = null
                                onPostalCodeSubmitted(postalCodeInput) { success, msg ->
                                    isSearching = false
                                    searchResultStatus = Pair(success, msg)
                                }
                            }
                        },
                        enabled = !isSearching && postalCodeInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.accentColor,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(50.dp)
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("検索", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                searchResultStatus?.let { (success, message) ->
                    Text(
                        text = message,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (success) palette.accentColor else Color(0xFFFF6B6B)
                    )
                }
            }
        }

        // Information Tip Box
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = palette.buttonColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = palette.accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "郵便番号を入力すると、自動で該当する市区町村の緯度経度を特定し、天気予報や日の出・日没時間、地域ニュースを正確にお届けします。\n※設定はダッシュボードの設定画面からいつでも変更できます。",
                    fontSize = 11.5.sp,
                    color = palette.secondaryTextColor,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

/**
 * Step 3: Color & Display Theme Selection
 */
@Composable
private fun StepThemeSelection(
    currentThemeIndex: Int,
    palette: ThemePalette,
    onThemeSelected: (Int) -> Unit
) {
    val themeNames = listOf(
        0 to Pair("アビス (深海ダークブルー)", Color(0xFF58A6FF)),
        1 to Pair("エメラルド (ミントグリーン)", Color(0xFF2EA043)),
        2 to Pair("トワイライト (コズミックパープル)", Color(0xFFBC8CFF)),
        3 to Pair("ルビー (クリムゾンレッド)", Color(0xFFFF7B72)),
        4 to Pair("カーボン (アンバーオレンジ)", Color(0xFFFFA657))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "テーマカラーを選択",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textColor
            )
            Text(
                text = "お部屋の雰囲気やお好みに合わせて配色をカスタマイズできます。選択するとリアルタイムでプレビューが反映されます。",
                fontSize = 11.5.sp,
                color = palette.secondaryTextColor
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            themeNames.forEach { (index, data) ->
                val (name, accent) = data
                val isSelected = currentThemeIndex == index
                val themePalette = remember(index) { getThemePalette(index) }

                Surface(
                    onClick = { onThemeSelected(index) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) themePalette.containerColor else palette.itemBackgroundColor,
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) accent else palette.cardBorderColor
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
                            // Color dot with gradient preview
                            Surface(
                                shape = CircleShape,
                                color = accent,
                                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = 0.3f)),
                                modifier = Modifier.size(28.dp)
                            ) {
                                if (isSelected) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = name,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) accent else palette.textColor
                                )
                                Text(
                                    text = if (isSelected) "現在プレビュー中" else "タップして適用",
                                    fontSize = 10.sp,
                                    color = palette.secondaryTextColor
                                )
                            }
                        }

                        // Mini Palette Preview
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(themePalette.backgroundColor)
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(themePalette.containerColor)
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(themePalette.accentColor)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Step 4: All Set & Ready
 */
@Composable
private fun StepAllSet(
    selectedRegion: RegionConfig,
    themeIndex: Int,
    palette: ThemePalette
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Success Icon Badge
        Surface(
            shape = CircleShape,
            color = palette.accentColor.copy(alpha = 0.2f),
            border = androidx.compose.foundation.BorderStroke(2.dp, palette.accentColor),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = palette.accentColor,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "セットアップが完了しました！",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = "これでいつでも快適なスマートダッシュボードをご利用いただけます。",
                fontSize = 12.sp,
                color = palette.secondaryTextColor,
                textAlign = TextAlign.Center
            )
        }

        // Summary Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = palette.itemBackgroundColor),
            border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "■ 設定内容の確認",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.accentColor
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "天気・地域設定:", fontSize = 11.sp, color = palette.secondaryTextColor)
                    Text(text = selectedRegion.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = palette.textColor)
                }

                HorizontalDivider(color = palette.cardBorderColor.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "テーマカラー:", fontSize = 11.sp, color = palette.secondaryTextColor)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(palette.accentColor)
                        )
                        val themeName = when (themeIndex) {
                            0 -> "アビス (ブルー)"
                            1 -> "エメラルド (グリーン)"
                            2 -> "トワイライト (パープル)"
                            3 -> "ルビー (レッド)"
                            4 -> "カーボン (アンバー)"
                            else -> "カスタム"
                        }
                        Text(text = themeName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = palette.textColor)
                    }
                }
            }
        }

        // Tips Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = palette.buttonColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = palette.accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "設定の変更や初期化について",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textColor
                    )
                }
                Text(
                    text = "ダッシュボード右上の歯車アイコン（設定）から、いつでも地域変更、テーマ変更、キオスク/ランチャー設定、および「全データの初期化」を行うことができます。",
                    fontSize = 10.sp,
                    color = palette.secondaryTextColor,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
