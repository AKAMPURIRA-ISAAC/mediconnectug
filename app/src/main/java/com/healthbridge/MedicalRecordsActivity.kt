package com.healthbridge

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.healthbridge.adapters.MedicalRecordAdapter
import com.healthbridge.data.repository.RepositoryFactory
import com.healthbridge.network.MedicalRecord
import kotlinx.coroutines.launch

class MedicalRecordsActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: MedicalRecordAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medical_records)

        recycler    = findViewById(R.id.recyclerMedicalRecords)
        emptyState  = findViewById(R.id.emptyState)
        progressBar = findViewById(R.id.progressBar)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        adapter = MedicalRecordAdapter(mutableListOf()) { record ->
            viewRecord(record)
        }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        loadMedicalRecords()
    }

    private fun loadMedicalRecords() {
        progressBar.visibility = View.VISIBLE
        recycler.visibility    = View.GONE
        emptyState.visibility  = View.GONE

        lifecycleScope.launch {
            val result = RepositoryFactory.medicalRecordRepository.getMedicalRecords(forceRefresh = true)
            progressBar.visibility = View.GONE
            result.onSuccess { entities ->
                if (entities.isNotEmpty()) {
                    val records = entities.map { e ->
                        MedicalRecord(e.id, e.title, e.type, e.date, e.doctorName, e.description, e.fileUrl, e.createdAt)
                    }
                    adapter.updateData(records)
                    recycler.visibility   = View.VISIBLE
                    emptyState.visibility = View.GONE
                } else {
                    showEmptyState()
                }
            }
            result.onFailure { showEmptyState() }
        }
    }

    private fun viewRecord(record: MedicalRecord) {
        Toast.makeText(this,
            "📄 ${record.title}\n${record.type} — ${record.date}",
            Toast.LENGTH_LONG).show()
    }

    private fun showEmptyState() {
        recycler.visibility   = View.GONE
        emptyState.visibility = View.VISIBLE
    }
}
