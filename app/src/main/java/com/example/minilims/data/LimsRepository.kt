package com.example.minilims.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ReportData(val sample: Sample, val patient: Patient, val lines: List<SampleTestRow>)

class LimsRepository(private val db: AppDatabase) {
    val patients = db.patients().observeAll()
    val tests = db.tests().observeAll()
    val samples = db.samples().observeRows()

    fun patientRates(patientId: Long) = db.rates().observeFor(patientId)
    fun sampleLines(sampleId: Long) = db.samples().observeLines(sampleId)

    suspend fun addPatient(p: Patient) = db.patients().insert(p)
    suspend fun updatePatient(p: Patient) = db.patients().update(p)
    suspend fun addTest(t: LabTest): Boolean = try { db.tests().insert(t); true } catch (e: SQLiteConstraintException) { false }
    suspend fun updateTest(t: LabTest): Boolean = try { db.tests().update(t); true } catch (e: SQLiteConstraintException) { false }

    /** False when the record is still referenced by samples. */
    suspend fun deletePatient(id: Long): Boolean =
        try { db.patients().delete(id); true } catch (e: SQLiteConstraintException) { false }

    suspend fun deleteTest(id: Long): Boolean =
        try { db.tests().delete(id); true } catch (e: SQLiteConstraintException) { false }

    /** null rate removes the override so the default test rate applies again. */
    suspend fun setPatientRate(patientId: Long, testId: Long, rate: Double?) {
        if (rate == null) db.rates().delete(patientId, testId)
        else db.rates().upsert(PatientRate(patientId, testId, rate))
    }

    /** Patient-specific rate wins; otherwise the test's default rate. */
    suspend fun rateFor(patientId: Long, test: LabTest): Double =
        db.rates().get(patientId, test.id) ?: test.rate

    /** Returns false if the barcode already exists. */
    suspend fun registerSample(barcode: String, patientId: Long, tests: List<LabTest>): Boolean =
        db.withTransaction {
            if (db.samples().countBarcode(barcode) > 0) return@withTransaction false
            val priced = tests.map { it to rateFor(patientId, it) }
            val id = db.samples().insert(
                Sample(barcode = barcode, patientId = patientId, total = priced.sumOf { it.second })
            )
            db.samples().insertLines(priced.map { (t, r) -> SampleTest(id, t.id, r) })
            true
        }

    suspend fun saveResults(sampleId: Long, results: Map<Long, String>, status: String?) =
        db.withTransaction {
            results.forEach { (testId, r) -> db.samples().updateResult(sampleId, testId, r.trim()) }
            if (status != null) db.samples().setStatus(sampleId, status)
        }

    suspend fun setSampleStatus(id: Long, status: String) = db.samples().setStatus(id, status)
    suspend fun deleteSample(id: Long) = db.samples().delete(id)

    suspend fun report(sampleId: Long): ReportData? {
        val s = db.samples().getById(sampleId) ?: return null
        val p = db.patients().get(s.patientId) ?: return null
        return ReportData(s, p, db.samples().lines(sampleId))
    }

    /** Flush the write-ahead log so the .db file is complete before copying it. */
    suspend fun checkpoint() = withContext(Dispatchers.IO) {
        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
    }

    /** Room's schema fingerprint; a backup is only restorable if it matches. */
    suspend fun identityHash(): String? = withContext(Dispatchers.IO) {
        try {
            db.openHelper.readableDatabase
                .query("SELECT identity_hash FROM room_master_table WHERE id = 42")
                .use { if (it.moveToFirst()) it.getString(0) else null }
        } catch (e: Exception) { null }
    }
}
