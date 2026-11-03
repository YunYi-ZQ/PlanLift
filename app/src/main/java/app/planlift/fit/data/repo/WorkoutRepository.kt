package app.planlift.fit.data.repo

import app.planlift.fit.data.content.ContentRepository
import app.planlift.fit.data.content.DayDto
import app.planlift.fit.data.content.PlanDto
import app.planlift.fit.data.local.EnrollmentEntity
import app.planlift.fit.data.local.MetricDao
import app.planlift.fit.data.local.ProfileDao
import app.planlift.fit.data.local.SessionDao
import app.planlift.fit.data.local.SessionEntity
import app.planlift.fit.data.local.SetLogDao
import app.planlift.fit.data.local.SetLogEntity
import app.planlift.fit.data.local.EnrollmentDao
import app.planlift.fit.domain.ProgressionEngine
import app.planlift.fit.domain.ProgressionTarget
import app.planlift.fit.domain.SetPerformance
import kotlinx.coroutines.flow.Flow
import java.util.UUID

data class SlotSuggestion(
    val weightKg: Double,
    val reps: Int,
    val action: String,
    val reason: String,
    val lastSummary: String
)

class WorkoutRepository(
    private val enrollmentDao: EnrollmentDao,
    private val sessionDao: SessionDao,
    private val setLogDao: SetLogDao,
    private val metricDao: MetricDao,
    private val profileDao: ProfileDao,
    private val content: ContentRepository,
    private val deviceId: String
) {
    fun observeActiveEnrollment(): Flow<EnrollmentEntity?> = enrollmentDao.observeActive()
    fun observeHistory(): Flow<List<SessionEntity>> = sessionDao.observeAll()
    fun observeRecentSessions(limit: Int): Flow<List<SessionEntity>> = sessionDao.observeRecent(limit)

    suspend fun activeEnrollmentOnce(): EnrollmentEntity? = enrollmentDao.getActive()

    suspend fun plans(): List<PlanDto> = content.plans()
    suspend fun plan(id: String): PlanDto? = content.plan(id)

    suspend fun enroll(planId: String) {
        enrollmentDao.getActive()?.let { old ->
            enrollmentDao.setStatus(old.id, "replaced", System.currentTimeMillis())
        }
        val now = System.currentTimeMillis()
        enrollmentDao.upsert(
            EnrollmentEntity(
                id = UUID.randomUUID().toString(),
                planId = planId,
                startDate = now,
                currentWeek = 1,
                updatedAt = now,
                rev = 1,
                deviceId = deviceId
            )
        )
    }

    suspend fun quitPlan() {
        enrollmentDao.getActive()?.let {
            enrollmentDao.setStatus(it.id, "dropped", System.currentTimeMillis())
        }
    }

    /**
     * 今天该练第几天：按已完成次数取模。
     * 顺序训练（漏练不跳天），与计划模板的 day 列表对齐。
     */
    suspend fun nextDayIndex(plan: PlanDto, enrollment: EnrollmentEntity): Int =
        sessionDao.count() % plan.days.size

    /**
     * 保存一次训练。logs 只包含实际完成的组。
     */
    suspend fun saveSession(
        enrollment: EnrollmentEntity,
        plan: PlanDto,
        day: DayDto,
        dayIndex: Int,
        logs: List<SetLogEntity>,
        durationMin: Int
    ): String {
        val now = System.currentTimeMillis()
        val sessionId = UUID.randomUUID().toString()
        val doneLogs = logs.filter { it.done && it.reps > 0 }
        val volume = doneLogs.sumOf { it.weightKg * it.reps }

        sessionDao.upsert(
            SessionEntity(
                id = sessionId,
                enrollmentId = enrollment.id,
                planDayIndex = dayIndex,
                dayNameZh = day.zh,
                dayNameEn = day.en,
                date = now,
                durationMin = durationMin,
                totalSets = doneLogs.size,
                totalVolumeKg = volume,
                updatedAt = now,
                rev = 1,
                deviceId = deviceId
            )
        )
        setLogDao.upsertAll(
            doneLogs.mapIndexed { i, log ->
                log.copy(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    setIndex = i,
                    updatedAt = now,
                    rev = 1,
                    deviceId = deviceId
                )
            }
        )

        // 周次按「已完成训练次数 / 每周天数」推进，漏练不会被跳周
        val completed = sessionDao.countByEnrollment(enrollment.id)
        val week = ((completed / plan.daysPerWeek) + 1).coerceAtMost(plan.durationWeeks)
        if (week != enrollment.currentWeek) {
            enrollmentDao.setWeek(enrollment.id, week, now)
        }
        return sessionId
    }

    suspend fun setsOf(sessionId: String): List<SetLogEntity> = setLogDao.bySession(sessionId)

    fun observeMetrics() = metricDao.observeAll()

    suspend fun addMetric(weightKg: Double?, bodyFat: Double?, waistCm: Double?, hipCm: Double?) {
        val now = System.currentTimeMillis()
        metricDao.upsert(
            app.planlift.fit.data.local.BodyMetricEntity(
                id = UUID.randomUUID().toString(),
                date = now,
                weightKg = weightKg,
                bodyFat = bodyFat,
                waistCm = waistCm,
                hipCm = hipCm,
                updatedAt = now,
                rev = 1,
                deviceId = deviceId
            )
        )
    }

    suspend fun allSessionsOnce(): List<SessionEntity> = sessionDao.observeAllOnce()

    suspend fun allMetricsOnce() = metricDao.observeAllOnce()

    /**
     * 为一天的每个动作算出「下次该用多重、做几次」。
     * 依据上一次同动作的实际表现 + 目标的次数区间 / RIR。
     */
    suspend fun suggestionsFor(day: DayDto, sex: String): Map<String, SlotSuggestion> {
        val exMap = content.exercises()
        val result = mutableMapOf<String, SlotSuggestion>()
        for (slot in day.slots.filter { !it.isBlank }) {
            if (result.containsKey(slot.ex)) continue
            val meta = exMap[slot.ex]
            val lastId = setLogDao.lastSessionIdFor(slot.ex)
            val lastSets = lastId?.let { id ->
                setLogDao.bySession(id).filter { it.exerciseId == slot.ex && it.done }
            } ?: emptyList()

            val lower = meta?.primary in LOWER_MUSCLES
            val machine = meta?.equipment == "machine" || meta?.equipment == "cable"
            val inc = ProgressionEngine.increment(lower, machine, sex)

            val res = ProgressionEngine.next(
                last = lastSets.map { SetPerformance(it.weightKg, it.reps, it.rir) },
                target = ProgressionTarget(slot.repMin, slot.repMax, slot.rir, slot.sets),
                currentWeight = lastSets.maxOfOrNull { it.weightKg } ?: 0.0,
                increment = inc
            )
            result[slot.ex] = SlotSuggestion(
                weightKg = res.nextWeightKg,
                reps = res.nextReps,
                action = res.action.name,
                reason = res.reason,
                lastSummary = if (lastSets.isEmpty()) {
                    "首次"
                } else {
                    lastSets.joinToString(", ") { "${fmt(it.weightKg)}×${it.reps}" }
                }
            )
        }
        return result
    }

    /** 最近 7 天每个肌群的正式组数 */
    suspend fun weeklySetsByMuscle(): Map<String, Int> {
        val since = System.currentTimeMillis() - 7L * 24 * 3600 * 1000
        val exMap = content.exercises()
        return setLogDao.setsSince(since)
            .groupingBy { exMap[it.exerciseId]?.primary ?: "other" }
            .eachCount()
    }

    /** 每个动作的 e1RM 最好成绩（按 Epley 估算），降序 */
    suspend fun bestE1rm(): List<Triple<String, Double, SetLogEntity>> {
        val exMap = content.exercises()
        return setLogDao.allDoneSets()
            .groupBy { it.exerciseId }
            .mapNotNull { (exId, sets) ->
                val best = sets.maxByOrNull { ProgressionEngine.e1rm(it.weightKg, it.reps) } ?: return@mapNotNull null
                val value = ProgressionEngine.e1rm(best.weightKg, best.reps)
                if (value <= 0.0) null
                else Triple(exMap[exId]?.let { if (true) it.zh else it.en } ?: exId, value, best)
            }
            .sortedByDescending { it.second }
    }

    private fun fmt(v: Double): String =
        if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()

    companion object {
        private val LOWER_MUSCLES = setOf("quads", "glutes", "hams", "calves")
    }
}
