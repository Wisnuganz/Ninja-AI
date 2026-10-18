package com.example.overlay

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.gemini.HomeworkSolution
import com.example.ui.theme.StealthCardBorder
import com.example.ui.theme.StealthCyan
import com.example.ui.theme.StealthCyanDim
import com.example.ui.theme.StealthDarkBg
import com.example.ui.theme.StealthDarkSurface
import com.example.ui.theme.StealthDarkSurfaceVariant
import com.example.ui.theme.StealthGreen
import com.example.ui.theme.StealthTextPrimary
import com.example.ui.theme.StealthTextSecondary

enum class OverlayMode {
    MINIMIZED,
    EXPANDED,
    DISGUISE_CALCULATOR
}

@Composable
fun FloatingOverlayContent(
    mode: OverlayMode,
    solution: HomeworkSolution?,
    isLoading: Boolean,
    statusMessage: String,
    onModeChange: (OverlayMode) -> Unit,
    onLiveScreenCapture: () -> Unit,
    onSolveQuestion: (String) -> Unit,
    onCloseService: () -> Unit,
    onDragDelta: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var opacity by remember { mutableFloatStateOf(0.95f) }
    var inputText by remember { mutableStateOf("") }
    var showOpacitySlider by remember { mutableStateOf(false) }

    when (mode) {
        OverlayMode.MINIMIZED -> {
            // Tiny discreet floating bubble (Draggable)
            Box(
                modifier = modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                StealthDarkSurfaceVariant,
                                StealthDarkBg
                            )
                        )
                    )
                    .border(2.dp, StealthCyan, CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDragDelta(dragAmount.x, dragAmount.y)
                        }
                    }
                    .clickable { onModeChange(OverlayMode.EXPANDED) }
                    .testTag("floating_bubble"),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = StealthCyan,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Buka NinjaTugas",
                        tint = StealthCyan,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }

        OverlayMode.DISGUISE_CALCULATOR -> {
            // Disguised as standard calculator for stealth
            Box(
                modifier = modifier
                    .width(320.dp)
                    .height(440.dp)
                    .alpha(opacity)
            ) {
                StealthCalculator(
                    onExitDisguise = { onModeChange(OverlayMode.EXPANDED) },
                    onCloseWindow = { onModeChange(OverlayMode.MINIMIZED) }
                )
            }
        }

        OverlayMode.EXPANDED -> {
            // Full floating window helper
            Surface(
                modifier = modifier
                    .width(340.dp)
                    .height(500.dp)
                    .alpha(opacity)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, StealthCardBorder, RoundedCornerShape(16.dp)),
                color = StealthDarkBg,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Draggable Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StealthDarkSurface)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    onDragDelta(dragAmount.x, dragAmount.y)
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = StealthCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "NinjaTugas",
                                color = StealthTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Silent Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF064E3B))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeMute,
                                        contentDescription = "Hening",
                                        tint = StealthGreen,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "100% Hening",
                                        color = StealthGreen,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Action Buttons in header
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Disguise Calculator Panic Button
                            IconButton(
                                onClick = { onModeChange(OverlayMode.DISGUISE_CALCULATOR) },
                                modifier = Modifier.size(28.dp),
                                content = {
                                    Icon(
                                        imageVector = Icons.Default.Calculate,
                                        contentDescription = "Mode Samaran (Kalkulator)",
                                        tint = StealthCyanDim,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )

                            // Transparency Toggle
                            IconButton(
                                onClick = { showOpacitySlider = !showOpacitySlider },
                                modifier = Modifier.size(28.dp),
                                content = {
                                    Icon(
                                        imageVector = if (showOpacitySlider) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Transparansi",
                                        tint = StealthTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )

                            // Minimize to bubble
                            IconButton(
                                onClick = { onModeChange(OverlayMode.MINIMIZED) },
                                modifier = Modifier.size(28.dp),
                                content = {
                                    Icon(
                                        imageVector = Icons.Default.ExpandLess,
                                        contentDescription = "Kecilkan",
                                        tint = StealthTextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )

                            // Close / Dismiss
                            IconButton(
                                onClick = onCloseService,
                                modifier = Modifier.size(28.dp),
                                content = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Tutup Floating Window",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }

                    // Opacity slider bar (if toggled)
                    AnimatedVisibility(visible = showOpacitySlider) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(StealthDarkSurfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Transparansi: ${(opacity * 100).toInt()}%",
                                color = StealthTextSecondary,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Slider(
                                value = opacity,
                                onValueChange = { opacity = it },
                                valueRange = 0.25f..1f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = StealthCyan,
                                    activeTrackColor = StealthCyan
                                )
                            )
                        }
                    }

                    // Quick Actions bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Live Screen Analysis Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF005F73),
                                            Color(0xFF0A9396)
                                        )
                                    )
                                )
                                .clickable(enabled = !isLoading) { onLiveScreenCapture() }
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ScreenShare,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "⚡ Analisis Layar",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Panic Disguise Button
                        Box(
                            modifier = Modifier
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF334155))
                                .clickable { onModeChange(OverlayMode.DISGUISE_CALCULATOR) }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = null,
                                    tint = StealthCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Samaran",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Content Scroll Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 10.dp)
                    ) {
                        if (isLoading) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    color = StealthCyan
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = statusMessage.ifBlank { "Menganalisis tugas secara hening..." },
                                    color = StealthTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        } else if (solution != null) {
                            val scrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
                            ) {
                                // Subject tag & Copy button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(StealthDarkSurfaceVariant)
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "📚 ${solution.subject}",
                                            color = StealthCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Copy Answer
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF1E293B))
                                            .clickable {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("Jawaban Tugas", "${solution.shortAnswer}\n\n${solution.detailedSteps}")
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(context, "Jawaban disalin ke papan klip", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Salin",
                                            tint = StealthTextSecondary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Salin",
                                            color = StealthTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Direct Short Answer Highlight Card
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF0F2338))
                                        .border(1.dp, StealthCyanDim.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "🎯 JAWABAN UTAMA:",
                                            color = StealthCyan,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = solution.shortAnswer,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                // Key Formulas if present
                                if (solution.keyFormulas.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1E1E2E))
                                            .padding(8.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "📐 Rumus / Konsep:",
                                                color = Color(0xFFA78BFA),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = solution.keyFormulas,
                                                color = Color(0xFFDDD6FE),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                // Detailed Steps
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "📝 Langkah Pengerjaan:",
                                    color = StealthTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(StealthDarkSurface)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = solution.detailedSteps,
                                        color = StealthTextPrimary,
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        } else {
                            // Empty State / Instructions
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 16.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ScreenShare,
                                    contentDescription = null,
                                    tint = StealthCyanDim,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Siap Bantu Tugas",
                                    color = StealthTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tekan '⚡ Analisis Layar' untuk membaca soal di layar kamu seketika, atau ketik soal di bawah.",
                                    color = StealthTextSecondary,
                                    fontSize = 11.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }

                    // Input Text Bar at Bottom
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StealthDarkSurface)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    text = "Ketik atau tempel soal...",
                                    fontSize = 12.sp,
                                    color = StealthTextSecondary
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StealthCyan,
                                unfocusedBorderColor = StealthCardBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    val q = inputText
                                    inputText = ""
                                    onSolveQuestion(q)
                                }
                            },
                            enabled = inputText.isNotBlank() && !isLoading,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (inputText.isNotBlank()) StealthCyan else StealthDarkSurfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Kirim",
                                tint = if (inputText.isNotBlank()) Color(0xFF00363D) else StealthTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
