package com.example.minilims.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ImportResult(val added: Int, val updated: Int, val skipped: Int)

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
            db.samples().insertLines(priced.map { (t, r) -> SampleTest(id, t.id, r, labChargeApplied = t.labCharge) })
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

    suspend fun testCount(): Int = db.tests().count()

    /**
     * Merges a rate list into the test table. Tests are matched by name (case-insensitive).
     * Existing tests are updated with whatever the file provides (patient rate, lab charge,
     * sample type, report day...); unknown names are added when they have a patient rate.
     */
    suspend fun importTests(rows: List<CsvTestRow>): ImportResult = db.withTransaction {
        val existing = db.tests().getAll()
        val byName = existing.associateBy { it.name.trim().uppercase() }.toMutableMap()
        val codes = existing.map { it.code.uppercase() }.toMutableSet()
        val codePattern = Regex("^T(\\d+)$")
        var next = (existing.mapNotNull { codePattern.find(it.code.uppercase())?.groupValues?.get(1)?.toIntOrNull() }
            .maxOrNull() ?: 0) + 1
        var added = 0
        var updated = 0
        var skipped = 0
        val toInsert = mutableListOf<LabTest>()

        for (r in rows) {
            val key = r.name.trim().uppercase()
            if (key.isEmpty()) { skipped++; continue }
            val ex = byName[key]
            if (ex != null) {
                if (ex.id == 0L) { skipped++; continue } // duplicate row inside the same file
                val u = ex.copy(
                    rate = r.rate ?: ex.rate,
                    labCharge = r.labCharge ?: ex.labCharge,
                    sampleType = r.sampleType.ifBlank { ex.sampleType },
                    reportDay = r.reportDay.ifBlank { ex.reportDay },
                    unit = r.unit.ifBlank { ex.unit },
                    refRange = r.refRange.ifBlank { ex.refRange }
                )
                db.tests().update(u)
                byName[key] = u
                updated++
            } else {
                val rate = r.rate
                if (rate == null) { skipped++; continue }
                var code = r.code.trim().uppercase()
                if (code.isEmpty() || code in codes) {
                    do { code = "T%03d".format(next++) } while (code in codes)
                }
                codes.add(code)
                val t = LabTest(code = code, name = r.name.trim(), rate = rate, unit = r.unit,
                    refRange = r.refRange, sampleType = r.sampleType, reportDay = r.reportDay,
                    labCharge = r.labCharge ?: 0.0)
                toInsert.add(t)
                byName[key] = t
                added++
            }
        }
        if (toInsert.isNotEmpty()) db.tests().insertAll(toInsert)
        ImportResult(added, updated, skipped)
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
