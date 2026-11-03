package app.planlift.fit.ui.plans

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
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
import app.planlift.fit.data.content.PlanDto
import app.planlift.fit.data.content.title
import app.planlift.fit.ui.common.Pill
import app.planlift.fit.ui.common.rememberVm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class GenderFilter { ALL, MALE, FEMALE, CUSTOM }

class PlanListViewModel(container: AppContainer) : ViewModel() {
    private val repo = container.repo
    private val _plans = MutableStateFlow<List<PlanDto>>(emptyList())
    val plans: StateFlow<List<PlanDto>> = _plans.asStateFlow()

    val gender = MutableStateFlow(GenderFilter.ALL)
    val exactDays = MutableStateFlow(0)

    init {
        viewModelScope.launch { _plans.value = repo.plans() }
    }

    /** 返回列表时调用，保证新建/删除自定义计划后立刻可见 */
    fun refresh() = viewModelScope.launch { _plans.value = repo.plans() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanListScreen(
    isZh: Boolean,
    onOpenPlan: (String) -> Unit,
    onNewPlan: () -> Unit
) {
    val vm = rememberVm { PlanListViewModel(it) }
    val plans by vm.plans.collectAsState()
    val gender by vm.gender.collectAsState()
    val exactDays by vm.exactDays.collectAsState()

    androidx.compose.runtime.LaunchedEffect(Unit) { vm.refresh() }

    val filtered = plans.filter { p ->
        (gender == GenderFilter.ALL ||
            (gender == GenderFilter.MALE && p.gender == "male") ||
            (gender == GenderFilter.FEMALE && p.gender == "female")) &&
            (exactDays == 0 || p.daysPerWeek == exactDays)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text(stringResource(R.string.nav_plans), fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.plans_subtitle, filtered.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            })
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            // 第一组：性别
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = gender == GenderFilter.ALL,
                        onClick = { vm.gender.value = GenderFilter.ALL },
                        label = { Text(stringResource(R.string.filter_all)) }
                    )
                }
                item {
                    FilterChip(
                        selected = gender == GenderFilter.MALE,
                        onClick = { vm.gender.value = GenderFilter.MALE },
                        label = { Text(stringResource(R.string.filter_male)) }
                    )
                }
                item {
                    FilterChip(
                        selected = gender == GenderFilter.FEMALE,
                        onClick = { vm.gender.value = GenderFilter.FEMALE },
                        label = { Text(stringResource(R.string.filter_female)) }
                    )
                }
                item {
                    FilterChip(
                        selected = gender == GenderFilter.CUSTOM,
                        onClick = { vm.gender.value = GenderFilter.CUSTOM },
                        label = { Text(stringResource(R.string.filter_custom)) }
                    )
                }
            }

            // 第二组：每周天数（精确匹配）
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = exactDays == 0,
                        onClick = { vm.exactDays.value = 0 },
                        label = { Text(stringResource(R.string.filter_days_any)) }
                    )
                }
                items(listOf(2, 3, 4, 5, 6)) { d ->
                    FilterChip(
                        selected = exactDays == d,
                        onClick = { vm.exactDays.value = if (exactDays == d) 0 else d },
                        label = { Text(stringResource(R.string.filter_days_exact, d)) }
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedButton(
                        onClick = onNewPlan,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("＋ " + stringResource(R.string.plan_new_title))
                    }
                }
                items(filtered, key = { it.id }) { plan ->
                    PlanCard(plan, isZh) { onOpenPlan(plan.id) }
                }
            }
        }
    }
}

@Composable
private fun PlanCard(plan: PlanDto, isZh: Boolean, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    plan.title(isZh),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text("›", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (plan.gender == "custom") {
                    Pill(stringResource(R.string.filter_custom), highlight = true)
                }
                Pill(stringResource(R.string.days_per_week, plan.daysPerWeek))
                Pill(stringResource(R.string.sets_per_week, plan.weeklySets))
                Pill(stringResource(R.string.minutes_range, plan.sessionMin))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                plan.note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}
