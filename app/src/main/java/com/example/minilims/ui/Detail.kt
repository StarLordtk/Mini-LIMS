package com.example.minilims.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.minilims.data.LabTest
import com.example.minilims.data.Patient
import com.example.minilims.data.PatientRate
import kotlinx.coroutines.flow.flowOf
import java.text.DateFormat
import java.util.Date

@Composable
fun ScannerOverlay(onResult: (String) -> Unit, onCancel: () -> Unit) {
    BackHandler(onBack = onCancel)
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        BarcodeScanner(onResult)
        Box(
            Modifier.align(Alignment.Center).size(width = 280.dp, height = 180.dp)
                .border(3.dp, Color.White, RoundedCornerShape(20.dp))
        )
        Text(
            "Point the camera at the barcode",
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(32.dp),
            color = Color.White
        )
        IconButton(onClick = onCancel, modifier = Modifier.statusBarsPadding().padding(8.dp)) {
            Icon(Icons.Default.Close, "Close", tint = Color.White)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleDetailScreen(vm: LimsViewModel, id: Long, onClose: () -> Unit) {
    val sample = vm.samples.collectAsState().value.firstOrNull { it.id == id }
    val lines by remember(id) { vm.sampleLines(id) }.collectAsState(emptyList())
    val edits = remember { mutableStateMapOf<Long, String>() }
    var confirmDelete by remember { mutableStateOf(false) }
    val df = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }

    if (sample == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    fun autoStatus(): String? =
        if (sample.status != "COMPLETED" && edits.values.any { it.isNotBlank() }) "IN_PROGRESS" else null

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(sample.barcode) },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = {
                        vm.saveResults(id, edits.toMap(), autoStatus()) { edits.clear(); vm.shareReport(id) }
                    }) { Icon(Icons.Default.Share, "PDF report") }
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "Delete sample") }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(Modifier.navigationBarsPadding().padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { vm.saveResults(id, edits.toMap(), autoStatus()) { edits.clear() } },
                        modifier = Modifier.weight(1f)
                    ) { Text("Save") }
                    Button(
                        onClick = { vm.saveResults(id, edits.toMap(), "COMPLETED") { onClose() } },
                        modifier = Modifier.weight(1f)
                    ) { Text("Mark completed") }
                }
            }
        }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Avatar(sample.patientName)
                        Column(Modifier.weight(1f)) {
                            Text(sample.patientName, style = MaterialTheme.typography.titleMedium)
                            Text(df.format(Date(sample.collectedAt)), style = MaterialTheme.typography.bodySmall)
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(money(sample.total), style = MaterialTheme.typography.titleLarge)
                            StatusPill(sample.status)
                        }
                    }
                }
            }
            item {
                val cost = lines.sumOf { it.labChargeApplied }
                if (cost > 0) {
                    val margin = sample.total - cost
                    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = softCardColors()) {
                        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            Column {
                                Text("Lab charges (internal)", style = MaterialTheme.typography.labelMedium, color = subtle)
                                Text(money(cost), style = MaterialTheme.typography.titleMedium)
                            }
                            Column {
                                Text("Margin", style = MaterialTheme.typography.labelMedium, color = subtle)
                                Text(money(margin), style = MaterialTheme.typography.titleMedium,
                                    color = if (margin < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            item { SectionLabel("Results") }
            items(lines, key = { it.testId }) { l ->
                val hint = listOf(l.unit, l.refRange.takeIf { it.isNotBlank() }?.let { "ref $it" })
                    .filterNot { it.isNullOrBlank() }.joinToString(" • ")
                Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = softCardColors()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(l.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            Pill(l.code)
                        }
                        OutlinedTextField(
                            value = edits[l.testId] ?: l.result,
                            onValueChange = { edits[l.testId] = it },
                            label = { Text("Result") },
                            supportingText = if (hint.isNotBlank()) ({ Text(hint) }) else null,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
    if (confirmDelete) ConfirmDialog("Delete this sample and its results?", "Delete",
        onConfirm = { confirmDelete = false; vm.deleteSample(id) { onClose() } },
        onDismiss = { confirmDelete = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSampleScreen(vm: LimsViewModel, onClose: () -> Unit) {
    val patients by vm.patients.collectAsState()
    val tests by vm.tests.collectAsState()
    var barcode by remember { mutableStateOf("") }
    var scanning by remember { mutableStateOf(false) }
    var patient by remember { mutableStateOf<Patient?>(null) }
    var menu by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<Long>()) }
    var error by remember { mutableStateOf<String?>(null) }
    var testQuery by remember { mutableStateOf("") }

    val ratesFlow = remember(patient) {
        patient?.let { vm.patientRates(it.id) } ?: flowOf(emptyList<PatientRate>())
    }
    val overrides by ratesFlow.collectAsState(emptyList())
    fun rateOf(t: LabTest) = overrides.firstOrNull { it.testId == t.id }?.rate ?: t.rate
    val chosen = tests.filter { it.id in selected }
    val total = chosen.sumOf { rateOf(it) }
    val shownTests = tests.filter {
        testQuery.isBlank() || it.name.contains(testQuery, true) || it.code.contains(testQuery, true)
    }
    val tubes = chosen.flatMap { it.sampleType.split(",") }.map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    if (scanning) {
        ScannerOverlay(onResult = { barcode = it; scanning = false }, onCancel = { scanning = false })
        return
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text("New sample") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(Modifier.navigationBarsPadding().padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Total", style = MaterialTheme.typography.labelMedium, color = subtle)
                        Text(money(total), style = MaterialTheme.typography.titleLarge)
                        if (tubes.isNotEmpty()) Text("Tubes: " + tubes.joinToString(", "),
                            style = MaterialTheme.typography.labelSmall, color = subtle)
                    }
                    Button(
                        enabled = barcode.isNotBlank() && patient != null && chosen.isNotEmpty(),
                        onClick = {
                            vm.registerSample(barcode.trim(), patient!!.id, chosen) { ok ->
                                if (ok) onClose() else error = "That barcode is already registered."
                            }
                        }
                    ) { Text("Save sample") }
                }
            }
        }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(barcode, { barcode = it; error = null }, label = { Text("Barcode") },
                        singleLine = true, modifier = Modifier.weight(1f))
                    FilledTonalButton(onClick = { scanning = true }) { Text("Scan") }
                }
            }
            error?.let { e -> item { Text(e, color = MaterialTheme.colorScheme.error) } }
            item {
                Box {
                    OutlinedButton(onClick = { menu = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Person, null)
                        Spacer(Modifier.width(8.dp))
                        Text(patient?.name ?: "Select patient", Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(menu, { menu = false }) {
                        if (patients.isEmpty()) DropdownMenuItem(text = { Text("Add a patient first") }, onClick = { menu = false })
                        patients.forEach { p ->
                            DropdownMenuItem(text = { Text(p.name) }, onClick = { patient = p; menu = false })
                        }
                    }
                }
            }
            item { SectionLabel("Tests (${chosen.size} selected)") }
            item { SearchField(testQuery, { testQuery = it }, "Search tests by name or code") }
            if (tests.isEmpty()) item { Text("No tests yet. Add some on the Tests tab.", color = subtle) }
            else if (shownTests.isEmpty()) item { Text("No matching tests.", color = subtle) }
            items(shownTests, key = { it.id }) { t ->
                val on = t.id in selected
                Card(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).clickable {
                        selected = if (on) selected - t.id else selected + t.id
                    },
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = if (on) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(on, null, Modifier.padding(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.name, style = MaterialTheme.typography.titleMedium)
                            Text(listOf(t.code, t.sampleType, t.reportDay).filter { it.isNotBlank() }.joinToString(" • "),
                                style = MaterialTheme.typography.labelMedium, color = subtle)
                        }
                        Text(money(rateOf(t)), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(end = 8.dp))
                    }
                }
            }
        }
    }
}
