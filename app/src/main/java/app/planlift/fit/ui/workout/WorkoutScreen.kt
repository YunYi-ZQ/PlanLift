package app.planlift.fit.ui.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import app.planlift.fit.data.local.SetLogEntity
import app.planlift.fit.ui.common.SectionCard
import app.planlift.fit.ui.common.Stepper
import app.planlift.fit.ui.common.rememberVm
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class SetRow(
    val uid: String,
    val exId: String,
    val exName: String,
    val setNo: Int,
    val target: String,
    val rir: Int,
    val rest: Int,
    val weight: Double = 0.0,
    val reps: Int = 0,
    val done: Boolean = false,
    val hint: String = "",
    val reason: String = ""
)

class WorkoutViewModel(container: AppContainer, private val dayIndex: Int) : ViewModel() {
    private val repo = container.repo
    private val content = container.content

    var plan by mutableStateOf<PlanDto?>(null)
        private set
    var enrollment by mutableStateOf<EnrollmentEntity?>(null)
        private set
    var day by mutableStateOf<DayDto?>(null)
        private set
    val rows = mutableStateListOf<SetRow>()
    var exercises = mapOf<String, ExerciseDto>()
        private set

    var restLeft by mutableIntStateOf(0)
        private set
    var restTotal by mutableIntStateOf(0)
        private set
    private var timerJob: Job? = null
    private var startedAt = System.currentTimeMillis()

    var finished by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            val e = repo.activeEnrollmentOnce()
            val p = e?.let { repo.plan(it.planId) }
            val exMap = content.exercises()
            exercises = exMap
            enrollment = e
            plan = p
            val d = p?.days?.getOrNull(dayIndex)
            day = d
            if (d != null) {
                val sex = if (p?.gender == "female") "female" else "male"
                val suggestions = repo.suggestionsFor(d, sex)
                d.slots.filter { !it.isBlank }.forEach { slot ->
                    val name = exMap[slot.ex]?.title(true) ?: slot.ex
                    val sug = suggestions[slot.ex]
                    repeat(slot.sets) { i ->
                        rows.add(
                            SetRow(
                                uid = "${slot.ex}_$i",
                                exId = slot.ex,
                                exName = name,
                                setNo = i + 1,
                                target = "${slot.repMin}-${slot.repMax}",
                                rir = slot.rir,
                                rest = slot.rest,
                                weight = sug?.weightKg ?: 0.0,
                                reps = sug?.reps ?: slot.repMin,
                                hint = sug?.lastSummary.orEmpty(),
                                reason = sug?.reason.orEmpty()
                            )
                        )
                    }
                }
            }
        }
    }

    fun setWeight(uid: String, value: Double) {
        val i = rows.indexOfFirst { it.uid == uid }
        if (i >= 0) rows[i] = rows[i].copy(weight = value.coerceAtLeast(0.0))
    }

    fun changeWeight(uid: String, delta: Double) {
        val i = rows.indexOfFirst { it.uid == uid }
        if (i >= 0) {
            val cur = rows[i]
            rows[i] = cur.copy(weight = (cur.weight + delta).coerceAtLeast(0.0))
        }
    }

    fun changeReps(uid: String, delta: Int) {
        val i = rows.indexOfFirst { it.uid == uid }
        if (i >= 0) {
            val cur = rows[i]
            rows[i] = cur.copy(reps = (cur.reps + delta).coerceAtLeast(0))
        }
    }

    fun toggleDone(row: SetRow) {
        val i = rows.indexOfFirst { it.uid == row.uid }
        if (i < 0) return
        val now = !rows[i].done
        val doneRow = rows[i].copy(done = now)
        rows[i] = doneRow
        // 完成后把重量/次数带给同动作的下一组，避免重复输入
        if (now && doneRow.weight > 0) {
            val nextSame = rows.indexOfFirst {
                it.exId == doneRow.exId && it.setNo == doneRow.setNo + 1 && it.weight == 0.0
            }
            if (nextSame >= 0) {
                rows[nextSame] = rows[nextSame].copy(weight = doneRow.weight, reps = doneRow.reps)
            }
        }
        if (now && doneRow.rest > 0) startRest(doneRow.rest)
    }

    private fun startRest(seconds: Int) {
        timerJob?.cancel()
        restTotal = seconds
        restLeft = seconds
        timerJob = viewModelScope.launch {
            while (restLeft > 0) {
                delay(1000)
                restLeft -= 1
            }
        }
    }

    fun skipRest() {
        timerJob?.cancel()
        restLeft = 0
    }

    fun addRest(delta: Int) {
        restLeft = (restLeft + delta).coerceIn(0, 900)
        if (restLeft > restTotal) restTotal = restLeft
    }

    fun finish(onDone: () -> Unit) {
        val e = enrollment ?: return
        val p = plan ?: return
        val d = day ?: return
        viewModelScope.launch {
            val logs = rows.map { r ->
                SetLogEntity(
                    id = r.uid,
                    sessionId = "",
                    exerciseId = r.exId,
                    exerciseZh = r.exName,
                    exerciseEn = exercises[r.exId]?.en ?: r.exId,
                    setIndex = 0,
                    weightKg = r.weight,
                    reps = r.reps,
                    rir = r.rir,
                    done = r.done,
                    updatedAt = 0L,
                    rev = 0L,
                    deviceId = ""
                )
            }
            val minutes = ((System.currentTimeMillis() - startedAt) / 60000).toInt()
            repo.saveSession(e, p, d, dayIndex, logs, minutes)
            finished = true
            onDone()
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    dayIndex: Int,
    isZh: Boolean,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val vm = rememberVm(key = "workout_$dayIndex") { WorkoutViewModel(it, dayIndex) }
    val rows = vm.rows
    val doneCount = rows.count { it.done }
    var editing by remember { mutableStateOf<SetRow?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.workout_title), fontWeight = FontWeight.SemiBold)
                        Text(
                            "${vm.day?.title(isZh) ?: ""} · $doneCount/${rows.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = { vm.finish(onDone) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp),
                    enabled = doneCount > 0
                ) {
                    Text(stringResource(R.string.action_finish, doneCount))
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            if (vm.restLeft > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                stringResource(R.string.rest_countdown, vm.restLeft),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                stringResource(R.string.rest_hint),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { vm.addRest(15) }) { Text("+15s") }
                            TextButton(onClick = { vm.skipRest() }) { Text(stringResource(R.string.action_skip)) }
                        }
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(rows, key = { it.uid }) { row ->
                    SetCard(
                        row = row,
                        onWeight = { vm.changeWeight(row.uid, it) },
                        onReps = { vm.changeReps(row.uid, it) },
                        onToggle = { vm.toggleDone(row) },
                        onEditWeight = { editing = row }
                    )
                }
            }
        }
    }

    editing?.let { row ->
        var field by remember(row.uid) {
            val init = formatWeight(row.weight).replace(" kg", "")
            mutableStateOf(
                androidx.compose.ui.text.input.TextFieldValue(
                    text = init,
                    selection = androidx.compose.ui.text.TextRange(0, init.length)
                )
            )
        }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(stringResource(R.string.label_weight)) },
            text = {
                OutlinedTextField(
                    value = field,
                    onValueChange = { v ->
                        val clean = v.text.filter { c -> c.isDigit() || c == '.' }
                        field = v.copy(text = clean, selection = androidx.compose.ui.text.TextRange(clean.length))
                    },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.setWeight(row.uid, field.text.toDoubleOrNull() ?: 0.0)
                    editing = null
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { editing = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun SetCard(
    row: SetRow,
    onWeight: (Double) -> Unit,
    onReps: (Int) -> Unit,
    onToggle: () -> Unit,
    onEditWeight: () -> Unit
) {
    SectionCard {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    row.exName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "#${row.setNo} · ${row.target} · RIR ${row.rir}" +
                        if (row.hint.isNotEmpty() && row.hint != "首次") " · 上次 ${row.hint}" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (row.reason.isNotEmpty() && !row.done) {
                    Text(
                        row.reason,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Checkbox(checked = row.done, onCheckedChange = { onToggle() })
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    stringResource(R.string.label_weight),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Stepper(
                    value = formatWeight(row.weight),
                    onMinus = { onWeight(-2.5) },
                    onPlus = { onWeight(2.5) },
                    onValueClick = onEditWeight
                )
            }
            Column {
                Text(
                    stringResource(R.string.label_reps),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Stepper(
                    value = row.reps.toString(),
                    onMinus = { onReps(-1) },
                    onPlus = { onReps(1) }
                )
            }
        }
    }
}

private fun formatWeight(w: Double): String =
    if (w % 1.0 == 0.0) "${w.toInt()} kg" else "$w kg"
