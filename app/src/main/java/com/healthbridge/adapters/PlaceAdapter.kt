package com.healthbridge.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.healthbridge.R

data class PlaceItem(
    val name: String,
    val address: String,
    val hours: String,
    val isOpen: Boolean,
    val icon: String,
    val iconBgColor: Int,    // Android color int
    val mapLat: Double,
    val mapLng: Double
)

class PlaceAdapter(
    private val places: MutableList<PlaceItem> = mutableListOf()
) : RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder>() {

    fun updateData(newPlaces: List<PlaceItem>) {
        places.clear()
        places.addAll(newPlaces)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_place, parent, false)
        return PlaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        holder.bind(places[position])
    }

    override fun getItemCount(): Int = places.size

    class PlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvIcon: TextView = itemView.findViewById(R.id.tvPlaceIcon)
        private val tvName: TextView = itemView.findViewById(R.id.tvPlaceName)
        private val tvAddress: TextView = itemView.findViewById(R.id.tvPlaceAddress)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvPlaceStatus)
        private val tvHours: TextView = itemView.findViewById(R.id.tvPlaceHours)
        private val btnDirections: LinearLayout = itemView.findViewById(R.id.btnGetDirections)

        fun bind(place: PlaceItem) {
            tvIcon.text = place.icon
            val bg = itemView.context.getDrawable(R.drawable.avatar_circle)?.mutate()
            bg?.setTint(place.iconBgColor)
            tvIcon.background = bg

            tvName.text = place.name
            tvAddress.text = place.address
            tvHours.text = place.hours
            tvStatus.text = if (place.isOpen) "Open Now" else "Closed"
            tvStatus.setTextColor(
                if (place.isOpen) 0xFF2E7D32.toInt() else 0xFFC62828.toInt()
            )

            val mapsUrl = "https://maps.google.com/?q=${Uri.encode(place.name + " " + place.address)}"
            btnDirections.setOnClickListener {
                val mapsUri = Uri.parse("geo:${place.mapLat},${place.mapLng}?q=${Uri.encode(place.name)}")
                val mapsIntent = Intent(Intent.ACTION_VIEW, mapsUri)
                mapsIntent.setPackage("com.google.android.apps.maps")
                if (mapsIntent.resolveActivity(itemView.context.packageManager) != null) {
                    itemView.context.startActivity(mapsIntent)
                } else {
                    itemView.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl)))
                }
            }
            itemView.setOnClickListener {
                itemView.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl)))
            }
        }
    }
}

