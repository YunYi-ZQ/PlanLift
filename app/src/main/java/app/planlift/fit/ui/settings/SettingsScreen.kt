package app.planlift.fit.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.planlift.fit.R
import app.planlift.fit.ui.common.SectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(isZh: Boolean) {
    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text(stringResource(R.string.nav_settings), fontWeight = FontWeight.SemiBold)
            })
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
                    stringResource(R.string.settings_about_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.settings_about_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SectionCard {
                Text(
                    stringResource(R.string.settings_next_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.settings_next_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SectionCard(container = MaterialTheme.colorScheme.errorContainer) {
                Text(
                    stringResource(R.string.settings_disclaimer_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.settings_disclaimer_body),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
