package app.planlift.fit.ui.plans

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import app.planlift.fit.data.content.SlotDto
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
    val weekday: Int,
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
    var weekdays by mutableStateOf(listOf(0, 2, 4))
        private set
    var days by mutableStateOf<List<EditorDay>>(emptyList())
        private set
    var exercises by mutableStateOf<List<ExerciseDto>>(emptyList())
        private set
    var isEditing by mutableStateOf(false)
        private set

    private var existingPlan: PlanDto? = null
    private val slotMap = mutableMapOf<Int, List<EditorSlot>>()

    init {
        viewModelScope.launch {
            exercises = content.exercises().values.toList().sortedBy { it.primary }
            if (planId != null && planId != "new") {
                existingPlan = content.plan(planId)
                existingPlan?.let { p ->
                    isEditing = true
                    name = p.zh
                    weekdays = if (p.weekdays.isNotEmpty()) {
                        p.weekdays
                    } else {
                        p.days.indices.map { it }.toList()
                    }
                    p.days.forEachIndexed { index, d ->
                        val weekday = weekdays.getOrElse(index) { index }
                        slotMap[weekday] = d.slots.filter { !it.isBlank }.map { s ->
                            EditorSlot(
                                exId = s.ex,
                                name = content.exercises()[s.ex]?.zh ?: s.ex,
                                sets = s.sets,
                                repMin = s.repMin,
                                repMax = s.repMax,
                                rest = s.rest
                            )
                        }
                    }
                }
            }
            rebuild()
        }
    }

    private fun rebuild() {
        days = weekdays.sorted().map { wd -> EditorDay(wd, slotMap[wd].orEmpty()) }
    }

    fun updateName(v: String) {
        name = v
    }

    fun toggleWeekday(weekday: Int) {
        weekdays = if (weekday in weekdays) {
            if (weekdays.size == 1) weekdays else weekdays - weekday
        } else {
            (weekdays + weekday).sorted()
        }
        rebuild()
    }

    fun addSlot(weekday: Int, ex: ExerciseDto) {
        val slot = EditorSlot(
            exId = ex.id,
            name = ex.zh,
            sets = 3,
            repMin = 8,
            repMax = 12,
            rest = if (ex.primary in setOf("quads", "glutes", "hams", "back", "chest")) 120 else 90
        )
        slotMap[weekday] = slotMap[weekday].orEmpty() + slot
        rebuild()
    }

    fun removeSlot(weekday: Int, slotIndex: Int) {
        val list = slotMap[weekday].orEmpty().toMutableList()
        if (slotIndex in list.indices) {
            list.removeAt(slotIndex)
            slotMap[weekday] = list
            rebuild()
        }
    }

    fun changeSets(weekday: Int, slotIndex: Int, delta: Int) =
        updateSlot(weekday, slotIndex) { it.copy(sets = (it.sets + delta).coerceIn(1, 8)) }

    fun changeReps(weekday: Int, slotIndex: Int, delta: Int) =
        updateSlot(weekday, slotIndex) {
            val min = (it.repMin + delta).coerceIn(1, 30)
            val max = (it.repMax + delta).coerceIn(min, 30)
            it.copy(repMin = min, repMax = max)
        }

    private fun updateSlot(weekday: Int, slotIndex: Int, block: (EditorSlot) -> EditorSlot) {
        val list = slotMap[weekday].orEmpty().toMutableList()
        if (slotIndex !in list.indices) return
        list[slotIndex] = block(list[slotIndex])
        slotMap[weekday] = list
        rebuild()
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
            weekdays = days.map { it.weekday },
            days = days.map { d ->
                DayDto(
                    zh = weekdayZh(d.weekday),
                    en = weekdayEn(d.weekday),
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

fun weekdayZh(index: Int): String =
    listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日").getOrElse(index) { "周一" }

fun weekdayEn(index: Int): String =
    listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").getOrElse(index) { "Mon" }

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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
                    Spacer(Modifier.height(14.dp))
                    Text(
                        stringResource(R.string.plan_weekdays_title),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(R.string.plan_weekdays_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        (0..6).forEach { wd ->
                            val selected = wd in vm.weekdays
                            FilterChip(
                                selected = selected,
                                onClick = { vm.toggleWeekday(wd) },
                                label = {
                                    Text(
                                        if (isZh) weekdayZh(wd).removePrefix("周")
                                        else weekdayEn(wd).take(1)
                                    )
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.plan_days_count, vm.days.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
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
                            if (isZh) weekdayZh(day.weekday) else weekdayEn(day.weekday),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(onClick = { pickerForDay = day.weekday }) {
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
                            TextButton(onClick = { vm.removeSlot(day.weekday, slotIndex) }) {
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
                                onMinus = { vm.changeSets(day.weekday, slotIndex, -1) },
                                onPlus = { vm.changeSets(day.weekday, slotIndex, 1) }
                            )
                            Stepper(
                                value = "${slot.repMin}-${slot.repMax}",
                                onMinus = { vm.changeReps(day.weekday, slotIndex, -1) },
                                onPlus = { vm.changeReps(day.weekday, slotIndex, 1) }
                            )
                        }
                    }
                }
            }
        }
    }

    pickerForDay?.let { weekday ->
        ExercisePicker(
            exercises = vm.exercises,
            isZh = isZh,
            onDismiss = { pickerForDay = null },
            onPick = { ex ->
                vm.addSlot(weekday, ex)
                pickerForDay = null
            }
        )
    }
}

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
