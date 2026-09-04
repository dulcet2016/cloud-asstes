package com.manha.eventassettracker.repository

import com.manha.eventassettracker.data.AppDatabase
import com.manha.eventassettracker.data.entity.AdminEntity
import com.manha.eventassettracker.data.entity.AssetEntity
import com.manha.eventassettracker.data.entity.EventEntity
import com.manha.eventassettracker.data.entity.QrLabelEntity
import com.manha.eventassettracker.data.entity.ScanEntity
import com.manha.eventassettracker.data.entity.ScanType
import com.manha.eventassettracker.data.entity.StaffEntity
import com.manha.eventassettracker.util.derivePrefix
import com.manha.eventassettracker.util.nextSequentialIds
import kotlinx.coroutines.flow.Flow
import java.util.UUID

sealed class ScanResult {
    data class Success(val asset: AssetEntity, val type: String) : ScanResult()
    data class AlreadyInThatState(val asset: AssetEntity) : ScanResult()
    data class WrongEvent(val asset: AssetEntity, val expectedEventName: String) : ScanResult()
    data class UnknownAsset(val assetId: String) : ScanResult()
}

class AppRepository(private val db: AppDatabase) {

    // ---- Admins ----
    fun observeAdmins(): Flow<List<AdminEntity>> = db.adminDao().observeAll()
    suspend fun findAdminByUsername(username: String) = db.adminDao().findByUsername(username)
    suspend fun addAdmin(name: String, username: String, password: String, note: String) {
        db.adminDao().insert(
            AdminEntity(id = "adm-${UUID.randomUUID()}", name = name, username = username, password = password, note = note)
        )
    }
    suspend fun updateAdmin(admin: AdminEntity) = db.adminDao().update(admin)
    suspend fun deleteAdmin(admin: AdminEntity) = db.adminDao().delete(admin)

    // ---- Staff ----
    fun observeStaff(): Flow<List<StaffEntity>> = db.staffDao().observeAll()
    suspend fun findStaffByMobile(mobile: String) = db.staffDao().findByMobile(mobile)
    suspend fun loginOrCreateStaff(name: String, mobile: String): StaffEntity {
        val existing = db.staffDao().findByMobile(mobile)
        if (existing != null) {
            val updated = existing.copy(name = name)
            db.staffDao().update(updated)
            return updated
        }
        val created = StaffEntity(id = "stf-${UUID.randomUUID()}", name = name, mobile = mobile)
        db.staffDao().insert(created)
        return created
    }
    suspend fun addStaff(name: String, mobile: String, note: String) {
        db.staffDao().insert(StaffEntity(id = "stf-${UUID.randomUUID()}", name = name, mobile = mobile, note = note))
    }
    suspend fun updateStaff(staff: StaffEntity) = db.staffDao().update(staff)
    suspend fun deleteStaff(staff: StaffEntity) = db.staffDao().delete(staff)

    // ---- Events ----
    fun observeEvents(): Flow<List<EventEntity>> = db.eventDao().observeAll()
    fun observeEvent(id: String): Flow<EventEntity?> = db.eventDao().observeById(id)
    suspend fun addEvent(name: String, date: String, venue: String, note: String): EventEntity {
        val event = EventEntity(id = "evt-${UUID.randomUUID()}", name = name, eventDate = date, venue = venue, note = note)
        db.eventDao().insert(event)
        return event
    }
    suspend fun updateEvent(event: EventEntity) = db.eventDao().update(event)
    suspend fun deleteEvent(event: EventEntity) = db.eventDao().delete(event)

    // ---- Assets ----
    fun observeAssets(): Flow<List<AssetEntity>> = db.assetDao().observeAll()
    fun observeAssetsForEvent(eventId: String): Flow<List<AssetEntity>> = db.assetDao().observeByEvent(eventId)
    suspend fun findAsset(assetId: String) = db.assetDao().findById(assetId)
    suspend fun updateAsset(asset: AssetEntity) = db.assetDao().update(asset)
    suspend fun deleteAsset(asset: AssetEntity) = db.assetDao().delete(asset)

    // ---- Scans ----
    fun observeScans(): Flow<List<ScanEntity>> = db.scanDao().observeAll()
    fun observeScansForEvent(eventId: String): Flow<List<ScanEntity>> = db.scanDao().observeByEvent(eventId)
    suspend fun updateScan(scan: ScanEntity) = db.scanDao().update(scan)
    suspend fun deleteScan(scan: ScanEntity) = db.scanDao().delete(scan)

    /** Records an OUT or RETURN scan for [assetId]. If the asset has never been scanned before
     *  (a fresh label that was never assigned) it is created in inventory automatically, using
     *  the name/category encoded in the QR itself, if available. */
    suspend fun recordScan(
        assetId: String,
        qrName: String,
        qrCategory: String,
        type: String,
        event: EventEntity,
        staffId: String,
        staffName: String
    ): ScanResult {
        val cleanId = assetId.trim().uppercase()
        if (cleanId.isEmpty()) return ScanResult.UnknownAsset(cleanId)

        var asset = db.assetDao().findById(cleanId)
        if (asset == null) {
            val label = db.qrLabelDao().findByGroup(qrName, qrCategory).find { it.assetId == cleanId }
            asset = AssetEntity(
                assetId = cleanId,
                name = qrName.ifBlank { label?.name ?: cleanId },
                category = qrCategory.ifBlank { label?.category ?: "" },
                status = ScanType.RETURN
            )
        }

        if (asset.status == type) {
            return ScanResult.AlreadyInThatState(asset)
        }

        if (type == ScanType.RETURN && asset.status == ScanType.OUT &&
            asset.currentEventId != null && asset.currentEventId != event.id
        ) {
            return ScanResult.WrongEvent(asset, asset.currentEventName ?: "another event")
        }

        val updated = asset.copy(
            status = type,
            currentEventId = if (type == ScanType.OUT) event.id else asset.currentEventId,
            currentEventName = if (type == ScanType.OUT) event.name else asset.currentEventName,
            lastStaffId = staffId,
            lastStaffName = staffName,
            lastScanAt = System.currentTimeMillis()
        )
        db.assetDao().upsert(updated)
        db.scanDao().insert(
            ScanEntity(
                assetId = updated.assetId,
                assetName = updated.name,
                category = updated.category,
                eventId = event.id,
                eventName = event.name,
                type = type,
                staffId = staffId,
                staffName = staffName
            )
        )
        return ScanResult.Success(updated, type)
    }

    // ---- QR Labels ----
    fun observeQrLabels(): Flow<List<QrLabelEntity>> = db.qrLabelDao().observeAll()

    suspend fun generateLabels(name: String, category: String, sizeCm: Int, count: Int): List<QrLabelEntity> {
        val prefix = derivePrefix(name).ifBlank { "AST" }
        val currentMax = db.qrLabelDao().maxNumberForPrefix(prefix, prefix.length) ?: 0
        val ids = nextSequentialIds(prefix, count, currentMax)
        val labels = ids.map { id ->
            QrLabelEntity(assetId = id, name = name, category = category, sizeCm = sizeCm)
        }
        db.qrLabelDao().insertAll(labels)
        return labels
    }

    suspend fun labelsForGroup(name: String, category: String) = db.qrLabelDao().findByGroup(name, category)
    suspend fun deleteUnassignedInGroup(name: String, category: String) = db.qrLabelDao().deleteUnassignedInGroup(name, category)
    suspend fun deleteLabel(label: QrLabelEntity) = db.qrLabelDao().delete(label)
}
