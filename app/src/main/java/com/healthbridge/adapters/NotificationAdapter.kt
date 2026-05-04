package com.healthbridge.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.healthbridge.R
import com.healthbridge.network.Notification

class NotificationAdapter(
    private val items: MutableList<Notification>,
    private val onMarkRead: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView    = view.findViewById(R.id.tvIcon)
        val tvTitle: TextView   = view.findViewById(R.id.tvTitle)
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val tvTime: TextView    = view.findViewById(R.id.tvTime)
        val unreadDot: View     = view.findViewById(R.id.unreadDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvTitle.text   = item.title
        holder.tvMessage.text = item.message
        holder.tvTime.text    = item.createdAt.take(10)  // Show date portion

        // Icon based on type
        holder.tvIcon.text = when (item.type) {
            "appointment" -> "📅"
            "prescription" -> "💊"
            "message"     -> "💬"
            "alert"       -> "⚠️"
            else          -> "🔔"
        }

        // Show unread dot
        holder.unreadDot.visibility = if (!item.isRead) View.VISIBLE else View.GONE

        // Dim read notifications
        holder.itemView.alpha = if (item.isRead) 0.7f else 1.0f

        // Click to mark as read
        holder.itemView.setOnClickListener {
            if (!item.isRead) onMarkRead(item)
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<Notification>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}

