package app.planlift.fit.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.planlift.fit.R
import app.planlift.fit.data.export.Exporter
import app.planlift.fit.ui.common.SectionCard
import app.planlift.fit.ui.common.rememberVm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val vm = rememberVm { SettingsViewModel(it, ctx.applicationContext) }
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_export_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("‹ " + stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionCard {
                Text(
                    stringResource(R.string.settings_export_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { vm.export(json = true) { Exporter.share(ctx, it) } },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.settings_export_json)) }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { vm.export(json = false) { Exporter.share(ctx, it) } },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.settings_export_csv)) }
                if (state.lastExport.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.settings_export_done, state.lastExport),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
