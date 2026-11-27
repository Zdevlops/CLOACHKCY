package com.zdevlops.cloachy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zdevlops.cloachy.model.FflagItem
import com.zdevlops.cloachy.ui.theme.*

@Composable
fun FflagScreen(
    customFlags: List<FflagItem>,
    jsonOutput: String,
    onAddCustomFlag: (String, String) -> Unit,
    onEditCustomFlag: (String, String) -> Unit,
    onDeleteCustomFlag: (String) -> Unit,
    onCopyJson: () -> Unit
) {
    var manualKey by remember { mutableStateOf("") }
    var manualVal by remember { mutableStateOf("True") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .frostedGlassDarkCard(18)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "FASTFLAG EDITOR",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = AmberGlow
            )
            Text(
                text = "Tambahkan FastFlag kustom tambahan (opsional). Pengaturan utama sudah diatur otomatis dari tab Engine & Graphics.",
                fontSize = 11.sp,
                color = Taupe200.copy(alpha = 0.85f),
                lineHeight = 15.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 9.dp)
                ) {
                    if (manualKey.isEmpty()) {
                        Text(
                            text = "Nama FastFlag...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Taupe200.copy(alpha = 0.55f)
                        )
                    }
                    BasicTextField(
                        value = manualKey,
                        onValueChange = { manualKey = it },
                        singleLine = true,
                        cursorBrush = SolidColor(Color.White),
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 6.dp, vertical = 9.dp)
                ) {
                    BasicTextField(
                        value = manualVal,
                        onValueChange = { manualVal = it },
                        singleLine = true,
                        cursorBrush = SolidColor(Color.White),
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White, textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.22f))
                        .border(1.dp, Color.White.copy(alpha = 0.32f), RoundedCornerShape(12.dp))
                        .clickable {
                            if (manualKey.isNotBlank()) {
                                onAddCustomFlag(manualKey.trim(), manualVal.trim())
                                manualKey = ""
                                manualVal = "True"
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Text("Add", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }

        if (customFlags.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .frostedGlassCard(18)
                    .padding(14.dp)
            ) {
                Text(
                    text = "Belum ada FastFlag tambahan. Semua konfigurasi aktif saat ini bersumber dari Engine & Graphics.",
                    fontSize = 11.sp,
                    color = Taupe200.copy(alpha = 0.8f)
                )
            }
        } else {
            for (item in customFlags) {
                var editText by remember(item.key, item.value) { mutableStateOf(item.value) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .frostedGlassCard(18)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.key,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(76.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.35f))
                                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                BasicTextField(
                                    value = editText,
                                    onValueChange = {
                                        editText = it
                                        if (it.isNotBlank()) onEditCustomFlag(item.key, it.trim())
                                    },
                                    singleLine = true,
                                    cursorBrush = SolidColor(Color.White),
                                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White, textAlign = TextAlign.End),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x38EF4444))
                                    .border(1.dp, Color(0x55FCA5A5), RoundedCornerShape(8.dp))
                                    .clickable { onDeleteCustomFlag(item.key) }
                                    .padding(horizontal = 7.dp, vertical = 5.dp)
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Hapus", tint = Color(0xFFFECACA), modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .frostedGlassCard(18)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Generated ClientAppSettings.json", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .clickable { onCopyJson() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Salin JSON", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White)
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = jsonOutput,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = Taupe100,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
