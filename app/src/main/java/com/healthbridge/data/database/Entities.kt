package com.healthbridge.data.database

import androidx.room.*

// ─── Doctor Entity ───────────────────────────────────────────
@Entity(tableName = "doctors")
data class DoctorEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val specialty: String,
    val rating: Double,
    val reviewCount: Int,
    val consultationFee: Int,
    val experienceYears: Int,
    val isOnline: Boolean = true,
    val lastSync: Long = System.currentTimeMillis()
)

@Dao
interface DoctorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(doctor: DoctorEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(doctors: List<DoctorEntity>)

    @Query("SELECT * FROM doctors")
    suspend fun getAllDoctors(): List<DoctorEntity>

    @Query("SELECT * FROM doctors WHERE id = :id")
    suspend fun getDoctorById(id: Int): DoctorEntity?

    @Query("DELETE FROM doctors")
    suspend fun deleteAll()
}

// ─── Appointment Entity ──────────────────────────────────────
@Entity(tableName = "appointments")
data class AppointmentEntity(
    @PrimaryKey val id: Int,
    val doctorName: String,
    val specialty: String,
    val appointmentDate: String,
    val appointmentTime: String,
    val type: String, // "in_person" | "video"
    val status: String, // "upcoming" | "past" | "cancelled"
    val fee: Int,
    val notes: String? = null,
    val lastSync: Long = System.currentTimeMillis()
)

@Dao
interface AppointmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(appointment: AppointmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(appointments: List<AppointmentEntity>)

    @Query("SELECT * FROM appointments ORDER BY appointmentDate DESC")
    suspend fun getAllAppointments(): List<AppointmentEntity>

    @Query("SELECT * FROM appointments WHERE id = :id")
    suspend fun getAppointmentById(id: Int): AppointmentEntity?

    @Query("DELETE FROM appointments WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM appointments")
    suspend fun deleteAll()
}

// ─── User Entity ─────────────────────────────────────────────
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val email: String,
    val phone: String? = null,
    val bloodType: String? = null,
    val dateOfBirth: String? = null,
    val address: String? = null,
    val allergies: String? = null,
    val lastSync: Long = System.currentTimeMillis()
)

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    @Query("DELETE FROM users")
    suspend fun delete()
}

// ─── Chat Message Entity ────────────────────────────────────
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val conversationId: String,
    val message: String,
    val isUser: Boolean,
    val timestamp: String,
    val aiContext: String? = null
)

@Dao
interface ChatMessageDao {
    @Insert
    suspend fun insert(message: ChatMessageEntity)

    @Insert
    suspend fun insertAll(messages: List<ChatMessageEntity>)

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp")
    suspend fun getConversationHistory(conversationId: String): List<ChatMessageEntity>

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun deleteConversation(conversationId: String)

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAll()
}

// ─── Medical Record Entity ──────────────────────────────────
@Entity(tableName = "medical_records")
data class MedicalRecordEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val type: String,
    val date: String,
    val doctorName: String? = null,
    val description: String? = null,
    val fileUrl: String? = null,
    val createdAt: String,
    val lastSync: Long = System.currentTimeMillis()
)

@Dao
interface MedicalRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: MedicalRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<MedicalRecordEntity>)

    @Query("SELECT * FROM medical_records ORDER BY date DESC")
    suspend fun getAllRecords(): List<MedicalRecordEntity>

    @Query("SELECT * FROM medical_records WHERE id = :id")
    suspend fun getRecordById(id: Int): MedicalRecordEntity?

    @Query("DELETE FROM medical_records WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM medical_records")
    suspend fun deleteAll()
}

// ─── Prescription Entity ────────────────────────────────────
@Entity(tableName = "prescriptions")
data class PrescriptionEntity(
    @PrimaryKey val id: Int,
    val medicationName: String,
    val dosage: String,
    val frequency: String,
    val duration: String,
    val doctorName: String,
    val issuedDate: String,
    val status: String,
    val refillsRemaining: Int,
    val instructions: String? = null,
    val lastSync: Long = System.currentTimeMillis()
)

@Dao
interface PrescriptionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(prescription: PrescriptionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(prescriptions: List<PrescriptionEntity>)

    @Query("SELECT * FROM prescriptions WHERE status = 'active' ORDER BY issuedDate DESC")
    suspend fun getActivePrescriptions(): List<PrescriptionEntity>

    @Query("SELECT * FROM prescriptions ORDER BY issuedDate DESC")
    suspend fun getAllPrescriptions(): List<PrescriptionEntity>

    @Query("SELECT * FROM prescriptions WHERE id = :id")
    suspend fun getPrescriptionById(id: Int): PrescriptionEntity?

    @Query("DELETE FROM prescriptions")
    suspend fun deleteAll()
}

// ─── Notification Entity ────────────────────────────────────
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val message: String,
    val type: String,
    val isRead: Boolean,
    val createdAt: String,
    val actionUrl: String? = null,
    val lastSync: Long = System.currentTimeMillis()
)

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<NotificationEntity>)

    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    suspend fun getAllNotifications(): List<NotificationEntity>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    suspend fun getUnreadCount(): Int

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Int)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM notifications")
    suspend fun deleteAll()
}

// ─── Health Tip Entity ──────────────────────────────────────
@Entity(tableName = "health_tips")
data class HealthTipEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val content: String,
    val category: String, // "hydration", "sleep", "nutrition", "activity", "mental", "hygiene"
    val icon: String,     // emoji for the tip
    val lastSync: Long = System.currentTimeMillis()
)

@Dao
interface HealthTipDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tips: List<HealthTipEntity>)

    @Query("SELECT * FROM health_tips")
    suspend fun getAll(): List<HealthTipEntity>

    @Query("DELETE FROM health_tips")
    suspend fun deleteAll()
}
