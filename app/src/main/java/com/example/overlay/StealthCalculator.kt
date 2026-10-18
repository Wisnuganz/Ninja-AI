package com.example.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StealthCyan

@Composable
fun StealthCalculator(
    onExitDisguise: () -> Unit,
    onCloseWindow: () -> Unit,
    modifier: Modifier = Modifier
) {
    var display by remember { mutableStateOf("0") }
    var previousValue by remember { mutableStateOf<Double?>(null) }
    var pendingOperation by remember { mutableStateOf<String?>(null) }
    var clearOnNextDigit by remember { mutableStateOf(false) }

    fun onDigit(digit: String) {
        if (clearOnNextDigit || display == "0") {
            display = digit
            clearOnNextDigit = false
        } else {
            if (display.length < 12) {
                display += digit
            }
        }
    }

    fun onDecimal() {
        if (clearOnNextDigit) {
            display = "0."
            clearOnNextDigit = false
        } else if (!display.contains(".")) {
            display += "."
        }
    }

    fun onOperator(op: String) {
        val currentVal = display.toDoubleOrNull() ?: 0.0
        if (previousValue != null && pendingOperation != null && !clearOnNextDigit) {
            val res = when (pendingOperation) {
                "+" -> previousValue!! + currentVal
                "-" -> previousValue!! - currentVal
                "×" -> previousValue!! * currentVal
                "÷" -> if (currentVal != 0.0) previousValue!! / currentVal else 0.0
                else -> currentVal
            }
            display = if (res % 1.0 == 0.0) res.toLong().toString() else "%.4f".format(res).trimEnd('0').trimEnd('.')
            previousValue = res
        } else {
            previousValue = currentVal
        }
        pendingOperation = op
        clearOnNextDigit = true
    }

    fun onEquals() {
        val currentVal = display.toDoubleOrNull() ?: return
        val prev = previousValue ?: return
        val op = pendingOperation ?: return
        val res = when (op) {
            "+" -> prev + currentVal
            "-" -> prev - currentVal
            "×" -> prev * currentVal
            "÷" -> if (currentVal != 0.0) prev / currentVal else 0.0
            else -> currentVal
        }
        display = if (res % 1.0 == 0.0) res.toLong().toString() else "%.4f".format(res).trimEnd('0').trimEnd('.')
        previousValue = null
        pendingOperation = null
        clearOnNextDigit = true
    }

    fun onClear() {
        display = "0"
        previousValue = null
        pendingOperation = null
        clearOnNextDigit = false
    }

    fun onPercent() {
        val current = display.toDoubleOrNull() ?: return
        val res = current / 100.0
        display = res.toString()
        clearOnNextDigit = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF17171C))
            .padding(12.dp)
    ) {
        // Disguise header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Kalkulator Standar",
                color = Color(0xFF9E9E9E),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Secret restore button disguised as icon
                IconButton(
                    onClick = onExitDisguise,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Kembali ke AI",
                        tint = Color(0xFF4A4A5A),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onCloseWindow,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Color(0xFF9E9E9E),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Display screen (tapping long on display also restores AI)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onExitDisguise() }
                    )
                }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Text(
                text = display,
                color = Color.White,
                fontSize = 32.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.End,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Keypad grid
        val rows = listOf(
            listOf("C", "±", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "🔙", "=")
        )

        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    val isOperator = key in listOf("÷", "×", "-", "+", "=")
                    val isSpecial = key in listOf("C", "±", "%", "🔙")

                    val btnBg = when {
                        isOperator -> StealthCyan.copy(alpha = 0.85f)
                        isSpecial -> Color(0xFF4E505F)
                        else -> Color(0xFF2E2F3E)
                    }

                    val textColor = when {
                        isOperator -> Color(0xFF00252E)
                        else -> Color.White
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(CircleShape)
                            .background(btnBg)
                            .clickable {
                                when (key) {
                                    "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" -> onDigit(key)
                                    "." -> onDecimal()
                                    "C" -> onClear()
                                    "±" -> {
                                        val v = display.toDoubleOrNull() ?: 0.0
                                        display = (-v).toString().removeSuffix(".0")
                                    }
                                    "%" -> onPercent()
                                    "🔙" -> {
                                        if (display.length > 1) {
                                            display = display.dropLast(1)
                                        } else {
                                            display = "0"
                                        }
                                    }
                                    "+", "-", "×", "÷" -> onOperator(key)
                                    "=" -> onEquals()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = key,
                            color = textColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
