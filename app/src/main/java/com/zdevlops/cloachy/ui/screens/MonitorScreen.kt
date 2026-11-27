package com.zdevlops.cloachy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zdevlops.cloachy.model.*
import com.zdevlops.cloachy.ui.components.OptionChipGroup
import com.zdevlops.cloachy.ui.components.TrueFalseSelector
import com.zdevlops.cloachy.ui.theme.*
import java.util.Locale

@Composable
fun MonitorScreen(
    deviceSpecs: DeviceSpecs,
    envStatus: EnvironmentStatus,
    telemetry: TelemetryState,
    engineSettings: EngineGraphicsSettings,
    engineMode: InjectorEngineMode,
    updatePolicy: UpdatePolicyMode,
    overlayEnabled: Boolean,
    overlayPosition: OverlayPosition,
    overlayOpacity: Int,
    hasOverlayPermission: Boolean,
    gpuAutoGuardEnabled: Boolean,
    onToggleOverlay: (Boolean) -> Unit,
    onOverlayPositionChange: (OverlayPosition) -> Unit,
    onOverlayOpacityChange: (Int) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onToggleGpuAutoGuard: (Boolean) -> Unit,
    onLaunchRoblox: () -> Unit,
    onRefreshDetection: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // 1. LAUNCH ROBLOX CARD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .frostedGlassCard(24)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CLOACHY LAUNCHER",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = AmberGlow
                    )
                    Text(
                        text = "Launch Roblox",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        text = "Graphics Quality ${engineSettings.graphicsQuality}/21 · FPS Limit ${engineSettings.framerateLimit} · Pengaturan otomatis terkunci di balik layar",
                        fontSize = 11.sp,
                        color = Taupe200.copy(alpha = 0.85f),
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (envStatus.isShizukuRunning && envStatus.isShizukuGranted)
                                Color(0x336EE7B7)
                            else
                                Color.White.copy(alpha = 0.12f)
                        )
                        .border(
                            1.dp,
                            if (envStatus.isShizukuRunning && envStatus.isShizukuGranted)
                                Color(0x666EE7B7)
                            else
                                Color.White.copy(alpha = 0.22f),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (envStatus.isShizukuRunning && envStatus.isShizukuGranted) "SHIZUKU READY" else "SHIZUKU OFF",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (envStatus.isShizukuRunning && envStatus.isShizukuGranted) Color(0xFFA7F3D0) else Taupe100
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusPill(Modifier.weight(1f), "Engine", engineMode.label)
                StatusPill(Modifier.weight(1f), "Update", updatePolicy.label)
                StatusPill(Modifier.weight(1f), "Client", envStatus.robloxVersionName)
            }

            Button(
                onClick = onLaunchRoblox,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Taupe950
                )
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Launch Roblox",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 2. TEPAT DI BAWAH LAUNCH ROBLOX: PERFORMANCE OVERLAY (100% AKURAT FPS, CPU, GPU)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .frostedGlassDarkCard(20)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "PERFORMANCE OVERLAY",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = AmberGlow
                    )
                    Text(
                        text = "Real-Time FPS · CPU · GPU",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        text = "Menampilkan FPS SurfaceFlinger, beban CPU & GPU asli saat bermain (bisa digeser di layar)",
                        fontSize = 10.sp,
                        color = Taupe200.copy(alpha = 0.8f),
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                TrueFalseSelector(
                    value = overlayEnabled,
                    onValueChange = onToggleOverlay
                )
            }

            // Preview kapsul overlay minimalis (tidak alay)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xCC0D1117))
                    .border(1.dp, Color(0x5538BDF8), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (telemetry.fps > 0) "FPS ${telemetry.fps}" else "FPS --",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFFF1F5F9)
                )
                Text("·", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Taupe200.copy(alpha = 0.5f))
                Text(
                    text = "CPU ${telemetry.cpuLoadPercent}%",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFFBAE6FD)
                )
                Text("·", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Taupe200.copy(alpha = 0.5f))
                Text(
                    text = "GPU ${telemetry.gpuLoadPercent}%",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFF7DD3FC)
                )
            }

            if (!hasOverlayPermission) {
                Button(
                    onClick = onRequestOverlayPermission,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.16f),
                        contentColor = Color.White
                    )
                ) {
                    Text("Izinkan Tampil di Atas Aplikasi Lain", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Overlay Position", fontSize = 11.sp, color = Taupe100)
                val posList = OverlayPosition.values().toList()
                OptionChipGroup(
                    options = posList.map { it.label },
                    selectedIndex = posList.indexOf(overlayPosition),
                    onSelectIndex = { onOverlayPositionChange(posList[it]) }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Overlay Opacity", fontSize = 11.sp, color = Taupe100)
                    Text("$overlayOpacity%", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = AmberGlow)
                }
                Slider(
                    value = overlayOpacity.toFloat(),
                    onValueChange = { onOverlayOpacityChange(it.toInt().coerceIn(35, 100)) },
                    valueRange = 35f..100f
                )
            }
        }

        // 3. Real-Time Background Detection & Auto GPU Guard
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .frostedGlassDarkCard(18)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (telemetry.isRobloxActive) Color(0x336EE7B7) else Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.ShowChart,
                            contentDescription = null,
                            tint = if (telemetry.isRobloxActive) Color(0xFF6EE7B7) else Taupe200,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text("Deteksi Sesi Latar Belakang", fontSize = 10.sp, color = Taupe200.copy(alpha = 0.75f))
                        Text(
                            text = telemetry.sessionStatusTitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (telemetry.isRobloxActive) Color.White else Taupe200
                        )
                    }
                }

                IconButton(onClick = onRefreshDetection, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Scan Ulang", tint = Taupe100, modifier = Modifier.size(16.dp))
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "GPU Auto-Guard",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "Otomatis beri notifikasi & turunkan grafik saat beban GPU melewati 80% atau mencapai 100%.",
                        fontSize = 10.sp,
                        color = Taupe200.copy(alpha = 0.8f),
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                TrueFalseSelector(value = gpuAutoGuardEnabled, onValueChange = onToggleGpuAutoGuard)
            }
        }

        if (telemetry.autoActionSummary != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .frostedWarnCard(18)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x40F59E0B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = Color(0xFFFDE68A), modifier = Modifier.size(16.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AKSI OTOMATIS GPU GUARD",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFDE68A)
                    )
                    Text(
                        text = telemetry.autoActionSummary,
                        fontSize = 11.sp,
                        color = Color.White,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // 4. Telemetri Hardware Real-Time 2x2
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LoadGaugeCard(
                modifier = Modifier.weight(1f),
                title = "GPU Usage",
                percent = telemetry.gpuLoadPercent,
                subLabel = telemetry.gpuSensorSource,
                alertLevel = telemetry.gpuAlertLevel
            )
            LoadGaugeCard(
                modifier = Modifier.weight(1f),
                title = "CPU Usage",
                percent = telemetry.cpuLoadPercent,
                subLabel = "${deviceSpecs.cpuCores}-Core",
                alertLevel = GpuAlertLevel.NONE
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .frostedGlassCard(18)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Real-Time FPS", fontSize = 11.sp, color = Taupe200.copy(alpha = 0.85f))
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (telemetry.fps > 0) "${telemetry.fps}" else "--",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text("FPS", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Taupe200)
                }
                Text(
                    text = String.format(Locale.US, "%.1f ms · %s", telemetry.frameTimeMs, telemetry.fpsSource),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = Taupe200.copy(alpha = 0.75f)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .frostedGlassCard(18)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Temp & RAM", fontSize = 11.sp, color = Taupe200.copy(alpha = 0.85f))
                Text(
                    text = String.format(Locale.US, "%.1f°C", telemetry.tempC),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = String.format(Locale.US, "RAM: %.1f / %.1f GB", telemetry.ramUsedGb, telemetry.ramTotalGb),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = Taupe200.copy(alpha = 0.75f)
                )
            }
        }

        // 5. Info Perangkat
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .frostedGlassCard(24)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DEVICE HARDWARE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Taupe200.copy(alpha = 0.8f)
                    )
                    Text(
                        text = deviceSpecs.modelName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        text = deviceSpecs.socInfo,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Taupe200.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = deviceSpecs.tierBadge,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HardwareChip(Modifier.weight(1f), "Chipset", deviceSpecs.chipShort)
                HardwareChip(Modifier.weight(1f), "RAM", "${deviceSpecs.ramGb} GB")
                HardwareChip(Modifier.weight(1f), "Layar", "${deviceSpecs.refreshRateHz} Hz")
                HardwareChip(Modifier.weight(1f), "Grafik", "Lv.${engineSettings.graphicsQuality}/21")
            }
        }
    }
}

@Composable
private fun StatusPill(modifier: Modifier, label: String, value: String) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.25f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(vertical = 6.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 9.sp, color = Taupe200.copy(alpha = 0.72f))
        Text(value, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.White, maxLines = 1)
    }
}

@Composable
private fun HardwareChip(modifier: Modifier, label: String, value: String) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.22f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 9.sp, color = Taupe200.copy(alpha = 0.75f))
        Text(value, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.White, maxLines = 1)
    }
}

@Composable
private fun LoadGaugeCard(
    modifier: Modifier,
    title: String,
    percent: Int,
    subLabel: String,
    alertLevel: GpuAlertLevel
) {
    val badgeText = when (alertLevel) {
        GpuAlertLevel.MAX_100 -> "100% AUTO-LOW"
        GpuAlertLevel.HIGH_80 -> ">80% AUTO-ADJ"
        GpuAlertLevel.NONE -> "AMAN"
    }
    val badgeBg = when (alertLevel) {
        GpuAlertLevel.MAX_100 -> Color(0x4DEF4444)
        GpuAlertLevel.HIGH_80 -> Color(0x4DF59E0B)
        GpuAlertLevel.NONE -> Color.White.copy(alpha = 0.10f)
    }
    val barColor = when (alertLevel) {
        GpuAlertLevel.MAX_100 -> Color(0xFFFCA5A5)
        GpuAlertLevel.HIGH_80 -> Color(0xFFFCD34D)
        GpuAlertLevel.NONE -> Color.White
    }

    Column(
        modifier = modifier
            .frostedGlassCard(18)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 11.sp, color = Taupe200.copy(alpha = 0.85f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(badgeBg)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badgeText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = Color.White
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text("$percent%", fontFamily = FontFamily.Monospace, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(subLabel, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Taupe200.copy(alpha = 0.75f), maxLines = 1)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.30f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percent / 100f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(barColor)
            )
        }
    }
}
