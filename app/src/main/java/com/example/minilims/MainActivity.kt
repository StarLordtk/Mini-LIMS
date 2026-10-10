package com.example.minilims

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.minilims.ui.*

private data class Dest(val title: String, val icon: ImageVector)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LimsTheme { AppRoot() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot() {
    val vm: LimsViewModel = viewModel()
    val dests = listOf(
        Dest("Samples", Icons.AutoMirrored.Filled.List),
        Dest("Patients", Icons.Default.Person),
        Dest("Tests", Icons.Default.CheckCircle),
        Dest("Settings", Icons.Default.Settings)
    )
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var newSample by remember { mutableStateOf(false) }
    var openSample by remember { mutableStateOf<Long?>(null) }

    BackHandler(enabled = newSample || openSample != null) { newSample = false; openSample = null }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        val open = openSample
        when {
            newSample -> NewSampleScreen(vm) { newSample = false }
            open != null -> SampleDetailScreen(vm, open) { openSample = null }
            else -> Scaffold(
                topBar = { TopAppBar(title = { Text(dests[tab].title, style = MaterialTheme.typography.headlineSmall) }) },
                bottomBar = {
                    NavigationBar {
                        dests.forEachIndexed { i, d ->
                            NavigationBarItem(
                                selected = tab == i,
                                onClick = { tab = i },
                                icon = { Icon(d.icon, contentDescription = d.title) },
                                label = { Text(d.title) }
                            )
                        }
                    }
                }
            ) { pad ->
                Box(Modifier.padding(pad).fillMaxSize()) {
                    when (tab) {
                        0 -> SamplesScreen(vm, onNew = { newSample = true }, onOpen = { openSample = it })
                        1 -> PatientsScreen(vm)
                        2 -> TestsScreen(vm)
                        else -> SettingsScreen(vm)
                    }
                }
            }
        }
    }
}
