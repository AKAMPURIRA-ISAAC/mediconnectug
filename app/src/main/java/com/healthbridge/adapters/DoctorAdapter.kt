package com.healthbridge.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.healthbridge.R
import java.text.NumberFormat
import java.util.Locale

data class DoctorItem(
    val id: String,
    val name: String,
    val specialty: String,
    val rating: Double,
    val reviewCount: Int,
    val distance: String,
    val fee: Int,
    val isOnline: Boolean = true,
    val isAvailableToday: Boolean = true
)

class DoctorAdapter(
    private val doctors: MutableList<DoctorItem> = mutableListOf(),
    private val onBookClick: (DoctorItem) -> Unit
) : RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder>() {

    private val avatarColors = listOf(
        "#1565C0", "#7B1FA2", "#00796B", "#E64A19", "#1976D2", "#388E3C",
        "#AD1457", "#0097A7", "#5D4037", "#1B5E20"
    )

    fun updateData(newDoctors: List<DoctorItem>) {
        doctors.clear()
        doctors.addAll(newDoctors)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_doctor, parent, false)
        return DoctorViewHolder(view)
    }

    override fun onBindViewHolder(holder: DoctorViewHolder, position: Int) {
        holder.bind(doctors[position], onBookClick, avatarColors[position % avatarColors.size])
    }

    override fun getItemCount(): Int = doctors.size

    class DoctorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAvatarInitials: TextView = itemView.findViewById(R.id.tvAvatarInitials)
        private val vStatusIndicator: View = itemView.findViewById(R.id.vStatusIndicator)
        private val tvName: TextView = itemView.findViewById(R.id.tvDoctorName)
        private val tvSpecialty: TextView = itemView.findViewById(R.id.tvSpecialty)
        private val tvRatingValue: TextView = itemView.findViewById(R.id.tvRatingValue)
        private val tvReviewCount: TextView = itemView.findViewById(R.id.tvReviewCount)
        private val tvDistance: TextView = itemView.findViewById(R.id.tvDistance)
        private val tvFee: TextView = itemView.findViewById(R.id.tvFee)
        private val tvAvailableBadge: TextView = itemView.findViewById(R.id.tvAvailableBadge)
        private val btnBook: Button = itemView.findViewById(R.id.btnBook)

        fun bind(doctor: DoctorItem, onBookClick: (DoctorItem) -> Unit, avatarColor: String) {
            // Generate initials avatar
            val nameWords = doctor.name.removePrefix("Dr. ").split(" ")
            val initials = nameWords.filter { it.isNotEmpty() }.take(2)
                .joinToString("") { it.first().uppercase() }
            tvAvatarInitials.text = initials

            val color = Color.parseColor(avatarColor)
            val bg = itemView.context.getDrawable(R.drawable.avatar_circle)?.mutate()
            bg?.setTint(color)
            tvAvatarInitials.background = bg

            tvName.text = doctor.name
            tvSpecialty.text = doctor.specialty
            tvRatingValue.text = doctor.rating.toString()
            tvReviewCount.text = "(${doctor.reviewCount})"
            tvDistance.text = doctor.distance

            val formatter = NumberFormat.getInstance(Locale.US)
            tvFee.text = "UGX ${formatter.format(doctor.fee)}"

            // Online status dot
            vStatusIndicator.visibility = if (doctor.isOnline) View.VISIBLE else View.GONE
            val dotBg = itemView.context.getDrawable(R.drawable.avatar_circle)?.mutate()
            dotBg?.setTint(if (doctor.isOnline) Color.parseColor("#4CAF50") else Color.parseColor("#BDBDBD"))
            vStatusIndicator.background = dotBg

            // Available Today badge
            tvAvailableBadge.visibility = if (doctor.isAvailableToday) View.VISIBLE else View.GONE

            btnBook.setOnClickListener { onBookClick(doctor) }
        }
    }
}
