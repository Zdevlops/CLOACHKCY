package com.zdevlops.cloachy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zdevlops.cloachy.model.*
import com.zdevlops.cloachy.ui.components.OptionChipGroup
import com.zdevlops.cloachy.ui.components.TrueFalseSelector
import com.zdevlops.cloachy.ui.theme.*

@Composable
fun VisualScreen(
    settings: EngineGraphicsSettings,
    onSettingsChange: (EngineGraphicsSettings) -> Unit,
    onApplyAndSave: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var qualityText by remember(settings.graphicsQuality) { mutableStateOf(settings.graphicsQuality.toString()) }
    var fpsText by remember(settings.framerateLimit) { mutableStateOf(settings.framerateLimit.toString()) }

    fun commitQuality() {
        val parsed = qualityText.toIntOrNull() ?: settings.graphicsQuality
        val clamped = parsed.coerceIn(1, 21)
        qualityText = clamped.toString()
        onSettingsChange(settings.copy(graphicsQuality = clamped))
    }

    fun commitFps() {
        val parsed = fpsText.toIntOrNull() ?: settings.framerateLimit
        val valid = parsed.coerceAtLeast(1)
        fpsText = valid.toString()
        onSettingsChange(settings.copy(framerateLimit = valid))
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // 1. GRAPHICS QUALITY (1 - 21) & FRAMERATE LIMIT (BEBAS)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .frostedGlassCard(22)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "ENGINE & GRAPHICS SETTINGS",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = AmberGlow
            )

            // Graphics Quality (Ketik angka 1 - 21, mentok 21)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Graphics Quality",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "Ketik tingkat grafik sendiri dari 1 sampai maksimal 21",
                        fontSize = 10.sp,
                        color = Taupe200.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(84.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .border(1.dp, AmberGlow.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
                        value = qualityText,
                        onValueChange = { raw ->
                            val digits = raw.filter { it.isDigit() }.take(4)
                            if (digits.isEmpty()) {
                                qualityText = ""
                            } else {
                                val num = (digits.toIntOrNull() ?: 21).coerceIn(1, 21)
                                qualityText = num.toString()
                                onSettingsChange(settings.copy(graphicsQuality = num))
                            }
                        },
                        singleLine = true,
                        cursorBrush = SolidColor(Color.White),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            commitQuality()
                            focusManager.clearFocus()
                        }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { if (!it.isFocused) commitQuality() }
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

            // Framerate Limit (Bebas tanpa batas atas)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Framerate Limit",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "Bebas isi berapa saja (misal 60, 90, 120, 240, atau 9999)",
                        fontSize = 10.sp,
                        color = Taupe200.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(84.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
                        value = fpsText,
                        onValueChange = { raw ->
                            val digits = raw.filter { it.isDigit() }.take(6)
                            fpsText = digits
                            val num = digits.toIntOrNull()
                            if (num != null && num >= 1) {
                                onSettingsChange(settings.copy(framerateLimit = num))
                            }
                        },
                        singleLine = true,
                        cursorBrush = SolidColor(Color.White),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            commitFps()
                            focusManager.clearFocus()
                        }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { if (!it.isFocused) commitFps() }
                    )
                }
            }
        }

        // 2. RENDER PIPELINE OPTIONS (Bloxstrap Style)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .frostedGlassCard(20)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val texOptions = TextureQualityOption.values().toList()
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Texture Quality", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                OptionChipGroup(
                    options = texOptions.map { it.label },
                    selectedIndex = texOptions.indexOf(settings.textureQuality),
                    onSelectIndex = { onSettingsChange(settings.copy(textureQuality = texOptions[it])) }
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

            val lightOptions = LightingTechOption.values().toList()
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Lighting Technology", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                OptionChipGroup(
                    options = lightOptions.map { it.label },
                    selectedIndex = lightOptions.indexOf(settings.lightingTechnology),
                    onSelectIndex = { onSettingsChange(settings.copy(lightingTechnology = lightOptions[it])) }
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

            val msaaOptions = MsaaOption.values().toList()
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("MSAA (Anti-Aliasing)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                OptionChipGroup(
                    options = msaaOptions.map { it.label },
                    selectedIndex = msaaOptions.indexOf(settings.msaa),
                    onSelectIndex = { onSettingsChange(settings.copy(msaa = msaaOptions[it])) }
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

            val apiOptions = GraphicsApiOption.values().toList()
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Preferred Graphics API", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                OptionChipGroup(
                    options = apiOptions.map { it.label },
                    selectedIndex = apiOptions.indexOf(settings.preferredGraphicsApi),
                    onSelectIndex = { onSettingsChange(settings.copy(preferredGraphicsApi = apiOptions[it])) }
                )
            }
        }

        // 3. BOOLEAN EFFECTS (True / False langsung)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .frostedGlassDarkCard(20)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BooleanSettingRow(
                title = "Shadows",
                subtitle = "Menampilkan bayangan objek & karakter di dunia game",
                value = settings.shadows,
                onValueChange = { onSettingsChange(settings.copy(shadows = it)) }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.10f))

            BooleanSettingRow(
                title = "Particles & Smoke",
                subtitle = "Menampilkan efek asap, percikan, dan ledakan",
                value = settings.particlesAndSmoke,
                onValueChange = { onSettingsChange(settings.copy(particlesAndSmoke = it)) }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.10f))

            BooleanSettingRow(
                title = "Distance Fog",
                subtitle = "Menampilkan kabut jarak jauh pada map",
                value = settings.distanceFog,
                onValueChange = { onSettingsChange(settings.copy(distanceFog = it)) }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.10f))

            BooleanSettingRow(
                title = "Post-Processing Effects",
                subtitle = "Efek bloom, sun rays, dan blur pasca-render",
                value = settings.postProcessingEffects,
                onValueChange = { onSettingsChange(settings.copy(postProcessingEffects = it)) }
            )
        }

        // 4. TOMBOL APPLY (Otomatis mengunci di balik layar tanpa label Lock)
        Button(
            onClick = {
                commitQuality()
                commitFps()
                focusManager.clearFocus()
                onApplyAndSave()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Taupe950
            )
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Apply Settings",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BooleanSettingRow(
    title: String,
    subtitle: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = Taupe200.copy(alpha = 0.78f),
                lineHeight = 14.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        TrueFalseSelector(value = value, onValueChange = onValueChange)
    }
}
