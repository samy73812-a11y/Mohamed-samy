package com.example

import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SecurityEvent(
    val id: String,
    val title: String,
    val description: String,
    val timestamp: String,
    val level: EventLevel
)

enum class EventLevel {
    SUCCESS,
    WARNING,
    INFO,
    DANGER
}

class CameraManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences = application.getSharedPreferences("CameraShieldPrefs", Context.MODE_PRIVATE)
    private val dpm = application.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComponent = ComponentName(application, CameraAdminReceiver::class.java)

    // UI States
    private val _isDeviceAdminActive = MutableStateFlow(false)
    val isDeviceAdminActive = _isDeviceAdminActive.asStateFlow()

    private val _isCameraGloballyDisabled = MutableStateFlow(false)
    val isCameraGloballyDisabled = _isCameraGloballyDisabled.asStateFlow()

    private val _isCameraLocalBlocked = MutableStateFlow(false)
    val isCameraLocalBlocked = _isCameraLocalBlocked.asStateFlow()

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked = _isUnlocked.asStateFlow()

    private val _isPinSet = MutableStateFlow(false)
    val isPinSet = _isPinSet.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(false)
    val isBiometricEnabled = _isBiometricEnabled.asStateFlow()

    private val _events = MutableStateFlow<List<SecurityEvent>>(emptyList())
    val events = _events.asStateFlow()

    init {
        checkSecurityState()
        loadPreferences()
        addEvent("إنشاء النظام", "تم تهيئة محرك حماية الكاميرا بنجاح", EventLevel.INFO)
    }

    fun checkSecurityState() {
        val adminActive = dpm.isAdminActive(adminComponent)
        _isDeviceAdminActive.value = adminActive

        if (adminActive) {
            _isCameraGloballyDisabled.value = dpm.getCameraDisabled(adminComponent)
        } else {
            _isCameraGloballyDisabled.value = false
        }
    }

    private fun loadPreferences() {
        _isPinSet.value = prefs.contains("secure_pin")
        _isBiometricEnabled.value = prefs.getBoolean("use_biometrics", true)
        _isCameraLocalBlocked.value = prefs.getBoolean("local_camera_block", false)
        
        // Load events
        val savedEventsCsv = prefs.getString("security_events", "") ?: ""
        if (savedEventsCsv.isNotEmpty()) {
            val list = mutableListOf<SecurityEvent>()
            savedEventsCsv.split("|||").forEach { line ->
                val parts = line.split(";;;")
                if (parts.size == 5) {
                    list.add(
                        SecurityEvent(
                            id = parts[0],
                            title = parts[1],
                            description = parts[2],
                            timestamp = parts[3],
                            level = EventLevel.valueOf(parts[4])
                        )
                    )
                }
            }
            _events.value = list
        } else {
            addEvent("تأمين التطبيق", "تأمين الكاميرا والوصول نشط وجاهز", EventLevel.SUCCESS)
        }
    }

    fun addEvent(title: String, description: String, level: EventLevel) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timeStr = sdf.format(Date())
        val newEvent = SecurityEvent(
            id = System.currentTimeMillis().toString(),
            title = title,
            description = description,
            timestamp = timeStr,
            level = level
        )
        val updatedList = (listOf(newEvent) + _events.value).take(30)
        _events.value = updatedList
        saveEventsCsv(updatedList)
    }

    private fun saveEventsCsv(list: List<SecurityEvent>) {
        val csv = list.joinToString("|||") { event ->
            "${event.id};;;${event.title};;;${event.description};;;${event.timestamp};;;${event.level.name}"
        }
        prefs.edit().putString("security_events", csv).apply()
    }

    fun setPin(pin: String) {
        prefs.edit().putString("secure_pin", pin).apply()
        _isPinSet.value = true
        addEvent("ضبط رمز الأمان", "تم تعيين رمز مرور PIN جديد للتطبيق", EventLevel.SUCCESS)
    }

    fun verifyPin(pin: String): Boolean {
        val savedPin = prefs.getString("secure_pin", "") ?: ""
        val success = savedPin == pin
        if (success) {
            _isUnlocked.value = true
            addEvent("تسجيل دخول", "تم فتح التطبيق باستخدام رمز المرور", EventLevel.SUCCESS)
        } else {
            addEvent("محاولة فشل", "محاولة إدخال رمز مرور خاطئ", EventLevel.DANGER)
        }
        return success
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("use_biometrics", enabled).apply()
        _isBiometricEnabled.value = enabled
        addEvent("تعديل البصمة", if (enabled) "تم تفعيل تسجيل الدخول بالبصمة" else "تم تعطيل تسجيل الدخول بالبصمة", EventLevel.INFO)
    }

    fun setUnlocked(unlocked: Boolean) {
        _isUnlocked.value = unlocked
        if (unlocked) {
            addEvent("التحقق الحيوي", "تم فتح التطبيق بالبصمة الحيوية بنجاح", EventLevel.SUCCESS)
        } else {
            addEvent("الخروج الآمن", "تم قفل التطبيق تلقائياً للمحافظة على الأمان", EventLevel.INFO)
        }
    }

    fun resetPin() {
        prefs.edit().remove("secure_pin").apply()
        _isPinSet.value = false
        _isUnlocked.value = false
        addEvent("إعادة ضبط الرمز", "تم مسح رمز المرور للتهيئة من جديد", EventLevel.WARNING)
    }

    fun lockApp() {
        _isUnlocked.value = false
    }

    // Camera action controls
    fun toggleLocalCameraBlock(blocked: Boolean) {
        prefs.edit().putBoolean("local_camera_block", blocked).apply()
        _isCameraLocalBlocked.value = blocked
        if (blocked) {
            addEvent("حجب محلي", "تم فرض حظر محلي على الكاميرا داخل التطبيق", EventLevel.WARNING)
        } else {
            addEvent("فك الحجب المحلي", "تم تفعيل الكاميرا محلياً داخل التطبيق", EventLevel.SUCCESS)
        }
    }

    fun applyGlobalCameraBlock(blocked: Boolean) {
        if (_isDeviceAdminActive.value) {
            try {
                dpm.setCameraDisabled(adminComponent, blocked)
                _isCameraGloballyDisabled.value = blocked
                if (blocked) {
                    addEvent("تعطيل الكاميرا كاملة", "تم إيقاف تشغيل الكاميرا على مستوى الجهاز بالكامل", EventLevel.DANGER)
                } else {
                    addEvent("تنشيط الكاميرا كاملة", "تم إعادة تفعيل الكاميرا على مستوى نظام الهاتف", EventLevel.SUCCESS)
                }
            } catch (e: SecurityException) {
                _isCameraGloballyDisabled.value = false
                addEvent("خطأ أمان", "فشل تطبيق حظر الكاميرا: نقص صلاحيات مدير الجهاز", EventLevel.DANGER)
            }
        } else {
            addEvent("تنبيه حماية", "يرجى تفعيل وضع مدير الجهاز أولاً لتتمكن من حظر الكاميرا نهائياً", EventLevel.WARNING)
        }
    }

    fun getAdminComponent(): ComponentName = adminComponent
}
