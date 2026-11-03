package app.planlift.fit.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 'me' AND deleted = 0")
    fun observe(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 'me' AND deleted = 0")
    suspend fun get(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profile")
    suspend fun getAll(): List<UserProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<UserProfileEntity>)
}

@Dao
interface EnrollmentDao {
    @Query("SELECT * FROM enrollment WHERE status = 'active' AND deleted = 0 ORDER BY startDate DESC LIMIT 1")
    fun observeActive(): Flow<EnrollmentEntity?>

    @Query("SELECT * FROM enrollment WHERE status = 'active' AND deleted = 0 ORDER BY startDate DESC LIMIT 1")
    suspend fun getActive(): EnrollmentEntity?

    @Query("SELECT * FROM enrollment WHERE deleted = 0 ORDER BY startDate DESC")
    fun observeAll(): Flow<List<EnrollmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(enrollment: EnrollmentEntity)

    @Query("UPDATE enrollment SET status = :status, updatedAt = :now, rev = rev + 1 WHERE id = :id")
    suspend fun setStatus(id: String, status: String, now: Long)

    @Query("UPDATE enrollment SET currentWeek = :week, updatedAt = :now, rev = rev + 1 WHERE id = :id")
    suspend fun setWeek(id: String, week: Int, now: Long)

    @Query("SELECT * FROM enrollment")
    suspend fun getAll(): List<EnrollmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<EnrollmentEntity>)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM training_session WHERE deleted = 0 ORDER BY date DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM training_session WHERE deleted = 0 ORDER BY date DESC")
    suspend fun observeAllOnce(): List<SessionEntity>

    @Query("SELECT * FROM training_session WHERE deleted = 0 ORDER BY date DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<SessionEntity>>

    @Query("SELECT * FROM training_session WHERE enrollmentId = :enrollmentId AND deleted = 0 ORDER BY date DESC")
    fun observeByEnrollment(enrollmentId: String): Flow<List<SessionEntity>>

    @Query("SELECT * FROM training_session WHERE id = :id")
    suspend fun get(id: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: SessionEntity)

    @Query("SELECT COUNT(*) FROM training_session WHERE deleted = 0")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM training_session WHERE enrollmentId = :enrollmentId AND deleted = 0")
    suspend fun countByEnrollment(enrollmentId: String): Int

    @Query("SELECT MAX(date) FROM training_session WHERE deleted = 0")
    suspend fun lastTrainedAt(): Long?

    @Query("SELECT * FROM training_session")
    suspend fun getAll(): List<SessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SessionEntity>)
}

@Dao
interface SetLogDao {
    @Query("SELECT * FROM set_log WHERE sessionId = :sessionId AND deleted = 0 ORDER BY setIndex ASC")
    fun observeBySession(sessionId: String): Flow<List<SetLogEntity>>

    @Query("SELECT * FROM set_log WHERE sessionId = :sessionId AND deleted = 0 ORDER BY setIndex ASC")
    suspend fun bySession(sessionId: String): List<SetLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(logs: List<SetLogEntity>)

    /** 该动作最近一次出现在哪次训练里 */
    @Query("SELECT sessionId FROM set_log WHERE exerciseId = :exId AND done = 1 AND deleted = 0 ORDER BY updatedAt DESC LIMIT 1")
    suspend fun lastSessionIdFor(exId: String): String?

    /** 某时间点之后的全部正式组（用于每周组数统计） */
    @Query(
        "SELECT s.* FROM set_log s INNER JOIN training_session t ON s.sessionId = t.id " +
            "WHERE t.date >= :since AND s.done = 1 AND s.deleted = 0 AND t.deleted = 0"
    )
    suspend fun setsSince(since: Long): List<SetLogEntity>

    @Query("SELECT * FROM set_log WHERE done = 1 AND deleted = 0")
    suspend fun allDoneSets(): List<SetLogEntity>

    @Query("SELECT * FROM set_log")
    suspend fun getAll(): List<SetLogEntity>
}

@Dao
interface MetricDao {
    @Query("SELECT * FROM body_metric WHERE deleted = 0 ORDER BY date DESC")
    fun observeAll(): Flow<List<BodyMetricEntity>>

    @Query("SELECT * FROM body_metric WHERE deleted = 0 ORDER BY date DESC")
    suspend fun observeAllOnce(): List<BodyMetricEntity>

    @Query("SELECT * FROM body_metric")
    suspend fun getAll(): List<BodyMetricEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metric: BodyMetricEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<BodyMetricEntity>)
}
