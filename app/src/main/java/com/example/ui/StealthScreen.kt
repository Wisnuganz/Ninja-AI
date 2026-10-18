package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.overlay.StealthCalculator
import com.example.ui.theme.StealthCardBorder
import com.example.ui.theme.StealthCyan
import com.example.ui.theme.StealthCyanDim
import com.example.ui.theme.StealthDarkBg
import com.example.ui.theme.StealthDarkSurface
import com.example.ui.theme.StealthDarkSurfaceVariant
import com.example.ui.theme.StealthGreen
import com.example.ui.theme.StealthTextPrimary
import com.example.ui.theme.StealthTextSecondary

@Composable
fun StealthScreen(
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var isTestingCalculator by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StealthDarkBg)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(StealthCyan.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = StealthCyan,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Mode Samaran & 100% Hening",
                    color = StealthTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Sistem penyamaran instan saat ada orang melihat",
                    color = StealthCyanDim,
                    fontSize = 12.sp
                )
            }
        }

        // Silent Pledge Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF07271F)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StealthGreen.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VolumeMute,
                        contentDescription = null,
                        tint = StealthGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Jaminan 100% Hening",
                        color = StealthGreen,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Aplikasi ini dirancang khusus tanpa suara: TIDAK ADA Text-To-Speech (TTS), TIDAK ADA efek suara audio, TIDAK ADA dering atau notifikasi bising. Semua jawaban dihadirkan murni dalam bentuk teks visual dan langkah pengerjaan.",
                    color = Color(0xFFE2E8F0),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }

        // Feature 1: Disguise Calculator Demo
        Card(
            colors = CardDefaults.cardColors(containerColor = StealthDarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StealthCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = null,
                        tint = StealthCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Uji Coba Kalkulator Samaran (Panic Disguise)",
                        color = StealthTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Saat jendela mengambang aktif, tombol 'Samaran' seketika mengubah popup menjadi kalkulator nyata. Coba gunakan kalkulator samaran di bawah:",
                    color = StealthTextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, StealthCardBorder, RoundedCornerShape(12.dp))
                ) {
                    StealthCalculator(
                        onExitDisguise = { isTestingCalculator = false },
                        onCloseWindow = { isTestingCalculator = false }
                    )
                }
            }
        }

        // Feature 2: Stealth Guide
        Card(
            colors = CardDefaults.cardColors(containerColor = StealthDarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StealthCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "4 Trik Penyamaran NinjaTugas",
                    color = StealthTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                StealthTipItem(
                    icon = Icons.Default.Calculate,
                    title = "1. Satu Ketuk Ganti ke Kalkulator",
                    desc = "Ketuk ikon kalkulator di pojok kanan jendela untuk langsung menyamarkan jawaban menjadi kalkulator biasa."
                )

                StealthTipItem(
                    icon = Icons.Default.Visibility,
                    title = "2. Atur Transparansi Jendela",
                    desc = "Ketuk ikon mata untuk mengatur kejelasan jendela dari 100% sampai 25% tembus pandang."
                )

                StealthTipItem(
                    icon = Icons.Default.VisibilityOff,
                    title = "3. Sembunyikan ke Bubble Kecil",
                    desc = "Tekan ikon minimize (➖) untuk mengecilkan jendela ke ikon dot bulat kecil di pinggir layar yang bisa digeser."
                )

                StealthTipItem(
                    icon = Icons.Default.NotificationsOff,
                    title = "4. Tombol Panik di Bar Notifikasi",
                    desc = "Tarik bilah status/notifikasi atas ponsel dan ketuk 'Mode Samaran' untuk menutup jendela tanpa beralih aplikasi."
                )
            }
        }
    }
}

@Composable
fun StealthTipItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(StealthDarkSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = StealthCyan,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                color = StealthTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                color = StealthTextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}
