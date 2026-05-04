package com.healthbridge.data.repository

import android.content.Context
import com.healthbridge.data.database.AppDatabase
import com.healthbridge.network.ApiClient

object RepositoryFactory {
    private var _doctorRepository: DoctorRepository? = null
    private var _appointmentRepository: AppointmentRepository? = null
    private var _profileRepository: ProfileRepository? = null
    private var _chatRepository: ChatRepository? = null
    private var _medicalRecordRepository: MedicalRecordRepository? = null
    private var _prescriptionRepository: PrescriptionRepository? = null
    private var _notificationRepository: NotificationRepository? = null
    private var _healthTipsRepository: HealthTipsRepository? = null

    private lateinit var database: AppDatabase

    fun initialize(context: Context) {
        database = AppDatabase.getDatabase(context)
    }

    val doctorRepository: DoctorRepository
        get() = _doctorRepository ?: DoctorRepository(
            ApiClient.instance,
            database.doctorDao()
        ).also { _doctorRepository = it }

    val appointmentRepository: AppointmentRepository
        get() = _appointmentRepository ?: AppointmentRepository(
            ApiClient.instance,
            database.appointmentDao()
        ).also { _appointmentRepository = it }

    val profileRepository: ProfileRepository
        get() = _profileRepository ?: ProfileRepository(
            ApiClient.instance,
            database.userDao()
        ).also { _profileRepository = it }

    val chatRepository: ChatRepository
        get() = _chatRepository ?: ChatRepository(
            ApiClient.instance,
            database.chatMessageDao()
        ).also { _chatRepository = it }

    val medicalRecordRepository: MedicalRecordRepository
        get() = _medicalRecordRepository ?: MedicalRecordRepository(
            ApiClient.instance,
            database.medicalRecordDao()
        ).also { _medicalRecordRepository = it }

    val prescriptionRepository: PrescriptionRepository
        get() = _prescriptionRepository ?: PrescriptionRepository(
            ApiClient.instance,
            database.prescriptionDao()
        ).also { _prescriptionRepository = it }

    val notificationRepository: NotificationRepository
        get() = _notificationRepository ?: NotificationRepository(
            ApiClient.instance,
            database.notificationDao()
        ).also { _notificationRepository = it }

    val healthTipsRepository: HealthTipsRepository
        get() = _healthTipsRepository ?: HealthTipsRepository(
            database.healthTipDao()
        ).also { _healthTipsRepository = it }
}
