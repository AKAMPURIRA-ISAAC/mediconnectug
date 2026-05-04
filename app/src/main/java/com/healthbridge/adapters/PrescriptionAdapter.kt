package com.healthbridge.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.healthbridge.R
import com.healthbridge.network.Prescription

class PrescriptionAdapter(
    private val items: MutableList<Prescription>,
    private val onRefill: (Prescription) -> Unit
) : RecyclerView.Adapter<PrescriptionAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMedication: TextView = view.findViewById(R.id.tvMedicationName)
        val tvDosage: TextView     = view.findViewById(R.id.tvDosage)
        val tvStatus: TextView     = view.findViewById(R.id.tvStatus)
        val tvDoctor: TextView     = view.findViewById(R.id.tvDoctorName)
        val tvRefills: TextView    = view.findViewById(R.id.tvRefills)
        val btnRefill: Button      = view.findViewById(R.id.btnRefill)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_prescription, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvMedication.text = item.medicationName
        holder.tvDosage.text     = "${item.dosage} — ${item.frequency}"
        holder.tvDoctor.text     = item.doctorName
        holder.tvRefills.text    = "${item.refillsRemaining} refill${if (item.refillsRemaining != 1) "s" else ""}"

        // Status label and background badge
        holder.tvStatus.text = item.status.replaceFirstChar { it.uppercase() }
        val bgRes = when (item.status) {
            "active" -> R.drawable.badge_green
            else     -> R.drawable.badge_blue
        }
        holder.tvStatus.setBackgroundResource(bgRes)

        // Refill button only for active with remaining refills
        val canRefill = item.status == "active" && item.refillsRemaining > 0
        holder.btnRefill.isEnabled = canRefill
        holder.btnRefill.alpha = if (canRefill) 1.0f else 0.4f
        holder.btnRefill.setOnClickListener { if (canRefill) onRefill(item) }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<Prescription>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}

