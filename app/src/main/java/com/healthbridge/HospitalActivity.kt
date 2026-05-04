package com.healthbridge

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.healthbridge.adapters.PlaceAdapter
import com.healthbridge.adapters.PlaceItem

class HospitalActivity : AppCompatActivity() {

    private lateinit var adapter: PlaceAdapter
    private var allPlaces: List<PlaceItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hospital)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // Open Google Maps for hospitals in Kampala
        findViewById<ImageView>(R.id.btnOpenMaps).setOnClickListener {
            openMaps("hospital Kampala Uganda")
        }

        val recycler = findViewById<RecyclerView>(R.id.recyclerHospitals)
        adapter = PlaceAdapter()
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        allPlaces = getUgandaHospitals()
        adapter.updateData(allPlaces)

        // Search
        findViewById<EditText>(R.id.etSearch).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s?.toString()?.lowercase()?.trim() ?: ""
                adapter.updateData(if (q.isEmpty()) allPlaces
                    else allPlaces.filter { it.name.lowercase().contains(q) || it.address.lowercase().contains(q) })
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun openMaps(query: String) {
        val mapsUri = Uri.parse("geo:0.3163,32.5822?q=${Uri.encode(query)}")
        val intent = Intent(Intent.ACTION_VIEW, mapsUri)
        intent.setPackage("com.google.android.apps.maps")
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://maps.google.com/?q=${Uri.encode(query)}")))
        }
    }

    private fun getUgandaHospitals(): List<PlaceItem> = listOf(
        PlaceItem("Mulago National Referral Hospital", "Mulago Hill, Kampala",            "24 Hours",           true,  "🏥", 0xFFE8F5E9.toInt(), 0.3397, 32.5718),
        PlaceItem("International Hospital Kampala",    "Namuwongo, Kampala",               "24 Hours",           true,  "🏥", 0xFFE8F5E9.toInt(), 0.3009, 32.5951),
        PlaceItem("Nakasero Hospital",                 "Nakasero, Kampala",                "24 Hours",           true,  "🏥", 0xFFE8F5E9.toInt(), 0.3228, 32.5810),
        PlaceItem("Case Medical Center",               "Nsambya, Kampala",                 "24 Hours",           true,  "🏥", 0xFFE8F5E9.toInt(), 0.2967, 32.5865),
        PlaceItem("Aga Khan Health Services",          "Plot 1B, Kaggwa Rd, Kampala",      "8:00 AM – 8:00 PM",  true,  "🏥", 0xFFE8F5E9.toInt(), 0.3170, 32.5851),
        PlaceItem("Uganda Martyrs Hospital Lubaga",    "Lubaga Road, Kampala",             "24 Hours",           true,  "🏥", 0xFFE8F5E9.toInt(), 0.3028, 32.5560),
        PlaceItem("Mengo Hospital",                    "Namirembe, Kampala",               "24 Hours",           true,  "🏥", 0xFFE8F5E9.toInt(), 0.3122, 32.5619),
        PlaceItem("Nsambya Hospital",                  "Nsambya, Kampala",                 "24 Hours",           true,  "🏥", 0xFFE8F5E9.toInt(), 0.2913, 32.5861),
        PlaceItem("Butabika National Psychiatric Hospital", "Butabika, Kampala",           "24 Hours",           true,  "🏥", 0xFFE8F5E9.toInt(), 0.2878, 32.6428),
        PlaceItem("Mbarara Regional Referral Hospital","Mbarara",                          "24 Hours",           true,  "🏥", 0xFFE8F5E9.toInt(),-0.6101, 30.6441),
        PlaceItem("Gulu Regional Referral Hospital",   "Gulu",                             "24 Hours",           true,  "🏥", 0xFFE8F5E9.toInt(), 2.7742, 32.3027),
        PlaceItem("Lacor Hospital",                    "Gulu",                             "24 Hours",           true,  "🏥", 0xFFE8F5E9.toInt(), 2.7869, 32.2791)
    )
}

