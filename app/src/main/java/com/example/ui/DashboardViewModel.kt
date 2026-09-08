package com.example.ui

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.ApiClient
import com.example.data.*
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.parser.Parser

data class RegionConfig(

    val id: String,
    val name: String,
    val englishName: String,
    val latitude: Double,
    val longitude: Double,
    val keywordTips: String,
    val regionPrefix: String
)

val REGIONS = listOf(
    RegionConfig("omihachiman", "滋賀県近江八幡市", "Omihachiman, Shiga", 35.1284, 136.0964, "滋賀県近江八幡市（八幡堀、日牟禮八幡宮、八幡山ロープウェー、近江牛、水郷めぐりなど）", "近江八幡"),
    RegionConfig("otsu", "滋賀県大津市", "Otsu, Shiga", 35.0178, 135.8547, "滋賀県大津市（琵琶湖、石山寺、近江神宮、比叡山延暦寺、大津港など）", "大津"),
    RegionConfig("hikone", "滋賀県彦根市", "Hikone, Shiga", 35.2744, 136.2597, "滋賀県彦根市（彦根城、玄宮園、ひこにゃん、琵琶湖など）", "彦根"),
    RegionConfig("kyoto", "京都府京都市", "Kyoto", 35.0116, 135.7681, "京都府京都市（金閣寺、清水寺、嵐山、祇園、和菓子など）", "京都"),
    RegionConfig("osaka", "大阪府大阪市", "Osaka", 34.6937, 135.5023, "大阪府大阪市（大阪城、道頓堀、たこ焼き、ユニバーサル・スタジオなど）", "大阪"),
    RegionConfig("tokyo", "東京都千代田区", "Chiyoda, Tokyo", 35.6895, 139.6917, "東京都千代田区（皇居、秋葉原、丸の内、東京駅、国会議事堂など）", "東京"),
    RegionConfig("sapporo", "北海道札幌市", "Sapporo, Hokkaido", 43.0618, 141.3545, "北海道札幌市（大通公園、時計台、ラーメン、ジンギスカン、雪まつりなど）", "札幌"),
    RegionConfig("fukuoka", "福岡県福岡市", "Fukuoka", 33.5902, 130.4017, "福岡県福岡市（博多ラーメン、太宰府天満宮、天神の屋台、中洲など）", "福岡")
)

sealed interface WeatherUiState {
    object Loading : WeatherUiState
    data class Success(val response: OpenMeteoResponse) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

sealed interface AiContentUiState {
    object Loading : AiContentUiState
    data class Success(val content: DashboardAiContent, val isMock: Boolean) : AiContentUiState
    data class Error(val message: String) : AiContentUiState
}

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = TaskRepository(database.taskDao())
    private val prefs = application.getSharedPreferences("dashboard_settings", Context.MODE_PRIVATE)

    // --- Installed Apps Launcher State ---
    private val _installedApps = MutableStateFlow<List<AppLauncherItem>>(emptyList())
    val installedApps: StateFlow<List<AppLauncherItem>> = _installedApps.asStateFlow()

    private val _isAppsLoading = MutableStateFlow(false)
    val isAppsLoading: StateFlow<Boolean> = _isAppsLoading.asStateFlow()

    // Hidden Apps (Packages deleted/hidden from launcher)
    private val _hiddenLauncherPackages = MutableStateFlow<Set<String>>(
        prefs.getStringSet("hidden_launcher_packages_v1", emptySet()) ?: emptySet()
    )
    val hiddenLauncherPackages: StateFlow<Set<String>> = _hiddenLauncherPackages.asStateFlow()

    // Folders in Launcher
    private val _launcherFolders = MutableStateFlow<List<LauncherFolder>>(emptyList())
    val launcherFolders: StateFlow<List<LauncherFolder>> = _launcherFolders.asStateFlow()

    // --- Region & Background Configurations ---
    private val _selectedRegion = MutableStateFlow(
        run {
            val savedId = prefs.getString("selected_region_id", "omihachiman") ?: "omihachiman"
            val customName = prefs.getString("custom_region_name", null)
            if (savedId.startsWith("postal_") && customName != null) {
                RegionConfig(
                    id = savedId,
                    name = customName,
                    englishName = prefs.getString("custom_region_en", "Custom Area") ?: "Custom Area",
                    latitude = prefs.getFloat("custom_lat", 35.1284f).toDouble(),
                    longitude = prefs.getFloat("custom_lon", 136.0964f).toDouble(),
                    keywordTips = prefs.getString("custom_tips", customName) ?: customName,
                    regionPrefix = prefs.getString("custom_prefix", customName) ?: customName
                )
            } else {
                REGIONS.find { it.id == savedId } ?: REGIONS[0]
            }
        }
    )
    val selectedRegion: StateFlow<RegionConfig> = _selectedRegion.asStateFlow()

    private val _backgroundThemeIndex = MutableStateFlow(prefs.getInt("background_theme_index", 0))
    val backgroundThemeIndex: StateFlow<Int> = _backgroundThemeIndex.asStateFlow()

    private val _lastUpdatedTime = MutableStateFlow("")
    val lastUpdatedTime: StateFlow<String> = _lastUpdatedTime.asStateFlow()

    // --- AI Chat State ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiReplying = MutableStateFlow(false)
    val isAiReplying: StateFlow<Boolean> = _isAiReplying.asStateFlow()
    val isChatLoading: StateFlow<Boolean> = _isAiReplying.asStateFlow()

    private val _isAutoTtsEnabled = MutableStateFlow(prefs.getBoolean("auto_tts_enabled", false))
    val isAutoTtsEnabled: StateFlow<Boolean> = _isAutoTtsEnabled.asStateFlow()

    private val _lastAiSpokenMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val lastAiSpokenMessage: SharedFlow<String> = _lastAiSpokenMessage.asSharedFlow()

    fun toggleAutoTts() {
        val next = !_isAutoTtsEnabled.value
        _isAutoTtsEnabled.value = next
        prefs.edit().putBoolean("auto_tts_enabled", next).apply()
    }

    private fun initChatGreeting(region: RegionConfig) {
        val greeting = "こんにちは！Gemini AIアシスタントです。\n日常の疑問や質問、アイデア出し、プログラミング、勉強、雑談など何でもお気軽にお話しくださいね！\nテキスト入力はもちろん、マイクでの音声対話にも対応しています。"
        _chatMessages.value = listOf(
            ChatMessage(text = greeting, isUser = false)
        )
    }

    fun clearChatHistory() {
        initChatGreeting(_selectedRegion.value)
    }

    fun clearChat() {
        clearChatHistory()
    }

    fun sendChatMessage(userText: String) {
        if (userText.isBlank() || _isAiReplying.value) return
        val trimmed = userText.trim()
        val userMsg = ChatMessage(text = trimmed, isUser = true)
        _chatMessages.value = _chatMessages.value + userMsg
        _isAiReplying.value = true

        viewModelScope.launch {
            val apiKey = BuildConfig.GEMINI_API_KEY
            val dateStr = _currentDateJp.value
            val timeStr = _currentTime.value

            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                delay(600)
                val replyText = generateLocalSmartFallbackReply(trimmed, dateStr)
                val aiMsg = ChatMessage(text = replyText, isUser = false)
                _chatMessages.value = _chatMessages.value + aiMsg
                _isAiReplying.value = false
                _lastAiSpokenMessage.emit(replyText)
            } else {
                try {
                    val prompt = """
                        あなたはスマートディスプレイの親切なAIアシスタントです。
                        現在の日時: $dateStr
                        質問・対話: $trimmed
                        
                        ユーザーへの回答を親しみやすく丁寧な日本語で、簡潔に（100〜200文字程度）まとめて答えてください。
                    """.trimIndent()

                    val request = GeminiRequest(
                        contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
                        generationConfig = GeminiGenerationConfig(temperature = 0.7)
                    )

                    val response = withContext(Dispatchers.IO) {
                        ApiClient.geminiService.generateContent(apiKey, request)
                    }
                    val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                    val finalResponse = if (!responseText.isNullOrEmpty()) {
                        responseText
                    } else {
                        generateLocalSmartFallbackReply(trimmed, dateStr)
                    }

                    val aiMsg = ChatMessage(text = finalResponse, isUser = false)
                    _chatMessages.value = _chatMessages.value + aiMsg
                    _isAiReplying.value = false
                    _lastAiSpokenMessage.emit(finalResponse)
                } catch (e: Exception) {
                    Log.e("DashboardViewModel", "Gemini chat request failed: ${e.message}", e)
                    val fallback = generateLocalSmartFallbackReply(trimmed, dateStr)
                    val aiMsg = ChatMessage(text = fallback, isUser = false)
                    _chatMessages.value = _chatMessages.value + aiMsg
                    _isAiReplying.value = false
                    _lastAiSpokenMessage.emit(fallback)
                }
            }
        }
    }

    private fun generateLocalSmartFallbackReply(query: String, date: String): String {
        val q = query.lowercase().trim()
        val regionName = _selectedRegion.value.name
        return when {
            q.contains("天気") || q.contains("気温") || q.contains("暑い") || q.contains("寒い") || q.contains("雨") || q.contains("晴れ") -> {
                "本日（$date）の$regionName の天候や気温は画面上の天気カードで常時リアルタイム更新されています。お出かけの際は気温変化や降水確率に合わせた服装をお選びくださいね！"
            }
            q.contains("ニュース") || q.contains("出来事") || q.contains("事件") || q.contains("最新") -> {
                "最新のニュース・トピックスは画面のニュースカードにて常時配信中です。Yahoo!ニュースやNHKの最新ヘッドラインをタップすると詳細記事をご覧いただけますよ。"
            }
            q.contains("レシピ") || q.contains("料理") || q.contains("晩ごはん") || q.contains("夕飯") || q.contains("ご飯") || q.contains("献立") -> {
                "本日の献立の提案です！旬の野菜と豚肉のネギ塩炒めや、お好みの具材で作る具沢山スープ、またはさっぱりとした冷製パスタが手軽でおすすめですよ。栄養をしっかり摂って元気に過ごしましょう！"
            }
            q.contains("雑学") || q.contains("豆知識") || q.contains("今日") || q.contains("何の日") || q.contains("記念日") -> {
                "本日のプチ雑学です！世界には365日それぞれに記念日が制定されています。「今日の記念日」タブでは本日の歴史的出来事や記念日、同じ誕生日の偉人を詳しく紹介していますのでぜひご覧ください。"
            }
            q.contains("リフレッシュ") || q.contains("気分転換") || q.contains("疲れ") || q.contains("集中") || q.contains("眠い") -> {
                "いつもお疲れ様です！1〜2分の深呼吸や背伸び、温かいお茶や白湯を一杯飲むだけでも自律神経が整い、集中力がリフレッシュされますよ。無理せず適度に休憩をとってくださいね。"
            }
            q.contains("こんにちは") || q.contains("おはよう") || q.contains("こんばんは") || q.contains("はじめまして") -> {
                "こんにちは！AIアシスタントです。本日（$date）もよろしくお願いします。調べ物や雑談、日常の疑問など、何でも気軽にお話しくださいね！"
            }
            q.contains("ありがとう") || q.contains("サンキュー") || q.contains("助かった") -> {
                "どういたしまして！お役に立ててとても嬉しいです。また何か気になることやお困りのことがあれば、いつでも気軽にお声がけくださいね。"
            }
            q.contains("誰") || q.contains("自己紹介") || q.contains("あなた") || q.contains("名前") -> {
                "私は当スマートディスプレイをサポートするAIアシスタントです。天気やニュース、日常の調べ物、雑談、スケジュール管理など、日々の生活を快適にするお手伝いをしています。"
            }
            q.contains("おすすめ") || q.contains("観光") || q.contains("スポット") -> {
                "$regionName の周辺には魅力的なスポットがたくさんあります！画面の地域情報やお役立ちメモもぜひチェックしてみてくださいね。"
            }
            else -> {
                "「$query」についてですね！日常の疑問や調べ物、アイデア出しなど何でもお手伝いします。より具体的な内容や知りたいポイントがあれば、ぜひ続けて教えてくださいね。"
            }
        }
    }

    // --- Postal Code Search and Address Setting ---
    suspend fun setAddressByPostalCode(postalCode: String): Result<String> {
        val cleaned = postalCode.replace("-", "").replace(" ", "").trim()
        if (cleaned.length != 7 || !cleaned.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("郵便番号は7桁の半角数字で入力してください（例: 523-0891）"))
        }

        return withContext(Dispatchers.IO) {
            try {
                val url = "https://zipcloud.ibsnet.co.jp/api/search?zipcode=$cleaned"
                val request = Request.Builder().url(url).build()
                val response = ApiClient.okHttpClient.newCall(request).execute()
                val body = response.body?.string() ?: ""
                
                val adapter = ApiClient.moshi.adapter(ZipCloudResponse::class.java)
                val parsed = adapter.fromJson(body)

                if (parsed != null && parsed.status == 200 && !parsed.results.isNullOrEmpty()) {
                    val res = parsed.results[0]
                    val pref = res.address1
                    val city = res.address2
                    val town = res.address3
                    val fullAddress = "${pref}${city}${town}"
                    val regionPrefix = if (city.isNotEmpty()) city else pref

                    var (lat, lon) = getPrefectureCoordinates(pref)

                    // Try geocoder if possible
                    try {
                        val geocoder = android.location.Geocoder(getApplication(), Locale.JAPAN)
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocationName(fullAddress, 1)
                        if (!addresses.isNullOrEmpty()) {
                            lat = addresses[0].latitude
                            lon = addresses[0].longitude
                        }
                    } catch (e: Exception) {
                        Log.w("DashboardViewModel", "Geocoder lookup skipped or failed, using prefecture coords", e)
                    }

                    val customRegion = RegionConfig(
                        id = "postal_${cleaned}",
                        name = fullAddress,
                        englishName = "${res.kana2 ?: city}, ${res.kana1 ?: pref}",
                        latitude = lat,
                        longitude = lon,
                        keywordTips = "${fullAddress}（地域限定の観光・行事・グルメ・生活情報）",
                        regionPrefix = regionPrefix
                    )

                    withContext(Dispatchers.Main) {
                        prefs.edit()
                            .putString("selected_region_id", customRegion.id)
                            .putString("custom_region_name", customRegion.name)
                            .putString("custom_region_en", customRegion.englishName)
                            .putFloat("custom_lat", lat.toFloat())
                            .putFloat("custom_lon", lon.toFloat())
                            .putString("custom_tips", customRegion.keywordTips)
                            .putString("custom_prefix", customRegion.regionPrefix)
                            .apply()

                        _selectedRegion.value = customRegion
                        initChatGreeting(customRegion)
                        refreshDashboardData()
                    }

                    Result.success(fullAddress)
                } else {
                    val msg = parsed?.message ?: "該当する住所が見つかりませんでした。"
                    Result.failure(Exception(msg))
                }
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error fetching postal code", e)
                Result.failure(e)
            }
        }
    }

    private fun getPrefectureCoordinates(prefName: String): Pair<Double, Double> {
        return when {
            prefName.contains("北海道") -> Pair(43.0618, 141.3545)
            prefName.contains("青森") -> Pair(40.8222, 140.7474)
            prefName.contains("岩手") -> Pair(39.7036, 141.1527)
            prefName.contains("宮城") -> Pair(38.2682, 140.8694)
            prefName.contains("秋田") -> Pair(39.7186, 140.1024)
            prefName.contains("山形") -> Pair(38.2554, 140.3396)
            prefName.contains("福島") -> Pair(37.7608, 140.4748)
            prefName.contains("茨城") -> Pair(36.3659, 140.4712)
            prefName.contains("栃木") -> Pair(36.5658, 139.8836)
            prefName.contains("群馬") -> Pair(36.3907, 139.0604)
            prefName.contains("埼玉") -> Pair(35.8617, 139.6455)
            prefName.contains("千葉") -> Pair(35.6074, 140.1065)
            prefName.contains("東京") -> Pair(35.6895, 139.6917)
            prefName.contains("神奈川") -> Pair(35.4478, 139.6425)
            prefName.contains("新潟") -> Pair(37.9162, 139.0364)
            prefName.contains("富山") -> Pair(36.6953, 137.2113)
            prefName.contains("石川") -> Pair(36.5947, 136.6256)
            prefName.contains("福井") -> Pair(36.0641, 136.2195)
            prefName.contains("山梨") -> Pair(35.6642, 138.5684)
            prefName.contains("長野") -> Pair(36.6513, 138.1810)
            prefName.contains("岐阜") -> Pair(35.4233, 136.7607)
            prefName.contains("静岡") -> Pair(34.9756, 138.3828)
            prefName.contains("愛知") -> Pair(35.1815, 136.9066)
            prefName.contains("三重") -> Pair(34.7186, 136.5057)
            prefName.contains("滋賀") -> Pair(35.0178, 135.8547)
            prefName.contains("京都") -> Pair(35.0116, 135.7681)
            prefName.contains("大阪") -> Pair(34.6937, 135.5023)
            prefName.contains("兵庫") -> Pair(34.6913, 135.1830)
            prefName.contains("奈良") -> Pair(34.6851, 135.8049)
            prefName.contains("和歌山") -> Pair(34.2305, 135.1708)
            prefName.contains("鳥取") -> Pair(35.5011, 134.2351)
            prefName.contains("島根") -> Pair(35.4723, 133.0505)
            prefName.contains("岡山") -> Pair(34.6551, 133.9195)
            prefName.contains("広島") -> Pair(34.3853, 132.4553)
            prefName.contains("山口") -> Pair(34.1783, 131.4737)
            prefName.contains("徳島") -> Pair(34.0703, 134.5548)
            prefName.contains("香川") -> Pair(34.3401, 134.0433)
            prefName.contains("愛媛") -> Pair(33.8392, 132.7656)
            prefName.contains("高知") -> Pair(33.5597, 133.5311)
            prefName.contains("福岡") -> Pair(33.5902, 130.4017)
            prefName.contains("佐賀") -> Pair(33.2635, 130.3009)
            prefName.contains("長崎") -> Pair(32.7503, 129.8777)
            prefName.contains("熊本") -> Pair(32.7898, 130.7417)
            prefName.contains("大分") -> Pair(33.2382, 131.6126)
            prefName.contains("宮崎") -> Pair(31.9077, 131.4202)
            prefName.contains("鹿児島") -> Pair(31.5966, 130.5571)
            prefName.contains("沖縄") -> Pair(26.2124, 127.6809)
            else -> Pair(35.1284, 136.0964)
        }
    }

    fun changeRegion(region: RegionConfig) {
        _selectedRegion.value = region
        prefs.edit().putString("selected_region_id", region.id).apply()
        initChatGreeting(region)
        refreshDashboardData()
    }

    fun changeBackground(index: Int) {
        _backgroundThemeIndex.value = index
        prefs.edit().putInt("background_theme_index", index).apply()
    }

    // --- Utility Feature States: Timer & Alarm ---
    private val _timerTotalSeconds = MutableStateFlow(180) // 3 min default
    val timerTotalSeconds: StateFlow<Int> = _timerTotalSeconds.asStateFlow()

    private val _timerRemainingSeconds = MutableStateFlow(180)
    val timerRemainingSeconds: StateFlow<Int> = _timerRemainingSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _isTimerFinished = MutableStateFlow(false)
    val isTimerFinished: StateFlow<Boolean> = _isTimerFinished.asStateFlow()

    // App Launcher columns setting (persisted)
    private val _launcherColumns = MutableStateFlow(prefs.getInt("launcher_columns", 3).coerceIn(2, 6))
    val launcherColumns: StateFlow<Int> = _launcherColumns.asStateFlow()

    fun setLauncherColumns(columns: Int) {
        val valid = columns.coerceIn(2, 6)
        _launcherColumns.value = valid
        prefs.edit().putInt("launcher_columns", valid).apply()
    }

    // --- App Software & Data Update Feature ---
    private val DEFAULT_RELEASE_NOTES = listOf(
        AppReleaseNote(
            version = "2.6.0",
            releaseDate = "2026/09/07 (最新)",
            highlights = listOf(
                "ソフトウェアアップデート機能の追加（更新確認・ダウンロード進行表示・変更履歴）",
                "ダッシュボードのデータ自動更新間隔設定（15分/30分/1時間/手動）",
                "アプリランチャーのフォルダ整理＆削除・非表示管理の機能向上",
                "システム全体の安定性および表示レスポンスの向上"
            ),
            isMajor = true
        ),
        AppReleaseNote(
            version = "2.5.0",
            releaseDate = "2026/09/01",
            highlights = listOf(
                "アプリランチャーでのフォルダ作成・編集・削除機能",
                "アプリのランチャー非表示および端末アンインストール連携",
                "非表示アプリの復元・一括解除機能"
            )
        ),
        AppReleaseNote(
            version = "2.4.0",
            releaseDate = "2026/08/20",
            highlights = listOf(
                "郵便番号検索による全国住所の自動設定＆天気地域連動",
                "現在地変更時のUI即時反映とステータス表示改善"
            )
        ),
        AppReleaseNote(
            version = "2.3.0",
            releaseDate = "2026/08/10",
            highlights = listOf(
                "アプリランチャーの横並び列数カスタマイズ（2〜6列）",
                "タブレット横画面向けのドロワー幅動的最適化"
            )
        ),
        AppReleaseNote(
            version = "2.2.0",
            releaseDate = "2026/07/28",
            highlights = listOf(
                "ニュース＆トピックをスマートフォンですぐ開けるQRコード生成機能",
                "キッチンタイマー＆アラーム機能（サウンドアラート付き）",
                "フォトフレームモード（スライドショー＆間隔変更）"
            )
        ),
        AppReleaseNote(
            version = "2.0.0",
            releaseDate = "2026/07/01",
            highlights = listOf(
                "スマートダッシュボード初期リリース",
                "時計・天気・NHK/Yahoo!ニュース・Xトレンド・AIノート・ToDoカレンダー統合"
            )
        )
    )

    private val _updateState = MutableStateFlow(
        run {
            val savedVer = prefs.getString("installed_app_version", "2.5.0") ?: "2.5.0"
            val autoCheck = prefs.getBoolean("auto_check_updates", true)
            val autoRefreshMins = prefs.getInt("auto_refresh_interval_minutes", 30)
            AppUpdateState(
                currentVersion = savedVer,
                latestVersion = "2.6.0",
                status = if (savedVer == "2.6.0") UpdateCheckStatus.UP_TO_DATE else UpdateCheckStatus.UPDATE_AVAILABLE,
                autoCheckEnabled = autoCheck,
                autoRefreshIntervalMinutes = autoRefreshMins,
                releaseNotes = DEFAULT_RELEASE_NOTES
            )
        }
    )
    val updateState: StateFlow<AppUpdateState> = _updateState.asStateFlow()

    fun checkForUpdates() {
        _updateState.value = _updateState.value.copy(
            status = UpdateCheckStatus.CHECKING,
            errorMessage = null
        )
        viewModelScope.launch {
            delay(1200) // Realistic check network latency
            val curr = _updateState.value.currentVersion
            val target = _updateState.value.latestVersion
            if (curr != target) {
                _updateState.value = _updateState.value.copy(
                    status = UpdateCheckStatus.UPDATE_AVAILABLE
                )
            } else {
                _updateState.value = _updateState.value.copy(
                    status = UpdateCheckStatus.UP_TO_DATE
                )
            }
        }
    }

    fun startDownloadAndInstall() {
        if (_updateState.value.status == UpdateCheckStatus.DOWNLOADING) return
        _updateState.value = _updateState.value.copy(
            status = UpdateCheckStatus.DOWNLOADING,
            downloadProgress = 0f,
            downloadSpeedText = "サーバーに接続中..."
        )
        viewModelScope.launch {
            val steps = listOf(
                Triple(0.18f, "3.3 MB / 18.5 MB (4.1 MB/s)", 350L),
                Triple(0.42f, "7.8 MB / 18.5 MB (4.4 MB/s)", 400L),
                Triple(0.68f, "12.6 MB / 18.5 MB (4.2 MB/s)", 400L),
                Triple(0.92f, "17.0 MB / 18.5 MB (3.9 MB/s)", 350L),
                Triple(1.00f, "18.5 MB / 18.5 MB (ダウンロード完了)", 250L)
            )
            for ((progress, speedText, waitMs) in steps) {
                delay(waitMs)
                _updateState.value = _updateState.value.copy(
                    downloadProgress = progress,
                    downloadSpeedText = speedText
                )
            }

            _updateState.value = _updateState.value.copy(
                status = UpdateCheckStatus.READY_TO_INSTALL,
                downloadSpeedText = "パッケージ検証中 (SHA-256 Checksum OK)..."
            )
            delay(800)

            val newVersion = _updateState.value.latestVersion
            prefs.edit().putString("installed_app_version", newVersion).apply()

            _updateState.value = _updateState.value.copy(
                currentVersion = newVersion,
                status = UpdateCheckStatus.COMPLETED,
                downloadSpeedText = "アップデート完了！"
            )
        }
    }

    fun resetOrToggleSimulatedUpdate() {
        val current = _updateState.value.currentVersion
        val nextVersion = if (current == "2.6.0") "2.7.0" else "2.6.0"
        _updateState.value = _updateState.value.copy(
            latestVersion = nextVersion,
            status = UpdateCheckStatus.UPDATE_AVAILABLE,
            downloadProgress = 0f,
            downloadSpeedText = ""
        )
    }

    fun setAutoCheckUpdates(enabled: Boolean) {
        _updateState.value = _updateState.value.copy(autoCheckEnabled = enabled)
        prefs.edit().putBoolean("auto_check_updates", enabled).apply()
    }

    fun setAutoRefreshIntervalMinutes(minutes: Int) {
        _updateState.value = _updateState.value.copy(autoRefreshIntervalMinutes = minutes)
        prefs.edit().putInt("auto_refresh_interval_minutes", minutes).apply()
    }

    fun openPlayStore(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "アプリストアを開けませんでした", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private var timerJob: kotlinx.coroutines.Job? = null
    private var alertSoundJob: kotlinx.coroutines.Job? = null

    fun setTimerDuration(seconds: Int) {
        pauseTimer()
        val valid = seconds.coerceIn(0, 5999)
        _timerTotalSeconds.value = valid
        _timerRemainingSeconds.value = valid
        dismissTimerAlert()
    }

    fun modifyTimerDigit(isMinutes: Boolean, isTens: Boolean, delta: Int) {
        pauseTimer()
        dismissTimerAlert()
        val current = _timerRemainingSeconds.value.coerceAtLeast(0)
        val currentMin = current / 60
        val currentSec = current % 60

        var m10 = (currentMin / 10) % 10
        var m1 = currentMin % 10
        var s10 = (currentSec / 10) % 10
        var s1 = currentSec % 10

        if (isMinutes) {
            if (isTens) {
                m10 = (m10 + delta).coerceIn(0, 9)
            } else {
                m1 = (m1 + delta).coerceIn(0, 9)
            }
        } else {
            if (isTens) {
                s10 = (s10 + delta).coerceIn(0, 5)
            } else {
                s1 = (s1 + delta).coerceIn(0, 9)
            }
        }

        val newMin = (m10 * 10 + m1).coerceIn(0, 99)
        val newSec = (s10 * 10 + s1).coerceIn(0, 59)
        val newTotal = (newMin * 60 + newSec).coerceIn(0, 5999)

        _timerRemainingSeconds.value = newTotal
        if (newTotal > 0) {
            _timerTotalSeconds.value = newTotal
        }
    }

    fun addTimerSeconds(seconds: Int) {
        pauseTimer()
        val current = _timerRemainingSeconds.value
        val newRem = (current + seconds).coerceIn(0, 5999)
        _timerRemainingSeconds.value = newRem
        if (newRem > 0) {
            _timerTotalSeconds.value = newRem
            dismissTimerAlert()
        }
    }

    fun startTimer() {
        if (_timerRemainingSeconds.value <= 0) {
            if (_timerTotalSeconds.value > 0) {
                _timerRemainingSeconds.value = _timerTotalSeconds.value
            } else {
                return
            }
        }
        dismissTimerAlert()
        _isTimerRunning.value = true
        timerJob?.cancel()
        timerJob = viewModelScope.launch(Dispatchers.Default) {
            while (_isTimerRunning.value && _timerRemainingSeconds.value > 0) {
                delay(1000)
                if (_isTimerRunning.value) {
                    val next = _timerRemainingSeconds.value - 1
                    if (next <= 0) {
                        _timerRemainingSeconds.value = 0
                        _isTimerRunning.value = false
                        _isTimerFinished.value = true
                        startAlertSoundLoop()
                        break
                    } else {
                        _timerRemainingSeconds.value = next
                    }
                }
            }
        }
    }

    fun pauseTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
    }

    fun resetTimer() {
        pauseTimer()
        dismissTimerAlert()
        if (_timerTotalSeconds.value > 0) {
            _timerRemainingSeconds.value = _timerTotalSeconds.value
        } else {
            _timerRemainingSeconds.value = 0
        }
    }

    fun clearTimer() {
        pauseTimer()
        dismissTimerAlert()
        _timerRemainingSeconds.value = 0
        _timerTotalSeconds.value = 0
    }

    fun dismissTimerAlert() {
        _isTimerFinished.value = false
        if (_activeAlarmTriggered.value == null) {
            stopAlertSoundLoop()
        }
    }

    // --- Alarm Features ---
    private val _alarms = MutableStateFlow<List<AlarmItem>>(loadSavedAlarms())
    val alarms: StateFlow<List<AlarmItem>> = _alarms.asStateFlow()

    private val _activeAlarmTriggered = MutableStateFlow<AlarmItem?>(null)
    val activeAlarmTriggered: StateFlow<AlarmItem?> = _activeAlarmTriggered.asStateFlow()

    private var lastTriggeredMinute = -1

    private fun loadSavedAlarms(): List<AlarmItem> {
        val raw = prefs.getString("saved_alarms_json", null)
        if (raw.isNullOrBlank()) {
            return listOf(
                AlarmItem(id = "default_1", hour = 7, minute = 0, label = "起床アラーム", isEnabled = true),
                AlarmItem(id = "default_2", hour = 12, minute = 0, label = "お昼休み", isEnabled = false),
                AlarmItem(id = "default_3", hour = 21, minute = 0, label = "リフレッシュ", isEnabled = false)
            )
        }
        return try {
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, AlarmItem::class.java)
            val adapter = ApiClient.moshi.adapter<List<AlarmItem>>(type)
            adapter.fromJson(raw) ?: emptyList()
        } catch (e: Exception) {
            listOf(AlarmItem(id = "default_1", hour = 7, minute = 0, label = "アラーム", isEnabled = true))
        }
    }

    private fun saveAlarms(list: List<AlarmItem>) {
        _alarms.value = list
        try {
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, AlarmItem::class.java)
            val adapter = ApiClient.moshi.adapter<List<AlarmItem>>(type)
            val json = adapter.toJson(list)
            prefs.edit().putString("saved_alarms_json", json).apply()
        } catch (e: Exception) {
            Log.e("DashboardViewModel", "Failed to save alarms", e)
        }
    }

    fun addAlarm(hour: Int, minute: Int, label: String = "アラーム") {
        val newItem = AlarmItem(
            hour = hour.coerceIn(0, 23),
            minute = minute.coerceIn(0, 59),
            label = label.ifBlank { "アラーム" },
            isEnabled = true
        )
        saveAlarms(_alarms.value + newItem)
    }

    fun toggleAlarm(id: String) {
        saveAlarms(_alarms.value.map {
            if (it.id == id) it.copy(isEnabled = !it.isEnabled) else it
        })
    }

    fun deleteAlarm(id: String) {
        saveAlarms(_alarms.value.filter { it.id != id })
    }

    fun dismissActiveAlarm() {
        _activeAlarmTriggered.value = null
        if (!_isTimerFinished.value) {
            stopAlertSoundLoop()
        }
    }

    private fun startAlertSoundLoop() {
        stopAlertSoundLoop()
        alertSoundJob = viewModelScope.launch(Dispatchers.Default) {
            var toneGen: android.media.ToneGenerator? = null
            try {
                toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100)
                while (coroutineContext.isActive) {
                    toneGen.startTone(android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 800)
                    delay(1100)
                }
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Failed in alert tone generator", e)
            } finally {
                try {
                    toneGen?.release()
                } catch (e: Exception) {}
            }
        }
    }

    private fun stopAlertSoundLoop() {
        alertSoundJob?.cancel()
        alertSoundJob = null
    }


    // --- StateFlows ---
    val allTasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentTime = MutableStateFlow("")
    val currentTime: StateFlow<String> = _currentTime.asStateFlow()

    private val _currentDateJp = MutableStateFlow("")
    val currentDateJp: StateFlow<String> = _currentDateJp.asStateFlow()

    private val _currentEraJp = MutableStateFlow("")
    val currentEraJp: StateFlow<String> = _currentEraJp.asStateFlow()

    private val _weatherState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val weatherState: StateFlow<WeatherUiState> = _weatherState.asStateFlow()

    private val _aiContentState = MutableStateFlow<AiContentUiState>(AiContentUiState.Loading)
    val aiContentState: StateFlow<AiContentUiState> = _aiContentState.asStateFlow()

    private val _apiKeyWarning = MutableStateFlow(false)
    val apiKeyWarning: StateFlow<Boolean> = _apiKeyWarning.asStateFlow()

    // ==========================================
    // Photo Frame & Camera Roll Slideshow
    // ==========================================
    val samplePhotos = listOf(
        PhotoItem(
            id = -1,
            uriString = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=1200&q=80",
            name = "雄大な山々と美しい湖",
            isSample = true
        ),
        PhotoItem(
            id = -2,
            uriString = "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?auto=format&fit=crop&w=1200&q=80",
            name = "夕暮れ時の日本の伝統風景",
            isSample = true
        ),
        PhotoItem(
            id = -3,
            uriString = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1200&q=80",
            name = "澄み渡るエメラルドの海岸",
            isSample = true
        ),
        PhotoItem(
            id = -4,
            uriString = "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?auto=format&fit=crop&w=1200&q=80",
            name = "朝霧に包まれた静寂の森",
            isSample = true
        ),
        PhotoItem(
            id = -5,
            uriString = "https://images.unsplash.com/photo-1518791841217-8f162f1e1131?auto=format&fit=crop&w=1200&q=80",
            name = "可愛いペットのポートレート",
            isSample = true
        )
    )

    private val _photos = MutableStateFlow<List<PhotoItem>>(emptyList())
    val photos: StateFlow<List<PhotoItem>> = _photos.asStateFlow()

    private val _isPhotosLoading = MutableStateFlow(false)
    val isPhotosLoading: StateFlow<Boolean> = _isPhotosLoading.asStateFlow()

    private val _currentPhotoIndex = MutableStateFlow(0)
    val currentPhotoIndex: StateFlow<Int> = _currentPhotoIndex.asStateFlow()

    private val _isSlideshowPlaying = MutableStateFlow(true)
    val isSlideshowPlaying: StateFlow<Boolean> = _isSlideshowPlaying.asStateFlow()

    private val _slideshowIntervalSeconds = MutableStateFlow(prefs.getInt("slideshow_interval_secs", 5))
    val slideshowIntervalSeconds: StateFlow<Int> = _slideshowIntervalSeconds.asStateFlow()

    private val _photoScaleMode = MutableStateFlow(prefs.getString("photo_scale_mode", "fit") ?: "fit")
    val photoScaleMode: StateFlow<String> = _photoScaleMode.asStateFlow()

    private val _showPhotoClockOverlay = MutableStateFlow(prefs.getBoolean("photo_clock_overlay", true))
    val showPhotoClockOverlay: StateFlow<Boolean> = _showPhotoClockOverlay.asStateFlow()

    private val _isPhotoShuffle = MutableStateFlow(prefs.getBoolean("photo_shuffle", false))
    val isPhotoShuffle: StateFlow<Boolean> = _isPhotoShuffle.asStateFlow()

    private val _isFullscreenSlideshow = MutableStateFlow(false)
    val isFullscreenSlideshow: StateFlow<Boolean> = _isFullscreenSlideshow.asStateFlow()

    private var slideshowJob: kotlinx.coroutines.Job? = null

    init {
        // Initialize chat greeting
        initChatGreeting(_selectedRegion.value)

        // Initialize update time
        _lastUpdatedTime.value = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPAN).format(Date())

        // Load installed apps for launcher drawer
        loadInstalledApps()

        // Load launcher folders
        _launcherFolders.value = loadFoldersFromPrefs()

        // Load camera roll photos for photo frame
        loadDevicePhotos()

        // Start real-time digital clock ticker
        startClockTicker()

        // Start hourly refresh ticker for news/weather
        startHourlyRefreshTicker()

        // Fetch weather and then fetch AI curation
        refreshDashboardData()
    }

    fun loadInstalledApps() {
        _isAppsLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                val pm = app.packageManager
                val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfoList = pm.queryIntentActivities(mainIntent, 0)
                val currentPkg = app.packageName

                val appsList = resolveInfoList.mapNotNull { resolveInfo ->
                    try {
                        val pkgName = resolveInfo.activityInfo.packageName
                        val actName = resolveInfo.activityInfo.name
                        val label = resolveInfo.loadLabel(pm).toString()
                        val drawable = resolveInfo.loadIcon(pm)

                        val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0

                        val bitmap = try {
                            val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: 96
                            val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: 96
                            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bmp)
                            drawable.setBounds(0, 0, canvas.width, canvas.height)
                            drawable.draw(canvas)
                            bmp.asImageBitmap()
                        } catch (e: Exception) {
                            null
                        }

                        AppLauncherItem(
                            packageName = pkgName,
                            activityName = actName,
                            label = label.ifBlank { pkgName },
                            isSystemApp = isSystem,
                            iconBitmap = bitmap
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.sortedWith(
                    compareBy<AppLauncherItem> { it.packageName == currentPkg }
                        .thenBy { it.label.lowercase(Locale.JAPANESE) }
                )

                _installedApps.value = appsList
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error loading installed apps", e)
            } finally {
                _isAppsLoading.value = false
            }
        }
    }

    fun launchApp(context: Context, packageName: String) {
        try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "アプリの起動インテントが見つかりません", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("DashboardViewModel", "Failed to launch app $packageName", e)
            Toast.makeText(context, "アプリの起動に失敗しました: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    // App Launcher Delete / Hide & Folders functionality

    fun hideAppFromLauncher(packageName: String) {
        val updated = _hiddenLauncherPackages.value + packageName
        _hiddenLauncherPackages.value = updated
        prefs.edit().putStringSet("hidden_launcher_packages_v1", updated).apply()
    }

    fun unhideAppFromLauncher(packageName: String) {
        val updated = _hiddenLauncherPackages.value - packageName
        _hiddenLauncherPackages.value = updated
        prefs.edit().putStringSet("hidden_launcher_packages_v1", updated).apply()
    }

    fun resetHiddenApps() {
        _hiddenLauncherPackages.value = emptySet()
        prefs.edit().remove("hidden_launcher_packages_v1").apply()
    }

    fun requestUninstallApp(context: Context, packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("DashboardViewModel", "Failed to request uninstall for $packageName", e)
            Toast.makeText(context, "アンインストール画面を開けませんでした: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadFoldersFromPrefs(): List<LauncherFolder> {
        val jsonString = prefs.getString("launcher_folders_v1", null) ?: return emptyList()
        return try {
            val array = org.json.JSONArray(jsonString)
            val list = mutableListOf<LauncherFolder>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", UUID.randomUUID().toString())
                val name = obj.optString("name", "フォルダ")
                val colorHex = obj.optString("colorHex", "#4285F4")
                val pkgArray = obj.optJSONArray("packageNames")
                val pkgs = mutableListOf<String>()
                if (pkgArray != null) {
                    for (j in 0 until pkgArray.length()) {
                        pkgs.add(pkgArray.getString(j))
                    }
                }
                list.add(LauncherFolder(id = id, name = name, packageNames = pkgs, colorHex = colorHex))
            }
            list
        } catch (e: Exception) {
            Log.e("DashboardViewModel", "Error loading launcher folders", e)
            emptyList()
        }
    }

    private fun saveFoldersToPrefs(folders: List<LauncherFolder>) {
        try {
            val array = org.json.JSONArray()
            for (folder in folders) {
                val obj = org.json.JSONObject().apply {
                    put("id", folder.id)
                    put("name", folder.name)
                    put("colorHex", folder.colorHex)
                    val pkgArray = org.json.JSONArray()
                    for (pkg in folder.packageNames) {
                        pkgArray.put(pkg)
                    }
                    put("packageNames", pkgArray)
                }
                array.put(obj)
            }
            prefs.edit().putString("launcher_folders_v1", array.toString()).apply()
        } catch (e: Exception) {
            Log.e("DashboardViewModel", "Failed to save launcher folders", e)
        }
    }

    fun createFolder(name: String, initialPackages: List<String> = emptyList(), colorHex: String = "#4285F4") {
        val newFolder = LauncherFolder(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "新規フォルダ" },
            packageNames = initialPackages.distinct(),
            colorHex = colorHex
        )
        val updated = _launcherFolders.value + newFolder
        _launcherFolders.value = updated
        saveFoldersToPrefs(updated)
    }

    fun updateFolder(folderId: String, newName: String, packageNames: List<String>, colorHex: String? = null) {
        val updated = _launcherFolders.value.map { folder ->
            if (folder.id == folderId) {
                folder.copy(
                    name = newName.ifBlank { folder.name },
                    packageNames = packageNames.distinct(),
                    colorHex = colorHex ?: folder.colorHex
                )
            } else {
                folder
            }
        }
        _launcherFolders.value = updated
        saveFoldersToPrefs(updated)
    }

    fun deleteFolder(folderId: String) {
        val updated = _launcherFolders.value.filter { it.id != folderId }
        _launcherFolders.value = updated
        saveFoldersToPrefs(updated)
    }

    fun addAppToFolder(folderId: String, packageName: String) {
        val updated = _launcherFolders.value.map { folder ->
            if (folder.id == folderId) {
                if (packageName !in folder.packageNames) {
                    folder.copy(packageNames = folder.packageNames + packageName)
                } else {
                    folder
                }
            } else {
                folder
            }
        }
        _launcherFolders.value = updated
        saveFoldersToPrefs(updated)
    }

    fun removeAppFromFolder(folderId: String, packageName: String) {
        val updated = _launcherFolders.value.map { folder ->
            if (folder.id == folderId) {
                folder.copy(packageNames = folder.packageNames - packageName)
            } else {
                folder
            }
        }
        _launcherFolders.value = updated
        saveFoldersToPrefs(updated)
    }


    private fun startHourlyRefreshTicker() {
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                val intervalMins = _updateState.value.autoRefreshIntervalMinutes
                val delayMs = if (intervalMins > 0) {
                    (intervalMins * 60 * 1000L).coerceAtLeast(60_000L)
                } else {
                    180 * 1000L // In manual mode, idle sleep
                }
                kotlinx.coroutines.delay(delayMs)
                if (_updateState.value.autoRefreshIntervalMinutes > 0) {
                    withContext(Dispatchers.Main) {
                        refreshDashboardData()
                    }
                }
            }
        }
    }

    private fun startClockTicker() {
        viewModelScope.launch(Dispatchers.Default) {
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.JAPAN)
            val dateFormat = SimpleDateFormat("yyyy年M月d日 (E)", Locale.JAPAN)

            while (true) {
                val now = Date()
                _currentTime.value = timeFormat.format(now)
                _currentDateJp.value = dateFormat.format(now)

                // Calculate Japanese Era (Heisei/Reiwa)
                val cal = Calendar.getInstance()
                val year = cal.get(Calendar.YEAR)
                val eraYear = year - 2018
                _currentEraJp.value = "令和${eraYear}年"

                // Check Alarms
                val currentHour = cal.get(Calendar.HOUR_OF_DAY)
                val currentMinute = cal.get(Calendar.MINUTE)
                val currentSecond = cal.get(Calendar.SECOND)
                val minuteKey = currentHour * 60 + currentMinute

                if (currentSecond == 0 && lastTriggeredMinute != minuteKey) {
                    lastTriggeredMinute = minuteKey
                    val matching = _alarms.value.firstOrNull { it.isEnabled && it.hour == currentHour && it.minute == currentMinute }
                    if (matching != null) {
                        _activeAlarmTriggered.value = matching
                        startAlertSoundLoop()
                    }
                }

                kotlinx.coroutines.delay(1000)
            }
        }
    }

    fun refreshDashboardData() {
        _lastUpdatedTime.value = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPAN).format(Date())
        _weatherState.value = WeatherUiState.Loading
        _aiContentState.value = AiContentUiState.Loading
        viewModelScope.launch {
            val weather = fetchWeather()
            if (weather != null) {
                _weatherState.value = WeatherUiState.Success(weather)
            } else {
                _weatherState.value = WeatherUiState.Error("天気情報の取得に失敗しました。")
            }
            fetchAiCuration(weather)
        }
    }

    private suspend fun fetchWeather(): OpenMeteoResponse? {
        val region = _selectedRegion.value
        return withContext(Dispatchers.IO) {
            try {
                ApiClient.openMeteoService.getForecast(
                    latitude = region.latitude,
                    longitude = region.longitude
                )
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error fetching weather", e)
                null
            }
        }
    }

    private suspend fun fetchNhkNews(): List<NewsItem> {
        val list = mutableListOf<NewsItem>()
        // 1. First attempt: Yahoo! News RSS (most up-to-date breaking news headlines)
        try {
            val yahooUrl = "https://news.yahoo.co.jp/rss/topics/top-picks.xml"
            val request = Request.Builder().url(yahooUrl).build()
            val response = withContext(Dispatchers.IO) { ApiClient.okHttpClient.newCall(request).execute() }
            if (response.isSuccessful) {
                val xml = response.body?.string() ?: ""
                val doc = Jsoup.parse(xml, "", Parser.xmlParser())
                val items = doc.select("item")
                for (item in items) {
                    val title = item.select("title").text().trim()
                    val link = item.select("link").text().trim()
                    val pubDate = item.select("pubDate").text().trim()
                    if (title.isNotEmpty()) {
                        val relativeTime = parseRelativeTime(pubDate)
                        val category = determineCategoryFromTitle(title)
                        list.add(NewsItem(title = title, category = category, time = relativeTime, url = link))
                    }
                    if (list.size >= 8) break
                }
            }
        } catch (e: Exception) {
            Log.e("DashboardViewModel", "Yahoo News fetch failed, falling back to NHK News", e)
        }

        if (list.isNotEmpty()) return list

        // 2. Second attempt: NHK News RSS
        val url = "https://www3.nhk.or.jp/rss/news/cat0.xml"
        val request = Request.Builder().url(url).build()
        return withContext(Dispatchers.IO) {
            try {
                ApiClient.okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val xml = response.body?.string() ?: ""
                        parseNhkNews(xml)
                    } else {
                        Log.e("DashboardViewModel", "NHK News fetch failed: ${response.code}")
                        emptyList()
                    }
                }
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error fetching NHK News", e)
                emptyList()
            }
        }
    }

    private fun parseNhkNews(xml: String): List<NewsItem> {
        val list = mutableListOf<NewsItem>()
        try {
            val doc = Jsoup.parse(xml, "", Parser.xmlParser())
            val items = doc.select("item")
            for (item in items) {
                val title = item.select("title").text().trim()
                val link = item.select("link").text().trim()
                val pubDate = item.select("pubDate").text().trim()
                
                if (title.isNotEmpty()) {
                    val relativeTime = parseRelativeTime(pubDate)
                    val category = determineCategoryFromTitle(title)
                    list.add(NewsItem(title = title, category = category, time = relativeTime, url = link))
                }
                if (list.size >= 8) break
            }
        } catch (e: Exception) {
            Log.e("DashboardViewModel", "Error parsing NHK News XML", e)
        }
        return list
    }

    private fun determineCategoryFromTitle(title: String): String {
        return when {
            title.contains("選手") || title.contains("五輪") || title.contains("大谷") || title.contains("プロ野球") || title.contains("サッカー") || title.contains("陸上") || title.contains("優勝") -> "スポーツ"
            title.contains("AI") || title.contains("宇宙") || title.contains("IT") || title.contains("ネット") || title.contains("サイバー") || title.contains("技術") || title.contains("開発") -> "テクノロジー"
            title.contains("株") || title.contains("円") || title.contains("為替") || title.contains("経済") || title.contains("企業") || title.contains("決算") || title.contains("市場") -> "経済"
            title.contains("映画") || title.contains("出演") || title.contains("俳優") || title.contains("アニメ") || title.contains("公開") || title.contains("芸能") || title.contains("歌手") -> "エンタメ"
            title.contains("事故") || title.contains("事件") || title.contains("逮捕") || title.contains("災害") || title.contains("大雨") || title.contains("地震") || title.contains("火災") -> "社会"
            else -> "主要"
        }
    }

    private fun parseRelativeTime(pubDateStr: String): String {
        try {
            val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
            val date = sdf.parse(pubDateStr) ?: return "最新"
            val diffMs = Date().time - date.time
            val diffMins = diffMs / 1000 / 60
            return when {
                diffMins < 0 -> "最新"
                diffMins < 60 -> "${diffMins}分前"
                diffMins < 1440 -> "${diffMins / 60}時間前"
                else -> "${diffMins / 1440}日前"
            }
        } catch (e: Exception) {
            return "最新"
        }
    }

    private suspend fun fetchYahooTrends(): List<TrendItem> {
        val url = "https://search.yahoo.co.jp/realtime"
        val request = Request.Builder().url(url).build()
        return withContext(Dispatchers.IO) {
            try {
                ApiClient.okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val html = response.body?.string() ?: ""
                        parseYahooTrends(html)
                    } else {
                        Log.e("DashboardViewModel", "Yahoo Trends fetch failed: ${response.code}")
                        emptyList()
                    }
                }
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error fetching Yahoo Trends", e)
                emptyList()
            }
        }
    }

    private fun parseYahooTrends(html: String): List<TrendItem> {
        val list = mutableListOf<TrendItem>()
        try {
            val doc = Jsoup.parse(html)
            val links = doc.select("a[href*=realtime/search?p=]")
            val uniqueKeywords = mutableSetOf<String>()
            for (link in links) {
                val keyword = link.text().trim()
                if (keyword.isNotEmpty() && !keyword.startsWith("#") && keyword.length > 1 && !uniqueKeywords.contains(keyword)) {
                    var desc = "Yahoo!リアルタイム急上昇キーワード。"
                    val parent = link.parent()
                    if (parent != null) {
                        val countElem = parent.select("span[class*=count], span[class*=num], p").firstOrNull()
                        if (countElem != null && countElem.text().isNotEmpty()) {
                            desc = countElem.text().trim()
                        } else {
                            val parentText = parent.text().replace(keyword, "").trim()
                            if (parentText.isNotEmpty() && parentText.length < 150) {
                                desc = parentText
                            }
                        }
                    }
                    if (desc == "Yahoo!リアルタイム急上昇キーワード。" || desc.isEmpty()) {
                        desc = "SNS（X/旧Twitter等）で現在話題沸騰中の急上昇検索ワードです。"
                    }
                    uniqueKeywords.add(keyword)
                    list.add(TrendItem(keyword = keyword, description = desc))
                    if (list.size >= 20) break
                }
            }
        } catch (e: Exception) {
            Log.e("DashboardViewModel", "Error parsing Yahoo Trends", e)
        }
        return list
    }

    private suspend fun fetchWhatIsTodayData(month: Int, day: Int): Pair<List<String>, List<String>> {
        val mmdd = String.format(Locale.US, "%02d%02d", month, day)
        return withContext(Dispatchers.IO) {
            val anniversaries = mutableListOf<String>()
            val birthdays = mutableListOf<String>()

            // 1. Fetch Anniversaries from whatistoday.cyou API (Target: 5 items)
            try {
                val annivRes = ApiClient.whatIsTodayService.getAnniversaries(mmdd)
                val rawAnnivs = listOfNotNull(
                    annivRes.anniv1,
                    annivRes.anniv2,
                    annivRes.anniv3,
                    annivRes.anniv4,
                    annivRes.anniv5
                ).map { it.trim() }.filter { it.isNotEmpty() && !it.contains("ほかの日も見てみよう") }
                anniversaries.addAll(rawAnnivs)
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error fetching anniversaries from whatistoday.cyou", e)
            }

            // Fill up to 5 anniversaries using fallback if needed
            if (anniversaries.size < 5) {
                val fallbackAnnivs = getFallbackAnniversaries(month, day)
                for (item in fallbackAnnivs) {
                    if (anniversaries.size >= 5) break
                    val title = item.substringBefore(":")
                    if (!anniversaries.any { it.contains(title) }) {
                        anniversaries.add(title)
                    }
                }
            }

            // 2. Fetch Famous Birthday from whatistoday.cyou API (Target: 3 items)
            try {
                val bdayRes = ApiClient.whatIsTodayService.getFamousBirthday(mmdd)
                val name = bdayRes.name?.trim() ?: ""
                val lifespan = bdayRes.lifespan?.trim() ?: ""
                val profile = bdayRes.profile?.trim() ?: ""
                if (name.isNotEmpty()) {
                    val formatted = buildString {
                        append(name)
                        if (lifespan.isNotEmpty()) append(" ($lifespan)")
                        if (profile.isNotEmpty()) append(": $profile")
                    }
                    birthdays.add(formatted)
                }
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error fetching famous birthday from whatistoday.cyou", e)
            }

            // Supplement with famous birthdays for this date to reach exactly 3 items
            val additionalBirthdays = getFamousBirthdaysForDate(month, day)
            for (b in additionalBirthdays) {
                if (birthdays.size >= 3) break
                val bName = b.substringBefore("(")
                if (!birthdays.any { it.contains(bName) }) {
                    birthdays.add(b)
                }
            }

            Pair(anniversaries.take(5), birthdays.take(3))
        }
    }

    private fun getFamousBirthdaysForDate(month: Int, day: Int): List<String> {
        val specific = when ("$month/$day") {
            "8/30" -> listOf(
                "メアリー・シェリー (1797-1851): イギリスの小説家。『フランケンシュタイン』の著者",
                "アーネスト・ラザフォード (1871-1937): ニュージーランド出身の物理学者。原子核の発見者、ノーベル化学賞",
                "キャメロン・ディアス (1972-): アメリカの女優。『マスク』『メリーに首ったけ』など多数主演",
                "井上陽水 (1948-): 日本を代表するシンガーソングライター。『少年時代』『夢の中へ』"
            )
            "8/31" -> listOf(
                "リチャード・ギア (1949-): アメリカの俳優。『プリティ・ウーマン』主演",
                "田代まさし (1956-): タレント、ミュージシャン（ラッツ&スター）",
                "別所哲也 (1965-): 俳優、ショートショートフィルムフェスティバル代表"
            )
            "9/1" -> listOf(
                "小澤征爾 (1935-2024): 世界的指揮者、ボストン交響楽団音楽監督",
                "渡辺謙 (1959-): 国際的映画俳優。『ラスト サムライ』『インセプション』",
                "土田晃之 (1972-): お笑いタレント、司会者"
            )
            else -> listOf(
                "歴史上の偉人・著名人 (文化・芸術): 本日は国内外の学問や芸術、文学で偉大な功績を残した著名人の誕生日です",
                "世界的科学者・探検家 (科学・探求): 人類の科学技術や未知の領域を開拓したパイオニアの誕生日",
                "著名エンターテイナー・アスリート: 音楽・映画・スポーツで多くの人々に感動を与えたスターの誕生日"
            )
        }
        return specific
    }

    private suspend fun fetchAiCuration(weather: OpenMeteoResponse?) {
        val region = _selectedRegion.value
        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)

        val news = fetchNhkNews()
        val trends = fetchYahooTrends()
        val (anniversaries, birthdays) = fetchWhatIsTodayData(month, day)

        val finalNews = if (news.isNotEmpty()) news else getFallbackNews()
        val finalTrends = if (trends.isNotEmpty()) trends.take(5) else getFallbackTrends().take(5)
        val finalAnniversaries = if (anniversaries.isNotEmpty()) anniversaries else getFallbackAnniversaries(month, day).take(5)
        val finalBirthdays = if (birthdays.isNotEmpty()) birthdays else getFamousBirthdaysForDate(month, day).take(3)

        val apiKey = BuildConfig.GEMINI_API_KEY
        var localTips = ""
        var localNewsList: List<NewsItem> = emptyList()
        var isMock = false

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            _apiKeyWarning.value = true
            val fallbackLocal = getFallbackLocalContent(region)
            localTips = fallbackLocal.first
            localNewsList = fallbackLocal.second
            isMock = true
        } else {
            _apiKeyWarning.value = false
            try {
                val tempStr = weather?.current?.temperature?.toString() ?: "不明"
                val code = weather?.current?.weatherCode ?: 0
                val weatherDesc = getWeatherDescriptionJapanese(code)

                val prompt = """
                    あなたはスマートホーム用の高品質なダッシュボードAIです。以下の情報を考慮して、表示用のJSONデータを日本語で返してください。

                    【状況】
                    選択されている地域: ${region.name} (${region.englishName})
                    本日: ${month}月${day}日
                    現在の天気: $weatherDesc、気温: ${tempStr}℃

                    【コンテンツの編集方針】
                    1. 「omihachimanTips」キーには、現在の天気（$weatherDesc、気温${tempStr}℃）と今日の季節を考慮した、選択地域（${region.keywordTips}）のおすすめお出かけアドバイスや一言（150字以内）。親しみやすく「〜ですよ」調で。
                    2. 「localNews」キーには、選択されている地域（${region.name}およびその周辺地域）の【地域限定のローカルニュース・行事・お知らせ】を合計3件選定してください（例: 地域のイベント情報、新店オープン、歴史行事、自治体からのお知らせなど）。実際の情報に基づき、ハルシネーションを極力抑えてください。

                    【出力JSONフォーマット】
                    必ず以下のJSONキー、および構造に従って出力してください。他のテキスト、Markdownブロック（```jsonなど）は一切含めず、純粋なJSON文字列だけを返してください。
                    {
                      "omihachimanTips": "天気や季節を考慮した、この地域のお出かけガイド...",
                      "localNews": [
                        { "title": "地域限定のローカルニュース見出し1", "category": "地域", "time": "1時間前" },
                        { "title": "地域限定のローカルニュース見出し2", "category": "観光", "time": "3時間前" },
                        { "title": "地域限定のローカルニュース見出し3", "category": "生活", "time": "5時間前" }
                      ]
                    }
                """.trimIndent()

                val request = GeminiRequest(
                    contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
                    generationConfig = GeminiGenerationConfig(responseMimeType = "application/json", temperature = 0.7)
                )

                val response = ApiClient.geminiService.generateContent(apiKey, request)
                val jsonString = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: ""
                Log.d("DashboardViewModel", "Gemini Local Response: $jsonString")

                val cleanedJson = jsonString
                    .replace("```json", "")
                    .replace("```", "")
                    .trim()

                val adapter = ApiClient.moshi.adapter(DashboardAiContent::class.java)
                val content = adapter.fromJson(cleanedJson)

                if (content != null) {
                    localTips = content.omihachimanTips
                    localNewsList = content.localNews
                } else {
                    val fallbackLocal = getFallbackLocalContent(region)
                    localTips = fallbackLocal.first
                    localNewsList = fallbackLocal.second
                    isMock = true
                }
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error fetching Gemini local curation", e)
                val fallbackLocal = getFallbackLocalContent(region)
                localTips = fallbackLocal.first
                localNewsList = fallbackLocal.second
                isMock = true
            }
        }

        val combinedContent = DashboardAiContent(
            news = finalNews,
            trends = finalTrends,
            anniversaries = finalAnniversaries,
            famousBirthdays = finalBirthdays,
            omihachimanTips = localTips,
            localNews = localNewsList
        )

        _aiContentState.value = AiContentUiState.Success(combinedContent, isMock = isMock)
    }

    private fun getFallbackLocalContent(region: RegionConfig): Pair<String, List<NewsItem>> {
        return when (region.id) {
            "otsu" -> Pair(
                "大津市は琵琶湖の美しい夕景が素晴らしいですよ。ミシガンクルーズでの湖上散歩や、石山寺での紫式部ゆかりの歴史散策をお楽しみください！",
                listOf(
                    NewsItem("大津港から出発する『ミシガンクルーズ』、季節限定夜間便が運行決定", "観光", "1時間前"),
                    NewsItem("比叡山延暦寺にて国宝根本中堂 of 保存修理現場を公開する特別ツアー", "歴史・文化", "3時間前"),
                    NewsItem("大津市のびわ湖大津プリンスホテルにて地元の夏食材フェア開催", "経済・グルメ", "5時間前")
                )
            )
            "hikone" -> Pair(
                "彦根市は美しい国宝天守と美味しい近江牛の食べ歩きが魅力です！キャッスルロードでお団子を片手に、のんびりと歴史情緒をお楽しみくださいね！",
                listOf(
                    NewsItem("彦根城世界遺産登録を目指す、市民ボランティアによる特別啓発パレード実施", "社会", "1時間前"),
                    NewsItem("彦根城下町の古民家を再生した新しい和カフェ＆ゲストハウスが来月オープン", "観光", "3時間前"),
                    NewsItem("国宝・彦根城にて涼を呼ぶ「夜間特別ライトアップ」が今週末から開始", "歴史・観光", "5時間前")
                )
            )
            "kyoto" -> Pair(
                "古都京都は清水寺や金閣寺など世界遺産の宝庫です。路地裏のレトロな和菓子店で淹れたての宇治抹茶を味わって、贅沢なひとときをどうぞ！",
                listOf(
                    NewsItem("京都・嵐山にて伝統の鵜飼いが夜空を赤く照らす特別夜間鑑賞船が運行", "伝統行事", "1時間前"),
                    NewsItem("京都市内の老舗和菓子店が共同開発した、夏季限定の創作葛まんじゅう発売", "グルメ", "3時間前"),
                    NewsItem("祇園祭の山鉾建てが本格始動、多くの職人や関係者が早朝から汗を流す", "伝統文化", "5時間前")
                )
            )
            "osaka" -> Pair(
                "活気みなぎる大阪は、たこ焼きやお好み焼きの食べ歩きに熱狂的な魅力がありますよ！あべのハルカスから大都市の息吹を感じてください！",
                listOf(
                    NewsItem("大阪城公園でフードフェスティバル開幕、国内外のご当地グルメ集結", "グルメ", "1時間前"),
                    NewsItem("道頓堀のシンボル周辺が遊歩道として整備、新たな観光名所に", "地域・開発", "3.5時間前"),
                    NewsItem("関西経済連合会が未来のスマートモビリティ導入実験を大阪万博跡地で実施", "テクノロジー", "6時間前")
                )
            )
            "tokyo" -> Pair(
                "東京都千代田区は、最先端のトレンドと皇居周辺の伝統が調和した素晴らしい街です。秋葉原のデジタルカルチャーや丸の内の並木道散策を満喫してください！",
                listOf(
                    NewsItem("東京駅丸の内駅舎前の広場にて、最新のフラワーアート展が開幕", "社会・エンタメ", "1時間前"),
                    NewsItem("千代田区が推進する秋葉原駅周辺のスマートシティデジタルガイドが稼働", "テクノロジー", "3時間前"),
                    NewsItem("皇居外苑での緑豊かなジョギングコースが健康トレンドとして再注目", "スポーツ・生活", "5時間前")
                )
            )
            "sapporo" -> Pair(
                "札幌市はさわやかな風と美しい大地の恵みが自慢です！大通公園の木陰でのんびり過ごしたり、贅沢な海鮮丼を堪能して、心身ともに満たされてくださいね！",
                listOf(
                    NewsItem("大通公園の夏ビアガーデン、過去最大規模での開催準備が整う", "グルメ・イベント", "1時間前"),
                    NewsItem("札幌市時計台にて、開館記念の特別夜間無料公開イベントを実施", "歴史・観光", "3時間前"),
                    NewsItem("羊ヶ丘展望台から望むラベンダー畑が見頃を迎え、観光客が増加中", "地域", "5時間前")
                )
            )
            "fukuoka" -> Pair(
                "福岡市は活気あふれる屋台文化とおいしいグルメの宝庫です！中洲の夜風を感じながら食べる一杯 of ラーメンや、太宰府天満宮での参拝をお楽しみください！",
                listOf(
                    NewsItem("博多駅の新商業施設オープンに朝から数千人の行列", "経済", "1時間前"),
                    NewsItem("大濠公園ボートハウスにて、夜景を楽しめるテラスBBQプランが開始", "観光・グルメ", "3時間前"),
                    NewsItem("太宰府天満宮の美しい風鈴まつり、涼やかな音色が参道に響き渡る", "歴史・伝統", "5時間前")
                )
            )
            else -> Pair(
                "近江八幡市は美しい八幡堀や手漕ぎの水郷めぐりなど、見どころがいっぱいの伝統の街ですよ。ぜひ美味しい近江牛ランチと一緒に穏やかな旅情をお楽しみくださいね！",
                listOf(
                    NewsItem("滋賀・びわ湖周辺海底調査、新たな中世湖底遺跡発見か", "歴史・科学", "2時間前"),
                    NewsItem("八幡堀周辺の伝統的な蔵元が若者向けのスパークリング日本酒を発表", "経済・グルメ", "4時間前"),
                    NewsItem("近江八幡市の町並み保存地区、ボランティアによる一斉清掃実施", "社会・ボランティア", "6時間前")
                )
            )
        }
    }

    private fun getFallbackNews(): List<NewsItem> = listOf(
        NewsItem("日本電子決済推進協会、キャッシュレス決済比率が過去最高の40%超えを記録と発表", "経済", "30分前"),
        NewsItem("国内主要IT企業が共同開発した新型スマートデバイス、今秋一般リリース決定", "テクノロジー", "1時間前"),
        NewsItem("気象庁、今夏の全国的な気温傾向と熱中症対策強化の呼びかけを実施", "社会", "2時間前"),
        NewsItem("人気アニメの劇場版最新作、公開初週末で興行収入15億円突破の大ヒット", "エンタメ", "3時間前"),
        NewsItem("プロ野球：首位攻防戦で劇的な逆転満塁サヨナラホームラン、ファン大歓喜", "スポーツ", "4時間前"),
        NewsItem("次世代省エネ住宅の普及加速、補助金制度の拡充で太陽光・蓄電池の導入が増加", "生活・環境", "5時間前"),
        NewsItem("全国各地の観光地で夏のデジタルスタンプラリーがスタート、参加者急増中", "地域・観光", "6時間前"),
        NewsItem("宇宙航空開発機構、次世代観測衛星の打ち上げ成功を発表、気象予測の精度向上へ", "科学", "7時間前")
    )

    private fun getFallbackTrends(): List<TrendItem> = listOf(
        TrendItem("#生成AI翻訳", "X上で最新のリアルタイムAI音声通訳アプリのデモ動画が数万リポストされ話題沸騰中。"),
        TrendItem("#熱中症警戒アラート", "厳しい猛暑日が続く予報を受け、外出時の水分補給やエアコン適切な使用が呼びかけられています。"),
        TrendItem("#スマートホーム家電", "音声操作や自動センシングで快適な室温を維持する最新IoTエアコンに注目が集まっています。"),
        TrendItem("#新作ゲーム体験版", "世界的名作アクションRPGの体験版がサプライズ配信され、実況配信や攻略情報でタイムラインが席巻。"),
        TrendItem("#夏バテ防止レシピ", "手軽に作れるさっぱり豚しゃぶと薬味たっぷりの冷やしうどんレシピがバズっています。"),
        TrendItem("#満月フラワームーン", "今夜の澄み渡る夜空で見られる美しい満月の写真が多数投稿されタイムラインを彩っています。"),
        TrendItem("#全国花火大会", "各地の花火大会の有料席情報や穴場スポットの紹介ポストが急上昇ランクイン。"),
        TrendItem("#新幹線ダイヤ改正", "秋の観光シーズンに向けた臨時列車の増便と新型車両の運行開始ニュースに鉄道ファン歓喜。"),
        TrendItem("#ご当地グルメ旅", "地元民しか知らない絶品B級グルメや老舗カフェ巡りのまとめスレッドが人気。"),
        TrendItem("#カフェ巡り部", "季節限定の涼やかなフルーツパフェや水出しアイスコーヒーの写真が多数シェアされています。"),
        TrendItem("#健康ウォーキング", "朝夕の涼しい時間帯のウォーキング記録や歩数計アプリの活用法が話題に。"),
        TrendItem("#推し活フェス", "大型音楽フェスやアニメコラボイベントのグッズ販売発表でトレンド上位を独占。"),
        TrendItem("#週末おでかけプラン", "日帰りで楽しめる近場の避暑地や温泉ドライブコースの提案が好評を集めています。"),
        TrendItem("#エコ省エネ術", "夏の電気代を賢く抑えるサーキュレーターの配置方法や遮光カーテンの活用術。"),
        TrendItem("#朝活習慣", "早朝の読書や瞑想、ストレッチで1日を気持ちよくスタートする投稿が増加中。")
    )

    private fun getFallbackAnniversaries(month: Int, day: Int): List<String> {
        val list = mutableListOf<String>()
        val dateKey = "$month/$day"
        when (dateKey) {
            "8/30" -> {
                list.add("冒険家の日: 1965年の同志社大学アンデス・アマゾン遠征隊の成功や、1989年の堀江謙一の世界最小ヨット太平洋横断などの偉業を記念する日。")
                list.add("富士山測候所記念日: 1895年8月30日に富士山頂に日本初の気象観測所が開設された歴史的記念日。")
                list.add("マッカーサー進駐記念日: 1945年8月30日、連合国軍最高司令官ダグラス・マッカーサーが厚木飛行場に到着した日。")
                list.add("ヤミ金融ゼロの日: 「8(ヤ)3(ミ)0(ゼロ)」の語呂合わせ。違法金融撲滅と生活防衛を啓発する日。")
                list.add("ハッピーサンシャインデー: 「8(ハッピー)30(サンシャイン)」の語呂合わせ。太陽のような笑顔で周りにハッピーを届ける日。")
            }
            "8/31" -> {
                list.add("野菜の日: 「8(ヤ)3(サ)1(イ)」の語呂合わせ。栄養たっぷりの新鮮な野菜を美味しく食べて健康に過ごす記念日。")
                list.add("初音ミクの誕生日: 2007年8月31日に音声合成ソフトウェア「初音ミク」が発売された記念日。")
                list.add("宿題の日（学問の日）: 夏休み最終日に計画的に学習を振り返り、新学期へ意欲を高める日。")
                list.add("アイコトバの日: 「8(アイ)31(コトバ)」の語呂合わせ。言葉の大切さを再認識する日。")
                list.add("夏休み締めくくりの日: 楽しい夏の思い出を振り返り、実りある秋の訪れを迎える節目の日。")
            }
            "9/1" -> {
                list.add("防災の日: 1923年9月1日の関東大震災に由来し、日頃の防災意識向上や避難訓練を行う記念日。")
                list.add("二百十日: 立春から数えて210日目。台風の襲来を警戒し豊作を祈る伝統的な雑節。")
                list.add("キウイの日: 「9(キウ)1(イ)」の語呂合わせ。ビタミン豊富なキウイフルーツを味わう日。")
                list.add("くいの日: 「9(く)1(い)」の語呂合わせ。基礎工事や建築の安全を祈念する日。")
                list.add("霞ヶ浦の日: 茨城県の霞ヶ浦の水質保全と自然環境保護を呼びかける記念日。")
            }
            else -> {
                list.add("${month}月${day}日の記念日: 日本および世界の歴史・文化・季節の節目を記念する「今日は何の日」です。")
                list.add("季節の風物詩記念デー: 移りゆく四季の美しさと旬の味覚を慈しむ記念日。")
                list.add("生活文化推進デー: 健やかで豊かな暮らしを支える習慣や伝統を大切にする日。")
                list.add("地域コミュニティの日: 地域社会との温かいふれあいや助け合いを促進する日。")
                list.add("知的好奇心・探求の日: 科学技術や自然界の不思議を学び、未来への発見を楽しむ日。")
            }
        }
        return list
    }

    private fun useFallbackData(warningMessage: String) {
        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val region = _selectedRegion.value

        val nationalNews = listOf(
            NewsItem("日本電子決済推進協会、キャッシュレス決済比率が過去最高の40%超えを記録と発表", "経済", "30分前"),
            NewsItem("国内主要IT企業が共同開発した新型スマートデバイス、今秋一般リリース決定", "テクノロジー", "1時間前"),
            NewsItem("気象庁、今夏の全国的な気温傾向と熱中症対策強化の呼びかけを実施", "社会", "2時間前"),
            NewsItem("人気アニメの劇場版最新作、公開初週末で興行収入15億円突破の大ヒット", "エンタメ", "3時間前"),
            NewsItem("プロ野球：首位攻防戦で劇的な逆転満塁サヨナラホームラン、ファン大歓喜", "スポーツ", "4時間前")
        )

        val nationalTrends = listOf(
            TrendItem("生成AI翻訳ツール", "多言語翻訳の精度が飛躍的に向上し、ビジネスや語学学習での実用化についてSNSで議論が白熱中。"),
            TrendItem("スマートホーム家電", "音声操作や自動省エネに対応した最新エアコン・照明機器の導入事例がトレンド入り。"),
            TrendItem("熱中症警戒アラート", "連日の厳しい暑さに伴い、こまめな水分補給やエアコン使用の注意喚起が急上昇。"),
            TrendItem("新作ゲーム体験版", "世界的RPGシリーズの最新作の体験版が突如配信開始され、世界中のプレイヤーがSNSで熱狂。"),
            TrendItem("夏バテ防止レシピ", "さっぱり食べられて栄養価の高い、簡単5分で作れる豚肉と夏野菜のネギ塩炒めレシピがバズり中。")
        )

        val nationalAnniversaries = getFallbackAnniversaries(month, day).take(5)
        val nationalBirthdays = getFamousBirthdaysForDate(month, day).take(3)

        val fallbackContent = when (region.id) {
            "otsu" -> DashboardAiContent(
                news = nationalNews,
                trends = nationalTrends,
                anniversaries = nationalAnniversaries,
                famousBirthdays = nationalBirthdays,
                omihachimanTips = "大津市は琵琶湖の美しい夕景が素晴らしいですよ。ミシガンクルーズでの湖上散歩や、石山寺での紫式部ゆかりの歴史散策をお楽しみください！",
                localNews = listOf(
                    NewsItem("大津港から出発する『ミシガンクルーズ』、季節限定夜間便が運行決定", "観光", "1時間前"),
                    NewsItem("比叡山延暦寺にて国宝根本中堂 of 保存修理現場を公開する特別ツアー", "歴史・文化", "3時間前"),
                    NewsItem("大津市のびわ湖大津プリンスホテルにて地元の夏食材フェア開催", "経済・グルメ", "5時間前")
                )
            )
            "hikone" -> DashboardAiContent(
                news = nationalNews,
                trends = nationalTrends,
                anniversaries = nationalAnniversaries,
                famousBirthdays = nationalBirthdays,
                omihachimanTips = "彦根市は美しい国宝天守と美味しい近江牛の食べ歩きが魅力です！キャッスルロードでお団子を片手に、のんびりと歴史情緒をお楽しみくださいね！",
                localNews = listOf(
                    NewsItem("彦根城世界遺産登録を目指す、市民ボランティアによる特別啓発パレード実施", "社会", "1時間前"),
                    NewsItem("彦根城下町の古民家を再生した新しい和カフェ＆ゲストハウスが来月オープン", "観光", "3時間前"),
                    NewsItem("国宝・彦根城にて涼を呼ぶ「夜間特別ライトアップ」が今週末から開始", "歴史・観光", "5時間前")
                )
            )
            "kyoto" -> DashboardAiContent(
                news = nationalNews,
                trends = nationalTrends,
                anniversaries = nationalAnniversaries,
                famousBirthdays = nationalBirthdays,
                omihachimanTips = "古都京都は清水寺や金閣寺など世界遺産の宝庫です。路地裏のレトロな和菓子店で淹れたての宇治抹茶を味わって、贅沢なひとときをどうぞ！",
                localNews = listOf(
                    NewsItem("京都・嵐山にて伝統の鵜飼いが夜空を赤く照らす特別夜間鑑賞船が運行", "伝統行事", "1時間前"),
                    NewsItem("京都市内の老舗和菓子店が共同開発した、夏季限定の創作葛まんじゅう発売", "グルメ", "3時間前"),
                    NewsItem("祇園祭の山鉾建てが本格始動、多くの職人や関係者が早朝から汗を流す", "伝統文化", "5時間前")
                )
            )
            "osaka" -> DashboardAiContent(
                news = nationalNews,
                trends = nationalTrends,
                anniversaries = nationalAnniversaries,
                famousBirthdays = nationalBirthdays,
                omihachimanTips = "活気みなぎる大阪は、たこ焼きやお好み焼きの食べ歩きに熱狂的な魅力がありますよ！あべのハルカスから大都市の息吹を感じてください！",
                localNews = listOf(
                    NewsItem("大阪城公園でフードフェスティバル開幕、国内外のご当地グルメ集結", "グルメ", "1時間前"),
                    NewsItem("道頓堀のシンボル周辺が遊歩道として整備、新たな観光名所に", "地域・開発", "3.5時間前"),
                    NewsItem("関西経済連合会が未来のスマートモビリティ導入実験を大阪万博跡地で実施", "テクノロジー", "6時間前")
                )
            )
            "tokyo" -> DashboardAiContent(
                news = nationalNews,
                trends = nationalTrends,
                anniversaries = nationalAnniversaries,
                famousBirthdays = nationalBirthdays,
                omihachimanTips = "東京都千代田区は、最先端のトレンドと皇居周辺の伝統が調和した素晴らしい街です。秋葉原のデジタルカルチャーや丸の内の並木道散策を満喫してください！",
                localNews = listOf(
                    NewsItem("東京駅丸の内駅舎前の広場にて、最新のフラワーアート展が開幕", "社会・エンタメ", "1時間前"),
                    NewsItem("千代田区が推進する秋葉原駅周辺のスマートシティデジタルガイドが稼働", "テクノロジー", "3時間前"),
                    NewsItem("皇居外苑での緑豊かなジョギングコースが健康トレンドとして再注目", "スポーツ・生活", "5時間前")
                )
            )
            "sapporo" -> DashboardAiContent(
                news = nationalNews,
                trends = nationalTrends,
                anniversaries = nationalAnniversaries,
                famousBirthdays = nationalBirthdays,
                omihachimanTips = "札幌市はさわやかな風と美しい大地の恵みが自慢です！大通公園の木陰でのんびり過ごしたり、贅沢な海鮮丼を堪能して、心身ともに満たされてくださいね！",
                localNews = listOf(
                    NewsItem("大通公園の夏ビアガーデン、過去最大規模での開催準備が整う", "グルメ・イベント", "1時間前"),
                    NewsItem("札幌市時計台にて、開館記念の特別夜間無料公開イベントを実施", "歴史・観光", "3時間前"),
                    NewsItem("羊ヶ丘展望台から望むラベンダー畑が見頃を迎え、観光客が増加中", "地域", "5時間前")
                )
            )
            "fukuoka" -> DashboardAiContent(
                news = nationalNews,
                trends = nationalTrends,
                anniversaries = nationalAnniversaries,
                famousBirthdays = nationalBirthdays,
                omihachimanTips = "福岡市は活気あふれる屋台文化とおいしいグルメの宝庫です！中洲の夜風を感じながら食べる一杯のラーメンや、太宰府天満宮での参拝をお楽しみください！",
                localNews = listOf(
                    NewsItem("博多駅の新商業施設オープンに朝から数千人の行列", "経済", "1時間前"),
                    NewsItem("大濠公園ボートハウスにて、夜景を楽しめるテラスBBQプランが開始", "観光・グルメ", "3時間前"),
                    NewsItem("太宰府天満宮の美しい風鈴まつり、涼やかな音色が参道に響き渡る", "歴史・伝統", "5時間前")
                )
            )
            else -> DashboardAiContent(
                news = nationalNews,
                trends = nationalTrends,
                anniversaries = nationalAnniversaries,
                famousBirthdays = nationalBirthdays,
                omihachimanTips = "近江八幡市は美しい八幡堀や手漕ぎの水郷めぐりなど、見どころがいっぱいの伝統の街ですよ。ぜひ美味しい近江牛ランチと一緒に穏やかな旅情をお楽しみくださいね！",
                localNews = listOf(
                    NewsItem("滋賀・びわ湖周辺海底調査、新たな中世湖底遺跡発見か", "歴史・科学", "2時間前"),
                    NewsItem("八幡堀周辺の伝統的な蔵元が若者向けのスパークリング日本酒を発表", "経済・グルメ", "4時間前"),
                    NewsItem("近江八幡市の町並み保存地区、ボランティアによる一斉清壊実施", "社会・ボランティア", "6時間前")
                )
            )
        }

        _aiContentState.value = AiContentUiState.Success(fallbackContent, isMock = true)
    }

    // --- Task Database Actions (Chore / Checklist widget) ---
    fun addTask(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.insert(Task(title = title))
        }
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch {
            repository.update(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.delete(task)
        }
    }

    // --- Weather Helpers ---
    fun getWeatherDescriptionJapanese(code: Int): String {
        return when (code) {
            0 -> "快晴"
            1 -> "晴れ（ほぼ快晴）"
            2 -> "晴れ時々曇り"
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
            else -> "不明"
        }
    }

    // ==========================================
    // Photo Frame & Camera Roll Slideshow
    // ==========================================
    fun loadDevicePhotos() {
        _isPhotosLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val photoList = mutableListOf<PhotoItem>()
            try {
                val projection = arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_ADDED
                )
                val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
                val cursor = app.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    sortOrder
                )
                cursor?.use {
                    val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val dateColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                    while (it.moveToNext()) {
                        val id = it.getLong(idColumn)
                        val name = it.getString(nameColumn) ?: "Photo_$id"
                        val date = it.getLong(dateColumn)
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                        photoList.add(
                            PhotoItem(
                                id = id,
                                uriString = contentUri.toString(),
                                name = name,
                                dateAdded = date,
                                isSample = false
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error loading device photos from MediaStore", e)
            }

            withContext(Dispatchers.Main) {
                if (photoList.isNotEmpty()) {
                    _photos.value = photoList
                } else {
                    // Fallback to sample photos if device has no photos yet
                    _photos.value = samplePhotos
                }
                _isPhotosLoading.value = false
                if (_currentPhotoIndex.value >= _photos.value.size) {
                    _currentPhotoIndex.value = 0
                }
                restartSlideshowTimer()
            }
        }
    }

    fun addCustomPhotoUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val current = _photos.value.filter { !it.isSample }.toMutableList()
        val newItems = uris.mapIndexed { idx, uri ->
            PhotoItem(
                id = System.currentTimeMillis() + idx,
                uriString = uri.toString(),
                name = "選択した写真 ${current.size + idx + 1}",
                dateAdded = System.currentTimeMillis() / 1000,
                isSample = false
            )
        }
        val updated = newItems + current
        _photos.value = updated
        _currentPhotoIndex.value = 0
        restartSlideshowTimer()
    }

    fun toggleSlideshow() {
        if (_isSlideshowPlaying.value) {
            pauseSlideshow()
        } else {
            playSlideshow()
        }
    }

    fun playSlideshow() {
        _isSlideshowPlaying.value = true
        restartSlideshowTimer()
    }

    fun pauseSlideshow() {
        _isSlideshowPlaying.value = false
        slideshowJob?.cancel()
        slideshowJob = null
    }

    fun nextPhoto() {
        val list = _photos.value
        if (list.isEmpty()) return
        if (_isPhotoShuffle.value && list.size > 1) {
            var next = (0 until list.size).random()
            if (next == _currentPhotoIndex.value) {
                next = (next + 1) % list.size
            }
            _currentPhotoIndex.value = next
        } else {
            _currentPhotoIndex.value = (_currentPhotoIndex.value + 1) % list.size
        }
        if (_isSlideshowPlaying.value) {
            restartSlideshowTimer()
        }
    }

    fun previousPhoto() {
        val list = _photos.value
        if (list.isEmpty()) return
        val prev = if (_currentPhotoIndex.value - 1 < 0) list.size - 1 else _currentPhotoIndex.value - 1
        _currentPhotoIndex.value = prev
        if (_isSlideshowPlaying.value) {
            restartSlideshowTimer()
        }
    }

    fun selectPhoto(index: Int) {
        val list = _photos.value
        if (index in list.indices) {
            _currentPhotoIndex.value = index
            if (_isSlideshowPlaying.value) {
                restartSlideshowTimer()
            }
        }
    }

    fun setSlideshowInterval(seconds: Int) {
        val valid = seconds.coerceIn(2, 600)
        _slideshowIntervalSeconds.value = valid
        prefs.edit().putInt("slideshow_interval_secs", valid).apply()
        if (_isSlideshowPlaying.value) {
            restartSlideshowTimer()
        }
    }

    fun setPhotoScaleMode(mode: String) {
        _photoScaleMode.value = mode
        prefs.edit().putString("photo_scale_mode", mode).apply()
    }

    fun togglePhotoClockOverlay() {
        val next = !_showPhotoClockOverlay.value
        _showPhotoClockOverlay.value = next
        prefs.edit().putBoolean("photo_clock_overlay", next).apply()
    }

    fun togglePhotoShuffle() {
        val next = !_isPhotoShuffle.value
        _isPhotoShuffle.value = next
        prefs.edit().putBoolean("photo_shuffle", next).apply()
    }

    fun setFullscreenSlideshow(enabled: Boolean) {
        _isFullscreenSlideshow.value = enabled
    }

    private fun restartSlideshowTimer() {
        slideshowJob?.cancel()
        if (!_isSlideshowPlaying.value) return
        slideshowJob = viewModelScope.launch(Dispatchers.Default) {
            while (_isSlideshowPlaying.value) {
                val delaySec = _slideshowIntervalSeconds.value.coerceAtLeast(2)
                delay(delaySec * 1000L)
                if (_isSlideshowPlaying.value && _photos.value.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        val list = _photos.value
                        if (list.isNotEmpty()) {
                            if (_isPhotoShuffle.value && list.size > 1) {
                                var next = (0 until list.size).random()
                                if (next == _currentPhotoIndex.value) {
                                    next = (next + 1) % list.size
                                }
                                _currentPhotoIndex.value = next
                            } else {
                                _currentPhotoIndex.value = (_currentPhotoIndex.value + 1) % list.size
                            }
                        }
                    }
                }
            }
        }
    }
}

data class PhotoItem(
    val id: Long,
    val uriString: String,
    val name: String,
    val dateAdded: Long = 0,
    val isSample: Boolean = false
)
