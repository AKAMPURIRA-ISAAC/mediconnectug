package com.healthbridge.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DoctorEntity::class,
        AppointmentEntity::class,
        UserEntity::class,
        ChatMessageEntity::class,
        MedicalRecordEntity::class,
        PrescriptionEntity::class,
        NotificationEntity::class,
        HealthTipEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun doctorDao(): DoctorDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun userDao(): UserDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun medicalRecordDao(): MedicalRecordDao
    abstract fun prescriptionDao(): PrescriptionDao
    abstract fun notificationDao(): NotificationDao
    abstract fun healthTipDao(): HealthTipDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "healthbridge_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
