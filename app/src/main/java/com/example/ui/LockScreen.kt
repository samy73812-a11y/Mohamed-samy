package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.CameraManagerViewModel
import com.example.ui.theme.*

@Composable
fun LockScreen(
    viewModel: CameraManagerViewModel,
    onBiometricTrigger: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPinSet by viewModel.isPinSet.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()

    var enteredPin by remember { mutableStateOf("") }
    var setupStep by remember { mutableStateOf(1) } // 1: Enter first, 2: Confirm
    var firstPinTemp by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val maxPinLength = 4

    val gradientBg = Brush.verticalGradient(
        colors = listOf(CyberBlack, SolidDark)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradientBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxHeight()
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Upper Header (Shield Icon & Title)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(CyberBlueDim, Color.Transparent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (!isPinSet) Icons.Default.Security else Icons.Default.Lock,
                        contentDescription = "قفل الخصوصية",
                        tint = CyberBlue,
                        modifier = Modifier
                            .size(64.dp)
                            .animateContentSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "درع حماية الكاميرا",
                    color = TextWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                val subtitle = when {
                    !isPinSet && setupStep == 1 -> "تهيئة النظام: الرجاء إعداد رمز مرور PIN جديد للتطبيق"
                    !isPinSet && setupStep == 2 -> "تأكيد الرمز: أعد إدخال الرمز للتأكيد والربط"
                    else -> "أدخل رمز المرور PIN المكون من 4 أرقام للمتابعة"
                }

                Text(
                    text = subtitle,
                    color = TextGray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage,
                        color = CyberRed,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // PIN Dots Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 24.dp)
            ) {
                for (i in 0 until maxPinLength) {
                    val active = i < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (active) CyberBlue else CardDark)
                            .border(width = 1.5.dp, color = if (active) CyberBlue else TextGray.copy(alpha = 0.3f), shape = CircleShape)
                    )
                }
            }

            // Virtual Numpad
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                val rowModifier = Modifier.fillMaxWidth().widthIn(max = 320.dp)
                
                val handleNumberClick: (String) -> Unit = { num ->
                    errorMessage = ""
                    if (enteredPin.length < maxPinLength) {
                        enteredPin += num
                        
                        // Check if complete
                        if (enteredPin.length == maxPinLength) {
                            if (!isPinSet) {
                                if (setupStep == 1) {
                                    firstPinTemp = enteredPin
                                    enteredPin = ""
                                    setupStep = 2
                                    errorMessage = "أعد إدخال الرمز لتأكيده"
                                } else {
                                    if (enteredPin == firstPinTemp) {
                                        viewModel.setPin(enteredPin)
                                        viewModel.setUnlocked(true)
                                    } else {
                                        enteredPin = ""
                                        setupStep = 1
                                        errorMessage = "الرمزان غير متطابقان، ابدأ من جديد!"
                                    }
                                }
                            } else {
                                val success = viewModel.verifyPin(enteredPin)
                                if (!success) {
                                    enteredPin = ""
                                    errorMessage = "رمز مرور خاطئ، يرجى المحاولة مجدداً"
                                }
                            }
                        }
                    }
                }

                val numbers = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )

                for (row in numbers) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = rowModifier
                    ) {
                        for (cell in row) {
                            KeypadButton(
                                text = cell,
                                onClick = { handleNumberClick(cell) },
                                modifier = Modifier.testTag("keypad_$cell")
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = rowModifier
                ) {
                    // Left action button: Biometric Trigger or Info
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .clickable(enabled = isPinSet && isBiometricEnabled) { onBiometricTrigger() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPinSet && isBiometricEnabled) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "بصمة الإصبع",
                                tint = CyberGreen,
                                modifier = Modifier.size(36.dp).testTag("fingerprint_button")
                            )
                        }
                    }

                    // Number 0
                    KeypadButton(
                        text = "0",
                        onClick = { handleNumberClick("0") },
                        modifier = Modifier.testTag("keypad_0")
                    )

                    // Right action button: Backspace
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .clickable(enabled = enteredPin.isNotEmpty()) {
                                if (enteredPin.isNotEmpty()) {
                                    enteredPin = enteredPin.dropLast(1)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (enteredPin.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Backspace,
                                contentDescription = "مسح",
                                tint = TextWhite,
                                modifier = Modifier.size(24.dp).testTag("backspace_button")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeypadButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(CardDark)
            .border(width = 1.dp, color = TextGray.copy(alpha = 0.1f), shape = CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = TextWhite,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
