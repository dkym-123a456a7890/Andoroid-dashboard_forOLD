package com.example.api

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.GitHubReleaseAsset
import com.example.data.GitHubReleaseInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

object GitHubUpdateManager {
    private const val TAG = "GitHubUpdateManager"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /**
     * Query GitHub Releases API for the latest published release.
     * First checks /releases/latest, then falls back to /releases?per_page=1
     * to support pre-releases or tags without 'latest' mark.
     */
    suspend fun fetchLatestRelease(
        repo: String,
        token: String? = null
    ): Result<GitHubReleaseInfo?> = withContext(Dispatchers.IO) {
        val cleanRepo = repo.trim().removePrefix("https://github.com/").removeSuffix("/")
        if (!cleanRepo.contains("/")) {
            return@withContext Result.failure(IllegalArgumentException("リポジトリ形式は 'owner/repo' である必要があります (例: dkym-123a456a7890/Andoroid-dashboard_forOLD)"))
        }

        val requestBuilder = { path: String ->
            val b = Request.Builder()
                .url("https://api.github.com/repos/$cleanRepo/$path")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "SmartDashboard-AndroidApp")
            if (!token.isNullOrBlank()) {
                b.header("Authorization", "Bearer ${token.trim()}")
            }
            b.build()
        }

        try {
            // 1. Try /releases/latest
            val respLatest = httpClient.newCall(requestBuilder("releases/latest")).execute()
            val codeLatest = respLatest.code
            val bodyLatest = respLatest.body?.string()

            val jsonToParse = if (respLatest.isSuccessful && !bodyLatest.isNullOrEmpty()) {
                JSONObject(bodyLatest)
            } else if (codeLatest == 404) {
                // 2. Fallback to /releases?per_page=1
                val respList = httpClient.newCall(requestBuilder("releases?per_page=1")).execute()
                val codeList = respList.code
                val bodyList = respList.body?.string()

                if (respList.isSuccessful && !bodyList.isNullOrEmpty()) {
                    val array = org.json.JSONArray(bodyList)
                    if (array.length() > 0) {
                        array.getJSONObject(0)
                    } else {
                        // Repository exists, but 0 releases published
                        return@withContext Result.success(null)
                    }
                } else if (codeList == 404) {
                    return@withContext Result.failure(Exception("GitHubリポジトリ '$cleanRepo' が見つかりませんでした (404 Not Found)"))
                } else {
                    val errMsg = when (codeList) {
                        403 -> "GitHub APIの利用レート制限に達しました。時間を置いてから再度お試しください (403 Forbidden)"
                        else -> "GitHub からの取得に失敗しました (HTTP $codeList)"
                    }
                    return@withContext Result.failure(Exception(errMsg))
                }
            } else {
                val errMsg = when (codeLatest) {
                    403 -> "GitHub APIの利用レート制限に達しました。時間を置いてから再度お試しください (403 Forbidden)"
                    else -> "GitHub からの取得に失敗しました (HTTP $codeLatest)"
                }
                return@withContext Result.failure(Exception(errMsg))
            }

            val tagName = jsonToParse.optString("tag_name", "")
            val version = tagName.removePrefix("v").removePrefix("V").trim()
            val name = jsonToParse.optString("name", tagName).ifBlank { tagName }
            val body = jsonToParse.optString("body", "")
            val publishedAt = jsonToParse.optString("published_at", "")
            val htmlUrl = jsonToParse.optString("html_url", "https://github.com/$cleanRepo/releases")

            var apkAsset: GitHubReleaseAsset? = null
            val assetsArray = jsonToParse.optJSONArray("assets")
            if (assetsArray != null) {
                for (i in 0 until assetsArray.length()) {
                    val assetObj = assetsArray.getJSONObject(i)
                    val assetName = assetObj.optString("name", "")
                    if (assetName.endsWith(".apk", ignoreCase = true)) {
                        apkAsset = GitHubReleaseAsset(
                            name = assetName,
                            size = assetObj.optLong("size", 0L),
                            downloadUrl = assetObj.optString("browser_download_url", ""),
                            contentType = assetObj.optString("content_type", "application/vnd.android.package-archive")
                        )
                        break
                    }
                }
            }

            Result.success(
                GitHubReleaseInfo(
                    tagName = tagName,
                    version = version,
                    name = name,
                    body = body,
                    publishedAt = publishedAt,
                    htmlUrl = htmlUrl,
                    apkAsset = apkAsset
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching GitHub release for $cleanRepo", e)
            Result.failure(e)
        }
    }

    /**
     * Download the APK asset from the specified URL with streaming progress feedback.
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        targetFileName: String,
        onProgress: (bytesDownloaded: Long, totalBytes: Long, progressFraction: Float, speedBps: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val downloadDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir,
            "updates"
        )
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }

        val apkFile = File(downloadDir, targetFileName)
        val tempFile = File(downloadDir, "$targetFileName.download")

        val request = Request.Builder()
            .url(downloadUrl)
            .header("User-Agent", "SmartDashboard-AndroidApp")
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("ダウンロードに失敗しました (HTTP ${response.code})"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("レスポンスデータが空です"))
            val totalBytes = body.contentLength()

            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null

            try {
                inputStream = body.byteStream()
                outputStream = FileOutputStream(tempFile)

                val buffer = ByteArray(16 * 1024)
                var bytesRead: Int
                var totalRead = 0L
                var lastTime = System.currentTimeMillis()
                var bytesSinceLastTime = 0L
                var speedBps = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    bytesSinceLastTime += bytesRead

                    val now = System.currentTimeMillis()
                    val duration = now - lastTime
                    if (duration >= 300) {
                        speedBps = (bytesSinceLastTime * 1000) / duration
                        lastTime = now
                        bytesSinceLastTime = 0L

                        val progressFraction = if (totalBytes > 0) {
                            (totalRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        onProgress(totalRead, totalBytes, progressFraction, speedBps)
                    }
                }

                outputStream.flush()
                // Final progress notification
                onProgress(totalRead, totalBytes, 1.0f, speedBps)

                // Swap temp file to target file
                if (apkFile.exists()) {
                    apkFile.delete()
                }
                if (!tempFile.renameTo(apkFile)) {
                    tempFile.copyTo(apkFile, overwrite = true)
                    tempFile.delete()
                }

                Result.success(apkFile)
            } finally {
                try { inputStream?.close() } catch (_: Exception) {}
                try { outputStream?.close() } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading APK from $downloadUrl", e)
            if (tempFile.exists()) {
                tempFile.delete()
            }
            Result.failure(e)
        }
    }

    /**
     * Launch the Android Package Installer for the downloaded APK via FileProvider.
     */
    fun installApk(context: Context, apkFile: File): Result<Boolean> {
        if (!apkFile.exists() || apkFile.length() <= 0) {
            return Result.failure(IllegalStateException("インストール対象のAPKファイルが存在しません: ${apkFile.absolutePath}"))
        }

        try {
            // Check unknown app sources permission on Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                    return Result.failure(Exception("「不明なアプリのインストール」の許可が必要です。設定画面を開きましたので、許可してから再度お試しください。"))
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(installIntent)
            return Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer for ${apkFile.name}", e)
            return Result.failure(e)
        }
    }
}
