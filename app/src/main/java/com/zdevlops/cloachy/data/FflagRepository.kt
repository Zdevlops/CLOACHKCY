package com.zdevlops.cloachy.data

import android.content.Context
import android.os.Environment
import com.zdevlops.cloachy.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object FflagRepository {

    private const val PREFS_NAME = "cloachy_engine_prefs"

    fun loadEngineSettings(context: Context): EngineGraphicsSettings {
        val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val texIdx = p.getInt("textureQuality", 0)
        val lightIdx = p.getInt("lightingTechnology", 0)
        val msaaIdx = p.getInt("msaa", 0)
        val apiIdx = p.getInt("preferredGraphicsApi", 0)

        return EngineGraphicsSettings(
            graphicsQuality = p.getInt("graphicsQuality", 5).coerceIn(1, 21),
            framerateLimit = p.getInt("framerateLimit", 120).coerceAtLeast(1),
            textureQuality = TextureQualityOption.values().getOrElse(texIdx) { TextureQualityOption.AUTOMATIC },
            lightingTechnology = LightingTechOption.values().getOrElse(lightIdx) { LightingTechOption.AUTOMATIC },
            msaa = MsaaOption.values().getOrElse(msaaIdx) { MsaaOption.AUTOMATIC },
            preferredGraphicsApi = GraphicsApiOption.values().getOrElse(apiIdx) { GraphicsApiOption.AUTOMATIC },
            shadows = p.getBoolean("shadows", true),
            particlesAndSmoke = p.getBoolean("particlesAndSmoke", true),
            distanceFog = p.getBoolean("distanceFog", true),
            postProcessingEffects = p.getBoolean("postProcessingEffects", true)
        )
    }

    fun saveEngineSettings(context: Context, s: EngineGraphicsSettings) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt("graphicsQuality", s.graphicsQuality.coerceIn(1, 21))
            .putInt("framerateLimit", s.framerateLimit.coerceAtLeast(1))
            .putInt("textureQuality", s.textureQuality.ordinal)
            .putInt("lightingTechnology", s.lightingTechnology.ordinal)
            .putInt("msaa", s.msaa.ordinal)
            .putInt("preferredGraphicsApi", s.preferredGraphicsApi.ordinal)
            .putBoolean("shadows", s.shadows)
            .putBoolean("particlesAndSmoke", s.particlesAndSmoke)
            .putBoolean("distanceFog", s.distanceFog)
            .putBoolean("postProcessingEffects", s.postProcessingEffects)
            .apply()
    }

    fun loadCustomFlags(context: Context): List<FflagItem> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("custom_fflags_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<FflagItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    FflagItem(
                        key = obj.optString("key"),
                        value = obj.optString("value"),
                        description = obj.optString("description", "Custom FastFlag")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveCustomFlags(context: Context, list: List<FflagItem>) {
        val arr = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("key", item.key)
            obj.put("value", item.value)
            obj.put("description", item.description)
            arr.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("custom_fflags_json", arr.toString())
            .apply()
    }

    fun buildCombinedJson(settings: EngineGraphicsSettings, customFlags: List<FflagItem>): String {
        val root = JSONObject()
        val level = settings.graphicsQuality.coerceIn(1, 21)

        // 1. Kunci Kualitas Grafik (Auto-Lock di balik layar agar slider in-game tidak bisa mengubah)
        root.put("DFIntDebugFRMQualityLevelOverride", level)
        root.put("FIntRomarkStartWithGraphicQualityLevel", level)
        root.put("FFlagFixGraphicsQuality", true)

        // 2. Framerate Limit bebas tanpa batas maksimal
        root.put("DFIntTaskSchedulerTargetFps", settings.framerateLimit.coerceAtLeast(1))
        root.put("FFlagTaskSchedulerLimitTargetFpsTo2402", false)

        // 3. Texture Quality
        val texSamples = settings.textureQuality.flagVal
        if (texSamples != null) {
            root.put("FFlagTextureQualityOverrideEnabled", true)
            root.put("DFIntTextureQualityOverride", texSamples)
        } else {
            root.put("FFlagTextureQualityOverrideEnabled", false)
        }

        // 4. Lighting Technology
        when (settings.lightingTechnology) {
            LightingTechOption.AUTOMATIC -> Unit
            LightingTechOption.VOXEL -> {
                root.put("DFFlagDebugRenderForceTechnologyVoxel", true)
                root.put("FFlagDebugForceFutureIsBrightPhase2", false)
                root.put("FFlagDebugForceFutureIsBrightPhase3", false)
            }
            LightingTechOption.SHADOW_MAP -> {
                root.put("DFFlagDebugRenderForceTechnologyVoxel", false)
                root.put("FFlagDebugForceFutureIsBrightPhase2", true)
                root.put("FFlagDebugForceFutureIsBrightPhase3", false)
            }
            LightingTechOption.FUTURE -> {
                root.put("DFFlagDebugRenderForceTechnologyVoxel", false)
                root.put("FFlagDebugForceFutureIsBrightPhase2", false)
                root.put("FFlagDebugForceFutureIsBrightPhase3", true)
            }
        }

        // 5. MSAA (Anti-Aliasing)
        val msaaSamples = settings.msaa.samples
        if (msaaSamples != null) {
            root.put("FIntDebugForceMSAASamples", msaaSamples)
        }

        // 6. Preferred Graphics API
        when (settings.preferredGraphicsApi) {
            GraphicsApiOption.AUTOMATIC -> Unit
            GraphicsApiOption.VULKAN -> {
                root.put("FFlagDebugGraphicsPreferVulkan", true)
                root.put("FFlagDebugGraphicsDisableOpenGL", true)
            }
            GraphicsApiOption.OPENGL -> {
                root.put("FFlagDebugGraphicsPreferOpenGLES", true)
                root.put("FFlagDebugGraphicsDisableVulkan", true)
            }
        }

        // 7. Boolean Options (True / False langsung)
        root.put("FIntRenderShadowIntensity", if (settings.shadows) 100 else 0)
        root.put("FFlagRenderShadowMapEnabled", settings.shadows)

        root.put("FFlagDebugDisableParticleEffects", !settings.particlesAndSmoke)
        root.put("DFIntCSGLevelOfDetailSwitchingDistance", if (settings.particlesAndSmoke) 250 else 0)

        root.put("FFlagRenderDisableFog", !settings.distanceFog)
        root.put("FFlagDisablePostFx", !settings.postProcessingEffects)

        // 8. Custom user FastFlags
        for (item in customFlags) {
            val k = item.key.trim()
            val v = item.value.trim()
            if (k.isEmpty()) continue
            when {
                v.equals("true", ignoreCase = true) -> root.put(k, true)
                v.equals("false", ignoreCase = true) -> root.put(k, false)
                v.toIntOrNull() != null -> root.put(k, v.toInt())
                else -> root.put(k, v)
            }
        }

        return root.toString(2)
    }

    fun exportClientAppSettings(context: Context, jsonContent: String): File {
        val outDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "ClientSettings")
        if (!outDir.exists()) outDir.mkdirs()
        val outFile = File(outDir, "ClientAppSettings.json")
        outFile.writeText(jsonContent)
        return outFile
    }
}
