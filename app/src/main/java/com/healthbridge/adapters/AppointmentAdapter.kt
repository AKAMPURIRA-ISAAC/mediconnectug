package com.healthbridge.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.healthbridge.R
import java.io.Serializable

data class AppointmentItem(
    val id: String,
    val doctorName: String,
    val specialty: String,
    val date: String,
    val time: String,
    val type: String,       // "in_person" | "video"
    val fee: Int,
    val status: String = "upcoming",   // "upcoming" | "past" | "cancelled" | "pending"
    val patientId: Int? = null,
    val doctorId: Int? = null
) : Serializable

class AppointmentAdapter(
    private val items: MutableList<AppointmentItem> = mutableListOf(),
    private val onItemClick: ((AppointmentItem) -> Unit)? = null,
    private val onReschedule: ((AppointmentItem) -> Unit)? = null,
    private val onCancel: ((AppointmentItem) -> Unit)? = null
) : RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder>() {

    fun updateData(newItems: List<AppointmentItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment, parent, false)
        return AppointmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) =
        holder.bind(items[position], onItemClick, onReschedule, onCancel)

    override fun getItemCount() = items.size

    class AppointmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDoctorName: TextView  = itemView.findViewById(R.id.tvDoctorName)
        private val tvSpecialty: TextView   = itemView.findViewById(R.id.tvSpecialty)
        private val tvDateTime: TextView    = itemView.findViewById(R.id.tvDateTime)
        private val tvType: TextView        = itemView.findViewById(R.id.tvType)
        private val tvFee: TextView         = itemView.findViewById(R.id.tvFee)
        private val tvStatus: TextView      = itemView.findViewById(R.id.tvStatus)
        private val llActions: LinearLayout = itemView.findViewById(R.id.llActions)
        private val btnReschedule: MaterialButton = itemView.findViewById(R.id.btnReschedule)
        private val btnCancel: MaterialButton     = itemView.findViewById(R.id.btnCancel)
        private val card: CardView          = itemView as CardView

        fun bind(
            item: AppointmentItem,
            onClick: ((AppointmentItem) -> Unit)?,
            onReschedule: ((AppointmentItem) -> Unit)?,
            onCancel: ((AppointmentItem) -> Unit)?
        ) {
            tvDoctorName.text = item.doctorName
            tvSpecialty.text  = item.specialty
            tvDateTime.text   = "${item.date} • ${item.time}"
            tvType.text       = if (item.type == "video") "🎥 Video" else "🏥 In-person"
            tvFee.text        = "UGX ${"%,d".format(item.fee)}"

            // Status chip colour
            val (statusText, colorHex) = when (item.status) {
                "upcoming"  -> Pair("Upcoming",  "#1565C0")
                "past"      -> Pair("Completed", "#2E7D32")
                "cancelled" -> Pair("Cancelled", "#C62828")
                else        -> Pair(item.status, "#546E7A")
            }
            tvStatus.text = statusText
            tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor(colorHex)
            )

            // Show action buttons only for upcoming appointments
            if (item.status == "upcoming") {
                llActions.visibility = View.VISIBLE
                btnReschedule.setOnClickListener { onReschedule?.invoke(item) }
                btnCancel.setOnClickListener { onCancel?.invoke(item) }
            } else {
                llActions.visibility = View.GONE
            }

            card.setOnClickListener { onClick?.invoke(item) }
        }
    }
}
