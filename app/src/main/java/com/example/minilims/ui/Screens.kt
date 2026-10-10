package com.example.minilims.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.minilims.data.LabTest
import com.example.minilims.data.Patient
import java.text.DateFormat
import java.util.Date

// ---------------- shared dialogs ----------------
@Composable
fun FormDialog(
    title: String,
    labels: List<String>,
    initial: List<String> = labels.map { "" },
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val values = remember { mutableStateListOf<String>().apply { addAll(initial) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                labels.forEachIndexed { i, l ->
                    OutlinedTextField(values[i], { values[i] = it }, label = { Text(l) }, singleLine = true)
                }
            }
        },
        confirmButton = { TextButton({ onSave(values.toList()) }) { Text("Save") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ConfirmDialog(text: String, confirmLabel: String = "Yes", onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(text) },
        confirmButton = { TextButton(onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun MessageDialog(text: String, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, text = { Text(text) },
        confirmButton = { TextButton(onDismiss) { Text("OK") } })
}

// ---------------- Samples ----------------
@Composable
fun SamplesScreen(vm: LimsViewModel, onNew: () -> Unit, onOpen: (Long) -> Unit) {
    val rows by vm.samples.collectAsState()
    var query by remember { mutableStateOf("") }
    val df = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    val shown = rows.filter {
        query.isBlank() || it.barcode.contains(query, true) || it.patientName.contains(query, true)
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { SearchField(query, { query = it }, "Search barcode or patient") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Collected", rows.count { it.status == "COLLECTED" }, Modifier.weight(1f))
                    StatTile("In progress", rows.count { it.status == "IN_PROGRESS" }, Modifier.weight(1f))
                    StatTile("Completed", rows.count { it.status == "COMPLETED" }, Modifier.weight(1f))
                }
            }
            if (shown.isEmpty()) item {
                EmptyState(Icons.AutoMirrored.Filled.List,
                    if (rows.isEmpty()) "No samples yet" else "No matches",
                    if (rows.isEmpty()) "Tap New sample to scan a barcode and register one." else "Try a different search.")
            }
            items(shown, key = { it.id }) { s ->
                Card(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).clickable { onOpen(s.id) },
                    shape = MaterialTheme.shapes.large, colors = softCardColors()
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Avatar(s.patientName)
                        Column(Modifier.weight(1f)) {
                            Text(s.patientName, style = MaterialTheme.typography.titleMedium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(s.barcode, style = MaterialTheme.typography.bodyMedium, color = subtle)
                            Text(df.format(Date(s.collectedAt)), style = MaterialTheme.typography.labelSmall, color = subtle)
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(money(s.total), style = MaterialTheme.typography.titleMedium)
                            StatusPill(s.status)
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = onNew,
            icon = { Icon(Icons.Default.Add, null) },
            text = { Text("New sample") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }
}

// ---------------- Patients ----------------
@Composable
fun PatientsScreen(vm: LimsViewModel) {
    val patients by vm.patients.collectAsState()
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Patient?>(null) }
    var deleting by remember { mutableStateOf<Patient?>(null) }
    var ratesFor by remember { mutableStateOf<Patient?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    val labels = listOf("Name", "Phone", "Age", "Gender", "Address")
    val shown = patients.filter { query.isBlank() || it.name.contains(query, true) || it.phone.contains(query) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { SearchField(query, { query = it }, "Search name or phone") }
            if (shown.isEmpty()) item {
                EmptyState(Icons.Default.Person,
                    if (patients.isEmpty()) "No patients yet" else "No matches",
                    if (patients.isEmpty()) "Add your first patient or client." else "Try a different search.")
            }
            items(shown, key = { it.id }) { p ->
                Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = softCardColors()) {
                    Column(Modifier.padding(start = 14.dp, end = 6.dp, top = 14.dp, bottom = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Avatar(p.name)
                            Column(Modifier.weight(1f)) {
                                Text(p.name, style = MaterialTheme.typography.titleMedium)
                                val info = listOf(p.phone, p.age?.let { "$it y" }, p.gender)
                                    .filterNot { it.isNullOrBlank() }.joinToString(" • ")
                                if (info.isNotBlank()) Text(info, style = MaterialTheme.typography.bodyMedium, color = subtle)
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically) {
                            TextButton({ ratesFor = p }) { Text("Special rates") }
                            IconButton({ editing = p }) { Icon(Icons.Default.Edit, "Edit") }
                            IconButton({ deleting = p }) { Icon(Icons.Default.Delete, "Delete") }
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { adding = true },
            icon = { Icon(Icons.Default.Add, null) },
            text = { Text("Patient") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }

    if (adding) FormDialog("New patient", labels, onDismiss = { adding = false }) { v ->
        if (v[0].isNotBlank()) vm.addPatient(Patient(name = v[0].trim(), phone = v[1].trim(),
            age = v[2].toIntOrNull(), gender = v[3].trim(), address = v[4].trim()))
        adding = false
    }
    editing?.let { p ->
        FormDialog("Edit patient", labels,
            listOf(p.name, p.phone, p.age?.toString() ?: "", p.gender, p.address),
            onDismiss = { editing = null }) { v ->
            if (v[0].isNotBlank()) vm.updatePatient(p.copy(name = v[0].trim(), phone = v[1].trim(),
                age = v[2].toIntOrNull(), gender = v[3].trim(), address = v[4].trim()))
            editing = null
        }
    }
    deleting?.let { p ->
        ConfirmDialog("Delete ${p.name}?", "Delete", onConfirm = {
            vm.deletePatient(p.id) { ok -> if (!ok) message = "${p.name} has samples. Delete those samples first." }
            deleting = null
        }, onDismiss = { deleting = null })
    }
    ratesFor?.let { PatientRatesDialog(vm, it) { ratesFor = null } }
    message?.let { MessageDialog(it) { message = null } }
}

@Composable
fun PatientRatesDialog(vm: LimsViewModel, patient: Patient, onDismiss: () -> Unit) {
    val tests by vm.tests.collectAsState()
    val existing by remember(patient) { vm.patientRates(patient.id) }.collectAsState(emptyList())
    val edits = remember { mutableStateMapOf<Long, String>() }
    var q by remember { mutableStateOf("") }
    val shownTests = tests.filter { q.isBlank() || it.name.contains(q, true) || it.code.contains(q, true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Special rates – ${patient.name}") },
        text = {
            if (tests.isEmpty()) Text("Add tests first.")
            else Column {
                SearchField(q, { q = it }, "Search tests")
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shownTests, key = { it.id }) { t ->
                    val current = existing.firstOrNull { it.testId == t.id }?.rate?.let { plain(it) } ?: ""
                    OutlinedTextField(
                        value = edits[t.id] ?: current,
                        onValueChange = { edits[t.id] = it },
                        label = { Text("${t.name} (default ${money(t.rate)})") },
                        supportingText = { Text("Leave empty to use the default rate") },
                        singleLine = true
                    )
                }
                }
            }
        },
        confirmButton = {
            TextButton({
                edits.forEach { (testId, text) -> vm.setPatientRate(patient.id, testId, text.toDoubleOrNull()) }
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
    )
}

// ---------------- Tests (default rate list) ----------------
@Composable
fun TestsScreen(vm: LimsViewModel) {
    val tests by vm.tests.collectAsState()
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<LabTest?>(null) }
    var deleting by remember { mutableStateOf<LabTest?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    val labels = listOf("Code", "Name", "Default rate", "Unit", "Reference range", "Sample type", "Report day", "Lab charge (our cost)")
    val shown = tests.filter { query.isBlank() || it.name.contains(query, true) || it.code.contains(query, true) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { SearchField(query, { query = it }, "Search tests") }
            if (shown.isEmpty()) item {
                EmptyState(Icons.Default.CheckCircle,
                    if (tests.isEmpty()) "No tests yet" else "No matches",
                    if (tests.isEmpty()) "Add the tests your lab offers, with their rates." else "Try a different search.")
            }
            items(shown, key = { it.id }) { t ->
                Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = softCardColors()) {
                    Column(Modifier.padding(start = 14.dp, end = 6.dp, top = 14.dp, bottom = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(t.name, style = MaterialTheme.typography.titleMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Pill(t.code)
                                    if (t.sampleType.isNotBlank()) Pill(t.sampleType)
                                    val extra = listOf(t.reportDay, t.unit, t.refRange).filter { it.isNotBlank() }.joinToString(" • ")
                                    if (extra.isNotBlank()) Text(extra, style = MaterialTheme.typography.labelMedium, color = subtle)
                                }
                                if (t.labCharge > 0) {
                                    val margin = t.rate - t.labCharge
                                    Text("Lab ${money(t.labCharge)} • Margin ${money(margin)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (margin < 0) MaterialTheme.colorScheme.error else subtle)
                                }
                            }
                            Text(money(t.rate), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton({ editing = t }) { Icon(Icons.Default.Edit, "Edit") }
                            IconButton({ deleting = t }) { Icon(Icons.Default.Delete, "Delete") }
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { adding = true },
            icon = { Icon(Icons.Default.Add, null) },
            text = { Text("Test") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }

    if (adding) FormDialog("New test", labels, onDismiss = { adding = false }) { v ->
        val rate = v[2].toDoubleOrNull()
        if (v[0].isNotBlank() && v[1].isNotBlank() && rate != null) {
            vm.addTest(LabTest(code = v[0].trim(), name = v[1].trim(), rate = rate, unit = v[3].trim(), refRange = v[4].trim(), sampleType = v[5].trim(), reportDay = v[6].trim(), labCharge = v[7].toDoubleOrNull() ?: 0.0)) { ok ->
                if (!ok) message = "That test code already exists."
            }
        }
        adding = false
    }
    editing?.let { t ->
        FormDialog("Edit test", labels,
            listOf(t.code, t.name, plain(t.rate), t.unit, t.refRange, t.sampleType, t.reportDay, if (t.labCharge > 0) plain(t.labCharge) else ""),
            onDismiss = { editing = null }) { v ->
            val rate = v[2].toDoubleOrNull()
            if (v[0].isNotBlank() && v[1].isNotBlank() && rate != null) {
                vm.updateTest(t.copy(code = v[0].trim(), name = v[1].trim(), rate = rate, unit = v[3].trim(), refRange = v[4].trim(), sampleType = v[5].trim(), reportDay = v[6].trim(), labCharge = v[7].toDoubleOrNull() ?: 0.0)) { ok ->
                    if (!ok) message = "That test code already exists."
                }
            }
            editing = null
        }
    }
    deleting?.let { t ->
        ConfirmDialog("Delete ${t.name}?", "Delete", onConfirm = {
            vm.deleteTest(t.id) { ok -> if (!ok) message = "${t.name} is used on existing samples and can't be deleted." }
            deleting = null
        }, onDismiss = { deleting = null })
    }
    message?.let { MessageDialog(it) { message = null } }
}
