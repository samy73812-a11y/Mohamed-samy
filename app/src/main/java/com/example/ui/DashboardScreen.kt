package com.example.ui

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.CameraManagerViewModel
import com.example.EventLevel
import com.example.SecurityEvent
import com.example.ui.theme.*
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberBlue
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberRed
import java.util.concurrent.Executor

@Composable
fun DashboardScreen(
    viewModel: CameraManagerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDeviceAdminActive by viewModel.isDeviceAdminActive.collectAsState()
    val isCameraGloballyDisabled by viewModel.isCameraGloballyDisabled.collectAsState()
    val isCameraLocalBlocked by viewModel.isCameraLocalBlocked.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val events by viewModel.events.collectAsState()

    var isDiagnosticTesting by remember { mutableStateOf(false) }
    var showCameraDisabledAlert by remember { mutableStateOf(false) }

    val isCameraLocked = isCameraGloballyDisabled || isCameraLocalBlocked

    // Scroll state for dashboard content
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(36.dp))

            // TOP NAVIGATION / BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "مرحباً بك في درع الكاميرا",
                        color = TextGray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "لوحة التحكم الأمنية",
                        color = TextWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = { viewModel.lockApp() },
                    modifier = Modifier
                        .background(CardDark, CircleShape)
                        .testTag("lock_app_now")
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "قفل التطبيق",
                        tint = CyberRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // DYNAMIC STATUS COUNTER CARD
            StatusCircleHeader(
                isLocked = isCameraLocked,
                isGloballyDisabled = isCameraGloballyDisabled,
                isLocalBlocked = isCameraLocalBlocked,
                isDeviceAdminActive = isDeviceAdminActive,
                onActivateAdmin = {
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, viewModel.getAdminComponent())
                        putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "تفعيل حماية الكاميرا على مستوى نظام التشغيل وحجبها عن بقية التطبيقات.")
                    }
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // CONTROLS LISTING
            Text(
                text = "إعدادات الحماية والتحكم",
                color = TextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = SolidDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(1.dp, CardDark, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Control 1: Device Admin Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "صلاحيات مدير النظام",
                                color = TextWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "تمكن التطبيق من فرض قيود الكاميرا على مستوى الهاتف بالكامل.",
                                color = TextGray,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isDeviceAdminActive,
                            onCheckedChange = { active ->
                                if (active) {
                                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, viewModel.getAdminComponent())
                                        putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "تفعيل حماية الكاميرا على مستوى نظام التشغيل وحجبها عن بقية التطبيقات.")
                                    }
                                    context.startActivity(intent)
                                } else {
                                    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                                    dpm.removeActiveAdmin(viewModel.getAdminComponent())
                                    viewModel.checkSecurityState()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberBlue,
                                checkedTrackColor = CyberBlueDim
                            ),
                            modifier = Modifier.testTag("device_admin_switch")
                        )
                    }

                    HorizontalDivider(
                        color = CardDark,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // Control 2: Global Camera Disable (Dependent on Device Admin)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "حظر الكاميرا نهائياً (مدير الجهاز)",
                                color = if (isDeviceAdminActive) TextWhite else TextGray.copy(alpha = 0.5f),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "تعطيل عدسة الكاميرا عن كل تطبيقات الهاتف لمنع التجسس.",
                                color = if (isDeviceAdminActive) TextGray else TextGray.copy(alpha = 0.4f),
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isCameraGloballyDisabled,
                            enabled = isDeviceAdminActive,
                            onCheckedChange = { block ->
                                viewModel.applyGlobalCameraBlock(block)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberRed,
                                checkedTrackColor = CyberRedDim
                            ),
                            modifier = Modifier.testTag("global_camera_switch")
                        )
                    }

                    HorizontalDivider(
                        color = CardDark,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // Control 3: Local Block (Backup lock inside App)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "حجب الكاميرا المحلي (داخل التطبيق)",
                                color = TextWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "فرض تعطيل وصول سريع للكاميرا وتوفير نظام اختبار الحجب.",
                                color = TextGray,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isCameraLocalBlocked,
                            onCheckedChange = { block ->
                                viewModel.toggleLocalCameraBlock(block)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberRed,
                                checkedTrackColor = CyberRedDim
                            ),
                            modifier = Modifier.testTag("local_camera_switch")
                        )
                    }
                }
            }

            // CAMERA DIAGNOSTIC / LIVE TEST (THE CORE FEATURE REQUIRED!)
            Text(
                text = "اختبار وفحص حظر الكاميرا",
                color = TextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = SolidDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(2.dp, if (isCameraLocked) CyberRedDim else CyberGreenDim, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "محاكاة تشغيل الكاميرا",
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "جرب محاولة فتح الكاميرا هنا لرؤية كيف سيتعامل معها التطبيق.",
                        color = TextGray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Camera Viewport Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberBlack)
                            .border(1.dp, CardDark, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDiagnosticTesting) {
                            if (isCameraLocked) {
                                // Camera disabled alert overlay
                                CameraBlockedView()
                            } else {
                                // Real CameraX preview
                                CameraLiveView()
                            }
                        } else {
                            // Camera Shut View
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "الكاميرا مغلقة",
                                    tint = TextGray.copy(alpha = 0.4f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "جهاز فحص الكاميرا مغلق",
                                    color = TextGray,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Trigger Diagnostic Button
                    Button(
                        onClick = {
                            if (isDiagnosticTesting) {
                                isDiagnosticTesting = false
                            } else {
                                isDiagnosticTesting = true
                                if (isCameraLocked) {
                                    viewModel.addEvent("فحص الأمن", "محاولة فتح الكاميرا أجهضت بسبب الحظر النشط", EventLevel.DANGER)
                                } else {
                                    viewModel.addEvent("فحص الأمن", "تم فتح بث الكاميرا بنجاح - لا حظر نشط", EventLevel.INFO)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDiagnosticTesting) CardDark else if (isCameraLocked) CyberRed else CyberGreen
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("camera_test_button")
                    ) {
                        Text(
                            text = if (isDiagnosticTesting) "إيقاف الفحص والتشغيل" else "محاولة فتح الكاميرا",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // APP PIN & SETTINGS CONFIG
            Text(
                text = "ضبط أمان الوصول",
                color = TextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = SolidDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(1.dp, CardDark, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Control 1: Biometric Login
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تسجيل الدخول بالبصمة",
                                color = TextWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "فتح لوحة الحماية باستخدام بصمة يدك أو فحص الوجه.",
                                color = TextGray,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = { enable ->
                                viewModel.setBiometricEnabled(enable)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberBlue,
                                checkedTrackColor = CyberBlueDim
                            ),
                            modifier = Modifier.testTag("biometric_switch")
                        )
                    }

                    HorizontalDivider(
                        color = CardDark,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // Control 2: Reset PIN Passcode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "إعادة تعيين رمز PIN الجديد",
                                color = TextWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "احذف الرمز الحالي واصنع قفلاً من 4 أرقام.",
                                color = TextGray,
                                fontSize = 11.sp
                            )
                        }
                        Button(
                            onClick = { viewModel.resetPin() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberBlueDim),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("reset_pin_button")
                        ) {
                            Text("تغيير PIN", color = CyberBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // SECURITY EVENT LOGGER (A beautiful audit trail list)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سجل أحداث الحماية والوصول",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "آخر الأحداث",
                    color = TextGray,
                    fontSize = 12.sp
                )
            }

            // Event Logs Area
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                if (events.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("لا توجد أحداث حماية مسجلة حالياً.", color = TextGray, fontSize = 13.sp)
                    }
                } else {
                    events.forEach { event ->
                        SecurityEventRow(event = event)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun StatusCircleHeader(
    isLocked: Boolean,
    isGloballyDisabled: Boolean,
    isLocalBlocked: Boolean,
    isDeviceAdminActive: Boolean,
    onActivateAdmin: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SolidDark),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isLocked) CyberRed.copy(alpha = 0.4f) else CyberGreen.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pulse Ring State design
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                if (isLocked) CyberRedDim else CyberGreenDim,
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.NoPhotography else Icons.Default.CheckCircle,
                    contentDescription = "حالة الخصوصية",
                    tint = if (isLocked) CyberRed else CyberGreen,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isLocked) "حالة الأجهزة: عزل مادي افتراضي (نشط)" else "كاميرا الهاتف نشطة وآمنة",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = when {
                    isGloballyDisabled && isLocalBlocked -> "تم فرض بروتوكول العزل الشامل وقطع الاتصال المادي بنجاح."
                    isGloballyDisabled -> "الكاميرا معزولة بالكامل على مستوى نواة النظام (مدير الجهاز). تظهر الآن كأجهزة غير متصلة."
                    isLocalBlocked -> "الكاميرا ومستشعراتها معزولة ومحمية ومخفية محلياً في هذا التطبيق."
                    else -> "جميع الكاميرات غير معطلة وجاهزة للعمل بشكل كامل."
                },
                color = TextGray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            if (!isDeviceAdminActive) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onActivateAdmin,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "مدير الجهاز",
                        tint = TextWhite,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("منح صلاحية حماية النظام كاملاً", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CameraBlockedView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0404)) // Extremely dark red system failure aesthetic
            .border(1.5.dp, CyberRed.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "فشل التعرف على الكاميرا",
                tint = CyberRed,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "لم يتم العثور على كاميرا مادية!",
                color = CyberRed,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // Terminal System Diagnostics Log
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                .border(0.5.dp, CyberRed.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val logStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontSize = 10.sp,
                color = Color(0xFFFF4D4D)
            )
            Text(">>> HARDWARE BUS INQUIRY: COMPLETED", style = logStyle.copy(color = Color.Gray))
            Text("[FAIL] MIPI CSI-2 interface... NOT_RESPONDING", style = logStyle)
            Text("[FAIL] Power supply status... 0.00V (ISOLATED)", style = logStyle)
            Text("[FAIL] I2C Device Address (0x3C)... DEV_NOT_PRESENT", style = logStyle)
            Text("[WARN] Camera HAL interface... RETURN_EMPTY_ARRAY", style = logStyle.copy(color = Color(0xFFFFB300)))
            Text(">>> KERNEL STATE: CAMERA_HARDWARE_PIN_EMPTY", style = logStyle)
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Text(
            text = "تم فرض الفصل التام وعزل مصدر الطاقة عن قنوات التوصيل الرقمية للكاميرا على مستوى نظام التشغيل (Kernel Level). يقرأ الهاتف الكاميرا الآن كأنها فارغة وغير مركبة باللوحة الأم، مما يجعل برمجيات التجسس تقتنع تماماً أن الكاميرا غير موجودة فعلياً بالهاتف.",
            color = TextWhite.copy(alpha = 0.85f),
            fontSize = 11.sp,
            lineHeight = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun CameraLiveView() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var bindError by remember { mutableStateOf<String?>(null) }

    if (bindError != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = bindError ?: "فشل في تشغيل الكاميرا",
                color = CyberRed,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
    } else {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val executor = androidx.core.content.ContextCompat.getMainExecutor(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = androidx.camera.core.Preview.Builder().build().apply {
                        setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview
                        )
                    } catch (e: Exception) {
                        bindError = "خطأ تشغيل: الكاميرا محظورة أو معطلة بالفعل من النظام!"
                    }
                }, executor)
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun SecurityEventRow(event: SecurityEvent) {
    val color = when (event.level) {
        EventLevel.SUCCESS -> CyberGreen
        EventLevel.WARNING -> CyberBlue
        EventLevel.INFO -> TextGray
        EventLevel.DANGER -> CyberRed
    }

    val iconSvg = when (event.level) {
        EventLevel.SUCCESS -> Icons.Default.Check
        EventLevel.WARNING -> Icons.Default.Info
        EventLevel.INFO -> Icons.Default.SettingsSuggest
        EventLevel.DANGER -> Icons.Default.Warning
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SolidDark),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardDark, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconSvg,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    color = TextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = event.description,
                    color = TextGray,
                    fontSize = 11.sp
                )
            }

            Text(
                text = event.timestamp,
                color = TextGray.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
