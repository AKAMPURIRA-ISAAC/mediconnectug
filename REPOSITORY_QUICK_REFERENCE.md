# Quick Reference - Repository Usage

## 🚀 Quick Start

### Import in Activity
```kotlin
import com.healthbridge.data.repository.RepositoryFactory
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
```

---

## 📚 Repository API Reference

### DoctorRepository
```kotlin
// Get all doctors
lifecycleScope.launch {
    val result = RepositoryFactory.doctorRepository.getDoctors()
    result.onSuccess { doctors ->
        // doctors: List<DoctorEntity>
        updateUI(doctors)
    }
    result.onFailure { error ->
        showError(error.message)
    }
}

// Force refresh from API
val result = RepositoryFactory.doctorRepository.getDoctors(forceRefresh = true)
```

### AppointmentRepository
```kotlin
// Get user's appointments
lifecycleScope.launch {
    val result = RepositoryFactory.appointmentRepository.getAppointments()
    result.onSuccess { appointments ->
        adapter.submitList(appointments)
    }
}

// Book new appointment
val bookRequest = BookingRequest(
    doctorId = "1",
    doctorName = "Dr. Sarah",
    date = "2026-05-15",
    time = "10:00 AM",
    type = "in_person",
    notes = "Annual checkup"
)
val result = RepositoryFactory.appointmentRepository.bookAppointment(bookRequest)
result.onSuccess { appointment ->
    Toast.makeText(this, "Appointment booked!", Toast.LENGTH_SHORT).show()
}

// Cancel appointment
val result = RepositoryFactory.appointmentRepository.cancelAppointment(appointmentId = 5)
```

### ProfileRepository
```kotlin
// Get user profile
lifecycleScope.launch {
    val result = RepositoryFactory.profileRepository.getProfile()
    result.onSuccess { user ->
        tvName.text = user.name
        tvEmail.text = user.email
        tvBloodType.text = user.bloodType ?: "Unknown"
    }
}

// Update profile
val updateMap = mapOf(
    "name" to "John Doe",
    "phone" to "+256123456789",
    "blood_type" to "O+",
    "address" to "Kampala, Uganda"
)
val result = RepositoryFactory.profileRepository.updateProfile(updateMap)

// Save user locally (manual)
val userEntity = UserEntity(
    id = 1,
    name = "John Doe",
    email = "john@example.com",
    phone = "+256123456789"
)
RepositoryFactory.profileRepository.saveUserLocally(userEntity)

// Get locally cached user
val cachedUser = RepositoryFactory.profileRepository.getCurrentUserLocal()
```

### ChatRepository
```kotlin
// Send chat message
lifecycleScope.launch {
    val request = ChatRequest(
        message = "I have a headache and fever",
        conversationId = "conv_123abc",
        symptoms = listOf("headache", "fever")
    )
    val result = RepositoryFactory.chatRepository.sendMessage(request)
    result.onSuccess { response ->
        tvAIResponse.text = response.aiResponse
        conversationId = response.conversationId
        
        // Show suggestions
        if (!response.suggestedActions.isNullOrEmpty()) {
            showSuggestions(response.suggestedActions)
        }
    }
    result.onFailure {
        // Backend offline - use local AI
        processOffline(request.message)
    }
}

// Get chat history
val result = RepositoryFactory.chatRepository.getChatHistory(conversationId = "conv_123abc")
result.onSuccess { messages ->
    adapter.submitList(messages)
}

// Get locally cached chat
val localMessages = RepositoryFactory.chatRepository.getLocalChatHistory("conv_123abc")

// Store message manually
val messageEntity = ChatMessageEntity(
    conversationId = "conv_123abc",
    message = "I feel better now",
    isUser = true,
    timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault()).format(java.util.Date())
)
RepositoryFactory.chatRepository.storeChatMessage(messageEntity)
```

### MedicalRecordRepository
```kotlin
// Get medical records
lifecycleScope.launch {
    val result = RepositoryFactory.medicalRecordRepository.getMedicalRecords()
    result.onSuccess { records ->
        adapter.submitList(records)
    }
}

// Upload medical record
val recordMap = mapOf(
    "title" to "Blood Test Results",
    "type" to "lab_test",
    "date" to "2026-05-04",
    "description" to "Complete Blood Count",
    "file_url" to "https://storage.example.com/file.pdf"
)
val result = RepositoryFactory.medicalRecordRepository.uploadRecord(recordMap)
result.onSuccess { newRecord ->
    Toast.makeText(this, "Record uploaded!", Toast.LENGTH_SHORT).show()
}

// Delete medical record
val result = RepositoryFactory.medicalRecordRepository.deleteRecord(recordId = 5)
```

### PrescriptionRepository
```kotlin
// Get all prescriptions
lifecycleScope.launch {
    val result = RepositoryFactory.prescriptionRepository.getPrescriptions()
    result.onSuccess { prescriptions ->
        adapter.submitList(prescriptions)
    }
}

// Get only active prescriptions
val result = RepositoryFactory.prescriptionRepository.getActivePrescriptions()
result.onSuccess { activePrescriptions ->
    tvActiveCount.text = activePrescriptions.size.toString()
}

// Request refill
val result = RepositoryFactory.prescriptionRepository.requestRefill(prescriptionId = 3)
result.onSuccess {
    Toast.makeText(this, "Refill requested!", Toast.LENGTH_SHORT).show()
}
```

### NotificationRepository
```kotlin
// Get notifications
lifecycleScope.launch {
    val result = RepositoryFactory.notificationRepository.getNotifications()
    result.onSuccess { notifications ->
        adapter.submitList(notifications)
    }
}

// Get unread count
val result = RepositoryFactory.notificationRepository.getUnreadCount()
result.onSuccess { count ->
    tvBadgeCount.text = count.toString()
}

// Mark notification as read
val result = RepositoryFactory.notificationRepository.markAsRead(notificationId = 7)

// Mark all as read
val result = RepositoryFactory.notificationRepository.markAllAsRead()
```

---

## 🔄 Result Handling Patterns

### Pattern 1: onSuccess/onFailure
```kotlin
val result = RepositoryFactory.doctorRepository.getDoctors()
result.onSuccess { doctors ->
    // Handle success
    updateUI(doctors)
}
result.onFailure { error ->
    // Handle error
    showError(error.message)
}
```

### Pattern 2: getOrNull
```kotlin
val result = RepositoryFactory.doctorRepository.getDoctors()
val doctors = result.getOrNull()
if (doctors != null) {
    updateUI(doctors)
} else {
    showError("Failed to load doctors")
}
```

### Pattern 3: fold
```kotlin
val result = RepositoryFactory.doctorRepository.getDoctors()
result.fold(
    onSuccess = { doctors -> updateUI(doctors) },
    onFailure = { error -> showError(error.message) }
)
```

### Pattern 4: Try-catch with Coroutines
```kotlin
lifecycleScope.launch {
    try {
        val result = RepositoryFactory.doctorRepository.getDoctors()
        result.onSuccess { doctors ->
            updateUI(doctors)
        }
    } catch (e: Exception) {
        showError(e.message)
    }
}
```

---

## 📋 Common Scenarios

### Scenario 1: Load data from cache or API
```kotlin
lifecycleScope.launch {
    val result = RepositoryFactory.doctorRepository.getDoctors(forceRefresh = false)
    result.onSuccess { doctors ->
        // Will use cache if available, otherwise fetch from API
        adapter.submitList(doctors)
    }
}
```

### Scenario 2: Force refresh from API
```kotlin
lifecycleScope.launch {
    val result = RepositoryFactory.doctorRepository.getDoctors(forceRefresh = true)
    result.onSuccess { freshDoctors ->
        adapter.submitList(freshDoctors)
    }
}
```

### Scenario 3: Handle offline gracefully
```kotlin
lifecycleScope.launch {
    val result = RepositoryFactory.appointmentRepository.getAppointments()
    result
        .onSuccess { appointments ->
            tvStatus.text = "✅ Online"
            adapter.submitList(appointments)
        }
        .onFailure { error ->
            tvStatus.text = "📴 Offline (showing cached)"
            // Repository already shows cached data
        }
}
```

### Scenario 4: Chain multiple requests
```kotlin
lifecycleScope.launch {
    // Get appointments
    val apptResult = RepositoryFactory.appointmentRepository.getAppointments()
    apptResult.onSuccess { appointments ->
        tvApptCount.text = appointments.size.toString()
        
        // Get doctors
        val doctorResult = RepositoryFactory.doctorRepository.getDoctors()
        doctorResult.onSuccess { doctors ->
            tvDoctorCount.text = doctors.size.toString()
        }
    }
}
```

### Scenario 5: Poll for updates
```kotlin
lifecycleScope.launch {
    // Refresh every 30 seconds
    while (isActive) {
        val result = RepositoryFactory.notificationRepository.getNotifications(forceRefresh = true)
        result.onSuccess { notifications ->
            updateNotificationBadge(notifications)
        }
        delay(30000) // 30 seconds
    }
}
```

---

## 🛠️ Error Handling Best Practices

### ✅ Good: Specific error messages
```kotlin
result.onFailure { error ->
    val message = when {
        error is TimeoutException -> "Request timed out. Check your connection."
        error is IOException -> "Network error. Please try again."
        else -> "Failed to load data: ${error.message}"
    }
    showError(message)
}
```

### ✅ Good: Show loading state
```kotlin
lifecycleScope.launch {
    progressBar.visibility = View.VISIBLE
    val result = RepositoryFactory.doctorRepository.getDoctors(forceRefresh = true)
    progressBar.visibility = View.GONE
    
    result.onSuccess { doctors ->
        adapter.submitList(doctors)
    }
    result.onFailure { error ->
        showError(error.message)
    }
}
```

### ❌ Bad: Ignore errors
```kotlin
lifecycleScope.launch {
    val result = RepositoryFactory.doctorRepository.getDoctors()
    // Not handling failure!
}
```

### ❌ Bad: Crash on error
```kotlin
lifecycleScope.launch {
    val result = RepositoryFactory.doctorRepository.getDoctors()
    val doctors = result.getOrThrow() // Will crash if error!
}
```

---

## 📱 Common Complete Examples

### Example 1: Doctor List Activity
```kotlin
class FindDoctorsActivity : AppCompatActivity() {
    private var allDoctors: List<DoctorItem> = emptyList()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        loadDoctors()
    }
    
    private fun loadDoctors() {
        lifecycleScope.launch {
            val result = RepositoryFactory.doctorRepository.getDoctors()
            result.onSuccess { doctors ->
                allDoctors = doctors.map { d ->
                    DoctorItem(
                        id = d.id.toString(),
                        name = d.name,
                        specialty = d.specialty,
                        rating = d.rating,
                        fee = d.consultationFee
                    )
                }
                adapter.updateData(allDoctors)
            }
            result.onFailure {
                Toast.makeText(this@FindDoctorsActivity, "Showing cached doctors", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
```

### Example 2: Chat Activity
```kotlin
private fun sendMessage() {
    val text = etMessage.text.toString().trim()
    if (text.isEmpty()) return
    
    adapter.addMessage(SimpleChatMessage(text, true))
    etMessage.text.clear()
    
    lifecycleScope.launch {
        val request = ChatRequest(
            message = text,
            conversationId = conversationId
        )
        
        val result = RepositoryFactory.chatRepository.sendMessage(request)
        result.onSuccess { response ->
            conversationId = response.conversationId
            addBotMessage(response.aiResponse ?: "...")
        }
        result.onFailure {
            processLocalAI(text) // Fallback
        }
    }
}
```

### Example 3: Profile Activity
```kotlin
override fun onResume() {
    super.onResume()
    
    lifecycleScope.launch {
        val result = RepositoryFactory.profileRepository.getProfile()
        result.onSuccess { user ->
            tvName.text = user.name
            tvEmail.text = user.email
            tvPhone.text = user.phone ?: ""
            tvBloodType.text = user.bloodType ?: "Unknown"
        }
    }
}
```

---

## 🎯 Tips & Tricks

### Tip 1: Cache invalidation
```kotlin
// Refresh data after making changes
val result = RepositoryFactory.appointmentRepository
    .bookAppointment(request)
    
result.onSuccess {
    // Refresh appointments list
    val refreshResult = RepositoryFactory.appointmentRepository
        .getAppointments(forceRefresh = true)
}
```

### Tip 2: Get data synchronously (if needed)
```kotlin
// For initialization or testing only
runBlocking {
    val result = RepositoryFactory.doctorRepository.getDoctors()
    result.onSuccess { doctors -> /* use doctors */ }
}
```

### Tip 3: Access local data directly
```kotlin
// Get profile from local database without API call
val user = RepositoryFactory.profileRepository.getCurrentUserLocal()
if (user != null) {
    updateUI(user)
}
```

---

## 🔗 Architecture Reminder

```
Activity
  ↓
RepositoryFactory.repository
  ↓
Repository (onSuccess/onFailure)
  ├→ Try API (Network)
  ├→ Save to Room DB
  ├→ Return cached if API fails
  ↓
DAO (Room Database)
  ↓
SQLite DB
```

---

**Happy coding! 🚀**

For more info, see `DATABASE_INTEGRATION_GUIDE.md` and `IMPLEMENTATION_SUMMARY.md`

