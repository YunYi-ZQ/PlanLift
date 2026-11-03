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
        PersonalRecordEntity::class,
        CustomPlanEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class PlanLiftDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun enrollmentDao(): EnrollmentDao
    abstract fun sessionDao(): SessionDao
    abstract fun setLogDao(): SetLogDao
    abstract fun metricDao(): MetricDao
    abstract fun customPlanDao(): CustomPlanDao

    companion object {
        @Volatile private var INSTANCE: PlanLiftDatabase? = null

        /** v2：新增 custom_plan 表，不动老数据 */
        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `custom_plan` (" +
                        "`id` TEXT NOT NULL, `json` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                        "`rev` INTEGER NOT NULL, `deviceId` TEXT NOT NULL, `deleted` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
            }
        }

        fun get(context: Context): PlanLiftDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                PlanLiftDatabase::class.java,
                "planlift.db"
            ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
        }
    }
}
