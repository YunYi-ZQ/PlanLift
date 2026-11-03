package app.planlift.fit.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.planlift.fit.AppContainer
import app.planlift.fit.R
import app.planlift.fit.data.local.BodyMetricEntity
import app.planlift.fit.data.local.SetLogEntity
import app.planlift.fit.ui.common.SectionCard
import app.planlift.fit.ui.common.rememberVm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProgressState(
    val loading: Boolean = true,
    val weekly: Map<String, Int> = emptyMap(),
    val weeklyDone: Int = 0,
    val weeklyTarget: Int = 0,
    val streak: Int = 0,
    val perDay: List<Int> = List(7) { 0 },
    val e1rm: List<Triple<String, Double, SetLogEntity>> = emptyList(),
    val metrics: List<BodyMetricEntity> = emptyList()
)

class ProgressViewModel(container: AppContainer) : ViewModel() {
    private val repo = container.repo
    private val _state = MutableStateFlow(ProgressState())
    val state = _state.asStateFlow()

    init { reload() }

    private fun reload() = viewModelScope.launch {
        val enrollment = repo.activeEnrollmentOnce()
        val plan = enrollment?.let { repo.plan(it.planId) }
        _state.value = ProgressState(
            loading = false,
            weekly = repo.weeklySetsByMuscle(),
            weeklyDone = repo.weeklyDoneCount(),
            weeklyTarget = plan?.daysPerWeek ?: 0,
            streak = repo.streakDays(),
            perDay = repo.weeklySetsByDay().toList(),
            e1rm = repo.bestE1rm(),
            metrics = repo.allMetricsOnce()
        )
    }

    fun addMetric(weight: Double?, fat: Double?, waist: Double?, hip: Double?) = viewModelScope.launch {
        repo.addMetric(weight, fat, waist, hip)
        reload()
    }
}

private val MUSCLE_ZH = mapOf(
    "quads" to "股四头肌", "glutes" to "臀", "hams" to "腘绳肌", "calves" to "小腿",
    "chest" to "胸", "back" to "背", "shoulders" to "肩", "biceps" to "二头",
    "triceps" to "三头", "core" to "核心", "cardio" to "有氧", "other" to "其他"
)
private val MUSCLE_EN = mapOf(
    "quads" to "Quads", "glutes" to "Glutes", "hams" to "Hamstrings", "calves" to "Calves",
    "chest" to "Chest", "back" to "Back", "shoulders" to "Shoulders", "biceps" to "Biceps",
    "triceps" to "Triceps", "core" to "Core", "cardio" to "Cardio", "other" to "Other"
)
private val DAY_LABELS_ZH = listOf("一", "二", "三", "四", "五", "六", "日")
private val DAY_LABELS_EN = listOf("M", "T", "W", "T", "F", "S", "S")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(isZh: Boolean) {
    val vm = rememberVm { ProgressViewModel(it) }
    val state by vm.state.collectAsState()
    var showMetricDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text(stringResource(R.string.nav_progress), fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.progress_subtitle),
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
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                WeekOverviewCard(state = state)
            }

            item {
                BodyCard(metrics = state.metrics, isZh = isZh, onAdd = { showMetricDialog = true })
            }

            item {
                SectionCard {
                    Text(
                        stringResource(R.string.weekly_sets_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.weekly_sets_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    if (state.weekly.isEmpty()) {
                        Text(
                            stringResource(R.string.weekly_sets_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        state.weekly.entries.sortedByDescending { it.value }.forEach { (muscle, sets) ->
                            MuscleBar(
                                label = if (isZh) MUSCLE_ZH[muscle] ?: muscle else MUSCLE_EN[muscle] ?: muscle,
                                sets = sets
                            )
                        }
                    }
                }
            }

            item {
                SectionCard {
                    Text(
                        stringResource(R.string.e1rm_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.e1rm_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.e1rm.isEmpty()) {
                item {
                    SectionCard {
                        Text(
                            stringResource(R.string.e1rm_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(state.e1rm) { row ->
                    SectionCard {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    row.first,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "${fmt(row.third.weightKg)} kg × ${row.third.reps}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                "${fmt(row.second)} kg",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showMetricDialog) {
        MetricDialog(
            onDismiss = { showMetricDialog = false },
            onSave = { w, f, wa, h ->
                vm.addMetric(w, f, wa, h)
                showMetricDialog = false
            }
        )
    }
}

@Composable
private fun WeekOverviewCard(state: ProgressState) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val done = state.weeklyDone
            val target = state.weeklyTarget.coerceAtLeast(1)
            val progress = (done.toFloat() / target).coerceIn(0f, 1f)
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(96.dp)) {
                val track = MaterialTheme.colorScheme.surfaceContainerHighest
                val accent = MaterialTheme.colorScheme.primary
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 12.dp.toPx()
                    val inset = stroke / 2
                    drawArc(
                        color = track,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = accent,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$done",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "/ $target",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.progress_done_of, state.weeklyDone, state.weeklyTarget),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(R.string.progress_streak, state.streak),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (state.weeklyTarget > 0) {
                    Text(
                        stringResource(R.string.progress_target_hint, state.weeklyTarget),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.progress_week_sets),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(88.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            val maxSets = (state.perDay.maxOrNull() ?: 0).coerceAtLeast(1)
            state.perDay.forEachIndexed { index, sets ->
                val ratio = sets.toFloat() / maxSets
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(34.dp)
                ) {
                    Text(
                        if (sets > 0) "$sets" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(3.dp))
                    Box(
                        Modifier
                            .width(18.dp)
                            .height((6 + 52 * ratio).dp)
                            .background(
                                if (sets > 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerHighest,
                                RoundedCornerShape(6.dp)
                            )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (java.util.Locale.getDefault().language.startsWith("zh")) DAY_LABELS_ZH[index]
                        else DAY_LABELS_EN[index],
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun BodyCard(metrics: List<BodyMetricEntity>, isZh: Boolean, onAdd: () -> Unit) {
    SectionCard {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.body_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(onClick = onAdd) { Text(stringResource(R.string.body_add)) }
        }
        val latest = metrics.firstOrNull()
        if (latest == null) {
            Text(
                stringResource(R.string.body_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val parts = buildList {
                latest.weightKg?.let { add("${if (isZh) "体重" else "Weight"} ${fmt(it)} kg") }
                latest.bodyFat?.let { add("${if (isZh) "体脂" else "Fat"} ${fmt(it)}%") }
                latest.waistCm?.let { add("${if (isZh) "腰围" else "Waist"} ${fmt(it)} cm") }
                latest.hipCm?.let { add("${if (isZh) "臀围" else "Hip"} ${fmt(it)} cm") }
            }
            Text(
                stringResource(R.string.body_latest, parts.joinToString(" · ")),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun MetricDialog(
    onDismiss: () -> Unit,
    onSave: (Double?, Double?, Double?, Double?) -> Unit
) {
    var weight by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var waist by remember { mutableStateOf("") }
    var hip by remember { mutableStateOf("") }

    fun num(s: String): Double? = s.trim().toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.body_title)) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricField(stringResource(R.string.body_weight), weight) { weight = it }
                MetricField(stringResource(R.string.body_fat), fat) { fat = it }
                MetricField(stringResource(R.string.body_waist), waist) { waist = it }
                MetricField(stringResource(R.string.body_hip), hip) { hip = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(num(weight), num(fat), num(waist), num(hip)) }) {
                Text(stringResource(R.string.body_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
private fun MetricField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() || it == '.' }) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun MuscleBar(label: String, sets: Int) {
    val target = 10..20
    val ratio = (sets / 20f).coerceIn(0f, 1f)
    val color = when {
        sets < 10 -> MaterialTheme.colorScheme.tertiary
        sets > 20 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    Column(Modifier.padding(vertical = 5.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(
                if (sets in target) "$sets 组 ✓" else "$sets 组",
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(3.dp))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(ratio)
                    .height(6.dp)
                    .background(color, RoundedCornerShape(3.dp))
            )
        }
    }
}

private fun fmt(v: Double): String =
    if (v % 1.0 == 0.0) v.toInt().toString() else String.format("%.1f", v)
