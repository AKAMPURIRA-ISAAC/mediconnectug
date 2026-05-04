package com.healthbridge.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.healthbridge.R
import com.healthbridge.network.ChatSession

class DoctorChatSessionsAdapter(
    private val onSessionClick: (ChatSession) -> Unit
) : ListAdapter<ChatSession, DoctorChatSessionsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_doctor_chat_session, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPatientName: TextView = itemView.findViewById(R.id.tvPatientName)
        private val tvChiefComplaint: TextView = itemView.findViewById(R.id.tvChiefComplaint)
        private val tvUrgency: TextView = itemView.findViewById(R.id.tvUrgency)
        private val tvTimeAgo: TextView = itemView.findViewById(R.id.tvTimeAgo)

        fun bind(session: ChatSession) {
            tvPatientName.text = session.patientName
            tvChiefComplaint.text = session.chiefComplaint

            val urgencyText = when (session.urgency.uppercase()) {
                "URGENT" -> "🔴 URGENT"
                "MODERATE" -> "🟡 Moderate"
                else -> "🟢 Mild"
            }
            tvUrgency.text = urgencyText
            tvTimeAgo.text = formatTimeAgo(session.lastMessageAt ?: session.createdAt)

            itemView.setOnClickListener { onSessionClick(session) }
        }

        private fun formatTimeAgo(timestamp: String): String {
            // Simple time ago formatting
            return try {
                val time = timestamp.toLongOrNull() ?: return "Just now"
                val diff = System.currentTimeMillis() - time
                val minutes = diff / 60000
                val hours = minutes / 60
                val days = hours / 24
                when {
                    minutes < 1 -> "Just now"
                    minutes < 60 -> "$minutes min ago"
                    hours < 24 -> "$hours hr ago"
                    else -> "$days days ago"
                }
            } catch (e: Exception) {
                "Recently"
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ChatSession>() {
        override fun areItemsTheSame(oldItem: ChatSession, newItem: ChatSession) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ChatSession, newItem: ChatSession) =
            oldItem == newItem
    }
}

