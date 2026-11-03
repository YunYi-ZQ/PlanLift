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
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM training_session WHERE deleted = 0 ORDER BY date DESC")
    fun observeAll(): Flow<List<SessionEntity>>

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

    @Query("SELECT MAX(date) FROM training_session WHERE deleted = 0")
    suspend fun lastTrainedAt(): Long?
}

@Dao
interface SetLogDao {
    @Query("SELECT * FROM set_log WHERE sessionId = :sessionId AND deleted = 0 ORDER BY setIndex ASC")
    fun observeBySession(sessionId: String): Flow<List<SetLogEntity>>

    @Query("SELECT * FROM set_log WHERE sessionId = :sessionId AND deleted = 0 ORDER BY setIndex ASC")
    suspend fun bySession(sessionId: String): List<SetLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(logs: List<SetLogEntity>)
}

@Dao
interface MetricDao {
    @Query("SELECT * FROM body_metric WHERE deleted = 0 ORDER BY date DESC")
    fun observeAll(): Flow<List<BodyMetricEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metric: BodyMetricEntity)
}
