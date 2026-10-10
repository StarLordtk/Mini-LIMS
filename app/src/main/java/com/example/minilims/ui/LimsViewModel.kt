package com.example.minilims.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.minilims.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LimsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = LimsRepository(AppDatabase.get(app))
    private val prefs = app.getSharedPreferences("prefs", 0)

    private fun <T> kotlinx.coroutines.flow.Flow<List<T>>.hot() =
        stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val patients = repo.patients.hot()
    val tests = repo.tests.hot()
    val samples = repo.samples.hot()
    val labName = MutableStateFlow(prefs.getString("lab", "My Laboratory") ?: "My Laboratory")

    fun setLabName(v: String) { prefs.edit().putString("lab", v).apply(); labName.value = v }

    fun patientRates(patientId: Long) = repo.patientRates(patientId)
    fun sampleLines(sampleId: Long) = repo.sampleLines(sampleId)

    fun addPatient(p: Patient) = viewModelScope.launch { repo.addPatient(p) }
    fun updatePatient(p: Patient) = viewModelScope.launch { repo.updatePatient(p) }
    fun deletePatient(id: Long, onResult: (Boolean) -> Unit) = viewModelScope.launch { onResult(repo.deletePatient(id)) }

    fun addTest(t: LabTest, onResult: (Boolean) -> Unit) = viewModelScope.launch { onResult(repo.addTest(t)) }
    fun updateTest(t: LabTest, onResult: (Boolean) -> Unit) = viewModelScope.launch { onResult(repo.updateTest(t)) }
    fun deleteTest(id: Long, onResult: (Boolean) -> Unit) = viewModelScope.launch { onResult(repo.deleteTest(id)) }

    fun setPatientRate(patientId: Long, testId: Long, rate: Double?) =
        viewModelScope.launch { repo.setPatientRate(patientId, testId, rate) }

    fun registerSample(barcode: String, patientId: Long, tests: List<LabTest>, onResult: (Boolean) -> Unit) =
        viewModelScope.launch { onResult(repo.registerSample(barcode, patientId, tests)) }

    fun saveResults(sampleId: Long, results: Map<Long, String>, status: String?, onDone: () -> Unit = {}) =
        viewModelScope.launch { repo.saveResults(sampleId, results, status); onDone() }

    fun setStatus(id: Long, status: String) = viewModelScope.launch { repo.setSampleStatus(id, status) }
    fun deleteSample(id: Long, onDone: () -> Unit) = viewModelScope.launch { repo.deleteSample(id); onDone() }

    fun shareReport(sampleId: Long) = viewModelScope.launch {
        val data = repo.report(sampleId) ?: return@launch
        val ctx = getApplication<Application>()
        val file = withContext(Dispatchers.IO) { Files.reportPdf(ctx, labName.value, data) }
        Files.share(ctx, file, "application/pdf")
    }

    fun shareBackup() = viewModelScope.launch {
        val ctx = getApplication<Application>()
        repo.checkpoint()
        val file = withContext(Dispatchers.IO) { Files.backup(ctx) }
        Files.share(ctx, file, "application/octet-stream")
    }

    fun saveBackupTo(uri: Uri, onDone: (Boolean) -> Unit) = viewModelScope.launch {
        val ctx = getApplication<Application>()
        repo.checkpoint()
        onDone(withContext(Dispatchers.IO) { Files.writeBackup(ctx, uri) })
    }

    /** onResult(null) = success, caller should restart the app. */
    fun restoreFrom(uri: Uri, onResult: (String?) -> Unit) = viewModelScope.launch {
        val ctx = getApplication<Application>()
        repo.checkpoint()
        val hash = repo.identityHash()
        onResult(withContext(Dispatchers.IO) { Files.restoreFromUri(ctx, uri, hash) })
    }

    fun undoRestore(onResult: (String?) -> Unit) = viewModelScope.launch {
        val ctx = getApplication<Application>()
        repo.checkpoint()
        val hash = repo.identityHash()
        onResult(withContext(Dispatchers.IO) { Files.undoRestore(ctx, hash) })
    }
}
