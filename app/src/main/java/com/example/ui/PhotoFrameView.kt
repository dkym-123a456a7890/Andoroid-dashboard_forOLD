package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ThemePalette
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

sealed class FramePhoto {
    data class ResourcePhoto(val resId: Int, val title: String, val location: String) : FramePhoto()
    data class UriPhoto(val uri: Uri, val title: String) : FramePhoto()
}

@Composable
fun PhotoFrameView(
    palette: ThemePalette,
    modifier: Modifier = Modifier,
    onToggleFullScreen: (() -> Unit)? = null
) {
    val context = LocalContext.current

    // Preset high-res aesthetic landscape photos
    val defaultPhotos = remember {
        listOf<FramePhoto>(
            FramePhoto.ResourcePhoto(
                resId = R.drawable.img_photo_lake_biwa,
                title = "琵琶湖の朝焼け",
                location = "滋賀県近江八幡・湖畔"
            ),
            FramePhoto.ResourcePhoto(
                resId = R.drawable.img_photo_fuji_sakura,
                title = "富士山と桜爛漫",
                location = "日本の春景"
            ),
            FramePhoto.ResourcePhoto(
                resId = R.drawable.img_photo_autumn_leaves,
                title = "秋の庭園と紅葉",
                location = "古都の夕暮れ"
            )
        )
    }

    var photos by remember { mutableStateOf<List<FramePhoto>>(defaultPhotos) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }
    var slideIntervalSeconds by remember { mutableIntStateOf(10) }
    var showClockOverlay by remember { mutableStateOf(true) }
    var showCaptionOverlay by remember { mutableStateOf(true) }

    // Live clock for overlay
    var overlayTime by remember { mutableStateOf("") }
    var overlayDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.JAPAN)
        val dateFormat = SimpleDateFormat("M月d日 (E)", Locale.JAPAN)
        while (true) {
            val now = Date()
            overlayTime = timeFormat.format(now)
            overlayDate = dateFormat.format(now)
            delay(1000)
        }
    }

    // Photo Picker launcher for adding user photos
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val newItems = uris.mapIndexed { index, uri ->
                FramePhoto.UriPhoto(uri, "マイフォト ${photos.size + index + 1}")
            }
            photos = photos + newItems
            currentIndex = photos.size - newItems.size // Jump to first added photo
        }
    }

    // Automatic Slideshow loop
    LaunchedEffect(isPlaying, photos.size, slideIntervalSeconds, currentIndex) {
        if (isPlaying && photos.size > 1) {
            delay(slideIntervalSeconds * 1000L)
            currentIndex = (currentIndex + 1) % photos.size
        }
    }

    val currentPhoto = photos.getOrNull(currentIndex) ?: photos.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        // Controls Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mode Title / Interval Chip
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = palette.buttonColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderColor)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isPlaying) Color(0xFF00E676) else Color(0xFFFFB74D),
                        modifier = Modifier.size(6.dp)
                    ) {}
                    Text(
                        text = if (isPlaying) "再生中 (${slideIntervalSeconds}秒)" else "一時停止中",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textColor
                    )
                }
            }

            // Quick Actions: Interval Switch, Clock toggle, Add Photo
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interval Switch
                FilledTonalButton(
                    onClick = {
                        slideIntervalSeconds = when (slideIntervalSeconds) {
                            5 -> 10
                            10 -> 30
                            30 -> 60
                            else -> 5
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("${slideIntervalSeconds}秒", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                }

                // Clock overlay toggle
                IconButton(
                    onClick = { showClockOverlay = !showClockOverlay },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = if (showClockOverlay) Icons.Default.Schedule else Icons.Default.TimerOff,
                        contentDescription = "時計表示切替",
                        tint = if (showClockOverlay) palette.accentColor else palette.secondaryTextColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Add Custom Photos button
                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accentColor),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "写真追加",
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                        Text("写真追加", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }

        // Main Display Frame (Takes remaining space)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF10141C)),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, palette.cardBorderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Crossfade animation between photos
                AnimatedContent(
                    targetState = currentPhoto,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(600)) togetherWith fadeOut(animationSpec = tween(600))
                    },
                    modifier = Modifier.fillMaxSize(),
                    label = "photo_fade"
                ) { photo ->
                    when (photo) {
                        is FramePhoto.ResourcePhoto -> {
                            androidx.compose.foundation.Image(
                                painter = painterResource(id = photo.resId),
                                contentDescription = photo.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        is FramePhoto.UriPhoto -> {
                            AsyncImage(
                                model = photo.uri,
                                contentDescription = photo.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        null -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("写真がありません", color = Color.White)
                            }
                        }
                    }
                }

                // Ambient Gradient Shadow (Top & Bottom for text readability)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x77000000),
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color(0x88000000)
                                )
                            )
                        )
                )

                // Smart Display Clock Overlay (Top Left or Bottom Left)
                if (showClockOverlay && overlayTime.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x99000000),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x44FFFFFF)),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = overlayTime,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                            Text(
                                text = overlayDate,
                                fontSize = 11.5.sp,
                                color = Color(0xFFE0E0E0)
                            )
                        }
                    }
                }

                // Photo Caption & Location Overlay (Bottom Left)
                if (showCaptionOverlay && currentPhoto != null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    ) {
                        when (currentPhoto) {
                            is FramePhoto.ResourcePhoto -> {
                                Text(
                                    text = currentPhoto.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = currentPhoto.location,
                                    fontSize = 11.sp,
                                    color = Color(0xFFE0E0E0),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            is FramePhoto.UriPhoto -> {
                                Text(
                                    text = currentPhoto.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Slideshow Play/Pause & Nav Controls (Bottom Right Floating)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xBB000000),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Previous Photo
                        IconButton(
                            onClick = {
                                if (photos.isNotEmpty()) {
                                    currentIndex = if (currentIndex > 0) currentIndex - 1 else photos.size - 1
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "前の写真",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Play / Pause
                        IconButton(
                            onClick = { isPlaying = !isPlaying },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "一時停止" else "再生",
                                tint = palette.accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Next Photo
                        IconButton(
                            onClick = {
                                if (photos.isNotEmpty()) {
                                    currentIndex = (currentIndex + 1) % photos.size
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "次の写真",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Thumbnail Strip along the bottom
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(photos) { index, item ->
                val isSelected = index == currentIndex
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            width = if (isSelected) 2.dp else 0.5.dp,
                            color = if (isSelected) palette.accentColor else Color(0x44FFFFFF),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { currentIndex = index }
                ) {
                    when (item) {
                        is FramePhoto.ResourcePhoto -> {
                            androidx.compose.foundation.Image(
                                painter = painterResource(id = item.resId),
                                contentDescription = item.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        is FramePhoto.UriPhoto -> {
                            AsyncImage(
                                model = item.uri,
                                contentDescription = item.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(palette.accentColor.copy(alpha = 0.25f))
                        )
                    }
                }
            }
        }
    }
}
