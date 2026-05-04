package com.healthbridge

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.healthbridge.adapters.NotificationAdapter
import com.healthbridge.data.repository.RepositoryFactory
import com.healthbridge.network.Notification
import kotlinx.coroutines.launch

class NotificationsActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        recycler     = findViewById(R.id.recyclerNotifications)
        emptyState   = findViewById(R.id.emptyState)
        progressBar  = findViewById(R.id.progressBar)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        adapter = NotificationAdapter(mutableListOf()) { notification ->
            markAsRead(notification)
        }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        findViewById<TextView>(R.id.tvMarkAllRead).setOnClickListener {
            markAllRead()
        }

        loadNotifications()
    }

    private fun loadNotifications() {
        progressBar.visibility = View.VISIBLE
        recycler.visibility    = View.GONE
        emptyState.visibility  = View.GONE

        lifecycleScope.launch {
            val result = RepositoryFactory.notificationRepository.getNotifications(forceRefresh = true)
            progressBar.visibility = View.GONE
            result.onSuccess { entities ->
                if (entities.isNotEmpty()) {
                    val notifications = entities.map { e ->
                        Notification(e.id, e.title, e.message, e.type, e.isRead, e.createdAt, e.actionUrl)
                    }
                    adapter.updateData(notifications)
                    recycler.visibility   = View.VISIBLE
                    emptyState.visibility = View.GONE
                } else {
                    showEmptyState()
                }
            }
            result.onFailure { showEmptyState() }
        }
    }

    private fun markAsRead(notification: Notification) {
        lifecycleScope.launch {
            RepositoryFactory.notificationRepository.markAsRead(notification.id)
            loadNotifications()
        }
    }

    private fun markAllRead() {
        lifecycleScope.launch {
            val result = RepositoryFactory.notificationRepository.markAllAsRead()
            result.onSuccess {
                Toast.makeText(this@NotificationsActivity, "All notifications marked as read", Toast.LENGTH_SHORT).show()
                loadNotifications()
            }
            result.onFailure {
                Toast.makeText(this@NotificationsActivity, "📴 Offline — marked locally", Toast.LENGTH_SHORT).show()
                loadNotifications()
            }
        }
    }

    private fun showEmptyState() {
        recycler.visibility   = View.GONE
        emptyState.visibility = View.VISIBLE
    }
}
