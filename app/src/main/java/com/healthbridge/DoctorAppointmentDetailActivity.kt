package com.healthbridge

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.healthbridge.adapters.AppointmentItem
import com.healthbridge.network.ApiClient
import kotlinx.coroutines.launch

class DoctorAppointmentDetailActivity : AppCompatActivity() {

    private lateinit var item: AppointmentItem
    private lateinit var llConfirmActions: LinearLayout
    private lateinit var llChatCall: LinearLayout
    private lateinit var btnConfirm: Button
    private lateinit var btnReject: Button
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_appointment_detail)

        val itemData = intent.getSerializableExtra("APPOINTMENT_ITEM") as? AppointmentItem
        if (itemData == null) {
            finish()
            return
        }
        item = itemData

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // Bind views
        tvStatus = findViewById(R.id.tvStatus)
        llConfirmActions = findViewById(R.id.llConfirmActions)
        llChatCall = findViewById(R.id.llChatCall)
        btnConfirm = findViewById(R.id.btnConfirm)
        btnReject = findViewById(R.id.btnReject)

        // Display appointment details
        findViewById<TextView>(R.id.tvPatientName).text = item.doctorName // In doctor view, this is patient name
        findViewById<TextView>(R.id.tvAppointmentType).text = item.type.replaceFirstChar { it.uppercase() }
        findViewById<TextView>(R.id.tvDateTime).text = "${item.date} at ${item.time}"
        tvStatus.text = item.status.replaceFirstChar { it.uppercase() }
        findViewById<TextView>(R.id.tvFee).text = "UGX ${item.fee}"

        // Show appropriate buttons based on status
        when (item.status) {
            "pending" -> {
                llConfirmActions.visibility = View.VISIBLE
                llChatCall.visibility = View.GONE
                tvStatus.setBackgroundColor(0xFFFFA000.toInt()) // Orange for pending
            }
            "upcoming", "confirmed" -> {
                llConfirmActions.visibility = View.GONE
                llChatCall.visibility = View.VISIBLE
                tvStatus.setBackgroundColor(0xFF1565C0.toInt()) // Blue for upcoming
            }
            else -> {
                llConfirmActions.visibility = View.GONE
                llChatCall.visibility = View.GONE
            }
        }

        // Confirm appointment button
        btnConfirm.setOnClickListener {
            confirmAppointment()
        }

        // Reject appointment button
        btnReject.setOnClickListener {
            showRejectDialog()
        }

        // Chat with patient button
        findViewById<Button>(R.id.btnChatWithPatient).setOnClickListener {
            startChatWithPatient()
        }

        // Call patient button
        findViewById<Button>(R.id.btnCallPatient).setOnClickListener {
            Toast.makeText(this, "Calling patient...", Toast.LENGTH_SHORT).show()
            // You can add actual calling functionality here
        }
    }

    private fun confirmAppointment() {
        btnConfirm.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.confirmAppointment(item.id.toInt())
                if (response.success) {
                    Toast.makeText(this@DoctorAppointmentDetailActivity,
                        "✅ Appointment confirmed!", Toast.LENGTH_SHORT).show()
                    item = item.copy(status = "upcoming")
                    updateUI()
                } else {
                    Toast.makeText(this@DoctorAppointmentDetailActivity,
                        "Failed: ${response.error}", Toast.LENGTH_SHORT).show()
                    btnConfirm.isEnabled = true
                }
            } catch (e: Exception) {
                Toast.makeText(this@DoctorAppointmentDetailActivity,
                    "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                btnConfirm.isEnabled = true
            }
        }
    }

    private fun showRejectDialog() {
        val reasons = arrayOf(
            "Schedule conflict",
            "Not available at that time",
            "Emergency situation",
            "Out of office",
            "Other"
        )

        AlertDialog.Builder(this)
            .setTitle("Reject Appointment")
            .setItems(reasons) { _, which ->
                rejectAppointment(reasons[which])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun rejectAppointment(reason: String) {
        btnReject.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.rejectAppointment(
                    item.id.toInt(),
                    mapOf("reason" to reason)
                )
                if (response.success) {
                    Toast.makeText(this@DoctorAppointmentDetailActivity,
                        "Appointment rejected", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@DoctorAppointmentDetailActivity,
                        "Failed: ${response.error}", Toast.LENGTH_SHORT).show()
                    btnReject.isEnabled = true
                }
            } catch (e: Exception) {
                Toast.makeText(this@DoctorAppointmentDetailActivity,
                    "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                btnReject.isEnabled = true
            }
        }
    }

    private fun startChatWithPatient() {
        lifecycleScope.launch {
            try {
                // Create or get existing chat session from appointment
                val appointmentId = item.id.toInt()
                android.util.Log.d("DoctorChat", "Starting chat for appointment ID: $appointmentId")
                val response = ApiClient.instance.createChatSessionFromAppointment(appointmentId)
                if (response.success && response.session != null) {
                    val session = response.session
                    android.util.Log.d("DoctorChat", "Chat session created: ${session.id}")
                    val intent = Intent(this@DoctorAppointmentDetailActivity, DoctorChatActivity::class.java)
                    intent.putExtra("session_id", session.id)
                    intent.putExtra("patient_name", session.patientName)
                    intent.putExtra("chief_complaint", session.chiefComplaint)
                    intent.putExtra("urgency_level", session.urgency)
                    startActivity(intent)
                } else {
                    android.util.Log.e("DoctorChat", "Failed to start chat: ${response.error}")
                    Toast.makeText(this@DoctorAppointmentDetailActivity,
                        "Failed to start chat: ${response.error}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("DoctorChat", "Error starting chat", e)
                Toast.makeText(this@DoctorAppointmentDetailActivity,
                    "Error starting chat: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateUI() {
        tvStatus.text = item.status.replaceFirstChar { it.uppercase() }
        when (item.status) {
            "pending" -> {
                llConfirmActions.visibility = View.VISIBLE
                llChatCall.visibility = View.GONE
                tvStatus.setBackgroundColor(0xFFFFA000.toInt())
            }
            "upcoming", "confirmed" -> {
                llConfirmActions.visibility = View.GONE
                llChatCall.visibility = View.VISIBLE
                tvStatus.setBackgroundColor(0xFF1565C0.toInt())
            }
            else -> {
                llConfirmActions.visibility = View.GONE
                llChatCall.visibility = View.GONE
            }
        }
    }
}

