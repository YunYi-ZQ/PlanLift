package app.planlift.fit.data.content

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class ContentRepository(
    private val context: Context,
    private val customPlans: suspend () -> List<PlanDto> = { emptyList() }
) {

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile private var plansCache: List<PlanDto>? = null
    @Volatile private var exerciseCache: Map<String, ExerciseDto>? = null

    /** 内置计划（缓存）+ 用户自定义计划（每次都读，保证新建后立刻出现） */
    suspend fun plans(): List<PlanDto> {
        val builtIn = plansCache ?: withContext(Dispatchers.IO) {
            val text = context.assets.open("plans.json").bufferedReader().use { it.readText() }
            json.decodeFromString<PlansFile>(text).plans
        }.also { plansCache = it }
        return customPlans() + builtIn
    }

    suspend fun exercises(): Map<String, ExerciseDto> {
        exerciseCache?.let { return it }
        val loaded = withContext(Dispatchers.IO) {
            val text = context.assets.open("exercises.json").bufferedReader().use { it.readText() }
            json.decodeFromString<List<ExerciseDto>>(text).associateBy { it.id }
        }
        exerciseCache = loaded
        return loaded
    }

    suspend fun plan(id: String): PlanDto? = plans().firstOrNull { it.id == id }

    /** 供编辑器保存用 */
    fun toJson(plan: PlanDto): String = json.encodeToString(PlanDto.serializer(), plan)

    fun fromJson(text: String): PlanDto? =
        runCatching { json.decodeFromString(PlanDto.serializer(), text) }.getOrNull()
}
