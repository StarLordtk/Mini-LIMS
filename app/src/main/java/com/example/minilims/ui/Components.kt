package com.example.minilims.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

fun money(v: Double) = "%.2f".format(v)

/** 3500.0 -> "3500", 12.5 -> "12.5" (for editing fields). */
fun plain(v: Double): String = if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()

val subtle: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

@Composable
fun softCardColors() = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)

@Composable
fun Avatar(name: String, modifier: Modifier = Modifier) {
    Box(
        modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            name.trim().take(1).uppercase().ifBlank { "?" },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun StatusPill(status: String) {
    val cs = MaterialTheme.colorScheme
    val (bg, fg, label) = when (status) {
        "COMPLETED" -> Triple(cs.primaryContainer, cs.onPrimaryContainer, "Completed")
        "IN_PROGRESS" -> Triple(cs.tertiaryContainer, cs.onTertiaryContainer, "In progress")
        else -> Triple(cs.secondaryContainer, cs.onSecondaryContainer, "Collected")
    }
    Surface(color = bg, contentColor = fg, shape = CircleShape) {
        Text(label, Modifier.padding(horizontal = 10.dp, vertical = 3.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun Pill(text: String) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer, shape = CircleShape) {
        Text(text, Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun SearchField(value: String, onChange: (String) -> Unit, hint: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(hint) },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = {
            if (value.isNotEmpty()) IconButton(onClick = { onChange("") }) { Icon(Icons.Default.Clear, "Clear") }
        },
        singleLine = true,
        shape = CircleShape,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun StatTile(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(modifier, shape = MaterialTheme.shapes.large, colors = softCardColors()) {
        Column(Modifier.padding(14.dp)) {
            Text(value.toString(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium, color = subtle)
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.outline)
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = subtle, textAlign = TextAlign.Center)
    }
}
