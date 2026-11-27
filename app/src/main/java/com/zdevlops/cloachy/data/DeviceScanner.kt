package com.zdevlops.cloachy.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.view.WindowManager
import com.zdevlops.cloachy.model.DeviceSpecs
import kotlin.math.roundToInt

object DeviceScanner {

    fun scanDevice(context: Context): DeviceSpecs {
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(4)

        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)
        val totalRamGb = if (memInfo.totalMem > 0L) {
            (memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)).roundToInt().coerceAtLeast(2)
        } else {
            8
        }

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        @Suppress("DEPRECATION")
        val hz = wm?.defaultDisplay?.refreshRate?.roundToInt()?.coerceAtLeast(60) ?: 120

        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val rawTemp = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 360) ?: 360
        val tempC = (rawTemp / 10f).coerceIn(25f, 55f)

        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        val fullModel = if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"

        val socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL.takeIf { it.isNotBlank() && it != Build.UNKNOWN } ?: Build.HARDWARE
        } else {
            Build.HARDWARE
        }

        val tier = when {
            totalRamGb <= 4 || cores <= 4 -> "Low-End Tier"
            totalRamGb <= 6 -> "Mid-Range Tier"
            else -> "High-End Tier"
        }

        return DeviceSpecs(
            modelName = fullModel,
            socInfo = "$socModel · Android ${Build.VERSION.RELEASE}",
            chipShort = socModel.take(12),
            tierBadge = tier,
            cpuCores = cores,
            ramGb = totalRamGb,
            refreshRateHz = hz,
            batteryTempC = tempC,
            isScanning = false
        )
    }
}
