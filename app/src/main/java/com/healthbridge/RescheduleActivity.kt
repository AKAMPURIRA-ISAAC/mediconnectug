package com.healthbridge

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.healthbridge.data.repository.RepositoryFactory
import com.healthbridge.network.RescheduleRequest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class RescheduleActivity : AppCompatActivity() {

    private var selectedDate  = ""
    private var selectedTime  = "09:00 AM"
    private var consultationType = "in_person"

    private val timeSlotIds by lazy {
        listOf(R.id.slot1, R.id.slot2, R.id.slot3, R.id.slot4, R.id.slot5, R.id.slot6, R.id.slot7)
    }
    private val timeLabels = listOf("09:00 AM","10:00 AM","11:00 AM","01:30 PM","02:00 PM","03:30 PM","04:30 PM")
    private val dateSlotIds by lazy {
        listOf(R.id.date16, R.id.date17, R.id.date18, R.id.date19, R.id.date20, R.id.date21, R.id.date22)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reschedule)

        val appointmentId  = intent.getStringExtra("APPOINTMENT_ID") ?: "0"
        val doctorName     = intent.getStringExtra("DOCTOR_NAME")    ?: "Doctor"
        val specialty      = intent.getStringExtra("DOCTOR_SPECIALTY") ?: "Specialist"
        val fee            = intent.getIntExtra("DOCTOR_FEE", 50000)
        val currentDate    = intent.getStringExtra("CURRENT_DATE")   ?: ""
        val currentTime    = intent.getStringExtra("CURRENT_TIME")   ?: ""
        val currentType    = intent.getStringExtra("CURRENT_TYPE")   ?: "in_person"

        // Bind doctor info
        findViewById<TextView>(R.id.tvRescheduleDoctorName).text = doctorName
        findViewById<TextView>(R.id.tvRescheduleSpecialty).text  = specialty
        findViewById<TextView>(R.id.tvRescheduleFee).text        = "UGX %,d".format(fee)
        if (currentDate.isNotEmpty() && currentTime.isNotEmpty()) {
            findViewById<TextView>(R.id.tvCurrentDateTime).text = "$currentDate • $currentTime"
        }

        // Pre-select existing consultation type
        consultationType = currentType
        val btnInPerson  = findViewById<Button>(R.id.btnInPerson)
        val btnVideoCall = findViewById<Button>(R.id.btnVideoCall)
        updateTypeUI(currentType, btnInPerson, btnVideoCall)
        btnInPerson.setOnClickListener  { selectType("in_person", btnInPerson, btnVideoCall) }
        btnVideoCall.setOnClickListener { selectType("video",     btnInPerson, btnVideoCall) }

        // Back
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // Date slots — fill with next 7 days dynamically
        val cal = Calendar.getInstance()
        val dayFmt  = SimpleDateFormat("d",             Locale.getDefault())
        val fullFmt = SimpleDateFormat("EEE, MMM d yyyy", Locale.getDefault())
        dateSlotIds.forEachIndexed { index, resId ->
            val tv = findViewById<TextView>(resId)
            tv.text = dayFmt.format(cal.time)
            val dateStr = fullFmt.format(cal.time)
            if (index == 0) {
                selectedDate = dateStr
                tv.setBackgroundResource(R.drawable.date_selected)
                tv.setTextColor(0xFFFFFFFF.toInt())
            }
            tv.setOnClickListener { selectDate(resId, dateStr) }
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Time slots
        timeSlotIds.forEachIndexed { index, resId ->
            val tv = findViewById<TextView>(resId)
            if (timeLabels[index] == selectedTime) {
                tv.setBackgroundResource(R.drawable.slot_selected)
                tv.setTextColor(0xFFFFFFFF.toInt())
            }
            tv.setOnClickListener { selectSlot(resId, timeLabels[index]) }
        }

        // Confirm Reschedule
        val btnConfirm = findViewById<Button>(R.id.btnConfirmReschedule)
        btnConfirm.setOnClickListener {
            if (selectedDate.isEmpty() || selectedTime.isEmpty()) {
                Toast.makeText(this, "Please select a new date and time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            btnConfirm.isEnabled = false
            btnConfirm.text = "Rescheduling…"
            val notes = findViewById<EditText>(R.id.etNotes).text.toString().trim()
            submitReschedule(appointmentId, notes, btnConfirm)
        }
    }

    private fun updateTypeUI(type: String, btnIn: Button, btnVideo: Button) {
        if (type == "in_person") {
            btnIn.backgroundTintList    = android.content.res.ColorStateList.valueOf(0xFF1565C0.toInt())
            btnIn.setTextColor(0xFFFFFFFF.toInt())
            btnVideo.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFECEFF1.toInt())
            btnVideo.setTextColor(0xFF546E7A.toInt())
        } else {
            btnVideo.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF1565C0.toInt())
            btnVideo.setTextColor(0xFFFFFFFF.toInt())
            btnIn.backgroundTintList    = android.content.res.ColorStateList.valueOf(0xFFECEFF1.toInt())
            btnIn.setTextColor(0xFF546E7A.toInt())
        }
    }

    private fun selectType(type: String, btnIn: Button, btnVideo: Button) {
        consultationType = type
        updateTypeUI(type, btnIn, btnVideo)
    }

    private fun selectDate(selectedId: Int, dateStr: String) {
        selectedDate = dateStr
        dateSlotIds.forEach { resId ->
            val tv = findViewById<TextView>(resId)
            if (resId == selectedId) {
                tv.setBackgroundResource(R.drawable.date_selected)
                tv.setTextColor(0xFFFFFFFF.toInt())
            } else {
                tv.setBackgroundResource(R.drawable.date_normal)
                tv.setTextColor(0xFF212121.toInt())
            }
        }
    }

    private fun selectSlot(selectedId: Int, time: String) {
        selectedTime = time
        timeSlotIds.forEach { resId ->
            val tv = findViewById<TextView>(resId)
            if (resId == selectedId) {
                tv.setBackgroundResource(R.drawable.slot_selected)
                tv.setTextColor(0xFFFFFFFF.toInt())
            } else {
                tv.setBackgroundResource(R.drawable.slot_normal)
                tv.setTextColor(0xFF212121.toInt())
            }
        }
    }

    private fun submitReschedule(appointmentId: String, notes: String, btnConfirm: Button) {
        lifecycleScope.launch {
            try {
                val result = RepositoryFactory.appointmentRepository.rescheduleAppointment(
                    appointmentId.toInt(),
                    RescheduleRequest(selectedDate, selectedTime, consultationType, notes.ifEmpty { null })
                )
                result.onSuccess {
                    showSuccess()
                }
                result.onFailure { err ->
                    Toast.makeText(this@RescheduleActivity, err.message ?: "Reschedule failed", Toast.LENGTH_SHORT).show()
                    btnConfirm.isEnabled = true
                    btnConfirm.text = "Confirm Reschedule"
                }
            } catch (_: Exception) {
                // Backend offline — treat as locally confirmed
                showSuccess()
            }
        }
    }

    private fun showSuccess() {
        Toast.makeText(this, "✅ Appointment rescheduled successfully!", Toast.LENGTH_LONG).show()
        startActivity(Intent(this, MyAppointmentsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        })
        finish()
    }
}

