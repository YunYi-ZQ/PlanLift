package app.planlift.fit.ui.plans

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import app.planlift.fit.data.content.SlotDto
import app.planlift.fit.data.content.title
import app.planlift.fit.ui.common.Pill
import app.planlift.fit.ui.common.SectionCard
import app.planlift.fit.ui.common.rememberVm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlanDetailViewModel(container: AppContainer, private val planId: String) : ViewModel() {
    private val repo = container.repo
    private val content = container.content
    private val _plan = MutableStateFlow<PlanDto?>(null)
    val plan: StateFlow<PlanDto?> = _plan
    var ex by mutableStateOf<Map<String, ExerciseDto>>(emptyMap())
        private set

    init {
        viewModelScope.launch {
            _plan.value = content.plan(planId)
            ex = content.exercises()
        }
    }

    fun enroll(onDone: () -> Unit) = viewModelScope.launch {
        repo.enroll(planId)
        onDone()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDetailScreen(
    planId: String,
    isZh: Boolean,
    onBack: () -> Unit,
    onEnrolled: () -> Unit
) {
    val vm = rememberVm(key = planId) { PlanDetailViewModel(it, planId) }
    val plan by vm.plan.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(plan?.title(isZh) ?: "", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("‹ " + stringResource(R.string.action_back)) }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = { vm.enroll(onEnrolled) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp)
                ) { Text(stringResource(R.string.action_enroll)) }
            }
        }
    ) { padding ->
        val p = plan
        if (p == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SectionCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Pill(stringResource(R.string.days_per_week, p.daysPerWeek))
                        Pill(stringResource(R.string.sets_per_week, p.weeklySets))
                        Pill(stringResource(R.string.weeks_count, p.durationWeeks))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(p.note, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.plan_source_note),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            itemsIndexed(p.days) { index, day ->
                DayCard(index + 1, day, isZh, vm.ex)
            }
        }
    }
}

@Composable
private fun DayCard(number: Int, day: DayDto, isZh: Boolean, ex: Map<String, ExerciseDto>) {
    var expanded by remember { mutableStateOf(number == 1) }
    SectionCard {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${if (isZh) "第 $number 天" else "Day $number"} · ${day.title(isZh)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(R.string.sets_count, day.workSets),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) stringResource(R.string.action_collapse) else stringResource(R.string.action_expand))
            }
        }
        if (expanded) {
            Spacer(Modifier.height(6.dp))
            day.slots.filter { !it.isBlank }.forEach { slot: SlotDto ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        ex[slot.ex]?.let { if (isZh) it.zh else it.en } ?: slot.ex,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${slot.sets} × ${slot.repMin}-${slot.repMax}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "RIR ${slot.rir}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
            }
        }
    }
}
