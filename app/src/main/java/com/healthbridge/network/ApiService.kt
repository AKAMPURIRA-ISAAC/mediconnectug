package com.healthbridge.network

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// ── Doctor models ──────────────────────────────────────────
data class Doctor(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("specialty") val specialty: String,
    @SerializedName("rating") val rating: Double,
    @SerializedName("review_count") val reviewCount: Int,
    @SerializedName("consultation_fee") val consultationFee: Int,
    @SerializedName("experience_years") val experienceYears: Int,
    @SerializedName("is_online") val isOnline: Boolean? = null
)

data class DoctorResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("count") val count: Int = 0,
    @SerializedName("doctors") val doctors: List<Doctor>?,
    @SerializedName("error") val error: String? = null
)

// ── Auth models ────────────────────────────────────────────
data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("user_type") val userType: String? = null  // "patient" | "doctor"
)

data class RegisterRequest(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("password") val password: String
)

data class DoctorRegisterRequest(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("password") val password: String,
    @SerializedName("specialty") val specialty: String,
    @SerializedName("hospital") val hospital: String,
    @SerializedName("license_number") val licenseNumber: String
)

data class AuthUser(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("user_type") val userType: String? = null,  // "patient" | "doctor"
    @SerializedName("doctor_id") val doctorId: Int? = null  // Only for doctor users
)

data class AuthResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("token") val token: String? = null,
    @SerializedName("user") val user: AuthUser? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("error") val error: String? = null
)

// ── Appointment models ─────────────────────────────────────
data class Appointment(
    @SerializedName("id") val id: Int,
    @SerializedName("doctor_name") val doctorName: String,
    @SerializedName("specialty") val specialty: String,
    @SerializedName("appointment_date") val appointmentDate: String,
    @SerializedName("appointment_time") val appointmentTime: String,
    @SerializedName("type") val type: String,         // "in_person" | "video"
    @SerializedName("status") val status: String,     // "upcoming" | "past" | "cancelled"
    @SerializedName("fee") val fee: Int,
    @SerializedName("notes") val notes: String? = null
)

data class AppointmentsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("appointments") val appointments: List<Appointment>?,
    @SerializedName("error") val error: String? = null
)

data class BookingRequest(
    @SerializedName("doctor_id") val doctorId: String,
    @SerializedName("doctor_name") val doctorName: String,
    @SerializedName("date") val date: String,
    @SerializedName("time") val time: String,
    @SerializedName("type") val type: String,
    @SerializedName("notes") val notes: String? = null
)

data class BookingResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("appointment") val appointment: Appointment? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("error") val error: String? = null
)

data class RescheduleRequest(
    @SerializedName("date") val date: String,
    @SerializedName("time") val time: String,
    @SerializedName("type") val type: String,
    @SerializedName("notes") val notes: String? = null
)

// ── Profile models ─────────────────────────────────────────
data class UserProfile(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("blood_type") val bloodType: String? = null,
    @SerializedName("date_of_birth") val dateOfBirth: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("allergies") val allergies: String? = null
)

data class ProfileResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("user") val user: UserProfile? = null,
    @SerializedName("error") val error: String? = null
)

// ── Chat models ────────────────────────────────────────────
data class ChatMessage(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("conversation_id") val conversationId: String,
    @SerializedName("message") val message: String,
    @SerializedName("is_user") val isUser: Boolean,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("ai_context") val aiContext: String? = null
)

data class ChatRequest(
    @SerializedName("message") val message: String,
    @SerializedName("conversation_id") val conversationId: String? = null,
    @SerializedName("symptoms") val symptoms: List<String>? = null
)

data class ChatResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null,
    @SerializedName("conversation_id") val conversationId: String? = null,
    @SerializedName("ai_response") val aiResponse: String? = null,
    @SerializedName("suggested_actions") val suggestedActions: List<String>? = null,
    @SerializedName("error") val error: String? = null
)

data class ChatHistoryResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("messages") val messages: List<ChatMessage>?,
    @SerializedName("error") val error: String? = null
)

// ── Medical Records models ─────────────────────────────────
data class MedicalRecord(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("type") val type: String, // "lab_test", "x_ray", "prescription", "report"
    @SerializedName("date") val date: String,
    @SerializedName("doctor_name") val doctorName: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("file_url") val fileUrl: String? = null,
    @SerializedName("created_at") val createdAt: String
)

data class MedicalRecordsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("records") val records: List<MedicalRecord>?,
    @SerializedName("count") val count: Int? = null,
    @SerializedName("error") val error: String? = null
)

// ── Prescriptions models ───────────────────────────────────
data class Prescription(
    @SerializedName("id") val id: Int,
    @SerializedName("medication_name") val medicationName: String,
    @SerializedName("dosage") val dosage: String,
    @SerializedName("frequency") val frequency: String,
    @SerializedName("duration") val duration: String,
    @SerializedName("doctor_name") val doctorName: String,
    @SerializedName("issued_date") val issuedDate: String,
    @SerializedName("status") val status: String, // "active", "completed", "cancelled"
    @SerializedName("refills_remaining") val refillsRemaining: Int,
    @SerializedName("instructions") val instructions: String? = null
)

data class PrescriptionsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("prescriptions") val prescriptions: List<Prescription>?,
    @SerializedName("error") val error: String? = null
)

// ── Notifications models ───────────────────────────────────
data class Notification(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("message") val message: String,
    @SerializedName("type") val type: String, // "appointment", "prescription", "message", "alert"
    @SerializedName("is_read") val isRead: Boolean,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("action_url") val actionUrl: String? = null
)

data class NotificationsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("notifications") val notifications: List<Notification>?,
    @SerializedName("unread_count") val unreadCount: Int? = null,
    @SerializedName("error") val error: String? = null
)

data class MarkReadResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null,
    @SerializedName("error") val error: String? = null
)

// ── Doctor-Patient Chat Session models ────────────────────
data class ChatSession(
    @SerializedName("id") val id: Int,
    @SerializedName("patient_id") val patientId: Int,
    @SerializedName("patient_name") val patientName: String,
    @SerializedName("doctor_id") val doctorId: Int?,
    @SerializedName("doctor_name") val doctorName: String?,
    @SerializedName("chief_complaint") val chiefComplaint: String,
    @SerializedName("urgency") val urgency: String,  // "URGENT", "MODERATE", "MILD"
    @SerializedName("status") val status: String,    // "waiting", "active", "completed"
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("last_message_at") val lastMessageAt: String?
)

data class DirectMessage(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("session_id") val sessionId: Int,
    @SerializedName("sender_id") val senderId: Int,
    @SerializedName("sender_name") val senderName: String,
    @SerializedName("sender_type") val senderType: String,  // "patient" | "doctor"
    @SerializedName("message") val message: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("is_read") val isRead: Boolean = false
)

data class CreateChatSessionRequest(
    @SerializedName("chief_complaint") val chiefComplaint: String,
    @SerializedName("symptoms") val symptoms: String?,
    @SerializedName("urgency") val urgency: String?  // "URGENT", "MODERATE", "MILD"
)

data class SendMessageRequest(
    @SerializedName("message") val message: String
)

data class ChatSessionResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("session") val session: ChatSession? = null,
    @SerializedName("sessions") val sessions: List<ChatSession>? = null,
    @SerializedName("error") val error: String? = null
)

data class DirectMessagesResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("messages") val messages: List<DirectMessage>? = null,
    @SerializedName("message") val message: DirectMessage? = null,
    @SerializedName("error") val error: String? = null
)

// ── API Interface ──────────────────────────────────────────
interface ApiService {

    // Doctors
    @GET("api/doctors")
    suspend fun getDoctors(): DoctorResponse

    @GET("api/doctors/online")
    suspend fun getOnlineDoctors(): DoctorResponse

    // Auth
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("api/auth/register-doctor")
    suspend fun registerDoctor(@Body request: DoctorRegisterRequest): AuthResponse

    @POST("api/auth/logout")
    suspend fun logout(): AuthResponse

    // Appointments
    @GET("api/appointments")
    suspend fun getAppointments(): AppointmentsResponse

    @POST("api/appointments")
    suspend fun bookAppointment(@Body request: BookingRequest): BookingResponse

    @PUT("api/appointments/{id}/reschedule")
    suspend fun rescheduleAppointment(@Path("id") id: Int, @Body request: RescheduleRequest): BookingResponse

    @DELETE("api/appointments/{id}")
    suspend fun cancelAppointment(@Path("id") id: Int): BookingResponse

    // Profile
    @GET("api/profile")
    suspend fun getProfile(): ProfileResponse

    @PUT("api/profile")
    suspend fun updateProfile(@Body profile: Map<String, String>): ProfileResponse

    // Chat / AI Assistant
    @POST("api/chat/message")
    suspend fun sendChatMessage(@Body request: ChatRequest): ChatResponse

    @GET("api/chat/history")
    suspend fun getChatHistory(@Query("conversation_id") conversationId: String? = null): ChatHistoryResponse

    @DELETE("api/chat/history/{conversation_id}")
    suspend fun clearChatHistory(@Path("conversation_id") conversationId: String): ChatResponse

    // Medical Records
    @GET("api/medical-records")
    suspend fun getMedicalRecords(): MedicalRecordsResponse

    @POST("api/medical-records")
    suspend fun uploadMedicalRecord(@Body record: Map<String, String>): MedicalRecordsResponse

    @DELETE("api/medical-records/{id}")
    suspend fun deleteMedicalRecord(@Path("id") id: Int): MedicalRecordsResponse

    // Prescriptions
    @GET("api/prescriptions")
    suspend fun getPrescriptions(): PrescriptionsResponse

    @POST("api/prescriptions/{id}/refill")
    suspend fun requestRefill(@Path("id") id: Int): PrescriptionsResponse

    // Notifications
    @GET("api/notifications")
    suspend fun getNotifications(): NotificationsResponse

    @PUT("api/notifications/{id}/read")
    suspend fun markNotificationAsRead(@Path("id") id: Int): MarkReadResponse

    @PUT("api/notifications/read-all")
    suspend fun markAllNotificationsAsRead(): MarkReadResponse

    // Patient-Doctor Chat Sessions
    @POST("api/chat-sessions")
    suspend fun createChatSession(@Body request: CreateChatSessionRequest): ChatSessionResponse

    @GET("api/chat-sessions")
    suspend fun getChatSessions(): ChatSessionResponse

    @GET("api/chat-sessions/{session_id}/messages")
    suspend fun getChatSessionMessages(@Path("session_id") sessionId: Int): DirectMessagesResponse

    @POST("api/chat-sessions/{session_id}/messages")
    suspend fun sendDirectMessage(@Path("session_id") sessionId: Int, @Body request: SendMessageRequest): DirectMessagesResponse

    @PUT("api/chat-sessions/{session_id}/read")
    suspend fun markMessagesAsRead(@Path("session_id") sessionId: Int): MarkReadResponse
}

// ── Singleton client with auth interceptor ─────────────────
object ApiClient {

    /*
     * ── HOW TO SWITCH ENVIRONMENTS ────────────────────────────────────────
     *
     *  LOCAL EMULATOR  →  "http://10.0.2.2:3001/"
     *  LOCAL DEVICE    →  "http://192.168.x.x:3001/"   (your PC's LAN IP)
     *  PRODUCTION      →  "https://mediconnectug-api.onrender.com/"
     *                      (replace with your actual Render URL after deploy)
     *
     * ─────────────────────────────────────────────────────────────────────
     */
    private const val BASE_URL = "https://mediconnectug.onrender.com/" // ← PRODUCTION (Render)
    // private const val BASE_URL = "http://10.0.2.2:3001/"               // ← Local emulator

    /** Set this after login; the interceptor reads it on every request. */
    var authToken: String? = null

    val instance: ApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val req = original.newBuilder().apply {
                    header("Content-Type", "application/json")
                    authToken?.let { header("Authorization", "Bearer $it") }
                }.build()
                chain.proceed(req)
            }
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)   // Render cold start can take ~15s
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)          // auto-retry once on transient failures
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
