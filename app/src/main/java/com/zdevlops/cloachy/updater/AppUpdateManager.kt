package com.zdevlops.cloachy.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.zdevlops.cloachy.BuildConfig
import com.zdevlops.cloachy.injector.ShizukuRobloxManager
import com.zdevlops.cloachy.model.AppUpdateInfo
import com.zdevlops.cloachy.model.AppUpdateStage
import com.zdevlops.cloachy.monitor.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object AppUpdateManager {

    const val DEFAULT_GITHUB_REPO = "Zdevlops/CLOACHY"
    private const val PREFS_NAME = "cloachy_updater_prefs"
    private const val KEY_AUTO_CHECK = "auto_check_app_update"
    private const val KEY_REPO_SLUG = "github_repo_slug"
    private const val KEY_LAST_NOTIFIED_VER = "last_notified_version"

    fun getCurrentVersion(): String {
        return BuildConfig.VERSION_NAME.removeSuffix("-debug")
    }

    fun isAutoCheckEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_CHECK, true)
    }

    fun setAutoCheckEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_CHECK, enabled)
            .apply()
    }

    fun getRepoSlug(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_REPO_SLUG, DEFAULT_GITHUB_REPO) ?: DEFAULT_GITHUB_REPO
    }

    fun setRepoSlug(context: Context, slug: String) {
        val cleaned = slug.trim().ifEmpty { DEFAULT_GITHUB_REPO }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_REPO_SLUG, cleaned)
            .apply()
    }

    fun isNewerVersion(latest: String, current: String): Boolean {
        val cleanLatest = latest.trim().removePrefix("v").removePrefix("V").substringBefore("-")
        val cleanCurrent = current.trim().removePrefix("v").removePrefix("V").substringBefore("-")

        val lParts = cleanLatest.split(".").map { it.toIntOrNull() ?: 0 }
        val cParts = cleanCurrent.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(lParts.size, cParts.size)

        for (i in 0 until maxLen) {
            val l = lParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    suspend fun checkForUpdates(
        context: Context,
        repoSlug: String,
        notifyIfAvailable: Boolean = true
    ): AppUpdateInfo = withContext(Dispatchers.IO) {
        val currentVer = getCurrentVersion()
        val cleanRepo = repoSlug.trim().removePrefix("https://github.com/").trim('/')

        try {
            val apiUrl = URL("https://api.github.com/repos/$cleanRepo/releases/latest")
            val conn = (apiUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "BLOXMOD-MOBILE/$currentVer")
            }

            val code = conn.responseCode
            if (code == 404) {
                return@withContext AppUpdateInfo(
                    currentVersion = currentVer,
                    latestVersion = currentVer,
                    stage = AppUpdateStage.UP_TO_DATE,
                    statusMessage = "v$currentVer (Release) adalah versi terbaru"
                )
            }
            if (code !in 200..299) {
                return@withContext AppUpdateInfo(
                    currentVersion = currentVer,
                    latestVersion = currentVer,
                    stage = AppUpdateStage.ERROR,
                    statusMessage = "Server rilis merespons HTTP $code"
                )
            }

            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val tagName = json.optString("tag_name", currentVer).removePrefix("v").removePrefix("V")
            val releaseNotes = json.optString("body", "Peningkatan performa & stabilitas FFlag.")
                .ifBlank { "Pembaruan fitur & optimalisasi CLOACHY." }
            val publishedAt = json.optString("published_at", "").take(10)

            var apkUrl = ""
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }
            }
            if (apkUrl.isEmpty()) {
                apkUrl = json.optString("html_url", "")
            }

            if (isNewerVersion(tagName, currentVer)) {
                if (notifyIfAvailable) {
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val lastNotified = prefs.getString(KEY_LAST_NOTIFIED_VER, "")
                    if (lastNotified != tagName) {
                        prefs.edit().putString(KEY_LAST_NOTIFIED_VER, tagName).apply()
                        NotificationHelper.sendAppUpdateNotification(context, tagName, releaseNotes)
                    }
                }
                AppUpdateInfo(
                    currentVersion = currentVer,
                    latestVersion = tagName,
                    releaseNotes = releaseNotes,
                    downloadUrl = apkUrl,
                    publishedAt = publishedAt,
                    stage = AppUpdateStage.UPDATE_AVAILABLE,
                    statusMessage = "Update v$tagName tersedia untuk dipasang"
                )
            } else {
                AppUpdateInfo(
                    currentVersion = currentVer,
                    latestVersion = tagName,
                    releaseNotes = releaseNotes,
                    downloadUrl = apkUrl,
                    publishedAt = publishedAt,
                    stage = AppUpdateStage.UP_TO_DATE,
                    statusMessage = "Sudah versi terbaru (v$currentVer Release)"
                )
            }
        } catch (e: Exception) {
            AppUpdateInfo(
                currentVersion = currentVer,
                latestVersion = currentVer,
                stage = AppUpdateStage.UP_TO_DATE,
                statusMessage = "v$currentVer Release · Mode Offline / Siap Cek Ulang"
            )
        }
    }

    suspend fun downloadAndInstallUpdate(
        context: Context,
        updateInfo: AppUpdateInfo,
        onProgress: (Int) -> Unit
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val urlStr = updateInfo.downloadUrl
        if (urlStr.isBlank()) {
            return@withContext false to "Tautan unduhan APK rilis belum tersedia."
        }

        if (!urlStr.endsWith(".apk", ignoreCase = true) && !urlStr.contains("/releases/download/")) {
            return@withContext try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(urlStr)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
                true to "Membuka halaman rilis CLOACHY v${updateInfo.latestVersion}"
            } catch (e: Exception) {
                false to "Gagal membuka tautan rilis: ${e.message}"
            }
        }

        try {
            val updatesDir = File(context.cacheDir, "updates")
            if (!updatesDir.exists()) updatesDir.mkdirs()
            val apkFile = File(updatesDir, "Cloachy-v${updateInfo.latestVersion}.apk")
            if (apkFile.exists()) apkFile.delete()

            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 20000
                instanceFollowRedirects = true
            }
            conn.connect()

            val totalBytes = conn.contentLength
            var downloadedBytes = 0L

            conn.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        if (totalBytes > 0) {
                            val pct = ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(1, 99)
                            onProgress(pct)
                        }
                    }
                }
            }
            onProgress(100)

            installApkFile(context, apkFile)
        } catch (e: Exception) {
            false to "Gagal mengunduh update: ${e.message ?: "Koneksi terputus"}"
        }
    }

    fun installApkFile(context: Context, apkFile: File): Pair<Boolean, String> {
        if (!apkFile.exists()) {
            return false to "File APK update tidak ditemukan."
        }

        // 1. Jika Shizuku aktif & diizinkan, coba silent install langsung via pm install -r
        val env = ShizukuRobloxManager.checkEnvironment(context)
        if (env.isShizukuRunning && env.isShizukuGranted) {
            val (code, out) = ShizukuRobloxManager.runShizukuShellCommand("pm install -r \"${apkFile.absolutePath}\"")
            if (code == 0) {
                return true to "CLOACHY berhasil diperbarui otomatis via Shizuku!"
            }
        }

        // 2. Fallback ke In-App Package Installer Android (FileProvider)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            try {
                val permIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(permIntent)
                return false to "Izinkan 'Pasang aplikasi tidak dikenal' untuk CLOACHY, lalu tekan Update lagi."
            } catch (_: Exception) {
            }
        }

        return try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(installIntent)
            true to "Membuka pemasang update CLOACHY di dalam aplikasi..."
        } catch (e: Exception) {
            false to "Gagal memulai pemasang APK: ${e.message}"
        }
    }
}
