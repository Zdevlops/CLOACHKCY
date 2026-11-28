package com.zdevlops.cloachy

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.zdevlops.cloachy.data.DeviceScanner
import com.zdevlops.cloachy.data.FflagRepository
import com.zdevlops.cloachy.injector.ShizukuRobloxManager
import com.zdevlops.cloachy.model.*
import com.zdevlops.cloachy.monitor.GpuGuardService
import com.zdevlops.cloachy.monitor.NotificationHelper
import com.zdevlops.cloachy.monitor.PerformanceOverlayService
import com.zdevlops.cloachy.monitor.SystemTelemetryMonitor
import com.zdevlops.cloachy.ui.screens.*
import com.zdevlops.cloachy.ui.theme.*
import com.zdevlops.cloachy.updater.AppUpdateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var requestedSectionState = mutableStateOf<SidebarSection?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.ensureChannels(this)
        handleTabIntent(intent)
        setContent {
            CloachyTheme {
                CloachyApp(
                    externalSectionRequest = requestedSectionState.value,
                    onConsumeExternalSection = { requestedSectionState.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleTabIntent(intent)
    }

    private fun handleTabIntent(intent: Intent?) {
        val target = intent?.getStringExtra(NotificationHelper.EXTRA_OPEN_TAB)
        if (target == "PROFIL") {
            requestedSectionState.value = SidebarSection.ABOUT_UPDATE
        }
    }
}

@Composable
fun CloachyApp(
    externalSectionRequest: SidebarSection? = null,
    onConsumeExternalSection: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val guardPrefs = remember { context.getSharedPreferences("cloachy_guard_prefs", Context.MODE_PRIVATE) }
    val overlayPrefs = remember { context.getSharedPreferences("cloachy_overlay_prefs", Context.MODE_PRIVATE) }
    val cachePrefs = remember { context.getSharedPreferences("cloachy_cache_prefs", Context.MODE_PRIVATE) }

    var selectedSection by remember { mutableStateOf(SidebarSection.LAUNCH_HOME) }

    LaunchedEffect(externalSectionRequest) {
        if (externalSectionRequest != null) {
            selectedSection = externalSectionRequest
            onConsumeExternalSection()
        }
    }

    val deviceSpecs by remember { mutableStateOf(DeviceScanner.scanDevice(context)) }
    var envStatus by remember { mutableStateOf(ShizukuRobloxManager.checkEnvironment(context)) }
    var engineMode by remember { mutableStateOf(InjectorEngineMode.HYBRID) }
    var updatePolicy by remember { mutableStateOf(UpdatePolicyMode.SKIP_UPDATE) }

    // Engine & Graphics Settings (Bloxstrap style)
    var engineSettings by remember { mutableStateOf(FflagRepository.loadEngineSettings(context)) }
    var customFlags by remember { mutableStateOf(FflagRepository.loadCustomFlags(context)) }

    // Overlay States
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var overlayEnabled by remember { mutableStateOf(overlayPrefs.getBoolean("overlay_enabled", false)) }
    var overlayPosition by remember {
        val idx = overlayPrefs.getInt("overlay_position", OverlayPosition.TOP_LEFT.ordinal)
        mutableStateOf(OverlayPosition.values().getOrElse(idx) { OverlayPosition.TOP_LEFT })
    }
    var overlayOpacity by remember { mutableIntStateOf(overlayPrefs.getInt("overlay_opacity", 82)) }

    // Storage & Cache States
    var cacheSizeLabel by remember { mutableStateOf("Memeriksa...") }
    var autoResetCacheEnabled by remember { mutableStateOf(cachePrefs.getBoolean("auto_reset_cache_enabled", true)) }
    var cacheInterval by remember {
        val days = cachePrefs.getInt("cache_reset_interval_days", 7)
        val found = CacheIntervalOption.values().find { it.days == days } ?: CacheIntervalOption.EVERY_7_DAYS
        mutableStateOf(found)
    }
    var lastCacheCleanTs by remember { mutableLongStateOf(cachePrefs.getLong("last_cache_clean_ts", 0L)) }

    // App Self-Updater States
    val currentAppVersion = remember { AppUpdateManager.getCurrentVersion() }
    var autoCheckAppUpdate by remember { mutableStateOf(AppUpdateManager.isAutoCheckEnabled(context)) }
    var repoSlug by remember { mutableStateOf(AppUpdateManager.getRepoSlug(context)) }
    var appUpdateInfo by remember {
        mutableStateOf(
            AppUpdateInfo(
                currentVersion = currentAppVersion,
                latestVersion = currentAppVersion
            )
        )
    }

    var gpuAutoGuardEnabled by remember {
        mutableStateOf(guardPrefs.getBoolean("gpu_auto_guard_enabled", true))
    }
    var lastAutoSummary by remember {
        mutableStateOf(guardPrefs.getString("last_auto_downgrade_msg", null))
    }
    var lastHandledServiceTs by remember {
        mutableLongStateOf(guardPrefs.getLong("last_auto_downgrade_ts", 0L))
    }
    var pollTick by remember { mutableIntStateOf(0) }
    var sessionHistory by remember { mutableStateOf<List<SessionRecord>>(emptyList()) }

    fun showMessage(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun syncOverlayService(shouldRun: Boolean) {
        hasOverlayPermission = Settings.canDrawOverlays(context)
        if (shouldRun && !hasOverlayPermission) {
            overlayEnabled = false
            overlayPrefs.edit().putBoolean("overlay_enabled", false).apply()
            showMessage("Izinkan 'Tampil di atas aplikasi lain' terlebih dahulu")
            return
        }
        try {
            val intent = Intent(context, PerformanceOverlayService::class.java)
            context.stopService(intent)
            if (shouldRun) {
                context.startService(intent)
            }
        } catch (_: Exception) {
        }
    }

    LaunchedEffect(Unit) {
        if (autoCheckAppUpdate) {
            appUpdateInfo = appUpdateInfo.copy(
                stage = AppUpdateStage.CHECKING,
                statusMessage = "Memeriksa pembaruan CLOACHY..."
            )
            appUpdateInfo = AppUpdateManager.checkForUpdates(
                context = context,
                repoSlug = repoSlug,
                notifyIfAvailable = true
            )
        }
        cacheSizeLabel = withContext(Dispatchers.IO) { ShizukuRobloxManager.getRobloxCacheSizeFormatted() }
        if (overlayEnabled) {
            syncOverlayService(true)
        }
    }

    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            showMessage("Izinkan notifikasi agar info update & peringatan GPU muncul")
        }
    }

    fun startGuardServiceIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPerm) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        try {
            val intent = Intent(context, GpuGuardService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (_: Exception) {
        }
    }

    LaunchedEffect(gpuAutoGuardEnabled) {
        guardPrefs.edit().putBoolean("gpu_auto_guard_enabled", gpuAutoGuardEnabled).apply()
        if (gpuAutoGuardEnabled) {
            startGuardServiceIfNeeded()
        } else {
            try {
                context.stopService(Intent(context, GpuGuardService::class.java))
            } catch (_: Exception) {
            }
        }
    }

    DisposableEffect(Unit) {
        val permListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == ShizukuRobloxManager.SHIZUKU_REQ_CODE) {
                envStatus = ShizukuRobloxManager.checkEnvironment(context)
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    showMessage("Izin Shizuku berhasil diberikan!")
                } else {
                    showMessage("Izin Shizuku ditolak")
                }
            }
        }
        val binderListener = Shizuku.OnBinderReceivedListener {
            envStatus = ShizukuRobloxManager.checkEnvironment(context)
        }
        try {
            Shizuku.addRequestPermissionResultListener(permListener)
            Shizuku.addBinderReceivedListenerSticky(binderListener)
        } catch (_: Exception) {
        }
        onDispose {
            try {
                Shizuku.removeRequestPermissionResultListener(permListener)
                Shizuku.removeBinderReceivedListener(binderListener)
            } catch (_: Exception) {
            }
        }
    }

    var telemetry by remember { mutableStateOf(TelemetryState()) }

    LaunchedEffect(Unit) {
        while (true) {
            val sampled = withContext(Dispatchers.IO) {
                SystemTelemetryMonitor.sampleLiveTelemetry(context, lastAutoSummary)
            }
            telemetry = sampled
            pollTick++
            hasOverlayPermission = Settings.canDrawOverlays(context)

            val newEnv = withContext(Dispatchers.IO) {
                ShizukuRobloxManager.checkEnvironment(context)
            }
            envStatus = newEnv

            val serviceTs = guardPrefs.getLong("last_auto_downgrade_ts", 0L)
            if (serviceTs > lastHandledServiceTs) {
                lastHandledServiceTs = serviceTs
                lastAutoSummary = guardPrefs.getString("last_auto_downgrade_msg", null)
                engineSettings = FflagRepository.loadEngineSettings(context)
                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                val serviceLevel = guardPrefs.getInt("last_auto_downgrade_level", 1)
                sessionHistory = listOf(
                    SessionRecord(
                        gameName = "Auto GPU Guard Trigger",
                        peakLoad = "GPU ${sampled.gpuLoadPercent}% · Turun ke Lv. $serviceLevel",
                        resultSummary = lastAutoSummary ?: "Grafik disesuaikan otomatis",
                        timestampLabel = timeStr
                    )
                ) + sessionHistory.take(9)
            }
            delay(3000L)
        }
    }

    val jsonOutput = remember(engineSettings, customFlags) {
        FflagRepository.buildCombinedJson(engineSettings, customFlags)
    }
    val hasAppUpdateBadge = appUpdateInfo.stage == AppUpdateStage.UPDATE_AVAILABLE ||
            appUpdateInfo.stage == AppUpdateStage.READY_TO_INSTALL

    val lastCleanFormatted = remember(lastCacheCleanTs) {
        if (lastCacheCleanTs <= 0L) "Belum pernah"
        else SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault()).format(Date(lastCacheCleanTs))
    }

    fun sectionIcon(sec: SidebarSection): ImageVector {
        return when (sec) {
            SidebarSection.LAUNCH_HOME -> Icons.Rounded.RocketLaunch
            SidebarSection.ENGINE_GRAPHICS -> Icons.Rounded.Tune
            SidebarSection.FASTFLAG_EDITOR -> Icons.Rounded.Code
            SidebarSection.STORAGE_CACHE -> Icons.Rounded.CleaningServices
            SidebarSection.ABOUT_UPDATE -> Icons.Rounded.Info
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xF50B1626),
                drawerContentColor = Taupe100,
                modifier = Modifier.width(290.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_cloachy_logo),
                            contentDescription = "Logo CLOACHY",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, AmberGlow.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        )
                        Column {
                            Text("CLOACHY", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(
                                text = "v$currentAppVersion · BY @Zdevlops",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = AmberGlow
                            )
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.14f))

                    for (section in SidebarSection.values()) {
                        val isSelected = selectedSection == section
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) Color(0x3838BDF8) else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (isSelected) AmberGlow.copy(alpha = 0.65f) else Color.Transparent,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    selectedSection = section
                                    scope.launch { drawerState.close() }
                                }
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = sectionIcon(section),
                                contentDescription = null,
                                tint = if (isSelected) Color.White else Taupe200.copy(alpha = 0.78f),
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = section.title,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Taupe100
                                    )
                                    if (section == SidebarSection.ABOUT_UPDATE && hasAppUpdateBadge) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFFBBF24))
                                        )
                                    }
                                }
                                Text(
                                    text = section.subtitle,
                                    fontSize = 10.sp,
                                    color = Taupe200.copy(alpha = 0.7f),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "Bloxstrap-Inspired Android Engine",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = Taupe200.copy(alpha = 0.55f)
                    )
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(RoomWarmTop, RoomWarmMid, RoomWarmLow, RoomWarmDeep)
                    )
                )
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x667DD3FC), Color.Transparent),
                        center = Offset(750f, 180f),
                        radius = 650f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .frostedGlassCard(18)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Menu,
                                contentDescription = "Buka Sidebar",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Image(
                            painter = painterResource(id = R.drawable.ic_cloachy_logo),
                            contentDescription = "Logo CLOACHY",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(9.dp))
                        )

                        Column {
                            Text(
                                text = selectedSection.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "CLOACHY v$currentAppVersion",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = AmberGlow
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.25f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (telemetry.isRobloxActive) Color(0xFF6EE7B7) else AmberGlow)
                        )
                        Text(
                            text = if (telemetry.isRobloxActive) "In-Game" else "Standby",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Taupe100
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp)
                        .padding(top = 4.dp, bottom = 24.dp)
                ) {
                    when (selectedSection) {
                        SidebarSection.LAUNCH_HOME -> MonitorScreen(
                            deviceSpecs = deviceSpecs,
                            envStatus = envStatus,
                            telemetry = telemetry,
                            engineSettings = engineSettings,
                            engineMode = engineMode,
                            updatePolicy = updatePolicy,
                            overlayEnabled = overlayEnabled,
                            overlayPosition = overlayPosition,
                            overlayOpacity = overlayOpacity,
                            hasOverlayPermission = hasOverlayPermission,
                            gpuAutoGuardEnabled = gpuAutoGuardEnabled,
                            onToggleOverlay = { enabled ->
                                overlayEnabled = enabled
                                overlayPrefs.edit().putBoolean("overlay_enabled", enabled).apply()
                                syncOverlayService(enabled)
                            },
                            onOverlayPositionChange = { pos ->
                                overlayPosition = pos
                                overlayPrefs.edit().putInt("overlay_position", pos.ordinal).apply()
                                if (overlayEnabled) syncOverlayService(true)
                            },
                            onOverlayOpacityChange = { op ->
                                overlayOpacity = op
                                overlayPrefs.edit().putInt("overlay_opacity", op).apply()
                                if (overlayEnabled) syncOverlayService(true)
                            },
                            onRequestOverlayPermission = {
                                try {
                                    val permIntent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                    context.startActivity(permIntent)
                                } catch (_: Exception) {
                                    showMessage("Buka Pengaturan -> Tampil di atas aplikasi lain")
                                }
                            },
                            onToggleGpuAutoGuard = { enabled ->
                                gpuAutoGuardEnabled = enabled
                                showMessage(if (enabled) "GPU Auto-Guard Aktif" else "GPU Auto-Guard Dimatikan")
                            },
                            onLaunchRoblox = {
                                FflagRepository.saveEngineSettings(context, engineSettings)
                                if (gpuAutoGuardEnabled) startGuardServiceIfNeeded()
                                val res = ShizukuRobloxManager.injectAndLaunchRoblox(
                                    context = context,
                                    jsonContent = jsonOutput,
                                    graphicsLevel = engineSettings.graphicsQuality,
                                    engineMode = engineMode,
                                    updatePolicy = updatePolicy
                                )
                                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                                sessionHistory = listOf(
                                    SessionRecord(
                                        gameName = "Roblox Launch (${res.methodUsed})",
                                        peakLoad = "Quality ${engineSettings.graphicsQuality}/21 · FPS ${engineSettings.framerateLimit}",
                                        resultSummary = if (res.success) "Launch & Locked" else "Gagal Launch",
                                        timestampLabel = timeStr
                                    )
                                ) + sessionHistory.take(9)
                                showMessage(res.message)
                            },
                            onRefreshDetection = {
                                pollTick++
                                envStatus = ShizukuRobloxManager.checkEnvironment(context)
                                showMessage("Status telemetri diperbarui")
                            }
                        )

                        SidebarSection.ENGINE_GRAPHICS -> VisualScreen(
                            settings = engineSettings,
                            onSettingsChange = { updated ->
                                engineSettings = updated
                                FflagRepository.saveEngineSettings(context, updated)
                            },
                            onApplyAndSave = {
                                FflagRepository.saveEngineSettings(context, engineSettings)
                                val combined = FflagRepository.buildCombinedJson(engineSettings, customFlags)
                                scope.launch {
                                    val (ok, msg) = withContext(Dispatchers.IO) {
                                        ShizukuRobloxManager.applyAndLockGraphicsSilently(
                                            context = context,
                                            jsonContent = combined,
                                            graphicsLevel = engineSettings.graphicsQuality,
                                            forceStopRoblox = false
                                        )
                                    }
                                    showMessage(if (ok) msg else "Pengaturan disimpan ($msg)")
                                }
                            }
                        )

                        SidebarSection.FASTFLAG_EDITOR -> FflagScreen(
                            customFlags = customFlags,
                            jsonOutput = jsonOutput,
                            onAddCustomFlag = { k, v ->
                                val filtered = customFlags.filterNot { it.key.equals(k, ignoreCase = true) }
                                val updated = listOf(FflagItem(k, v)) + filtered
                                customFlags = updated
                                FflagRepository.saveCustomFlags(context, updated)
                                showMessage("FastFlag \"$k\" disimpan")
                            },
                            onEditCustomFlag = { k, v ->
                                val updated = customFlags.map {
                                    if (it.key.equals(k, ignoreCase = true)) it.copy(value = v) else it
                                }
                                customFlags = updated
                                FflagRepository.saveCustomFlags(context, updated)
                            },
                            onDeleteCustomFlag = { k ->
                                val updated = customFlags.filterNot { it.key.equals(k, ignoreCase = true) }
                                customFlags = updated
                                FflagRepository.saveCustomFlags(context, updated)
                                showMessage("FastFlag \"$k\" dihapus")
                            },
                            onCopyJson = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("ClientAppSettings.json", jsonOutput))
                                FflagRepository.exportClientAppSettings(context, jsonOutput)
                                showMessage("ClientAppSettings.json disalin")
                            }
                        )

                        SidebarSection.STORAGE_CACHE -> StorageCacheScreen(
                            cacheSizeLabel = cacheSizeLabel,
                            lastCleanLabel = lastCleanFormatted,
                            autoResetEnabled = autoResetCacheEnabled,
                            selectedInterval = cacheInterval,
                            onToggleAutoReset = { enabled ->
                                autoResetCacheEnabled = enabled
                                cachePrefs.edit().putBoolean("auto_reset_cache_enabled", enabled).apply()
                            },
                            onSelectInterval = { option ->
                                cacheInterval = option
                                cachePrefs.edit().putInt("cache_reset_interval_days", option.days).apply()
                            },
                            onRefreshCacheSize = {
                                scope.launch {
                                    cacheSizeLabel = withContext(Dispatchers.IO) {
                                        ShizukuRobloxManager.getRobloxCacheSizeFormatted()
                                    }
                                }
                            },
                            onCleanCacheNow = {
                                scope.launch {
                                    val (ok, msg) = withContext(Dispatchers.IO) {
                                        ShizukuRobloxManager.cleanRobloxCacheSafely()
                                    }
                                    if (ok) {
                                        val now = System.currentTimeMillis()
                                        lastCacheCleanTs = now
                                        cachePrefs.edit().putLong("last_cache_clean_ts", now).apply()
                                        cacheSizeLabel = withContext(Dispatchers.IO) {
                                            ShizukuRobloxManager.getRobloxCacheSizeFormatted()
                                        }
                                    }
                                    showMessage(msg)
                                }
                            }
                        )

                        SidebarSection.ABOUT_UPDATE -> ProfilScreen(
                            envStatus = envStatus,
                            engineMode = engineMode,
                            updatePolicy = updatePolicy,
                            sessionHistory = sessionHistory,
                            appUpdateInfo = appUpdateInfo,
                            autoCheckAppUpdate = autoCheckAppUpdate,
                            repoSlug = repoSlug,
                            onToggleAutoCheckAppUpdate = { enabled ->
                                autoCheckAppUpdate = enabled
                                AppUpdateManager.setAutoCheckEnabled(context, enabled)
                            },
                            onRepoSlugChange = { newRepo ->
                                repoSlug = newRepo.trim().ifEmpty { AppUpdateManager.DEFAULT_GITHUB_REPO }
                                AppUpdateManager.setRepoSlug(context, repoSlug)
                                showMessage("Source repo disimpan: $repoSlug")
                            },
                            onCheckAppUpdateNow = {
                                scope.launch {
                                    appUpdateInfo = appUpdateInfo.copy(
                                        stage = AppUpdateStage.CHECKING,
                                        statusMessage = "Memeriksa rilis terbaru di $repoSlug..."
                                    )
                                    val res = AppUpdateManager.checkForUpdates(
                                        context = context,
                                        repoSlug = repoSlug,
                                        notifyIfAvailable = true
                                    )
                                    appUpdateInfo = res
                                    showMessage(res.statusMessage)
                                }
                            },
                            onInstallAppUpdateNow = {
                                scope.launch {
                                    appUpdateInfo = appUpdateInfo.copy(
                                        stage = AppUpdateStage.DOWNLOADING,
                                        downloadProgress = 1,
                                        statusMessage = "Mengunduh CLOACHY v${appUpdateInfo.latestVersion}..."
                                    )
                                    val (ok, msg) = AppUpdateManager.downloadAndInstallUpdate(
                                        context = context,
                                        updateInfo = appUpdateInfo,
                                        onProgress = { pct ->
                                            appUpdateInfo = appUpdateInfo.copy(
                                                stage = AppUpdateStage.DOWNLOADING,
                                                downloadProgress = pct
                                            )
                                        }
                                    )
                                    appUpdateInfo = appUpdateInfo.copy(
                                        stage = if (ok) AppUpdateStage.READY_TO_INSTALL else AppUpdateStage.UPDATE_AVAILABLE,
                                        statusMessage = msg
                                    )
                                    showMessage(msg)
                                }
                            },
                            onOpenDeveloperOptions = {
                                if (!ShizukuRobloxManager.openDeveloperOptions(context)) {
                                    showMessage("Buka Pengaturan -> Opsi Developer secara manual")
                                }
                            },
                            onRequestShizukuPermission = {
                                if (!ShizukuRobloxManager.requestShizukuPermission()) {
                                    showMessage("Pastikan service Shizuku sudah berjalan dulu")
                                }
                            },
                            onOpenShizukuApp = {
                                if (!ShizukuRobloxManager.openShizukuApp(context)) {
                                    showMessage("Aplikasi Shizuku belum terpasang di perangkat ini")
                                }
                            },
                            onRefreshStatus = {
                                envStatus = ShizukuRobloxManager.checkEnvironment(context)
                                showMessage("Status USB Debugging & Shizuku diperbarui")
                            },
                            onEngineModeChange = { engineMode = it },
                            onUpdatePolicyChange = { updatePolicy = it }
                        )
                    }
                }
            }
        }
    }
}
