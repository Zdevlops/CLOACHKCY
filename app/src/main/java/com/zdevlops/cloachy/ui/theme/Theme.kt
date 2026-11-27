package com.zdevlops.cloachy.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val RoomWarmTop = Color(0xFF2B4C6F)
val RoomWarmMid = Color(0xFF1D3550)
val RoomWarmLow = Color(0xFF122338)
val RoomWarmDeep = Color(0xFF0A1421)

val Taupe100 = Color(0xFFF0F7FF)
val Taupe200 = Color(0xFFD6E6F7)
val Taupe950 = Color(0xFF09131F)
val AmberGlow = Color(0xFF7DD3FC)
val AlertRedBorder = Color(0x6BFCA5A5)
val AlertRedFill = Color(0x57B92D26)
val WarnAmberBorder = Color(0x7038BDF8)
val WarnAmberFill = Color(0x4D0369A1)

private val CloachyBlueColorScheme = darkColorScheme(
    primary = Color(0xFFE0F2FE),
    onPrimary = Taupe950,
    secondary = AmberGlow,
    background = RoomWarmDeep,
    surface = Color(0x28E0F2FE),
    onBackground = Taupe100,
    onSurface = Taupe100
)

fun Modifier.frostedGlassCard(cornerRadius: Int = 24): Modifier {
    val shape = RoundedCornerShape(cornerRadius.dp)
    return this
        .clip(shape)
        .background(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0x2EE0F2FE),
                    Color(0x14BAE6FD),
                    Color(0x520B192C)
                )
            )
        )
        .border(1.dp, Color(0x3DBAE6FD), shape)
}

fun Modifier.frostedGlassDarkCard(cornerRadius: Int = 18): Modifier {
    val shape = RoundedCornerShape(cornerRadius.dp)
    return this
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0x94112238),
                    Color(0xBD08111E)
                )
            )
        )
        .border(1.dp, Color(0x2CBAE6FD), shape)
}

fun Modifier.frostedWarnCard(cornerRadius: Int = 24): Modifier {
    val shape = RoundedCornerShape(cornerRadius.dp)
    return this
        .clip(shape)
        .background(
            brush = Brush.linearGradient(
                colors = listOf(
                    WarnAmberFill,
                    Color(0xD60C223B)
                )
            )
        )
        .border(1.dp, WarnAmberBorder, shape)
}

fun Modifier.frostedAlertCard(cornerRadius: Int = 24): Modifier {
    val shape = RoundedCornerShape(cornerRadius.dp)
    return this
        .clip(shape)
        .background(
            brush = Brush.linearGradient(
                colors = listOf(
                    AlertRedFill,
                    Color(0xD1221412)
                )
            )
        )
        .border(1.dp, AlertRedBorder, shape)
}

@Composable
fun CloachyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CloachyBlueColorScheme,
        content = content
    )
}
