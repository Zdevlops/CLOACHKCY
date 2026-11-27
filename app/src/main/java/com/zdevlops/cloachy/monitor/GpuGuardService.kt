package com.zdevlops.cloachy.monitor

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.zdevlops.cloachy.data.FflagRepository
import com.zdevlops.cloachy.injector.ShizukuRobloxManager
import com.zdevlops.cloachy.model.GpuAlertLevel
import com.zdevlops.cloachy.model.LightingTechOption
import com.zdevlops.cloachy.model.TextureQualityOption
import kotlinx.coroutines.*

class GpuGuardService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastDowngradeTs = 0L
    private var lastLockVerifyTs = 0L

    override fun onCreate() {
        super.onCreate()
        startForeground(
            NotificationHelper.SERVICE_NOTIF_ID,
            NotificationHelper.buildServiceNotification(this, "Menjaga kunci grafik & telemetri Roblox...")
        )
        startMonitoringLoop()
    }

    private fun startMonitoringLoop() {
        serviceScope.launch {
            while (isActive) {
                delay(2500L)
                checkScheduledCacheReset()

                val prefs = getSharedPreferences("cloachy_guard_prefs", Context.MODE_PRIVATE)
                val guardEnabled = prefs.getBoolean("gpu_auto_guard_enabled", true)
                val telemetry = SystemTelemetryMonitor.sampleLiveTelemetry(this@GpuGuardService, null)
                if (!telemetry.isRobloxActive) continue

                val now = System.currentTimeMillis()

                // Watchdog: setiap 6 detik pastikan kunci grafik & FFlag tidak tertimpa saat in-game
                if (now - lastLockVerifyTs >= 6000L) {
                    lastLockVerifyTs = now
                    val settings = FflagRepository.loadEngineSettings(this@GpuGuardService)
                    val custom = FflagRepository.loadCustomFlags(this@GpuGuardService)
                    val json = FflagRepository.buildCombinedJson(settings, custom)
                    ShizukuRobloxManager.applyAndLockGraphicsSilently(
                        context = this@GpuGuardService,
                        jsonContent = json,
                        graphicsLevel = settings.graphicsQuality,
                        forceStopRoblox = false
                    )
                }

                if (!guardEnabled) continue
                if (now - lastDowngradeTs < 20_000L) continue

                if (telemetry.gpuAlertLevel == GpuAlertLevel.MAX_100) {
                    lastDowngradeTs = now
                    applyAutoDowngrade(isMax100 = true, gpuPercent = telemetry.gpuLoadPercent)
                } else if (telemetry.gpuAlertLevel == GpuAlertLevel.HIGH_80) {
                    lastDowngradeTs = now
                    applyAutoDowngrade(isMax100 = false, gpuPercent = telemetry.gpuLoadPercent)
                }
            }
        }
    }

    private fun checkScheduledCacheReset() {
        val cachePrefs = getSharedPreferences("cloachy_cache_prefs", Context.MODE_PRIVATE)
        val enabled = cachePrefs.getBoolean("auto_reset_cache_enabled", true)
        if (!enabled) return

        val intervalDays = cachePrefs.getInt("cache_reset_interval_days", 7).coerceAtLeast(1)
        val lastCleanTs = cachePrefs.getLong("last_cache_clean_ts", 0L)
        val now = System.currentTimeMillis()
        val intervalMs = intervalDays * 24L * 60L * 60L * 1000L

        if (lastCleanTs == 0L) {
            cachePrefs.edit().putLong("last_cache_clean_ts", now).apply()
            return
        }

        if (now - lastCleanTs >= intervalMs) {
            val (ok, _) = ShizukuRobloxManager.cleanRobloxCacheSafely()
            if (ok) {
                cachePrefs.edit().putLong("last_cache_clean_ts", now).apply()
            }
        }
    }

    private fun applyAutoDowngrade(isMax100: Boolean, gpuPercent: Int) {
        val currentSettings = FflagRepository.loadEngineSettings(this)
        val customFlags = FflagRepository.loadCustomFlags(this)
        val adjustedSettings = if (isMax100) {
            currentSettings.copy(
                graphicsQuality = 1,
                textureQuality = TextureQualityOption.LOW,
                lightingTechnology = LightingTechOption.VOXEL,
                shadows = false,
                particlesAndSmoke = false,
                postProcessingEffects = false
            )
        } else {
            currentSettings.copy(
                graphicsQuality = currentSettings.graphicsQuality.coerceAtMost(8),
                textureQuality = TextureQualityOption.MEDIUM,
                shadows = false
            )
        }

        FflagRepository.saveEngineSettings(this, adjustedSettings)
        val json = FflagRepository.buildCombinedJson(adjustedSettings, customFlags)
        ShizukuRobloxManager.applyAndLockGraphicsSilently(
            context = this,
            jsonContent = json,
            graphicsLevel = adjustedSettings.graphicsQuality,
            forceStopRoblox = false
        )

        val summaryText = if (isMax100) {
            "GPU mencapai 100%. Grafik otomatis diturunkan ke Level 1 (Voxel · Shadows False)."
        } else {
            "GPU tinggi (${gpuPercent}%). Grafik otomatis disesuaikan ke Level ${adjustedSettings.graphicsQuality}."
        }

        getSharedPreferences("cloachy_guard_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("last_auto_downgrade_msg", summaryText)
            .putInt("last_auto_downgrade_level", adjustedSettings.graphicsQuality)
            .putLong("last_auto_downgrade_ts", System.currentTimeMillis())
            .apply()

        NotificationHelper.sendGpuAutoDowngradeAlert(
            context = this,
            gpuPercent = gpuPercent,
            isMax100 = isMax100,
            actionDetail = summaryText
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
