package app.planlift.fit.ui.plans

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import app.planlift.fit.data.content.SlotDto
import app.planlift.fit.data.content.isChinese
import app.planlift.fit.ui.common.SectionCard
import app.planlift.fit.ui.common.Stepper
import app.planlift.fit.ui.common.rememberVm
import kotlinx.coroutines.launch
import java.util.UUID

data class EditorSlot(
    val exId: String,
    val name: String,
    val sets: Int,
    val repMin: Int,
    val repMax: Int,
    val rest: Int
)

data class EditorDay(
    val name: String,
    val slots: List<EditorSlot>
)

class PlanEditorViewModel(
    container: AppContainer,
    private val planId: String?
) : ViewModel() {
    private val content = container.content
    private val repo = container.repo

    var name by mutableStateOf("")
        private set
    var dayCount by mutableIntStateOf(3)
        private set
    var days by mutableStateOf<List<EditorDay>>(emptyList())
        private set
    var exercises by mutableStateOf<List<ExerciseDto>>(emptyList())
        private set
    var loaded by mutableStateOf(false)
        private set
    var isEditing by mutableStateOf(false)
        private set

    private var existingPlan: PlanDto? = null

    init {
        viewModelScope.launch {
            exercises = content.exercises().values.toList().sortedBy { it.primary }
            if (planId != null && planId != "new") {
                existingPlan = content.plan(planId)
                existingPlan?.let { p ->
                    isEditing = true
                    name = p.zh
                    dayCount = p.days.size
                    days = p.days.map { d ->
                        EditorDay(
                            name = d.zh,
                            slots = d.slots.filter { !it.isBlank }.map { s ->
                                EditorSlot(
                                    exId = s.ex,
                                    name = content.exercises()[s.ex]?.zh ?: s.ex,
                                    sets = s.sets,
                                    repMin = s.repMin,
                                    repMax = s.repMax,
                                    rest = s.rest
                                )
                            }
                        )
                    }
                }
            }
            if (days.isEmpty()) {
                days = (1..dayCount).map { EditorDay(dayLetter(it), emptyList()) }
            }
            loaded = true
        }
    }

    fun updateName(v: String) {
        name = v
    }

    fun updateDayCount(n: Int) {
        dayCount = n
        val current = days.toMutableList()
        while (current.size < n) current.add(EditorDay(dayLetter(current.size + 1), emptyList()))
        while (current.size > n) current.removeAt(current.size - 1)
        days = current
    }

    fun addSlot(dayIndex: Int, ex: ExerciseDto) {
        val current = days.toMutableList()
        val day = current[dayIndex]
        val slot = EditorSlot(
            exId = ex.id,
            name = ex.zh,
            sets = 3,
            repMin = 8,
            repMax = 12,
            rest = if (ex.primary in setOf("quads", "glutes", "hams", "back", "chest")) 120 else 90
        )
        current[dayIndex] = day.copy(slots = day.slots + slot)
        days = current
    }

    fun removeSlot(dayIndex: Int, slotIndex: Int) {
        val current = days.toMutableList()
        val day = current[dayIndex]
        if (slotIndex !in day.slots.indices) return
        current[dayIndex] = day.copy(slots = day.slots.filterIndexed { i, _ -> i != slotIndex })
        days = current
    }

    fun changeSets(dayIndex: Int, slotIndex: Int, delta: Int) =
        updateSlot(dayIndex, slotIndex) { it.copy(sets = (it.sets + delta).coerceIn(1, 8)) }

    fun changeReps(dayIndex: Int, slotIndex: Int, delta: Int) =
        updateSlot(dayIndex, slotIndex) {
            val min = (it.repMin + delta).coerceIn(1, 30)
            val max = (it.repMax + delta).coerceIn(min, 30)
            it.copy(repMin = min, repMax = max)
        }

    private fun updateSlot(dayIndex: Int, slotIndex: Int, block: (EditorSlot) -> EditorSlot) {
        val current = days.toMutableList()
        val day = current[dayIndex]
        if (slotIndex !in day.slots.indices) return
        val slots = day.slots.toMutableList()
        slots[slotIndex] = block(slots[slotIndex])
        current[dayIndex] = day.copy(slots = slots)
        days = current
    }

    fun canSave(): Boolean = name.isNotBlank() && days.any { it.slots.isNotEmpty() }

    fun save(onDone: (String) -> Unit) = viewModelScope.launch {
        val id = existingPlan?.id ?: ("c_" + UUID.randomUUID().toString().take(8))
        val plan = PlanDto(
            id = id,
            zh = name.trim(),
            en = name.trim(),
            gender = "custom",
            goal = "hypertrophy",
            level = "beginner",
            daysPerWeek = days.size,
            equipment = "gym",
            split = "custom",
            durationWeeks = 8,
            sessionMin = "30-60",
            weeklySets = days.sumOf { d -> d.slots.sumOf { it.sets } },
            note = "自定义计划",
            days = days.map { d ->
                DayDto(
                    zh = d.name,
                    en = d.name,
                    slots = d.slots.map { s ->
                        SlotDto(
                            ex = s.exId,
                            sets = s.sets,
                            repMin = s.repMin,
                            repMax = s.repMax,
                            rir = 2,
                            rest = s.rest
                        )
                    }
                )
            }
        )
        repo.saveCustomPlan(plan)
        onDone(id)
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        existingPlan?.let { repo.deleteCustomPlan(it.id) }
        onDone()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanEditorScreen(
    planId: String?,
    isZh: Boolean,
    onBack: () -> Unit,
    onSaved: (String) -> Unit
) {
    val vm = rememberVm(key = "editor_$planId") { PlanEditorViewModel(it, planId) }
    var pickerForDay by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(if (vm.isEditing) R.string.plan_edit_title else R.string.plan_new_title),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("‹ " + stringResource(R.string.action_back))
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (vm.isEditing) {
                        OutlinedButton(
                            onClick = { vm.delete(onBack) },
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.plan_delete)) }
                    }
                    Button(
                        onClick = { vm.save(onSaved) },
                        enabled = vm.canSave(),
                        modifier = Modifier.weight(2f)
                    ) { Text(stringResource(R.string.plan_save)) }
                }
            }
        }
    ) { padding ->
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
                    OutlinedTextField(
                        value = vm.name,
                        onValueChange = vm::updateName,
                        label = { Text(stringResource(R.string.plan_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.plan_days_count),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (2..6).forEach { n ->
                            FilterChip(
                                selected = vm.dayCount == n,
                                onClick = { vm.updateDayCount(n) },
                                label = { Text("$n") }
                            )
                        }
                    }
                }
            }

            items(vm.days.size) { dayIndex ->
                val day = vm.days[dayIndex]
                SectionCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            day.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(onClick = { pickerForDay = dayIndex }) {
                            Text(stringResource(R.string.plan_add_exercise))
                        }
                    }
                    if (day.slots.isEmpty()) {
                        Text(
                            stringResource(R.string.plan_day_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    day.slots.forEachIndexed { slotIndex, slot ->
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(slot.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    stringResource(R.string.plan_sets_reps, slot.sets, slot.repMin, slot.repMax),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { vm.removeSlot(dayIndex, slotIndex) }) {
                                Text("✕", color = MaterialTheme.colorScheme.error)
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Stepper(
                                value = "${slot.sets} ${if (isZh) "组" else "sets"}",
                                onMinus = { vm.changeSets(dayIndex, slotIndex, -1) },
                                onPlus = { vm.changeSets(dayIndex, slotIndex, 1) }
                            )
                            Stepper(
                                value = "${slot.repMin}-${slot.repMax}",
                                onMinus = { vm.changeReps(dayIndex, slotIndex, -1) },
                                onPlus = { vm.changeReps(dayIndex, slotIndex, 1) }
                            )
                        }
                    }
                }
            }
        }
    }

    pickerForDay?.let { dayIndex ->
        ExercisePicker(
            exercises = vm.exercises,
            isZh = isZh,
            onDismiss = { pickerForDay = null },
            onPick = { ex ->
                vm.addSlot(dayIndex, ex)
                pickerForDay = null
            }
        )
    }
}

private fun dayLetter(index: Int): String = ('A' + (index - 1)).toString()

private fun muscleLabel(key: String, isZh: Boolean): String {
    val zh = mapOf(
        "quads" to "股四头肌", "glutes" to "臀", "hams" to "腘绳肌", "calves" to "小腿",
        "chest" to "胸", "back" to "背", "shoulders" to "肩", "biceps" to "二头",
        "triceps" to "三头", "core" to "核心", "cardio" to "有氧"
    )
    val en = mapOf(
        "quads" to "Quads", "glutes" to "Glutes", "hams" to "Hamstrings", "calves" to "Calves",
        "chest" to "Chest", "back" to "Back", "shoulders" to "Shoulders", "biceps" to "Biceps",
        "triceps" to "Triceps", "core" to "Core", "cardio" to "Cardio"
    )
    return (if (isZh) zh[key] else en[key]) ?: key
}

@Composable
private fun ExercisePicker(
    exercises: List<ExerciseDto>,
    isZh: Boolean,
    onDismiss: () -> Unit,
    onPick: (ExerciseDto) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = exercises.filter {
        query.isBlank() || it.zh.contains(query) || it.en.contains(query, ignoreCase = true)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.plan_pick_exercise)) },
        text = {
            Column(Modifier.heightIn(max = 420.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.plan_search_exercise)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn {
                    items(filtered) { ex ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(ex) }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(if (isZh) ex.zh else ex.en, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                muscleLabel(ex.primary, isZh),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
