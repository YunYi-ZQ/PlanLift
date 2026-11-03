package app.planlift.fit.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * 所有可变记录都带同步字段：updatedAt / rev / deviceId / deleted。
 * WebDAV 合并依赖这四个字段，W4 不需要改表。
 */

@Entity(tableName = "user_profile")
@Serializable
data class UserProfileEntity(
    @PrimaryKey val id: String = "me",
    val sex: String,
    val age: Int,
    val heightCm: Double,
    val weightKg: Double,
    val goal: String,
    val level: String,
    val daysAvailable: Int,
    val equipment: String,
    val updatedAt: Long,
    val rev: Long,
    val deviceId: String,
    val deleted: Boolean = false
)

@Entity(tableName = "enrollment", indices = [Index("planId")])
@Serializable
data class EnrollmentEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val startDate: Long,
    val currentWeek: Int = 1,
    val weightOffsetKg: Double = 0.0,
    val status: String = "active",
    val updatedAt: Long,
    val rev: Long,
    val deviceId: String,
    val deleted: Boolean = false
)

@Entity(tableName = "training_session", indices = [Index("enrollmentId"), Index("date")])
@Serializable
data class SessionEntity(
    @PrimaryKey val id: String,
    val enrollmentId: String,
    val planDayIndex: Int,
    val dayNameZh: String,
    val dayNameEn: String,
    val date: Long,
    val durationMin: Int,
    val totalSets: Int,
    val totalVolumeKg: Double,
    val note: String = "",
    val updatedAt: Long,
    val rev: Long,
    val deviceId: String,
    val deleted: Boolean = false
)

@Entity(tableName = "set_log", indices = [Index("sessionId")])
@Serializable
data class SetLogEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val exerciseId: String,
    val exerciseZh: String,
    val exerciseEn: String,
    val setIndex: Int,
    val weightKg: Double,
    val reps: Int,
    val rir: Int,
    val done: Boolean,
    val updatedAt: Long,
    val rev: Long,
    val deviceId: String,
    val deleted: Boolean = false
)

@Entity(tableName = "body_metric", indices = [Index("date")])
@Serializable
data class BodyMetricEntity(
    @PrimaryKey val id: String,
    val date: Long,
    val weightKg: Double?,
    val bodyFat: Double?,
    val waistCm: Double?,
    val hipCm: Double?,
    val updatedAt: Long,
    val rev: Long,
    val deviceId: String,
    val deleted: Boolean = false
)

@Entity(tableName = "personal_record", indices = [Index(value = ["exerciseId"], unique = true)])
@Serializable
data class PersonalRecordEntity(
    @PrimaryKey val id: String,
    val exerciseId: String,
    val e1rm: Double,
    val weightKg: Double,
    val reps: Int,
    val date: Long,
    val updatedAt: Long,
    val rev: Long,
    val deviceId: String,
    val deleted: Boolean = false
)

/** 用户自建计划：整份 PlanDto 以 JSON 存下来，内容层与内置计划一起展示。 */
@Entity(tableName = "custom_plan")
data class CustomPlanEntity(
    @PrimaryKey val id: String,
    val json: String,
    val updatedAt: Long,
    val rev: Long,
    val deviceId: String,
    val deleted: Boolean = false
)
