package com.zdevlops.cloachy.injector

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Base64
import com.zdevlops.cloachy.data.FflagRepository
import com.zdevlops.cloachy.model.EnvironmentStatus
import com.zdevlops.cloachy.model.InjectorEngineMode
import com.zdevlops.cloachy.model.LaunchResult
import com.zdevlops.cloachy.model.UpdatePolicyMode
import rikka.shizuku.Shizuku
import java.io.File
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object ShizukuRobloxManager {

    const val ROBLOX_PACKAGE = "com.roblox.client"
    const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    const val SHIZUKU_REQ_CODE = 1042

    fun checkEnvironment(context: Context): EnvironmentStatus {
        val pm = context.packageManager

        var robloxInstalled = false
        var robloxVersion = "Tidak terpasang"
        try {
            @Suppress("DEPRECATION")
            val pkgInfo = pm.getPackageInfo(ROBLOX_PACKAGE, 0)
            robloxInstalled = true
            robloxVersion = "v${pkgInfo.versionName ?: "Terpasang"}"
        } catch (_: Exception) {
        }

        var shizukuInstalled = false
        try {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(SHIZUKU_PACKAGE, 0)
            shizukuInstalled = true
        } catch (_: Exception) {
        }

        val usbDebug = try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        } catch (_: Exception) {
            false
        }

        val wirelessDebug = try {
            Settings.Global.getInt(context.contentResolver, "adb_wifi_enabled", 0) == 1
        } catch (_: Exception) {
            false
        }

        val shizukuRunning = try {
            Shizuku.pingBinder()
        } catch (_: Exception) {
            false
        }

        val shizukuGranted = if (shizukuRunning) {
            try {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } catch (_: Exception) {
                false
            }
        } else {
            false
        }

        return EnvironmentStatus(
            isRobloxInstalled = robloxInstalled,
            robloxVersionName = robloxVersion,
            isUsbDebuggingEnabled = usbDebug,
            isWirelessDebuggingEnabled = wirelessDebug,
            isShizukuInstalled = shizukuInstalled,
            isShizukuRunning = shizukuRunning,
            isShizukuGranted = shizukuGranted
        )
    }

    fun openDeveloperOptions(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            try {
                val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun openShizukuApp(context: Context): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            true
        } else {
            false
        }
    }

    fun requestShizukuPermission(): Boolean {
        return try {
            if (Shizuku.pingBinder()) {
                Shizuku.requestPermission(SHIZUKU_REQ_CODE)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun runShizukuShellCommand(command: String): Pair<Int, String> {
        return try {
            if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                return -1 to "Shizuku belum aktif atau izin belum diberikan"
            }
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            exitCode to (output.ifBlank { error })
        } catch (e: Exception) {
            -1 to (e.message ?: "Gagal menjalankan perintah Shizuku")
        }
    }

    /**
     * Menyuntikkan ClientAppSettings.json & mengunci GlobalBasicSettings_13.xml
     * di external maupun internal Roblox serta mengunci direktori dengan chmod 555
     * agar slider grafik bawaan di dalam game Roblox tidak bisa menimpa hasil Cloachy.
     */
    fun applyAndLockGraphicsSilently(
        context: Context,
        jsonContent: String,
        graphicsLevel: Int,
        forceStopRoblox: Boolean = false
    ): Pair<Boolean, String> {
        FflagRepository.exportClientAppSettings(context, jsonContent)
        val env = checkEnvironment(context)
        if (!env.isShizukuRunning || !env.isShizukuGranted) {
            return false to "Shizuku belum aktif"
        }

        val clamped = graphicsLevel.coerceIn(1, 21)
        val encodedJson = Base64.encodeToString(jsonContent.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val extFiles = "/storage/emulated/0/Android/data/$ROBLOX_PACKAGE/files"
        val extClientDir = "$extFiles/ClientSettings"
        val extClientFile = "$extClientDir/ClientAppSettings.json"
        val extXml = "$extFiles/GlobalBasicSettings_13.xml"

        val shellCmd = buildString {
            if (forceStopRoblox) {
                append("am force-stop $ROBLOX_PACKAGE; ")
            }
            // Buka izin tulis sementara pada direktori & file
            append("chmod 777 "$extClientDir" 2>/dev/null || true; ")
            append("chmod 666 "$extClientFile" "$extXml" 2>/dev/null || true; ")
            append("mkdir -p "$extClientDir" && ")
            append("echo "$encodedJson" | base64 -d > "$extClientFile"; ")

            // Ubah nilai GraphicsQualityLevel & SavedQualityLevel di GlobalBasicSettings_13.xml
            append("if [ -f "$extXml" ]; then ")
            append("sed -i 's/<int name="GraphicsQualityLevel">[^<]*<\/int>/<int name="GraphicsQualityLevel">$clamped<\/int>/g' "$extXml"; ")
            append("sed -i 's/<int name="SavedQualityLevel">[^<]*<\/int>/<int name="SavedQualityLevel">$clamped<\/int>/g' "$extXml"; ")
            append("fi; ")

            // Kunci file (444) dan folder induk (555) agar anti-atomic rename dari dalam game
            append("chmod 444 "$extClientFile" "$extXml" 2>/dev/null || true; ")
            append("chmod 555 "$extClientDir" 2>/dev/null || true; ")

            // Jika perangkat/Shizuku punya akses ke /data/data (root/debuggable), sinkronkan juga ke internal
            append("INT_DIR="/data/data/$ROBLOX_PACKAGE/files"; ")
            append("if [ -d "\$INT_DIR" ]; then ")
            append("chmod 777 "\$INT_DIR/ClientSettings" 2>/dev/null || true; ")
            append("chmod 666 "\$INT_DIR/ClientSettings/ClientAppSettings.json" "\$INT_DIR/GlobalBasicSettings_13.xml" 2>/dev/null || true; ")
            append("mkdir -p "\$INT_DIR/ClientSettings" && echo "$encodedJson" | base64 -d > "\$INT_DIR/ClientSettings/ClientAppSettings.json"; ")
            append("if [ -f "\$INT_DIR/GlobalBasicSettings_13.xml" ]; then ")
            append("sed -i 's/<int name="GraphicsQualityLevel">[^<]*<\/int>/<int name="GraphicsQualityLevel">$clamped<\/int>/g' "\$INT_DIR/GlobalBasicSettings_13.xml"; ")
            append("sed -i 's/<int name="SavedQualityLevel">[^<]*<\/int>/<int name="SavedQualityLevel">$clamped<\/int>/g' "\$INT_DIR/GlobalBasicSettings_13.xml"; ")
            append("fi; ")
            append("chmod 444 "\$INT_DIR/ClientSettings/ClientAppSettings.json" "\$INT_DIR/GlobalBasicSettings_13.xml" 2>/dev/null || true; ")
            append("chmod 555 "\$INT_DIR/ClientSettings" 2>/dev/null || true; ")
            append("fi")
        }

        val (code, err) = runShizukuShellCommand(shellCmd)
        return if (code == 0) {
            true to "Grafik Lv. $clamped diterapkan & dikunci otomatis di balik layar"
        } else {
            false to err
        }
    }

    fun getRobloxCacheSizeFormatted(): String {
        val extCache = "/storage/emulated/0/Android/data/$ROBLOX_PACKAGE/cache"
        val (code, out) = runShizukuShellCommand("du -sk "$extCache" 2>/dev/null | cut -f1")
        val kb = out.trim().toLongOrNull() ?: 0L
        return when {
            kb <= 0L -> "0 MB (Bersih)"
            kb < 1024L -> "$kb KB"
            kb < 1024L * 1024L -> String.format(Locale.US, "%.1f MB", kb / 1024.0)
            else -> String.format(Locale.US, "%.2f GB", kb / (1024.0 * 1024.0))
        }
    }

    /**
     * Membersihkan hanya cache sementara & shader Roblox tanpa menyentuh file akun login.
     */
    fun cleanRobloxCacheSafely(): Pair<Boolean, String> {
        val extCache = "/storage/emulated/0/Android/data/$ROBLOX_PACKAGE/cache"
        val extHttp = "/storage/emulated/0/Android/data/$ROBLOX_PACKAGE/files/http"
        val cmd = buildString {
            append("rm -rf "$extCache"/* "$extHttp"/* 2>/dev/null || true; ")
            append("INT_CACHE="/data/data/$ROBLOX_PACKAGE/cache"; ")
            append("if [ -d "\$INT_CACHE" ]; then rm -rf "\$INT_CACHE"/* 2>/dev/null || true; fi")
        }
        val (code, err) = runShizukuShellCommand(cmd)
        return if (code == 0) {
            true to "Cache shader & HTTP Roblox berhasil dibersihkan (akun tetap aman)"
        } else {
            false to "Aktifkan Shizuku untuk membersihkan cache Roblox ($err)"
        }
    }

    fun buildPatchedApkDraft(context: Context, jsonContent: String): File? {
        return try {
            @Suppress("DEPRECATION")
            val appInfo = context.packageManager.getApplicationInfo(ROBLOX_PACKAGE, 0)
            val sourceApk = File(appInfo.sourceDir)
            if (!sourceApk.exists()) return null

            val patchDir = File(context.cacheDir, "patched_roblox")
            if (!patchDir.exists()) patchDir.mkdirs()
            val outApk = File(patchDir, "Roblox-Cloachy-Patched.apk")

            ZipFile(sourceApk).use { zipIn ->
                ZipOutputStream(outApk.outputStream().buffered()).use { zipOut ->
                    val entries = zipIn.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (entry.name == "assets/ClientSettings/ClientAppSettings.json") continue
                        if (entry.name.startsWith("META-INF/") && (entry.name.endsWith(".SF") || entry.name.endsWith(".RSA") || entry.name.endsWith(".DSA"))) {
                            continue
                        }
                        val newEntry = ZipEntry(entry.name)
                        zipOut.putNextEntry(newEntry)
                        if (!entry.isDirectory) {
                            zipIn.getInputStream(entry).use { it.copyTo(zipOut) }
                        }
                        zipOut.closeEntry()
                    }
                    val fflagEntry = ZipEntry("assets/ClientSettings/ClientAppSettings.json")
                    zipOut.putNextEntry(fflagEntry)
                    zipOut.write(jsonContent.toByteArray(Charsets.UTF_8))
                    zipOut.closeEntry()
                }
            }
            outApk
        } catch (_: Exception) {
            null
        }
    }

    fun injectAndLaunchRoblox(
        context: Context,
        jsonContent: String,
        graphicsLevel: Int,
        engineMode: InjectorEngineMode,
        updatePolicy: UpdatePolicyMode
    ): LaunchResult {
        val env = checkEnvironment(context)
        val localSaved = FflagRepository.exportClientAppSettings(context, jsonContent)

        if (!env.isRobloxInstalled) {
            return LaunchResult(
                success = false,
                methodUsed = "Belum Terpasang",
                message = "Aplikasi Roblox (com.roblox.client) belum terpasang. JSON disimpan ke ${localSaved.name}."
            )
        }

        var methodDesc = "Local Fallback"
        var injectStatusNote = ""

        if (env.isShizukuRunning && env.isShizukuGranted) {
            val (ok, msg) = applyAndLockGraphicsSilently(
                context = context,
                jsonContent = jsonContent,
                graphicsLevel = graphicsLevel,
                forceStopRoblox = true
            )
            if (ok) {
                methodDesc = if (engineMode == InjectorEngineMode.HYBRID) "Hybrid (Shizuku Locked)" else "Shizuku Direct"
                injectStatusNote = msg
            } else {
                if (engineMode == InjectorEngineMode.HYBRID || engineMode == InjectorEngineMode.APK_PATCHER) {
                    val patched = buildPatchedApkDraft(context, jsonContent)
                    methodDesc = if (patched != null) "Hybrid Patcher Ready" else "Local Export"
                    injectStatusNote = "Shizuku write terbatas ($msg); paket patch siap di cache"
                } else {
                    injectStatusNote = "Gagal tulis Shizuku: $msg"
                }
            }
        } else if (engineMode == InjectorEngineMode.APK_PATCHER) {
            val patched = buildPatchedApkDraft(context, jsonContent)
            methodDesc = if (patched != null) "APK Patcher" else "Local Export"
            injectStatusNote = if (patched != null) "Paket APK ter-patch dibuat (${patched.name})" else "Gagal membaca base APK"
        } else {
            methodDesc = "Local Export (Shizuku Off)"
            injectStatusNote = "Aktifkan Shizuku di About & Update untuk auto-inject & kunci grafik"
        }

        val policyTag = if (updatePolicy == UpdatePolicyMode.SKIP_UPDATE) "Abaikan Update" else "Auto-Update"
        val launchIntent = context.packageManager.getLaunchIntentForPackage(ROBLOX_PACKAGE)
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            LaunchResult(
                success = true,
                methodUsed = "$methodDesc · $policyTag",
                message = "Meluncurkan Roblox ($injectStatusNote)"
            )
        } else {
            LaunchResult(
                success = false,
                methodUsed = methodDesc,
                message = "Intent peluncuran Roblox tidak ditemukan."
            )
        }
    }
}
