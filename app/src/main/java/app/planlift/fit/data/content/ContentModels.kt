package app.planlift.fit.data.content

import kotlinx.serialization.Serializable

@Serializable
data class ExerciseDto(
    val id: String,
    val zh: String,
    val en: String,
    val primary: String,
    val equipment: String,
    val unilateral: Boolean = false,
    val cue: String = ""
)

@Serializable
data class SlotDto(
    val ex: String,
    val sets: Int,
    val repMin: Int,
    val repMax: Int,
    val rir: Int,
    val rest: Int,
    val note: String = ""
) {
    val isBlank: Boolean get() = sets <= 0
}

@Serializable
data class DayDto(
    val zh: String,
    val en: String,
    val slots: List<SlotDto>
) {
    val workSets: Int get() = slots.sumOf { if (it.isBlank) 0 else it.sets }
}

@Serializable
data class PlanDto(
    val id: String,
    val zh: String,
    val en: String,
    val gender: String,
    val goal: String,
    val level: String,
    val daysPerWeek: Int,
    val equipment: String,
    val split: String,
    val durationWeeks: Int,
    val sessionMin: String,
    val weeklySets: Int,
    val note: String = "",
    /** 自定义计划用：训练日是周几（0=周一）。内置计划为空，按老规则推断 */
    val weekdays: List<Int> = emptyList(),
    val days: List<DayDto>
)

@Serializable
data class PlansFile(
    val version: Int,
    val plans: List<PlanDto>
)

/**
 * 语言选择：zh 走中文，其余走英文。
 * 内容层自带双语字段，不依赖 strings.xml。
 */
fun isChinese(lang: String?): Boolean = lang?.startsWith("zh") == true

fun PlanDto.title(zh: Boolean): String = if (zh) this.zh else this.en
fun DayDto.title(zh: Boolean): String = if (zh) this.zh else this.en
fun ExerciseDto.title(zh: Boolean): String = if (zh) this.zh else this.en
