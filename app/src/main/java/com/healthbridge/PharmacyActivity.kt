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

class PharmacyActivity : AppCompatActivity() {

    private lateinit var adapter: PlaceAdapter
    private var allPlaces: List<PlaceItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pharmacy)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // Open Google Maps search for pharmacies in Kampala
        findViewById<ImageView>(R.id.btnOpenMaps).setOnClickListener {
            openMaps("pharmacy Kampala Uganda")
        }

        val recycler = findViewById<RecyclerView>(R.id.recyclerPharmacies)
        adapter = PlaceAdapter()
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        allPlaces = getUgandaPharmacies()
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

    private fun getUgandaPharmacies(): List<PlaceItem> = listOf(
        PlaceItem("Victoria Pharmacy",          "Kampala Road, Kampala",             "8:00 AM – 9:00 PM",  true,  "💊", 0xFFE3F2FD.toInt(), 0.3136, 32.5811),
        PlaceItem("Quality Chemicals Ltd",      "Plot 3, Nile Ave, Kampala",          "7:30 AM – 10:00 PM", true,  "💊", 0xFFE3F2FD.toInt(), 0.3161, 32.5825),
        PlaceItem("Pharmacy Plus",              "Garden City Mall, Kampala",          "9:00 AM – 9:00 PM",  true,  "💊", 0xFFE3F2FD.toInt(), 0.3282, 32.5881),
        PlaceItem("Wandegeya Pharmacy",         "Wandegeya, Kampala",                 "8:00 AM – 8:00 PM",  true,  "💊", 0xFFE3F2FD.toInt(), 0.3444, 32.5703),
        PlaceItem("Mulago Outpatient Pharmacy", "Mulago National Referral Hospital",  "24 Hours",           true,  "💊", 0xFFE3F2FD.toInt(), 0.3397, 32.5718),
        PlaceItem("Nakivubo Pharmacy",          "Nakivubo, Kampala",                  "7:00 AM – 9:00 PM",  true,  "💊", 0xFFE3F2FD.toInt(), 0.3063, 32.5780),
        PlaceItem("Entebbe Pharmacy",           "Kampala Rd, Entebbe",                "8:00 AM – 8:00 PM",  true,  "💊", 0xFFE3F2FD.toInt(), 0.0623, 32.4628),
        PlaceItem("Mbarara Pharmacy",           "High Street, Mbarara",               "8:30 AM – 7:30 PM",  false, "💊", 0xFFE3F2FD.toInt(),-0.6042, 30.6445),
        PlaceItem("Gulu Pharmacy",              "Aswa Road, Gulu",                    "8:00 AM – 7:00 PM",  true,  "💊", 0xFFE3F2FD.toInt(), 2.7809, 32.2992),
        PlaceItem("Jinja Pharmacy",             "Main Street, Jinja",                 "8:00 AM – 8:00 PM",  true,  "💊", 0xFFE3F2FD.toInt(), 0.4461, 33.2028)
    )
}

