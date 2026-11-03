package app.planlift.fit.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.planlift.fit.R
import app.planlift.fit.data.sync.SyncConfigStore
import app.planlift.fit.ui.common.SectionCard
import app.planlift.fit.ui.common.rememberVm
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val vm = rememberVm { SettingsViewModel(it, ctx.applicationContext) }
    val state by vm.state.collectAsState()
    var showPassword by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sync_title), fontWeight = FontWeight.SemiBold) },
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
                    stringResource(R.string.sync_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.url,
                    onValueChange = vm::updateUrl,
                    label = { Text(stringResource(R.string.sync_url)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.user,
                    onValueChange = vm::updateUser,
                    label = { Text(stringResource(R.string.sync_user)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.password,
                    onValueChange = vm::updatePassword,
                    label = { Text(stringResource(R.string.sync_password)) },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        TextButton(onClick = { showPassword = !showPassword }) {
                            Text(
                                stringResource(
                                    if (showPassword) R.string.password_hide else R.string.password_show
                                ),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.dir,
                    onValueChange = vm::updateDir,
                    label = { Text(stringResource(R.string.sync_dir)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = vm::testConnection,
                        enabled = !state.busy,
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.sync_test)) }
                    Button(
                        onClick = vm::syncNow,
                        enabled = !state.busy,
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.sync_now)) }
                }
                if (state.syncStatus.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        state.syncStatus,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (state.lastSyncAt > 0) {
                    Text(
                        stringResource(
                            R.string.sync_last,
                            SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                                .format(Date(state.lastSyncAt))
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SectionCard {
                Text(
                    stringResource(R.string.sync_how_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.sync_how_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
