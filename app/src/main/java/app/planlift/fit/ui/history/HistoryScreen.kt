package app.planlift.fit.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.planlift.fit.AppContainer
import app.planlift.fit.R
import app.planlift.fit.data.local.SessionEntity
import app.planlift.fit.ui.common.SectionCard
import app.planlift.fit.ui.common.rememberVm
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryViewModel(container: AppContainer) : ViewModel() {
    val sessions = container.repo.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(isZh: Boolean) {
    val vm = rememberVm { HistoryViewModel(it) }
    val sessions by vm.sessions.collectAsState()
    val fmt = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text(stringResource(R.string.nav_history), fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.history_count, sessions.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            })
        }
    ) { padding ->
        if (sessions.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.history_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sessions) { s: SessionEntity ->
                    SectionCard {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (isZh) s.dayNameZh else s.dayNameEn,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    fmt.format(Date(s.date)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                stringResource(R.string.sets_volume, s.totalSets, s.totalVolumeKg),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
