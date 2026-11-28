package com.zdevlops.cloachy.model

enum class SidebarSection(val index: Int, val title: String, val subtitle: String) {
    LAUNCH_HOME(0, "Launch & Home", "Launcher Roblox, Overlay & Telemetri"),
    ENGINE_GRAPHICS(1, "Engine & Graphics", "Pengaturan Grafik Gaya Bloxstrap"),
    FASTFLAG_EDITOR(2, "FastFlag Editor", "Custom JSON & Flag Tambahan"),
    STORAGE_CACHE(3, "Storage & Cache", "Auto-Reset Cache & Kebersihan"),
    ABOUT_UPDATE(4, "About & Update", "Shizuku, Engine & Pembaruan")
}

enum class TextureQualityOption(val label: String, val flagVal: Int?) {
    AUTOMATIC("Automatic", null),
    LOW("Low", 0),
    MEDIUM("Medium", 1),
    HIGH("High", 2)
}

enum class LightingTechOption(val label: String) {
    AUTOMATIC("Automatic"),
    VOXEL("Voxel"),
    SHADOW_MAP("ShadowMap"),
    FUTURE("Future")
}

enum class MsaaOption(val label: String, val samples: Int?) {
    AUTOMATIC("Automatic", null),
    OFF("Off", 0),
    X1("1x", 1),
    X2("2x", 2),
    X4("4x", 4)
}

enum class GraphicsApiOption(val label: String) {
    AUTOMATIC("Automatic"),
    VULKAN("Vulkan"),
    OPENGL("OpenGL")
}

enum class OverlayPosition(val label: String) {
    TOP_LEFT("Top Left"),
    TOP_RIGHT("Top Right")
}

enum class CacheIntervalOption(val days: Int, val label: String) {
    EVERY_3_DAYS(3, "Every 3 Days"),
    EVERY_7_DAYS(7, "Every 7 Days (Weekly)"),
    EVERY_14_DAYS(14, "Every 14 Days"),
    EVERY_30_DAYS(30, "Every 30 Days")
}

data class EngineGraphicsSettings(
    val graphicsQuality: Int = 5, // 1..21
    val framerateLimit: Int = 120, // bebas tanpa batas atas
    val textureQuality: TextureQualityOption = TextureQualityOption.AUTOMATIC,
    val lightingTechnology: LightingTechOption = LightingTechOption.AUTOMATIC,
    val msaa: MsaaOption = MsaaOption.AUTOMATIC,
    val preferredGraphicsApi: GraphicsApiOption = GraphicsApiOption.AUTOMATIC,
    val shadows: Boolean = true,
    val particlesAndSmoke: Boolean = true,
    val distanceFog: Boolean = true,
    val postProcessingEffects: Boolean = true
)

enum class InjectorEngineMode(val label: String, val subtitle: String) {
    HYBRID("Hybrid Auto", "Shizuku Direct + APK Patch Fallback"),
    SHIZUKU_FAST("Shizuku Direct", "Inject langsung ke data Roblox via Shizuku"),
    APK_PATCHER("Built-in Patcher", "Suntik aset APK + Re-sign Keystore tetap")
}

enum class UpdatePolicyMode(val label: String, val subtitle: String) {
    SKIP_UPDATE("Abaikan Update", "Langsung inject & masuk tanpa uninstall"),
    AUTO_UPDATE("Auto-Update", "Cek versi & otomatis inject ulang")
}

enum class GpuAlertLevel {
    NONE,
    HIGH_80,
    MAX_100
}

enum class AppUpdateStage {
    IDLE,
    CHECKING,
    UP_TO_DATE,
    UPDATE_AVAILABLE,
    DOWNLOADING,
    READY_TO_INSTALL,
    ERROR
}

data class AppUpdateInfo(
    val currentVersion: String = "1.0.1",
    val latestVersion: String = "1.0.1",
    val releaseNotes: String = "CLOACHY v1.0.1 Release stabil.",
    val downloadUrl: String = "",
    val publishedAt: String = "",
    val stage: AppUpdateStage = AppUpdateStage.IDLE,
    val downloadProgress: Int = 0,
    val statusMessage: String = "Versi v1.0.1 (Release) terpasang"
)

data class DeviceSpecs(
    val modelName: String = "Memindai perangkat...",
    val socInfo: String = "Membaca Chipset & Vulkan API...",
    val chipShort: String = "-",
    val tierBadge: String = "Scanning...",
    val cpuCores: Int = 8,
    val ramGb: Int = 8,
    val refreshRateHz: Int = 120,
    val batteryTempC: Float = 36.4f,
    val isScanning: Boolean = true
)

data class EnvironmentStatus(
    val isRobloxInstalled: Boolean = false,
    val robloxVersionName: String = "Tidak terpasang",
    val isUsbDebuggingEnabled: Boolean = false,
    val isWirelessDebuggingEnabled: Boolean = false,
    val isShizukuInstalled: Boolean = false,
    val isShizukuRunning: Boolean = false,
    val isShizukuGranted: Boolean = false
)

data class TelemetryState(
    val cpuLoadPercent: Int = 12,
    val gpuLoadPercent: Int = 8,
    val gpuSensorSource: String = "Hardware Idle",
    val fps: Int = 60,
    val fpsSource: String = "Standby",
    val frameTimeMs: Float = 16.6f,
    val tempC: Float = 35.2f,
    val ramUsedGb: Float = 4.2f,
    val ramTotalGb: Float = 8.0f,
    val isRobloxActive: Boolean = false,
    val sessionStatusTitle: String = "Standby · Tidak ada sesi Roblox aktif",
    val gpuAlertLevel: GpuAlertLevel = GpuAlertLevel.NONE,
    val autoActionSummary: String? = null
)

data class FflagItem(
    val key: String,
    val value: String,
    val description: String = "Custom FastFlag"
)

data class LaunchResult(
    val success: Boolean,
    val methodUsed: String,
    val message: String
)

data class SessionRecord(
    val gameName: String,
    val peakLoad: String,
    val resultSummary: String,
    val timestampLabel: String
)
