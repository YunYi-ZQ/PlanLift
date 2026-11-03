package app.planlift.fit.data.content

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class ContentRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile private var plansCache: List<PlanDto>? = null
    @Volatile private var exerciseCache: Map<String, ExerciseDto>? = null

    suspend fun plans(): List<PlanDto> {
        plansCache?.let { return it }
        val loaded = withContext(Dispatchers.IO) {
            val text = context.assets.open("plans.json").bufferedReader().use { it.readText() }
            json.decodeFromString<PlansFile>(text).plans
        }
        plansCache = loaded
        return loaded
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
}
