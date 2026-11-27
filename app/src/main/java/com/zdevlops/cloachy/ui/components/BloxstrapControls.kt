package com.zdevlops.cloachy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zdevlops.cloachy.ui.theme.AmberGlow
import com.zdevlops.cloachy.ui.theme.Taupe200

@Composable
fun TrueFalseSelector(
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.30f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val options = listOf(true to "True", false to "False")
        for ((boolVal, label) in options) {
            val selected = value == boolVal
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) Color(0x4738BDF8) else Color.Transparent)
                    .border(
                        1.dp,
                        if (selected) AmberGlow.copy(alpha = 0.75f) else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onValueChange(boolVal) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) Color.White else Taupe200.copy(alpha = 0.72f)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OptionChipGroup(
    options: List<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) Color(0x4238BDF8) else Color.White.copy(alpha = 0.08f))
                    .border(
                        1.dp,
                        if (selected) AmberGlow.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.15f),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelectIndex(index) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) Color.White else Taupe200.copy(alpha = 0.82f)
                )
            }
        }
    }
}
