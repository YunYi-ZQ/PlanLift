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
import kotlinx.coroutines.flow.Flow
import java.util.UUID

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

        // 一周 7 天后自动进入下一周（简单推进，W2 换成按训练次数推进）
        val weekDeadline = enrollment.startDate + enrollment.currentWeek * 7L * 24 * 3600 * 1000
        if (now > weekDeadline && enrollment.currentWeek < plan.durationWeeks) {
            enrollmentDao.setWeek(enrollment.id, enrollment.currentWeek + 1, now)
        }
        return sessionId
    }

    suspend fun setsOf(sessionId: String): List<SetLogEntity> = setLogDao.bySession(sessionId)
}
