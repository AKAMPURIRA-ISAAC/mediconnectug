package com.healthbridge.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.healthbridge.R
import com.healthbridge.network.MedicalRecord

class MedicalRecordAdapter(
    private val items: MutableList<MedicalRecord>,
    private val onView: (MedicalRecord) -> Unit
) : RecyclerView.Adapter<MedicalRecordAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTypeIcon: TextView    = view.findViewById(R.id.tvTypeIcon)
        val tvTitle: TextView       = view.findViewById(R.id.tvRecordTitle)
        val tvType: TextView        = view.findViewById(R.id.tvRecordType)
        val tvDoctor: TextView      = view.findViewById(R.id.tvRecordDoctor)
        val tvDate: TextView        = view.findViewById(R.id.tvRecordDate)
        val btnView: ImageView      = view.findViewById(R.id.btnView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medical_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvTitle.text  = item.title
        holder.tvType.text   = item.type.replace("_", " ").replaceFirstChar { it.uppercase() }
        holder.tvDoctor.text = item.doctorName ?: "Unknown Doctor"
        holder.tvDate.text   = item.date

        // Icon based on type
        holder.tvTypeIcon.text = when (item.type) {
            "lab_test"     -> "🧪"
            "x_ray"        -> "🩻"
            "prescription" -> "💊"
            "scan"         -> "🔬"
            else           -> "📋"
        }

        holder.btnView.setOnClickListener { onView(item) }
        holder.itemView.setOnClickListener { onView(item) }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<MedicalRecord>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}

