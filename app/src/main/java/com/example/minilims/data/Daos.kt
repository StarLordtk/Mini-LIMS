package com.example.minilims.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
    @Insert suspend fun insert(p: Patient): Long
    @Update suspend fun update(p: Patient)
    @Query("DELETE FROM patients WHERE id=:id") suspend fun delete(id: Long)
    @Query("SELECT * FROM patients WHERE id=:id") suspend fun get(id: Long): Patient?
    @Query("SELECT * FROM patients ORDER BY name") fun observeAll(): Flow<List<Patient>>
}

@Dao
interface TestDao {
    @Insert suspend fun insert(t: LabTest): Long
    @Update suspend fun update(t: LabTest)
    @Query("DELETE FROM tests WHERE id=:id") suspend fun delete(id: Long)
    @Query("SELECT * FROM tests ORDER BY name") fun observeAll(): Flow<List<LabTest>>
}

@Dao
interface RateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(r: PatientRate)
    @Query("DELETE FROM patient_rates WHERE patientId=:patientId AND testId=:testId")
    suspend fun delete(patientId: Long, testId: Long)
    @Query("SELECT rate FROM patient_rates WHERE patientId=:patientId AND testId=:testId")
    suspend fun get(patientId: Long, testId: Long): Double?
    @Query("SELECT * FROM patient_rates WHERE patientId=:patientId")
    fun observeFor(patientId: Long): Flow<List<PatientRate>>
}

@Dao
interface SampleDao {
    @Insert suspend fun insert(s: Sample): Long
    @Insert suspend fun insertLines(lines: List<SampleTest>)
    @Query("UPDATE samples SET status=:status WHERE id=:id") suspend fun setStatus(id: Long, status: String)
    @Query("SELECT COUNT(*) FROM samples WHERE barcode=:barcode") suspend fun countBarcode(barcode: String): Int
    @Query("SELECT * FROM samples WHERE id=:id") suspend fun getById(id: Long): Sample?
    @Query("DELETE FROM samples WHERE id=:id") suspend fun delete(id: Long)
    @Query("UPDATE sample_tests SET result=:result WHERE sampleId=:sampleId AND testId=:testId")
    suspend fun updateResult(sampleId: Long, testId: Long, result: String)

    @Query(
        "SELECT s.id, s.barcode, p.name AS patientName, s.total, s.status, s.collectedAt " +
        "FROM samples s JOIN patients p ON p.id = s.patientId ORDER BY s.collectedAt DESC"
    )
    fun observeRows(): Flow<List<SampleRow>>

    @Query(
        "SELECT t.id AS testId, t.code AS code, t.name AS name, t.unit AS unit, t.refRange AS refRange, " +
        "st.rateApplied AS rateApplied, st.result AS result " +
        "FROM sample_tests st JOIN tests t ON t.id = st.testId WHERE st.sampleId=:sampleId ORDER BY t.name"
    )
    fun observeLines(sampleId: Long): Flow<List<SampleTestRow>>

    @Query(
        "SELECT t.id AS testId, t.code AS code, t.name AS name, t.unit AS unit, t.refRange AS refRange, " +
        "st.rateApplied AS rateApplied, st.result AS result " +
        "FROM sample_tests st JOIN tests t ON t.id = st.testId WHERE st.sampleId=:sampleId ORDER BY t.name"
    )
    suspend fun lines(sampleId: Long): List<SampleTestRow>
}
