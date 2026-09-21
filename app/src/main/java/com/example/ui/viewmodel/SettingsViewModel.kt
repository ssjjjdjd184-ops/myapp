package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.location.LocationManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupManager
import com.example.data.database.AppDatabase
import com.example.data.preferences.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val backupManager = BackupManager(database)
    val prefManager = PreferenceManager.getInstance(application)

    val currency = prefManager.currency
    val distanceUnit = prefManager.distanceUnit
    val speedUnit = prefManager.speedUnit
    val themeMode = prefManager.themeMode

    private val _gpsEnabled = MutableStateFlow(false)
    val gpsEnabled: StateFlow<Boolean> = _gpsEnabled.asStateFlow()

    private val _backupJson = MutableStateFlow<String?>(null)
    val backupJson: StateFlow<String?> = _backupJson.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        checkGpsStatus()
    }

    fun checkGpsStatus() {
        val lm = getApplication<Application>().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        _gpsEnabled.value = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    fun setCurrency(newVal: String) = prefManager.setCurrency(newVal)
    fun setDistanceUnit(newVal: String) = prefManager.setDistanceUnit(newVal)
    fun setSpeedUnit(newVal: String) = prefManager.setSpeedUnit(newVal)
    fun setThemeMode(newVal: String) = prefManager.setThemeMode(newVal)

    fun createBackup(onReady: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = backupManager.exportBackupJson()
                _backupJson.value = json
                onReady(json)
            } catch (e: Exception) {
                _statusMessage.value = "Backup failed: ${e.message}"
            }
        }
    }

    fun restoreBackup(jsonString: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = backupManager.importBackupJson(jsonString)
            if (result.isSuccess) {
                onComplete(true, "Restore completed successfully!")
            } else {
                onComplete(false, result.exceptionOrNull()?.message ?: "Restore failed")
            }
        }
    }

    fun clearAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            backupManager.clearAllData()
            onComplete()
        }
    }

    fun dismissStatusMessage() {
        _statusMessage.value = null
    }
}
