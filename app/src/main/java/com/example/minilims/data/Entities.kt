package com.example.minilims.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class Patient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val age: Int? = null,
    val gender: String = "",
    val address: String = ""
)

/** Master test list with the default (walk-in) rate. */
@Entity(tableName = "tests", indices = [Index("code", unique = true)])
data class LabTest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val rate: Double,
    val unit: String = "",
    val refRange: String = "",
    val sampleType: String = "",
    val reportDay: String = "",
    /** What the lab charges us for this test (our cost). Margin = rate - labCharge. */
    val labCharge: Double = 0.0
)

/** Optional per-patient (or per-client) override of a test's rate. */
@Entity(
    tableName = "patient_rates",
    primaryKeys = ["patientId", "testId"],
    foreignKeys = [
        ForeignKey(Patient::class, ["id"], ["patientId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(LabTest::class, ["id"], ["testId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("testId")]
)
data class PatientRate(val patientId: Long, val testId: Long, val rate: Double)

@Entity(
    tableName = "samples",
    indices = [Index("barcode", unique = true), Index("patientId")],
    foreignKeys = [ForeignKey(Patient::class, ["id"], ["patientId"])]
)
data class Sample(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val barcode: String,
    val patientId: Long,
    val collectedAt: Long = System.currentTimeMillis(),
    val status: String = "COLLECTED", // COLLECTED -> IN_PROGRESS -> COMPLETED
    val total: Double = 0.0
)

/** Tests ordered on a sample; rate is frozen at order time, result entered later. */
@Entity(
    tableName = "sample_tests",
    primaryKeys = ["sampleId", "testId"],
    foreignKeys = [
        ForeignKey(Sample::class, ["id"], ["sampleId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(LabTest::class, ["id"], ["testId"])
    ],
    indices = [Index("testId")]
)
data class SampleTest(
    val sampleId: Long,
    val testId: Long,
    val rateApplied: Double,
    val result: String = "",
    val labChargeApplied: Double = 0.0
)

/** Read-only rows for queries. */
data class SampleRow(
    val id: Long,
    val barcode: String,
    val patientName: String,
    val total: Double,
    val status: String,
    val collectedAt: Long
)

data class SampleTestRow(
    val testId: Long,
    val code: String,
    val name: String,
    val unit: String,
    val refRange: String,
    val rateApplied: Double,
    val result: String,
    val labChargeApplied: Double
)
