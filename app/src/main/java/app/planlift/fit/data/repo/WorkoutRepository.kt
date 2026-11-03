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
import app.planlift.fit.data.local.CustomPlanDao
import app.planlift.fit.data.local.CustomPlanEntity
import app.planlift.fit.data.local.EnrollmentDao
import app.planlift.fit.data.sync.Snapshot
import app.planlift.fit.domain.ProgressionEngine
import app.planlift.fit.domain.SyncMerge
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
    private val customPlanDao: CustomPlanDao,
    private val content: ContentRepository,
    private val deviceId: String
) {
    fun observeActiveEnrollment(): Flow<EnrollmentEntity?> = enrollmentDao.observeActive()

    /** 本周每天（周一=0）完成的组数 */
    suspend fun weeklySetsByDay(): IntArray {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val since = cal.timeInMillis
        val result = IntArray(7)
        sessionDao.sessionsSince(since).forEach { s ->
            val c = java.util.Calendar.getInstance().apply { timeInMillis = s.date }
            val day = (c.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
            if (day in 0..6) result[day] += s.totalSets
        }
        return result
    }

    /** 连续打卡天数（今天或昨天为起点） */
    suspend fun streakDays(): Int {
        val days = sessionDao.allDates()
            .map { ms ->
                val c = java.util.Calendar.getInstance().apply { timeInMillis = ms }
                c.set(java.util.Calendar.HOUR_OF_DAY, 0)
                c.set(java.util.Calendar.MINUTE, 0)
                c.set(java.util.Calendar.SECOND, 0)
                c.set(java.util.Calendar.MILLISECOND, 0)
                c.timeInMillis
            }
            .toSortedSet()
            .toList()
            .reversed()
        if (days.isEmpty()) return 0
        val today = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val dayMs = 24L * 3600 * 1000
        var streak = 0
        var cursor = if (days[0] == today) today else if (days[0] == today - dayMs) today - dayMs else return 0
        val set = days.toHashSet()
        while (set.contains(cursor)) {
            streak++
            cursor -= dayMs
        }
        return streak
    }

    /** 本周（周一 00:00 起）已完成次数 */
    suspend fun weeklyDoneCount(): Int {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return sessionDao.countSince(cal.timeInMillis)
    }
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

    // ---------------- 自定义计划 ----------------

    suspend fun saveCustomPlan(plan: PlanDto) {
        val now = System.currentTimeMillis()
        val existing = customPlanDao.get(plan.id)
        customPlanDao.upsert(
            CustomPlanEntity(
                id = plan.id,
                json = content.toJson(plan),
                updatedAt = now,
                rev = (existing?.rev ?: 0L) + 1,
                deviceId = deviceId
            )
        )
    }

    suspend fun deleteCustomPlan(id: String) {
        customPlanDao.softDelete(id, System.currentTimeMillis())
    }

    // ---------------- 同步 ----------------

    fun deviceIdValue(): String = deviceId

    suspend fun collectSnapshot(): Snapshot = Snapshot(
        rev = 0,
        updatedAt = System.currentTimeMillis(),
        deviceId = deviceId,
        profiles = profileDao.getAll(),
        enrollments = enrollmentDao.getAll(),
        sessions = sessionDao.getAll(),
        setLogs = setLogDao.getAll(),
        metrics = metricDao.getAll(),
        records = emptyList()
    )

    data class MergeStats(val localWins: Int = 0, val remoteWins: Int = 0)

    /** 与远端快照逐条合并；remote 为空说明是第一次同步。 */
    suspend fun mergeInto(local: Snapshot, remote: Snapshot?): Pair<Snapshot, MergeStats> {
        if (remote == null) return local to MergeStats()

        val profiles = SyncMerge.merge(
            local.profiles, remote.profiles,
            { it.id }, { it.rev }, { it.updatedAt }, { it.deviceId }
        )
        val enrollments = SyncMerge.merge(
            local.enrollments, remote.enrollments,
            { it.id }, { it.rev }, { it.updatedAt }, { it.deviceId }
        )
        val sessions = SyncMerge.merge(
            local.sessions, remote.sessions,
            { it.id }, { it.rev }, { it.updatedAt }, { it.deviceId }
        )
        val setLogs = SyncMerge.merge(
            local.setLogs, remote.setLogs,
            { it.id }, { it.rev }, { it.updatedAt }, { it.deviceId }
        )
        val metrics = SyncMerge.merge(
            local.metrics, remote.metrics,
            { it.id }, { it.rev }, { it.updatedAt }, { it.deviceId }
        )

        val stats = MergeStats(
            localWins = profiles.localWins + enrollments.localWins + sessions.localWins +
                setLogs.localWins + metrics.localWins,
            remoteWins = profiles.remoteWins + enrollments.remoteWins + sessions.remoteWins +
                setLogs.remoteWins + metrics.remoteWins
        )
        return local.copy(
            profiles = profiles.merged,
            enrollments = enrollments.merged,
            sessions = sessions.merged,
            setLogs = setLogs.merged,
            metrics = metrics.merged
        ) to stats
    }

    suspend fun applySnapshot(snapshot: Snapshot) {
        profileDao.insertAll(snapshot.profiles)
        enrollmentDao.insertAll(snapshot.enrollments)
        sessionDao.insertAll(snapshot.sessions)
        setLogDao.upsertAll(snapshot.setLogs)
        metricDao.insertAll(snapshot.metrics)
    }

    companion object {
        private val LOWER_MUSCLES = setOf("quads", "glutes", "hams", "calves")
    }
}
