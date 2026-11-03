package app.planlift.fit.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserProfileEntity::class,
        EnrollmentEntity::class,
        SessionEntity::class,
        SetLogEntity::class,
        BodyMetricEntity::class,
        PersonalRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PlanLiftDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun enrollmentDao(): EnrollmentDao
    abstract fun sessionDao(): SessionDao
    abstract fun setLogDao(): SetLogDao
    abstract fun metricDao(): MetricDao

    companion object {
        @Volatile private var INSTANCE: PlanLiftDatabase? = null

        fun get(context: Context): PlanLiftDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                PlanLiftDatabase::class.java,
                "planlift.db"
            ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
        }
    }
}
