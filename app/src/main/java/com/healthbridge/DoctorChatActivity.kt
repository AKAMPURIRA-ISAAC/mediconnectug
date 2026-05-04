package com.healthbridge

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.healthbridge.network.ApiClient
import com.healthbridge.network.DirectMessage
import com.healthbridge.network.SendMessageRequest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DoctorChatActivity : AppCompatActivity() {

    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageView
    private lateinit var tvPatientName: TextView
    private lateinit var tvChiefComplaint: TextView

    private val messages = mutableListOf<DirectMessage>()
    private lateinit var adapter: DoctorMessageAdapter
    private val handler = Handler(Looper.getMainLooper())
    private var sessionId: Int = -1
    private var myDoctorId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_chat)

        sessionId = intent.getIntExtra("session_id", -1)
        val patientName = intent.getStringExtra("patient_name") ?: "Patient"
        val chiefComplaint = intent.getStringExtra("chief_complaint") ?: "General consultation"
        val urgencyLevel = intent.getStringExtra("urgency_level") ?: "normal"

        tvPatientName = findViewById(R.id.tvPatientName)
        tvChiefComplaint = findViewById(R.id.tvChiefComplaint)
        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)

        tvPatientName.text = patientName
        tvChiefComplaint.text = "📋 $chiefComplaint ${getUrgencyIcon(urgencyLevel)}"

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // Get doctor ID from prefs (stored during login)
        val prefs = getSharedPreferences("HealthBridge", MODE_PRIVATE)
        myDoctorId = prefs.getInt("userId", -1)

        setupRecyclerView()
        loadMessages()

        btnSend.setOnClickListener { sendMessage() }
        etMessage.setOnEditorActionListener { _, _, _ -> sendMessage(); true }
    }

    private fun setupRecyclerView() {
        adapter = DoctorMessageAdapter(messages, myDoctorId)
        rvMessages.layoutManager = LinearLayoutManager(this)
        rvMessages.adapter = adapter
    }

    private fun loadMessages() {
        if (sessionId == -1) return
        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.getChatSessionMessages(sessionId)
                if (response.success && response.messages != null) {
                    messages.clear()
                    messages.addAll(response.messages)
                    adapter.notifyDataSetChanged()
                    scrollToBottom()
                    markAsRead()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DoctorChatActivity, "Failed to load messages", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty() || sessionId == -1) return

        etMessage.text.clear()

        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.sendDirectMessage(
                    sessionId,
                    SendMessageRequest(text)
                )
                if (response.success && response.message != null) {
                    messages.add(response.message)
                    adapter.notifyItemInserted(messages.size - 1)
                    scrollToBottom()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DoctorChatActivity, "Failed to send message", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun markAsRead() {
        lifecycleScope.launch {
            try {
                ApiClient.instance.markMessagesAsRead(sessionId)
            } catch (_: Exception) { }
        }
    }

    private fun scrollToBottom() {
        rvMessages.post {
            if (messages.isNotEmpty()) rvMessages.smoothScrollToPosition(messages.size - 1)
        }
    }

    private fun getUrgencyIcon(urgency: String): String = when (urgency) {
        "emergency" -> "🔴"
        "urgent" -> "🟠"
        "moderate" -> "🟡"
        else -> "🟢"
    }
}

// ── Message Adapter for Doctor Chat ──────────────────────────────────────────
class DoctorMessageAdapter(
    private val messages: List<DirectMessage>,
    private val myDoctorId: Int
) : RecyclerView.Adapter<DoctorMessageAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(messages[position])
    override fun getItemCount(): Int = messages.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val container: LinearLayout = itemView.findViewById(R.id.layoutMessage)

        fun bind(msg: DirectMessage) {
            val isSent = msg.senderType == "doctor" && msg.senderId == myDoctorId
            tvMessage.text = msg.message
            tvTime.text = SimpleDateFormat("hh:mm a", Locale.getDefault())
                .format(Date(msg.timestamp.toLongOrNull() ?: System.currentTimeMillis()))

            if (isSent) {
                container.gravity = Gravity.END
                tvMessage.setBackgroundResource(R.drawable.chat_bubble_sent)
                tvMessage.setTextColor(0xFFFFFFFF.toInt())
            } else {
                container.gravity = Gravity.START
                tvMessage.setBackgroundResource(R.drawable.chat_bubble_received)
                tvMessage.setTextColor(0xFF212121.toInt())
            }
        }
    }
}

