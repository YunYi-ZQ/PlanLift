package app.planlift.fit.ui.today

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.planlift.fit.AppContainer
import app.planlift.fit.R
import app.planlift.fit.data.content.DayDto
import app.planlift.fit.data.content.ExerciseDto
import app.planlift.fit.data.content.PlanDto
import app.planlift.fit.data.content.title
import app.planlift.fit.data.local.EnrollmentEntity
import app.planlift.fit.data.local.SessionEntity
import app.planlift.fit.ui.common.Pill
import app.planlift.fit.ui.common.SectionCard
import app.planlift.fit.ui.common.rememberVm
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayState(
    val loading: Boolean = true,
    val plan: PlanDto? = null,
    val enrollment: EnrollmentEntity? = null,
    val day: DayDto? = null,
    val dayIndex: Int = 0,
    val recent: List<SessionEntity> = emptyList(),
    val ex: Map<String, ExerciseDto> = emptyMap()
)

class TodayViewModel(container: AppContainer) : ViewModel() {
    private val repo = container.repo
    private val content = container.content
    private val exFlow = kotlinx.coroutines.flow.MutableStateFlow<Map<String, ExerciseDto>>(emptyMap())

    init {
        viewModelScope.launch { exFlow.value = content.exercises() }
    }

    private val core = combine(
        repo.observeActiveEnrollment(),
        repo.observeRecentSessions(3)
    ) { enrollment, recent ->
        if (enrollment == null) {
            TodayState(loading = false, recent = recent)
        } else {
            val plan = repo.plan(enrollment.planId)
            val index = if (plan == null) 0 else repo.nextDayIndex(plan, enrollment)
            TodayState(
                loading = false,
                plan = plan,
                enrollment = enrollment,
                day = plan?.days?.getOrNull(index),
                dayIndex = index,
                recent = recent
            )
        }
    }

    val state: StateFlow<TodayState> = combine(core, exFlow) { s, ex ->
        s.copy(ex = ex)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayState())

    fun quit() = viewModelScope.launch { repo.quitPlan() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    isZh: Boolean,
    onBrowsePlans: () -> Unit,
    onStartWorkout: (Int) -> Unit
) {
    val vm = rememberVm { TodayViewModel(it) }
    val state by vm.state.collectAsState()
    var confirmQuit by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text(stringResource(R.string.today_title), fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.app_slogan),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val plan = state.plan
            val day = state.day
            if (plan == null || day == null) {
                item {
                    SectionCard(container = MaterialTheme.colorScheme.primaryContainer) {
                        Text(
                            stringResource(R.string.today_empty),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.today_hint),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(onClick = onBrowsePlans) {
                            Text(stringResource(R.string.action_browse_plans))
                        }
                    }
                }
            } else {
                item {
                    SectionCard(container = MaterialTheme.colorScheme.primaryContainer) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    plan.title(isZh),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    day.title(isZh),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Pill(
                                text = stringResource(
                                    R.string.week_progress,
                                    state.enrollment?.currentWeek ?: 1,
                                    plan.durationWeeks
                                ),
                                highlight = true
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pill(stringResource(R.string.sets_count, day.workSets))
                            Pill(stringResource(R.string.minutes_range, plan.sessionMin))
                            Pill(levelText(plan.level, isZh))
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { onStartWorkout(state.dayIndex) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text(
                                stringResource(R.string.action_start_workout),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                item {
                    SectionCard {
                        Text(
                            stringResource(R.string.today_plan_today),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(10.dp))
                        day.slots.filter { !it.isBlank }.forEach { slot ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    state.ex[slot.ex]?.let { if (isZh) it.zh else it.en } ?: slot.ex,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "${slot.sets} × ${slot.repMin}-${slot.repMax}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (state.recent.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.recent_sessions),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    items(state.recent) { s ->
                        SectionCard {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    if (isZh) s.dayNameZh else s.dayNameEn,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    stringResource(R.string.sets_volume, s.totalSets, s.totalVolumeKg),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    TextButton(
                        onClick = { confirmQuit = true },
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        Text(
                            stringResource(R.string.action_quit_plan),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    if (confirmQuit) {
        AlertDialog(
            onDismissRequest = { confirmQuit = false },
            title = { Text(stringResource(R.string.action_quit_plan)) },
            text = { Text(stringResource(R.string.quit_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmQuit = false
                    vm.quit()
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmQuit = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

private fun levelText(level: String, isZh: Boolean): String = when (level) {
    "beginner" -> if (isZh) "新手" else "Beginner"
    else -> if (isZh) "进阶" else "Intermediate"
}
