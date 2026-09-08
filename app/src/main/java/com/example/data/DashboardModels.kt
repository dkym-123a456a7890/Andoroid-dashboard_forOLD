package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// --- Open-Meteo Weather Models ---
@JsonClass(generateAdapter = true)
data class OpenMeteoResponse(
    @Json(name = "current") val current: CurrentWeather?,
    @Json(name = "daily") val daily: DailyWeather?
)

@JsonClass(generateAdapter = true)
data class CurrentWeather(
    @Json(name = "temperature_2m") val temperature: Double,
    @Json(name = "relative_humidity_2m") val humidity: Double?,
    @Json(name = "apparent_temperature") val apparentTemperature: Double?,
    @Json(name = "is_day") val isDay: Int?,
    @Json(name = "precipitation") val precipitation: Double?,
    @Json(name = "weather_code") val weatherCode: Int,
    @Json(name = "wind_speed_10m") val windSpeed: Double?
)

@JsonClass(generateAdapter = true)
data class DailyWeather(
    @Json(name = "time") val time: List<String>?,
    @Json(name = "weather_code") val weatherCode: List<Int>?,
    @Json(name = "temperature_2m_max") val tempMax: List<Double>?,
    @Json(name = "temperature_2m_min") val tempMin: List<Double>?,
    @Json(name = "precipitation_sum") val precipitationSum: List<Double>?
)

// --- Gemini Content Curation Models ---
@JsonClass(generateAdapter = true)
data class DashboardAiContent(
    @Json(name = "news") val news: List<NewsItem> = emptyList(),
    @Json(name = "trends") val trends: List<TrendItem> = emptyList(),
    @Json(name = "anniversaries") val anniversaries: List<String> = emptyList(),
    @Json(name = "famousBirthdays") val famousBirthdays: List<String> = emptyList(),
    @Json(name = "omihachimanTips") val omihachimanTips: String = "",
    @Json(name = "localNews") val localNews: List<NewsItem> = emptyList()
)

// --- whatistoday.cyou API Models ---
@JsonClass(generateAdapter = true)
data class WhatIsTodayAnnivResponse(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "mmdd") val mmdd: String? = null,
    @Json(name = "anniv1") val anniv1: String? = null,
    @Json(name = "anniv2") val anniv2: String? = null,
    @Json(name = "anniv3") val anniv3: String? = null,
    @Json(name = "anniv4") val anniv4: String? = null,
    @Json(name = "anniv5") val anniv5: String? = null
)

@JsonClass(generateAdapter = true)
data class WhatIsTodayFamousBirthdayResponse(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "mmdd") val mmdd: String? = null,
    @Json(name = "lifespan") val lifespan: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "profile") val profile: String? = null
)

@JsonClass(generateAdapter = true)
data class NewsItem(
    @Json(name = "title") val title: String,
    @Json(name = "category") val category: String,
    @Json(name = "time") val time: String,
    @Json(name = "url") val url: String? = null
)

@JsonClass(generateAdapter = true)
data class TrendItem(
    @Json(name = "keyword") val keyword: String,
    @Json(name = "description") val description: String
)

// --- Gemini Request / Response Models ---
@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "role") val role: String? = null,
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "responseMimeType") val responseMimeType: String? = null,
    @Json(name = "temperature") val temperature: Double? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>?
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent?
)

// --- AI Chat Message Model ---
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: String = java.text.SimpleDateFormat("HH:mm", java.util.Locale.JAPAN).format(java.util.Date()),
    val isError: Boolean = false
)

// --- Postal Code (ZipCloud) Models ---
@JsonClass(generateAdapter = true)
data class ZipCloudResponse(
    @Json(name = "message") val message: String? = null,
    @Json(name = "results") val results: List<ZipCloudResult>? = null,
    @Json(name = "status") val status: Int = 200
)

@JsonClass(generateAdapter = true)
data class ZipCloudResult(
    @Json(name = "address1") val address1: String, // 都道府県 (e.g. 滋賀県)
    @Json(name = "address2") val address2: String, // 市区町村 (e.g. 近江八幡市)
    @Json(name = "address3") val address3: String, // 町域 (e.g. 鷹飼町)
    @Json(name = "kana1") val kana1: String? = null,
    @Json(name = "kana2") val kana2: String? = null,
    @Json(name = "kana3") val kana3: String? = null,
    @Json(name = "prefcode") val prefcode: String? = null,
    @Json(name = "zipcode") val zipcode: String = ""
)

// --- App Launcher Model ---
data class AppLauncherItem(
    val packageName: String,
    val activityName: String,
    val label: String,
    val isSystemApp: Boolean = false,
    val iconBitmap: androidx.compose.ui.graphics.ImageBitmap? = null
)

// --- App Launcher Folder Model ---
data class LauncherFolder(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val packageNames: List<String> = emptyList(),
    val colorHex: String = "#4285F4"
)

// --- Utility Feature Models (Alarm & Timer) ---
@JsonClass(generateAdapter = true)
data class AlarmItem(
    @Json(name = "id") val id: String = java.util.UUID.randomUUID().toString(),
    @Json(name = "hour") val hour: Int,
    @Json(name = "minute") val minute: Int,
    @Json(name = "label") val label: String = "アラーム",
    @Json(name = "isEnabled") val isEnabled: Boolean = true
)

// --- App Software Update Feature Models ---
data class AppReleaseNote(
    val version: String,
    val releaseDate: String,
    val highlights: List<String>,
    val isMajor: Boolean = false
)

data class GitHubReleaseAsset(
    val name: String,
    val size: Long,
    val downloadUrl: String,
    val contentType: String
)

data class GitHubReleaseInfo(
    val tagName: String,
    val version: String,
    val name: String,
    val body: String,
    val publishedAt: String,
    val htmlUrl: String,
    val apkAsset: GitHubReleaseAsset?
)

enum class UpdateCheckStatus {
    IDLE,
    CHECKING,
    UP_TO_DATE,
    UPDATE_AVAILABLE,
    DOWNLOADING,
    READY_TO_INSTALL,
    COMPLETED,
    ERROR
}

data class AppUpdateState(
    val currentVersion: String = "1.0",
    val latestVersion: String = "1.0",
    val status: UpdateCheckStatus = UpdateCheckStatus.IDLE,
    val downloadProgress: Float = 0f,
    val downloadSpeedText: String = "",
    val errorMessage: String? = null,
    val autoCheckEnabled: Boolean = true,
    val autoRefreshIntervalMinutes: Int = 30,
    val releaseNotes: List<AppReleaseNote> = emptyList(),
    // GitHub Releases Integration
    val githubRepo: String = "dkym-123a456a7890/Andoroid-dashboard_forOLD",
    val releaseTitle: String = "",
    val releaseBody: String = "",
    val releaseHtmlUrl: String = "",
    val apkFileName: String = "",
    val apkFileSize: Long = 0L,
    val apkDownloadUrl: String = "",
    val localApkFilePath: String? = null,
    val hasApkInRelease: Boolean = false
)

