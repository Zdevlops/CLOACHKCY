package com.zdevlops.cloachy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zdevlops.cloachy.model.*
import com.zdevlops.cloachy.ui.components.TrueFalseSelector
import com.zdevlops.cloachy.ui.theme.*

@Composable
fun ProfilScreen(
    envStatus: EnvironmentStatus,
    engineMode: InjectorEngineMode,
    updatePolicy: UpdatePolicyMode,
    sessionHistory: List<SessionRecord>,
    appUpdateInfo: AppUpdateInfo,
    autoCheckAppUpdate: Boolean,
    repoSlug: String,
    onToggleAutoCheckAppUpdate: (Boolean) -> Unit,
    onRepoSlugChange: (String) -> Unit,
    onCheckAppUpdateNow: () -> Unit,
    onInstallAppUpdateNow: () -> Unit,
    onOpenDeveloperOptions: () -> Unit,
    onRequestShizukuPermission: () -> Unit,
    onOpenShizukuApp: () -> Unit,
    onRefreshStatus: () -> Unit,
    onEngineModeChange: (InjectorEngineMode) -> Unit,
    onUpdatePolicyChange: (UpdatePolicyMode) -> Unit
) {
    val engineModes = remember { InjectorEngineMode.values().toList() }
    val updatePolicies = remember { UpdatePolicyMode.values().toList() }
    var showRepoConfig by remember { mutableStateOf(false) }
    var editableRepo by remember(repoSlug) { mutableStateOf(repoSlug) }

    val hasUpdate = appUpdateInfo.stage == AppUpdateStage.UPDATE_AVAILABLE ||
            appUpdateInfo.stage == AppUpdateStage.DOWNLOADING ||
            appUpdateInfo.stage == AppUpdateStage.READY_TO_INSTALL

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // 1. AUTO-UPDATE APLIKASI CLOACHY
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (hasUpdate) Modifier.frostedWarnCard(24) else Modifier.frostedGlassCard(24))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (hasUpdate) Color(0x40F59E0B) else Color.White.copy(alpha = 0.15f))
                            .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(11.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (hasUpdate) Icons.Rounded.SystemUpdate else Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = if (hasUpdate) Color(0xFFFDE68A) else Color(0xFF6EE7B7),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "ABOUT & UPDATE · @Zdevlops",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = AmberGlow
                        )
                        Text(
                            text = "CLOACHY v${appUpdateInfo.currentVersion} (Release)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (hasUpdate) Color(0x4DF59E0B) else Color(0x336EE7B7))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when (appUpdateInfo.stage) {
                            AppUpdateStage.CHECKING -> "MEMERIKSA..."
                            AppUpdateStage.UPDATE_AVAILABLE -> "v${appUpdateInfo.latestVersion} BARU"
                            AppUpdateStage.DOWNLOADING -> "UNDUH ${appUpdateInfo.downloadProgress}%"
                            AppUpdateStage.READY_TO_INSTALL -> "SIAP PASANG"
                            else -> "v${appUpdateInfo.currentVersion}"
                        },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Text(
                text = appUpdateInfo.statusMessage,
                fontSize = 11.sp,
                color = Taupe100
            )

            if (hasUpdate) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.32f))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Pembaruan CLOACHY v${appUpdateInfo.latestVersion}:",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFDE68A)
                    )
                    Text(
                        text = appUpdateInfo.releaseNotes,
                        fontSize = 10.sp,
                        color = Taupe200,
                        lineHeight = 14.sp
                    )
                }

                if (appUpdateInfo.stage == AppUpdateStage.DOWNLOADING) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.35f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(appUpdateInfo.downloadProgress / 100f)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(Color(0xFFFDE68A))
                            )
                        }
                        Text(
                            text = "Mengunduh paket update... ${appUpdateInfo.downloadProgress}%",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Taupe200
                        )
                    }
                }

                Button(
                    onClick = onInstallAppUpdateNow,
                    enabled = appUpdateInfo.stage != AppUpdateStage.DOWNLOADING,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Taupe950
                    )
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (appUpdateInfo.stage == AppUpdateStage.DOWNLOADING)
                            "Mengunduh v${appUpdateInfo.latestVersion}..."
                        else
                            "Update Sekarang ke v${appUpdateInfo.latestVersion}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.14f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text("Auto-Check App Update", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text(
                        text = "Kirim notifikasi otomatis jika rilis CLOACHY baru tersedia",
                        fontSize = 10.sp,
                        color = Taupe200.copy(alpha = 0.8f)
                    )
                }
                TrueFalseSelector(value = autoCheckAppUpdate, onValueChange = onToggleAutoCheckAppUpdate)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onCheckAppUpdateNow,
                    enabled = appUpdateInfo.stage != AppUpdateStage.CHECKING && appUpdateInfo.stage != AppUpdateStage.DOWNLOADING,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.18f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (appUpdateInfo.stage == AppUpdateStage.CHECKING) "Memeriksa..." else "Cek Update",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.25f))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
                        .clickable { showRepoConfig = !showRepoConfig }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (showRepoConfig) "Tutup Repo" else "Source Repo",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Taupe100
                    )
                }
            }

            if (showRepoConfig) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        BasicTextField(
                            value = editableRepo,
                            onValueChange = { editableRepo = it },
                            singleLine = true,
                            cursorBrush = SolidColor(Color.White),
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.22f))
                            .border(1.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
                            .clickable { onRepoSlugChange(editableRepo) }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text("Simpan", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }

        // 2. AKTIVASI USB DEBUGGING & SHIZUKU
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("USB DEBUGGING & SHIZUKU", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = AmberGlow)
                    Text("Akses Inject, Kunci Grafik & FPS Asli", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
                        .clickable { onRefreshStatus() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Cek Ulang", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White)
                }
            }

            EnvStatusRow(
                title = "USB / Wireless Debugging",
                active = envStatus.isUsbDebuggingEnabled || envStatus.isWirelessDebuggingEnabled,
                activeText = "AKTIF",
                inactiveText = "MATI · PERLU DIAKTIFKAN"
            )
            EnvStatusRow(
                title = "Service Shizuku",
                active = envStatus.isShizukuRunning,
                activeText = "BERJALAN",
                inactiveText = if (envStatus.isShizukuInstalled) "TERPASANG · BELUM START" else "BELUM TERPASANG"
            )
            EnvStatusRow(
                title = "Izin CLOACHY di Shizuku",
                active = envStatus.isShizukuGranted,
                activeText = "DIIZINKAN",
                inactiveText = "BELUM DIIZINKAN"
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onOpenDeveloperOptions,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.16f), contentColor = Color.White)
                ) {
                    Text("Opsi Developer", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }

                Button(
                    onClick = {
                        if (envStatus.isShizukuRunning) onRequestShizukuPermission() else onOpenShizukuApp()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Taupe950)
                ) {
                    Text(
                        text = if (envStatus.isShizukuRunning) "Minta Izin Shizuku" else "Buka Shizuku",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 3. HYBRID ENGINE
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .frostedGlassDarkCard(18)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Hybrid Injector Engine", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            for (mode in engineModes) {
                val selected = mode == engineMode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.25f))
                        .border(1.dp, if (selected) Color.White.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                        .clickable { onEngineModeChange(mode) }
                        .padding(11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(mode.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text(mode.subtitle, fontSize = 10.sp, color = Taupe200.copy(alpha = 0.8f))
                    }
                    Text(
                        text = if (selected) "AKTIF" else "PILIH",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) AmberGlow else Taupe200
                    )
                }
            }
        }

        // 4. KEBIJAKAN UPDATE ROBLOX
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .frostedGlassCard(18)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Kebijakan Update Client Roblox", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (policy in updatePolicies) {
                    val selected = policy == updatePolicy
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) Color.White.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.10f))
                            .border(1.dp, if (selected) Color.White.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                            .clickable { onUpdatePolicyChange(policy) }
                            .padding(10.dp)
                    ) {
                        Text(policy.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text(policy.subtitle, fontSize = 9.sp, color = Taupe200.copy(alpha = 0.8f), modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }

        // 5. RIWAYAT LAUNCH
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .frostedGlassCard(18)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Riwayat Launch & Auto-Guard", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            if (sessionHistory.isEmpty()) {
                Text(
                    text = "Belum ada riwayat sesi.",
                    fontSize = 10.sp,
                    color = Taupe200.copy(alpha = 0.75f)
                )
            } else {
                for (rec in sessionHistory) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.25f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(rec.gameName, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.White)
                            Text(rec.timestampLabel, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Taupe200.copy(alpha = 0.75f))
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(rec.peakLoad, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Taupe200.copy(alpha = 0.8f))
                            Text(rec.resultSummary, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnvStatusRow(title: String, active: Boolean, activeText: String, inactiveText: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.28f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 11.sp, color = Taupe100)
        Text(
            text = if (active) activeText else inactiveText,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (active) Color(0xFF6EE7B7) else Color(0xFFFCD34D)
        )
    }
}
