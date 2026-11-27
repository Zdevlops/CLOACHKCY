package com.zdevlops.cloachy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Refresh
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
import com.zdevlops.cloachy.model.CacheIntervalOption
import com.zdevlops.cloachy.ui.components.OptionChipGroup
import com.zdevlops.cloachy.ui.components.TrueFalseSelector
import com.zdevlops.cloachy.ui.theme.*

@Composable
fun StorageCacheScreen(
    cacheSizeLabel: String,
    lastCleanLabel: String,
    autoResetEnabled: Boolean,
    selectedInterval: CacheIntervalOption,
    onToggleAutoReset: (Boolean) -> Unit,
    onSelectInterval: (CacheIntervalOption) -> Unit,
    onRefreshCacheSize: () -> Unit,
    onCleanCacheNow: () -> Unit
) {
    val intervals = CacheIntervalOption.values().toList()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .frostedGlassCard(22)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "STORAGE & CACHE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = AmberGlow
                    )
                    Text(
                        text = "Roblox Cache: $cacheSizeLabel",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        text = "Terakhir dibersihkan: $lastCleanLabel",
                        fontSize = 10.sp,
                        color = Taupe200.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                IconButton(onClick = onRefreshCacheSize) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Cek Ukuran Cache", tint = Color.White)
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.14f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                    Text("Auto-Reset Cache", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text(
                        text = "Bersihkan cache shader & HTTP Roblox secara berkala agar tidak menumpuk (akun tetap aman)",
                        fontSize = 10.sp,
                        color = Taupe200.copy(alpha = 0.8f),
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                TrueFalseSelector(value = autoResetEnabled, onValueChange = onToggleAutoReset)
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Reset Interval", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                OptionChipGroup(
                    options = intervals.map { it.label },
                    selectedIndex = intervals.indexOf(selectedInterval),
                    onSelectIndex = { onSelectInterval(intervals[it]) }
                )
            }

            Button(
                onClick = onCleanCacheNow,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Taupe950
                )
            ) {
                Icon(Icons.Rounded.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Clean Cache Now", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
