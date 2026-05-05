package com.healthbridge

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.healthbridge.network.ApiClient
import com.healthbridge.network.ChatSession
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PatientChatListActivity : AppCompatActivity() {

    private lateinit var rvChatSessions: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var adapter: PatientChatSessionsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_chat_list)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        rvChatSessions = findViewById(R.id.rvChatSessions)
        emptyState = findViewById(R.id.emptyState)

        setupRecyclerView()
        loadChatSessions()
    }

    private fun setupRecyclerView() {
        adapter = PatientChatSessionsAdapter { session ->
            val intent = Intent(this, PatientChatActivity::class.java).apply {
                putExtra("session_id", session.id)
                putExtra("doctor_name", session.doctorName ?: "Doctor")
                putExtra("chief_complaint", session.chiefComplaint)
                putExtra("urgency_level", session.urgency)
            }
            startActivity(intent)
        }
        rvChatSessions.layoutManager = LinearLayoutManager(this)
        rvChatSessions.adapter = adapter
    }

    private fun loadChatSessions() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.getChatSessions()
                if (response.success && response.sessions != null) {
                    adapter.submitList(response.sessions)
                    val isEmpty = response.sessions.isEmpty()
                    emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
                    rvChatSessions.visibility = if (isEmpty) View.GONE else View.VISIBLE
                } else {
                    Toast.makeText(this@PatientChatListActivity, "Failed to load chats", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PatientChatListActivity, "Connection error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadChatSessions()
    }
}

class PatientChatSessionsAdapter(private val onItemClick: (ChatSession) -> Unit) :
    RecyclerView.Adapter<PatientChatSessionsAdapter.ViewHolder>() {

    private var sessions = listOf<ChatSession>()

    fun submitList(newList: List<ChatSession>) {
        sessions = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_doctor_chat_session, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(sessions[position])
    override fun getItemCount() = sessions.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvPatientName)
        private val tvComplaint: TextView = itemView.findViewById(R.id.tvChiefComplaint)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTimeAgo)
        private val tvUrgency: TextView = itemView.findViewById(R.id.tvUrgency)

        fun bind(session: ChatSession) {
            tvName.text = if (session.doctorName != null) "Dr. ${session.doctorName}" else "Doctor"
            tvComplaint.text = session.chiefComplaint
            
            // Format time
            val dateStr = session.lastMessageAt ?: session.createdAt
            try {
                // dateStr is likely ISO 8601 from backend
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date = sdf.parse(dateStr)
                if (date != null) {
                    val now = Date()
                    val diff = now.time - date.time
                    val minutes = diff / (1000 * 60)
                    val hours = minutes / 60
                    val days = hours / 24
                    
                    tvTime.text = when {
                        minutes < 1 -> "Just now"
                        minutes < 60 -> "$minutes min ago"
                        hours < 24 -> "$hours hours ago"
                        else -> "$days days ago"
                    }
                }
            } catch (_: Exception) {
                tvTime.text = ""
            }

            tvUrgency.text = session.urgency
            val urgencyColor = when (session.urgency.uppercase()) {
                "URGENT" -> 0xFFE53935.toInt()
                "MODERATE" -> 0xFFFB8C00.toInt()
                else -> 0xFF43A047.toInt()
            }
            tvUrgency.setTextColor(urgencyColor)

            itemView.setOnClickListener { onItemClick(session) }
        }
    }
}
