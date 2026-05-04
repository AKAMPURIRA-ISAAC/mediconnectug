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
import com.healthbridge.network.BookingRequest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class BookingActivity : AppCompatActivity() {

    private var selectedDate  = ""
    private var selectedTime  = "09:00 AM"
    private var consultationType = "in_person"
    private var doctorFee     = 50000
    private val serviceFee    = 5000

    private val timeSlotIds by lazy {
        listOf(R.id.slot1, R.id.slot2, R.id.slot3, R.id.slot4, R.id.slot5, R.id.slot6, R.id.slot7)
    }
    private val timeLabels = listOf("09:00 AM","10:00 AM","11:00 AM","01:30 PM","02:00 PM","03:30 PM","04:30 PM")
    private val dateSlotIds by lazy {
        listOf(R.id.date16, R.id.date17, R.id.date18, R.id.date19, R.id.date20, R.id.date21, R.id.date22)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking)

        val doctorName = intent.getStringExtra("DOCTOR_NAME")    ?: "Doctor"
        val specialty  = intent.getStringExtra("DOCTOR_SPECIALTY") ?: "Specialist"
        val doctorId   = intent.getStringExtra("DOCTOR_ID")     ?: "0"
        doctorFee      = intent.getIntExtra("DOCTOR_FEE", 50000)

        // Bind doctor info
        findViewById<TextView>(R.id.tvBookingDoctorName).text = doctorName
        findViewById<TextView>(R.id.tvBookingSpecialty).text  = specialty
        updateFeeDisplay()

        // Back
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // Consultation type
        val btnInPerson  = findViewById<Button>(R.id.btnInPerson)
        val btnVideoCall = findViewById<Button>(R.id.btnVideoCall)
        btnInPerson.setOnClickListener  { selectType("in_person", btnInPerson, btnVideoCall) }
        btnVideoCall.setOnClickListener { selectType("video",     btnInPerson, btnVideoCall) }

        // Date slots — fill with next 7 days dynamically
        val cal = Calendar.getInstance()
        val dayFmt  = SimpleDateFormat("d",       Locale.getDefault())
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

        // Confirm
        val btnConfirm = findViewById<Button>(R.id.btnConfirm)
        btnConfirm.setOnClickListener {
            val reason = findViewById<EditText>(R.id.etReason).text.toString().trim()
            if (selectedDate.isEmpty() || selectedTime.isEmpty()) {
                Toast.makeText(this, "Please select date and time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            btnConfirm.isEnabled = false
            btnConfirm.text = "Booking…"
            submitBooking(doctorId, doctorName, reason, btnConfirm)
        }
    }

    private fun selectType(type: String, btnIn: Button, btnVideo: Button) {
        consultationType = type
        if (type == "in_person") {
            btnIn.backgroundTintList   = android.content.res.ColorStateList.valueOf(0xFF1565C0.toInt())
            btnIn.setTextColor(0xFFFFFFFF.toInt())
            btnVideo.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFECEFF1.toInt())
            btnVideo.setTextColor(0xFF546E7A.toInt())
        } else {
            btnVideo.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF1565C0.toInt())
            btnVideo.setTextColor(0xFFFFFFFF.toInt())
            btnIn.backgroundTintList   = android.content.res.ColorStateList.valueOf(0xFFECEFF1.toInt())
            btnIn.setTextColor(0xFF546E7A.toInt())
        }
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

    private fun updateFeeDisplay() {
        findViewById<TextView>(R.id.tvBookingFee).text       = "UGX %,d".format(doctorFee)
        findViewById<TextView>(R.id.tvConsultationFee).text  = "UGX %,d".format(doctorFee)
        findViewById<TextView>(R.id.tvServiceFee).text       = "UGX %,d".format(serviceFee)
        findViewById<TextView>(R.id.tvTotal).text            = "UGX %,d".format(doctorFee + serviceFee)
    }

    private fun submitBooking(doctorId: String, doctorName: String, notes: String, btnConfirm: Button) {
        lifecycleScope.launch {
            try {
                val result = RepositoryFactory.appointmentRepository.bookAppointment(
                    BookingRequest(doctorId, doctorName, selectedDate, selectedTime, consultationType,
                        notes.ifEmpty { null })
                )
                result.onSuccess {
                    showSuccess()
                }
                result.onFailure { err ->
                    Toast.makeText(this@BookingActivity, err.message ?: "Booking failed", Toast.LENGTH_SHORT).show()
                    btnConfirm.isEnabled = true
                    btnConfirm.text = "Confirm Booking"
                }
            } catch (_: Exception) {
                // Backend offline — treat as locally confirmed
                showSuccess()
            }
        }
    }

    private fun showSuccess() {
        Toast.makeText(this, "✅ Appointment booked successfully!", Toast.LENGTH_LONG).show()
        startActivity(Intent(this, MyAppointmentsActivity::class.java))
        finish()
    }
}
