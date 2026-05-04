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
import com.healthbridge.adapters.PrescriptionAdapter
import com.healthbridge.data.repository.RepositoryFactory
import com.healthbridge.network.Prescription
import kotlinx.coroutines.launch

class PrescriptionsActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: PrescriptionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prescriptions)

        recycler    = findViewById(R.id.recyclerPrescriptions)
        emptyState  = findViewById(R.id.emptyState)
        progressBar = findViewById(R.id.progressBar)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        adapter = PrescriptionAdapter(mutableListOf()) { prescription ->
            requestRefill(prescription)
        }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        loadPrescriptions()
    }

    private fun loadPrescriptions() {
        progressBar.visibility = View.VISIBLE
        recycler.visibility    = View.GONE
        emptyState.visibility  = View.GONE

        lifecycleScope.launch {
            val result = RepositoryFactory.prescriptionRepository.getPrescriptions(forceRefresh = true)
            progressBar.visibility = View.GONE
            result.onSuccess { entities ->
                if (entities.isNotEmpty()) {
                    val prescriptions = entities.map { e ->
                        Prescription(e.id, e.medicationName, e.dosage, e.frequency, e.duration,
                            e.doctorName, e.issuedDate, e.status, e.refillsRemaining, e.instructions)
                    }
                    adapter.updateData(prescriptions)
                    recycler.visibility   = View.VISIBLE
                    emptyState.visibility = View.GONE
                } else {
                    showEmptyState()
                }
            }
            result.onFailure { showEmptyState() }
        }
    }

    private fun requestRefill(prescription: Prescription) {
        lifecycleScope.launch {
            val result = RepositoryFactory.prescriptionRepository.requestRefill(prescription.id)
            result.onSuccess {
                Toast.makeText(this@PrescriptionsActivity,
                    "✅ Refill requested for ${prescription.medicationName}", Toast.LENGTH_SHORT).show()
                loadPrescriptions()
            }
            result.onFailure {
                Toast.makeText(this@PrescriptionsActivity,
                    "📴 Refill request queued (offline)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEmptyState() {
        recycler.visibility   = View.GONE
        emptyState.visibility = View.VISIBLE
    }
}
