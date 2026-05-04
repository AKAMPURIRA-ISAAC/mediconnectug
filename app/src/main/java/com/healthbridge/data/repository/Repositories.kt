package com.healthbridge.data.repository

import com.healthbridge.data.database.*
import com.healthbridge.network.*
import com.healthbridge.util.Result
import retrofit2.HttpException

class DoctorRepository(
    private val apiService: ApiService,
    private val doctorDao: DoctorDao
) {
    suspend fun getDoctors(forceRefresh: Boolean = false): Result<List<DoctorEntity>> {
        try {
            val localDoctors = doctorDao.getAllDoctors()

            // Return local if available and not forcing refresh
            if (localDoctors.isNotEmpty() && !forceRefresh) {
                return Result.success(localDoctors)
            }

            // Fetch from API
            val response = apiService.getDoctors()
            if (response.success && response.doctors != null) {
                // Save to local database
                val entities = response.doctors.map { doctor ->
                    DoctorEntity(
                        id = doctor.id,
                        name = doctor.name,
                        specialty = doctor.specialty,
                        rating = doctor.rating,
                        reviewCount = doctor.reviewCount,
                        consultationFee = doctor.consultationFee,
                        experienceYears = doctor.experienceYears,
                        isOnline = doctor.isOnline ?: true
                    )
                }
                doctorDao.deleteAll()
                doctorDao.insertAll(entities)
                return Result.success(entities)
            } else {
                // Return cached data if API fails
                if (localDoctors.isNotEmpty()) {
                    return Result.success(localDoctors)
                } else {
                    return Result.failure(Exception(response.error ?: "Failed to fetch doctors"))
                }
            }
        } catch (exception: Exception) {
            // Return cached data if network fails
            val cachedDoctors = doctorDao.getAllDoctors()
            if (cachedDoctors.isNotEmpty()) {
                return Result.success(cachedDoctors)
            } else {
                return Result.failure(exception)
            }
        }
    }
}


class AppointmentRepository(
    private val apiService: ApiService,
    private val appointmentDao: AppointmentDao
) {
    suspend fun getAppointments(forceRefresh: Boolean = false): Result<List<AppointmentEntity>> {
        try {
            val localAppointments = appointmentDao.getAllAppointments()

            if (localAppointments.isNotEmpty() && !forceRefresh) {
                return Result.success(localAppointments)
            }

            val response = apiService.getAppointments()
            if (response.success && response.appointments != null) {
                val entities = response.appointments.map { appointment ->
                    AppointmentEntity(
                        id = appointment.id,
                        doctorName = appointment.doctorName,
                        specialty = appointment.specialty,
                        appointmentDate = appointment.appointmentDate,
                        appointmentTime = appointment.appointmentTime,
                        type = appointment.type,
                        status = appointment.status,
                        fee = appointment.fee,
                        notes = appointment.notes
                    )
                }
                appointmentDao.deleteAll()
                appointmentDao.insertAll(entities)
                return Result.success(entities)
            } else {
                if (localAppointments.isNotEmpty()) {
                    return Result.success(localAppointments)
                } else {
                    return Result.failure(Exception(response.error ?: "Failed to fetch appointments"))
                }
            }
        } catch (exception: Exception) {
            val cachedAppointments = appointmentDao.getAllAppointments()
            if (cachedAppointments.isNotEmpty()) {
                return Result.success(cachedAppointments)
            } else {
                return Result.failure(exception)
            }
        }
    }

    suspend fun bookAppointment(request: BookingRequest): Result<AppointmentEntity> {
        return try {
            val response = apiService.bookAppointment(request)
            if (response.success && response.appointment != null) {
                val entity = AppointmentEntity(
                    id = response.appointment.id,
                    doctorName = response.appointment.doctorName,
                    specialty = response.appointment.specialty,
                    appointmentDate = response.appointment.appointmentDate,
                    appointmentTime = response.appointment.appointmentTime,
                    type = response.appointment.type,
                    status = response.appointment.status,
                    fee = response.appointment.fee,
                    notes = response.appointment.notes
                )
                appointmentDao.insert(entity)
                return Result.success(entity)
            } else {
                return Result.failure(Exception(response.error ?: "Failed to book appointment"))
            }
        } catch (e: HttpException) {
            // Read the real error message from the server response body
            val errorBody = e.response()?.errorBody()?.string()
            val serverMsg = try {
                org.json.JSONObject(errorBody ?: "{}").optString("error", null)
                    ?: org.json.JSONObject(errorBody ?: "{}").optString("message", null)
            } catch (_: Exception) { null }
            val message = when (e.code()) {
                401 -> "401 — ${serverMsg ?: "Session expired. Please log in again."}"
                403 -> "403 — ${serverMsg ?: "Access denied."}"
                404 -> "404 — Doctor not found."
                500 -> "500 — Server error. Try again shortly."
                else -> "${e.code()} — ${serverMsg ?: e.message()}"
            }
            Result.failure(Exception(message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelAppointment(id: Int): Result<Boolean> {
        return try {
            val response = apiService.cancelAppointment(id)
            if (response.success) {
                appointmentDao.delete(id)
                Result.success(true)
            } else {
                Result.failure(Exception(response.error ?: "Failed to cancel appointment"))
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun rescheduleAppointment(id: Int, request: RescheduleRequest): Result<AppointmentEntity> {
        return try {
            val response = apiService.rescheduleAppointment(id, request)
            if (response.success && response.appointment != null) {
                val entity = AppointmentEntity(
                    id = response.appointment.id,
                    doctorName = response.appointment.doctorName,
                    specialty = response.appointment.specialty,
                    appointmentDate = response.appointment.appointmentDate,
                    appointmentTime = response.appointment.appointmentTime,
                    type = response.appointment.type,
                    status = response.appointment.status,
                    fee = response.appointment.fee,
                    notes = response.appointment.notes
                )
                appointmentDao.insert(entity)
                Result.success(entity)
            } else {
                Result.failure(Exception(response.error ?: "Failed to reschedule appointment"))
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
}

class ProfileRepository(
    private val apiService: ApiService,
    private val userDao: UserDao
) {
    suspend fun getProfile(forceRefresh: Boolean = false): Result<UserEntity> {
        try {
            val localUser = userDao.getCurrentUser()

            if (localUser != null && !forceRefresh) {
                return Result.success(localUser)
            }

            val response = apiService.getProfile()
            if (response.success && response.user != null) {
                val entity = UserEntity(
                    id = response.user.id ?: 0,
                    name = response.user.name,
                    email = response.user.email,
                    phone = response.user.phone,
                    bloodType = response.user.bloodType,
                    dateOfBirth = response.user.dateOfBirth,
                    address = response.user.address,
                    allergies = response.user.allergies
                )
                userDao.delete()
                userDao.insert(entity)
                return Result.success(entity)
            } else {
                if (localUser != null) {
                    return Result.success(localUser)
                } else {
                    return Result.failure(Exception(response.error ?: "Failed to fetch profile"))
                }
            }
        } catch (exception: Exception) {
            val cachedUser = userDao.getCurrentUser()
            if (cachedUser != null) {
                return Result.success(cachedUser)
            } else {
                return Result.failure(exception)
            }
        }
    }

    suspend fun updateProfile(profile: Map<String, String>): Result<UserEntity> {
        try {
            val response = apiService.updateProfile(profile)
            if (response.success && response.user != null) {
                val entity = UserEntity(
                    id = response.user.id ?: 0,
                    name = response.user.name,
                    email = response.user.email,
                    phone = response.user.phone,
                    bloodType = response.user.bloodType,
                    dateOfBirth = response.user.dateOfBirth,
                    address = response.user.address,
                    allergies = response.user.allergies
                )
                userDao.delete()
                userDao.insert(entity)
                return Result.success(entity)
            } else {
                return Result.failure(Exception(response.error ?: "Failed to update profile"))
            }
        } catch (exception: Exception) {
            return Result.failure(exception)
        }
    }

    suspend fun saveUserLocally(user: UserEntity) {
        userDao.delete()
        userDao.insert(user)
    }

    suspend fun getCurrentUserLocal(): UserEntity? = userDao.getCurrentUser()
}

class ChatRepository(
    private val apiService: ApiService,
    private val chatMessageDao: ChatMessageDao
) {
    suspend fun sendMessage(request: ChatRequest): Result<ChatResponse> {
        try {
            val response = apiService.sendChatMessage(request)
            if (response.success) {
                // Store user message locally if conversation exists
                if (response.conversationId != null) {
                    chatMessageDao.insert(
                        ChatMessageEntity(
                            conversationId = response.conversationId,
                            message = request.message,
                            isUser = true,
                            timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault()).format(java.util.Date()),
                            aiContext = null
                        )
                    )
                }
                return Result.success(response)
            } else {
                return Result.failure(Exception(response.error ?: response.message ?: "Failed to send message"))
            }
        } catch (exception: Exception) {
            return Result.failure(exception)
        }
    }

    suspend fun getChatHistory(conversationId: String?): Result<List<ChatMessageEntity>> {
        try {
            if (conversationId != null) {
                val response = apiService.getChatHistory(conversationId)
                if (response.success && response.messages != null) {
                    val entities = response.messages.map { msg ->
                        ChatMessageEntity(
                            conversationId = msg.conversationId,
                            message = msg.message,
                            isUser = msg.isUser,
                            timestamp = msg.timestamp,
                            aiContext = msg.aiContext
                        )
                    }
                    // Cache locally
                    chatMessageDao.insertAll(entities)
                    return Result.success(entities)
                } else {
                    return Result.failure(Exception(response.error ?: "Failed to fetch chat history"))
                }
            } else {
                return Result.failure(Exception("Conversation ID required"))
            }
        } catch (exception: Exception) {
            return Result.failure(exception)
        }
    }

    suspend fun getLocalChatHistory(conversationId: String): List<ChatMessageEntity> {
        return chatMessageDao.getConversationHistory(conversationId)
    }

    suspend fun storeChatMessage(entity: ChatMessageEntity) {
        chatMessageDao.insert(entity)
    }
}

class MedicalRecordRepository(
    private val apiService: ApiService,
    private val recordDao: MedicalRecordDao
) {
    suspend fun getMedicalRecords(forceRefresh: Boolean = false): Result<List<MedicalRecordEntity>> {
        try {
            val localRecords = recordDao.getAllRecords()

            if (localRecords.isNotEmpty() && !forceRefresh) {
                return Result.success(localRecords)
            }

            val response = apiService.getMedicalRecords()
            if (response.success && response.records != null) {
                val entities = response.records.map { record ->
                    MedicalRecordEntity(
                        id = record.id,
                        title = record.title,
                        type = record.type,
                        date = record.date,
                        doctorName = record.doctorName,
                        description = record.description,
                        fileUrl = record.fileUrl,
                        createdAt = record.createdAt
                    )
                }
                recordDao.deleteAll()
                recordDao.insertAll(entities)
                return Result.success(entities)
            } else {
                if (localRecords.isNotEmpty()) {
                    return Result.success(localRecords)
                } else {
                    return Result.failure(Exception(response.error ?: "Failed to fetch medical records"))
                }
            }
        } catch (exception: Exception) {
            val cachedRecords = recordDao.getAllRecords()
            if (cachedRecords.isNotEmpty()) {
                return Result.success(cachedRecords)
            } else {
                return Result.failure(exception)
            }
        }
    }

    suspend fun uploadRecord(record: Map<String, String>): Result<MedicalRecordEntity> {
        try {
            val response = apiService.uploadMedicalRecord(record)
            if (response.success && response.records != null && response.records.isNotEmpty()) {
                val newRecord = response.records.first()
                val entity = MedicalRecordEntity(
                    id = newRecord.id,
                    title = newRecord.title,
                    type = newRecord.type,
                    date = newRecord.date,
                    doctorName = newRecord.doctorName,
                    description = newRecord.description,
                    fileUrl = newRecord.fileUrl,
                    createdAt = newRecord.createdAt
                )
                recordDao.insert(entity)
                return Result.success(entity)
            } else {
                return Result.failure(Exception(response.error ?: "Failed to upload record"))
            }
        } catch (exception: Exception) {
            return Result.failure(exception)
        }
    }

    suspend fun deleteRecord(id: Int): Result<Boolean> {
        try {
            val response = apiService.deleteMedicalRecord(id)
            if (response.success) {
                recordDao.delete(id)
                return Result.success(true)
            } else {
                return Result.failure(Exception(response.error ?: "Failed to delete record"))
            }
        } catch (exception: Exception) {
            return Result.failure(exception)
        }
    }
}

class PrescriptionRepository(
    private val apiService: ApiService,
    private val prescriptionDao: PrescriptionDao
) {
    suspend fun getPrescriptions(forceRefresh: Boolean = false): Result<List<PrescriptionEntity>> {
        return try {
            val localPrescriptions = prescriptionDao.getAllPrescriptions()

            if (localPrescriptions.isNotEmpty() && !forceRefresh) {
                return Result.success(localPrescriptions)
            }

            val response = apiService.getPrescriptions()
            if (response.success && response.prescriptions != null) {
                val entities = response.prescriptions.map { prescription ->
                    PrescriptionEntity(
                        id = prescription.id,
                        medicationName = prescription.medicationName,
                        dosage = prescription.dosage,
                        frequency = prescription.frequency,
                        duration = prescription.duration,
                        doctorName = prescription.doctorName,
                        issuedDate = prescription.issuedDate,
                        status = prescription.status,
                        refillsRemaining = prescription.refillsRemaining,
                        instructions = prescription.instructions
                    )
                }
                prescriptionDao.deleteAll()
                prescriptionDao.insertAll(entities)
                return Result.success(entities)
            } else {
                if (localPrescriptions.isNotEmpty()) {
                    return Result.success(localPrescriptions)
                } else {
                    return Result.failure(Exception(response.error ?: "Failed to fetch prescriptions"))
                }
            }
        } catch (exception: Exception) {
            val cachedPrescriptions = prescriptionDao.getAllPrescriptions()
            if (cachedPrescriptions.isNotEmpty()) {
                return Result.success(cachedPrescriptions)
            } else {
                return Result.failure(exception)
            }
        }
    }

    suspend fun getActivePrescriptions(forceRefresh: Boolean = false): Result<List<PrescriptionEntity>> {
        val all = getPrescriptions(forceRefresh)
        return all.map { list: List<PrescriptionEntity> ->
            list.filter { prescription: PrescriptionEntity -> prescription.status == "active" }
        }
    }

    suspend fun requestRefill(id: Int): Result<Boolean> {
        return try {
            val response = apiService.requestRefill(id)
            if (response.success) {
                return Result.success(true)
            } else {
                return Result.failure(Exception(response.error ?: "Failed to request refill"))
            }
        } catch (exception: Exception) {
            return Result.failure(exception)
        }
    }
}

class NotificationRepository(
    private val apiService: ApiService,
    private val notificationDao: NotificationDao
) {
    suspend fun getNotifications(forceRefresh: Boolean = false): Result<List<NotificationEntity>> {
        return try {
            val localNotifications = notificationDao.getAllNotifications()

            if (localNotifications.isNotEmpty() && !forceRefresh) {
                return Result.success(localNotifications)
            }

            val response = apiService.getNotifications()
            if (response.success && response.notifications != null) {
                val entities = response.notifications.map { notification ->
                    NotificationEntity(
                        id = notification.id,
                        title = notification.title,
                        message = notification.message,
                        type = notification.type,
                        isRead = notification.isRead,
                        createdAt = notification.createdAt,
                        actionUrl = notification.actionUrl
                    )
                }
                notificationDao.deleteAll()
                notificationDao.insertAll(entities)
                return Result.success(entities)
            } else {
                if (localNotifications.isNotEmpty()) {
                    return Result.success(localNotifications)
                } else {
                    return Result.failure(Exception(response.error ?: "Failed to fetch notifications"))
                }
            }
        } catch (exception: Exception) {
            val cachedNotifications = notificationDao.getAllNotifications()
            if (cachedNotifications.isNotEmpty()) {
                return Result.success(cachedNotifications)
            } else {
                return Result.failure(exception)
            }
        }
    }

    suspend fun getUnreadCount(): Result<Int> {
        return try {
            Result.success(notificationDao.getUnreadCount())
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun markAsRead(id: Int): Result<Boolean> {
        return try {
            apiService.markNotificationAsRead(id)
            notificationDao.markAsRead(id)
            Result.success(true)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun markAllAsRead(): Result<Boolean> {
        return try {
            apiService.markAllNotificationsAsRead()
            notificationDao.markAllAsRead()
            return Result.success(true)
        } catch (exception: Exception) {
            return Result.failure(exception)
        }
    }
}

class HealthTipsRepository(
    private val healthTipDao: HealthTipDao
) {
    // Curated local health tips — always available even offline
    private val localTips = listOf(
        HealthTipEntity(1, "Stay Hydrated 💧", "Drink at least 8 glasses (2L) of water daily. Proper hydration improves energy, skin health, and organ function.", "hydration", "💧"),
        HealthTipEntity(2, "Sleep 7–9 Hours 😴", "Quality sleep is essential for immune function, memory consolidation, and hormonal balance. Establish a consistent sleep schedule.", "sleep", "😴"),
        HealthTipEntity(3, "Eat More Vegetables 🥦", "Aim for 5 servings of fruits and vegetables daily. They provide essential vitamins, minerals, and antioxidants.", "nutrition", "🥦"),
        HealthTipEntity(4, "Walk 10,000 Steps 🚶", "Daily walking reduces risk of heart disease, improves mood, and helps maintain a healthy weight.", "activity", "🚶"),
        HealthTipEntity(5, "Wash Hands Frequently 🧼", "Handwashing for 20 seconds with soap is the most effective way to prevent spreading infections.", "hygiene", "🧼"),
        HealthTipEntity(6, "Limit Screen Time 📵", "Take a 20-second break every 20 minutes looking at something 20 feet away to reduce eye strain.", "mental", "📵"),
        HealthTipEntity(7, "Eat Breakfast Daily 🍳", "Starting your day with a nutritious breakfast improves concentration, metabolism, and energy levels.", "nutrition", "🍳"),
        HealthTipEntity(8, "Manage Stress 🧘", "Practice deep breathing, meditation, or yoga for 10 minutes daily to reduce cortisol and improve wellbeing.", "mental", "🧘"),
        HealthTipEntity(9, "Limit Sugar Intake 🚫", "Reduce added sugar to less than 25g/day. Excess sugar contributes to obesity, diabetes, and inflammation.", "nutrition", "🚫"),
        HealthTipEntity(10, "Check Blood Pressure ❤️", "Monitor your blood pressure regularly. Normal range is below 120/80 mmHg. High BP is a silent killer.", "health", "❤️"),
        HealthTipEntity(11, "Sunscreen Daily ☀️", "Apply SPF 30+ sunscreen every day, even indoors near windows, to prevent skin damage and cancer.", "hygiene", "☀️"),
        HealthTipEntity(12, "Stretch Every Morning 🤸", "5 minutes of morning stretching improves flexibility, reduces injury risk, and energises your day.", "activity", "🤸")
    )

    suspend fun getHealthTips(): Result<List<HealthTipEntity>> {
        return try {
            // Seed tips into the database if empty
            val cached = healthTipDao.getAll()
            if (cached.isEmpty()) {
                healthTipDao.insertAll(localTips)
                Result.success(localTips)
            } else {
                Result.success(cached)
            }
        } catch (e: Exception) {
            Result.success(localTips) // Always return local tips even if DB fails
        }
    }

    suspend fun getRandomTip(): HealthTipEntity = localTips.random()
}
