package com.manha.eventassettracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.manha.eventassettracker.data.AppDatabase
import com.manha.eventassettracker.data.DEFAULT_ADMIN_USERNAME
import com.manha.eventassettracker.data.entity.AdminEntity
import com.manha.eventassettracker.data.entity.AssetEntity
import com.manha.eventassettracker.data.entity.EventEntity
import com.manha.eventassettracker.data.entity.QrLabelEntity
import com.manha.eventassettracker.data.entity.ScanEntity
import com.manha.eventassettracker.data.entity.ScanType
import com.manha.eventassettracker.data.entity.StaffEntity
import com.manha.eventassettracker.datastore.AppSession
import com.manha.eventassettracker.datastore.SettingsDataStore
import com.manha.eventassettracker.repository.AppRepository
import com.manha.eventassettracker.repository.ScanResult
import com.manha.eventassettracker.util.LabelItem
import com.manha.eventassettracker.util.QrPayload
import com.manha.eventassettracker.util.ScanFeedback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class ScanUiEvent {
    data class Scanned(val assetId: String, val name: String, val type: String) : ScanUiEvent()
    data class AlreadyDone(val assetId: String, val name: String, val currentStatus: String) : ScanUiEvent()
    data class Error(val message: String) : ScanUiEvent()
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = AppRepository(db)
    private val settings = SettingsDataStore(application)
    private val feedback = ScanFeedback(application)

    private val _sessionLoaded = MutableStateFlow(false)
    val sessionLoaded: StateFlow<Boolean> = _sessionLoaded.asStateFlow()

    val session: StateFlow<AppSession> = settings.session
        .onEach { _sessionLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSession())

    val events: StateFlow<List<EventEntity>> = repository.observeEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val assets: StateFlow<List<AssetEntity>> = repository.observeAssets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scans: StateFlow<List<ScanEntity>> = repository.observeScans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val staffList: StateFlow<List<StaffEntity>> = repository.observeStaff()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminList: StateFlow<List<AdminEntity>> = repository.observeAdmins()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val qrLabels: StateFlow<List<QrLabelEntity>> = repository.observeQrLabels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scanMode: StateFlow<String> = settings.scanMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScanType.OUT)

    val activeEventId: StateFlow<String> = settings.activeEventId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val deviceName: StateFlow<String> = settings.deviceName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun setDeviceName(name: String) = viewModelScope.launch { settings.setDeviceName(name.trim()) }

    val activeEvent: StateFlow<EventEntity?> = combine(events, activeEventId) { list, id ->
        list.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _scanEvents = MutableStateFlow<ScanUiEvent?>(null)
    val scanEvents: StateFlow<ScanUiEvent?> = _scanEvents.asStateFlow()

    private var lastScannedRaw: String? = null
    private var lastScannedAt = 0L

    // ---------------- Auth ----------------

    fun loginAdmin(username: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val admin = repository.findAdminByUsername(username.trim())
            if (admin != null && admin.password == password) {
                settings.setSession(AppSession.ROLE_ADMIN, admin.id, admin.name)
                onResult(true, "")
            } else {
                onResult(false, "Galat Login ID ya Password.")
            }
        }
    }

    fun loginStaff(name: String, mobile: String, onResult: (Boolean, String) -> Unit) {
        val cleanMobile = mobile.filter { it.isDigit() }
        if (name.isBlank()) { onResult(false, "Naam likhein."); return }
        if (cleanMobile.length != 10) { onResult(false, "Sahi 10-digit mobile number likhein."); return }
        viewModelScope.launch {
            val staff = repository.loginOrCreateStaff(name.trim(), cleanMobile)
            settings.setSession(AppSession.ROLE_STAFF, staff.id, staff.name)
            onResult(true, "")
        }
    }

    fun logout() {
        viewModelScope.launch { settings.clearSession() }
    }

    // ---------------- Active event / scan mode ----------------

    fun setActiveEvent(event: EventEntity) {
        viewModelScope.launch { settings.setActiveEvent(event.id, event.name) }
    }

    fun setScanMode(mode: String) {
        viewModelScope.launch { settings.setScanMode(mode) }
    }

    // ---------------- Events ----------------

    fun addEvent(name: String, date: String, venue: String, note: String, onDone: (EventEntity) -> Unit = {}) {
        viewModelScope.launch { onDone(repository.addEvent(name, date, venue, note)) }
    }

    fun updateEvent(event: EventEntity) = viewModelScope.launch { repository.updateEvent(event) }
    fun deleteEvent(event: EventEntity) = viewModelScope.launch { repository.deleteEvent(event) }

    // ---------------- Staff / Admin ----------------

    fun addStaff(name: String, mobile: String, note: String) = viewModelScope.launch {
        repository.addStaff(name, mobile.filter { it.isDigit() }, note)
    }
    fun updateStaff(staff: StaffEntity) = viewModelScope.launch { repository.updateStaff(staff) }
    fun deleteStaff(staff: StaffEntity) = viewModelScope.launch {
        repository.deleteStaff(staff)
        if (session.value.id == staff.id) settings.clearSession()
    }

    fun addAdmin(name: String, username: String, password: String, note: String) = viewModelScope.launch {
        repository.addAdmin(name, username, password, note)
    }
    fun updateAdmin(admin: AdminEntity) = viewModelScope.launch { repository.updateAdmin(admin) }
    fun deleteAdmin(admin: AdminEntity) = viewModelScope.launch { repository.deleteAdmin(admin) }

    fun isDefaultAdminUsername(username: String) = username == DEFAULT_ADMIN_USERNAME

    // ---------------- QR Labels ----------------

    fun generateLabels(name: String, category: String, sizeCm: Int, count: Int, onDone: (List<QrLabelEntity>) -> Unit) {
        viewModelScope.launch { onDone(repository.generateLabels(name.trim(), category.trim(), sizeCm, count)) }
    }

    fun deleteUnassignedInGroup(name: String, category: String) = viewModelScope.launch {
        repository.deleteUnassignedInGroup(name, category)
    }

    fun deleteLabel(label: QrLabelEntity) = viewModelScope.launch { repository.deleteLabel(label) }

    fun labelItemsFor(labels: List<QrLabelEntity>): List<LabelItem> =
        labels.map { LabelItem(it.assetId, it.name, it.category, it.sizeCm) }

    // ---------------- Assets / Scans ----------------

    fun updateAsset(asset: AssetEntity) = viewModelScope.launch { repository.updateAsset(asset) }
    fun deleteAsset(asset: AssetEntity) = viewModelScope.launch { repository.deleteAsset(asset) }
    fun updateScan(scan: ScanEntity) = viewModelScope.launch { repository.updateScan(scan) }
    fun deleteScan(scan: ScanEntity) = viewModelScope.launch { repository.deleteScan(scan) }

    /** Called with the raw text decoded from a scanned QR code. Debounces rapid duplicate
     *  reads of the same code (CameraX can call back many times per second), records the
     *  scan, and plays the appropriate beep. */
    fun onQrScanned(rawText: String) {
        val now = System.currentTimeMillis()
        if (rawText == lastScannedRaw && now - lastScannedAt < 2000) return
        lastScannedRaw = rawText
        lastScannedAt = now

        val event = activeEvent.value
        val currentSession = session.value
        if (event == null) {
            feedback.error()
            _scanEvents.value = ScanUiEvent.Error("Pehle ek Event select karein.")
            return
        }
        if (!currentSession.isLoggedIn) {
            feedback.error()
            _scanEvents.value = ScanUiEvent.Error("Pehle login karein.")
            return
        }

        val payload = QrPayload.parse(rawText)
        if (payload.assetId.isBlank()) {
            feedback.error()
            _scanEvents.value = ScanUiEvent.Error("QR code padha nahi gaya.")
            return
        }

        viewModelScope.launch {
            val result = repository.recordScan(
                assetId = payload.assetId,
                qrName = payload.name,
                qrCategory = payload.category,
                type = scanMode.value,
                event = event,
                staffId = currentSession.id,
                staffName = currentSession.name
            )
            when (result) {
                is ScanResult.Success -> {
                    feedback.success()
                    _scanEvents.value = ScanUiEvent.Scanned(result.asset.assetId, result.asset.name, result.type)
                }
                is ScanResult.AlreadyInThatState -> {
                    feedback.error()
                    _scanEvents.value = ScanUiEvent.AlreadyDone(result.asset.assetId, result.asset.name, result.asset.status)
                }
                is ScanResult.WrongEvent -> {
                    feedback.error()
                    _scanEvents.value = ScanUiEvent.Error(
                        "${result.asset.name} (${result.asset.assetId}) was sent OUT for \"${result.expectedEventName}\" — return it under that event."
                    )
                }
                is ScanResult.UnknownAsset -> {
                    feedback.error()
                    _scanEvents.value = ScanUiEvent.Error("Asset ID khali hai.")
                }
            }
        }
    }

    /** Used by the checkbox-based Missing Items screen: ticking a box marks that asset as
     *  RETURNED directly, without needing a camera scan or any WhatsApp copy-paste. */
    fun markReturned(asset: AssetEntity) {
        val event = events.value.find { it.id == asset.currentEventId } ?: return
        val currentSession = session.value
        if (!currentSession.isLoggedIn) return
        viewModelScope.launch {
            val result = repository.recordScan(
                assetId = asset.assetId,
                qrName = asset.name,
                qrCategory = asset.category,
                type = ScanType.RETURN,
                event = event,
                staffId = currentSession.id,
                staffName = currentSession.name
            )
            if (result is ScanResult.Success) feedback.success() else feedback.error()
        }
    }

    fun consumeScanEvent() {
        _scanEvents.value = null
    }

    // ---------------- Reports ----------------

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    fun buildEventReportText(eventId: String, type: String): String {
        val event = events.value.find { it.id == eventId } ?: return ""
        val eventScans = scans.value.filter { it.eventId == eventId && it.type == type }
            .sortedBy { it.timestamp }
        val header = if (type == ScanType.OUT) "OUT REPORT" else "RETURN REPORT"
        val lines = StringBuilder()
        lines.append("*${event.name}*\n")
        lines.append("$header — ${eventScans.size} items\n")
        if (deviceName.value.isNotBlank()) lines.append("Device: ${deviceName.value}\n")
        lines.append("Generated: ${dateFormat.format(Date())}\n\n")
        eventScans.forEachIndexed { index, scan ->
            lines.append("${index + 1}. ${scan.assetName} — ${scan.assetId}\n")
        }
        if (eventScans.isEmpty()) lines.append("(No items scanned yet)\n")
        return lines.toString()
    }

    /** A single comprehensive text report across the whole database — every event, every
     *  asset's current status, and overall totals — ready to paste/share on WhatsApp. */
    fun buildAllDataReport(): String {
        val sb = StringBuilder()
        sb.append("*Event Asset Tracker — Full Data Report*\n")
        if (deviceName.value.isNotBlank()) sb.append("Device: ${deviceName.value}\n")
        sb.append("Generated: ${dateFormat.format(Date())}\n\n")

        sb.append("Events: ${events.value.size}\n")
        sb.append("Total Assets: ${assets.value.size}\n")
        sb.append("Currently OUT: ${assets.value.count { it.status == ScanType.OUT }}\n")
        sb.append("Currently RETURNED: ${assets.value.count { it.status == ScanType.RETURN }}\n")
        sb.append("Total Scans Logged: ${scans.value.size}\n")
        sb.append("Staff: ${staffList.value.size}   Admins: ${adminList.value.size}\n\n")

        sb.append("*Events (newest first)*\n")
        events.value.forEach { event ->
            val outCount = scans.value.count { it.eventId == event.id && it.type == ScanType.OUT }
            val returnCount = scans.value.count { it.eventId == event.id && it.type == ScanType.RETURN }
            sb.append("• ${event.name} (${event.eventDate}) — OUT $outCount / RETURN $returnCount\n")
        }

        val stillOut = assets.value.filter { it.status == ScanType.OUT }
        if (stillOut.isNotEmpty()) {
            sb.append("\n*Assets still OUT*\n")
            stillOut.forEachIndexed { i, asset ->
                sb.append("${i + 1}. ${asset.name} (${asset.assetId}) — ${asset.currentEventName ?: "?"}\n")
            }
        }

        return sb.toString()
    }

    override fun onCleared() {
        super.onCleared()
        feedback.release()
    }
}
