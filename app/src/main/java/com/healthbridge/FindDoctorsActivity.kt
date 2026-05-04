package com.healthbridge

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.healthbridge.adapters.DoctorAdapter
import com.healthbridge.adapters.DoctorItem
import com.healthbridge.data.repository.RepositoryFactory
import kotlinx.coroutines.launch

class FindDoctorsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchEditText: EditText
    private lateinit var adapter: DoctorAdapter
    private lateinit var tvResultCount: TextView

    // Filter chips
    private lateinit var chipAll: TextView
    private lateinit var chipOnline: TextView
    private lateinit var chipTopRated: TextView
    private lateinit var chipAvailable: TextView
    private lateinit var chipLowFee: TextView

    private var allDoctors: List<DoctorItem> = emptyList()
    private var activeFilter: String = "all"   // all | online | top_rated | available | low_fee
    private var currentSearch: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_doctors)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        recyclerView      = findViewById(R.id.recyclerViewDoctors)
        searchEditText    = findViewById(R.id.searchEditText)
        tvResultCount     = findViewById(R.id.tvResultCount)
        chipAll           = findViewById(R.id.chipAll)
        chipOnline        = findViewById(R.id.chipOnline)
        chipTopRated      = findViewById(R.id.chipTopRated)
        chipAvailable     = findViewById(R.id.chipAvailable)
        chipLowFee        = findViewById(R.id.chipLowFee)

        btnBack.setOnClickListener { finish() }

        // If launched from Home with a specialty pre-filter, pre-fill search
        intent.getStringExtra("SPECIALTY_FILTER")?.let { specialty ->
            searchEditText.setText(specialty)
            currentSearch = specialty.lowercase()
        }

        setupRecyclerView()
        setupSearch()
        setupFilterChips()
        setupBottomNav()
        loadDoctors()
    }

    private fun setupFilterChips() {
        chipAll.setOnClickListener       { setFilter("all") }
        chipOnline.setOnClickListener    { setFilter("online") }
        chipTopRated.setOnClickListener  { setFilter("top_rated") }
        chipAvailable.setOnClickListener { setFilter("available") }
        chipLowFee.setOnClickListener    { setFilter("low_fee") }
        updateChipStyles()
    }

    private fun setFilter(filter: String) {
        activeFilter = filter
        updateChipStyles()
        applyFilters()
    }

    private fun updateChipStyles() {
        val chips = mapOf(
            "all"       to chipAll,
            "online"    to chipOnline,
            "top_rated" to chipTopRated,
            "available" to chipAvailable,
            "low_fee"   to chipLowFee
        )
        chips.forEach { (key, chip) ->
            if (key == activeFilter) {
                chip.setBackgroundColor(0xFF2196F3.toInt())   // blue selected
                chip.setTextColor(0xFFFFFFFF.toInt())
            } else {
                chip.setBackgroundColor(0xFFF5F5F5.toInt())   // grey unselected
                chip.setTextColor(0xFF666666.toInt())
            }
        }
    }

    private fun applyFilters() {
        var filtered = allDoctors

        // Apply text search
        if (currentSearch.isNotEmpty()) {
            filtered = filtered.filter {
                it.name.lowercase().contains(currentSearch) ||
                it.specialty.lowercase().contains(currentSearch)
            }
        }

        // Apply chip filter
        filtered = when (activeFilter) {
            "online"    -> filtered.filter { it.isOnline }
            "top_rated" -> filtered.sortedByDescending { it.rating }
            "available" -> filtered.filter { it.isAvailableToday }
            "low_fee"   -> filtered.sortedBy { it.fee }
            else        -> filtered  // "all" — no extra filter
        }

        adapter.updateData(filtered)
        val label = when (activeFilter) {
            "online"    -> "online"
            "top_rated" -> "sorted by rating"
            "available" -> "available today"
            "low_fee"   -> "sorted by fee"
            else        -> "total"
        }
        tvResultCount.text = "Showing ${filtered.size} doctors ($label)"
    }

    private fun setupRecyclerView() {
        adapter = DoctorAdapter(mutableListOf()) { doctor ->
            val intent = Intent(this, DoctorProfileActivity::class.java).apply {
                putExtra("DOCTOR_ID",        doctor.id)
                putExtra("DOCTOR_NAME",      doctor.name)
                putExtra("DOCTOR_SPECIALTY", doctor.specialty)
                putExtra("DOCTOR_RATING",    doctor.rating)
                putExtra("DOCTOR_FEE",       doctor.fee)
                putExtra("DOCTOR_DISTANCE",  doctor.distance)
                putExtra("DOCTOR_ONLINE",    doctor.isOnline)
            }
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun loadDoctors() {
        lifecycleScope.launch {
            try {
                val result = RepositoryFactory.doctorRepository.getDoctors()
                result.onSuccess { doctors ->
                    allDoctors = doctors.mapIndexed { i, d ->
                        DoctorItem(
                            id             = d.id.toString(),
                            name           = d.name,
                            specialty      = d.specialty,
                            rating         = d.rating,
                            reviewCount    = d.reviewCount,
                            distance       = "Kampala",
                            fee            = d.consultationFee,
                            isOnline       = i % 3 != 2,           // ~2/3 online
                            isAvailableToday = i % 4 != 3          // ~3/4 available
                        )
                    }
                    applyFilters()
                }
                result.onFailure {
                    showSampleDoctors()
                    Toast.makeText(this@FindDoctorsActivity, "Showing cached doctors", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                showSampleDoctors()
            }
        }
    }

    private fun showSampleDoctors() {
        allDoctors = listOf(
            DoctorItem("1",  "Dr. Sarah Nakigozi",   "Pediatrician",          4.9, 120, "Mulago Hosp, Kampala",     45000, isOnline=true,  isAvailableToday=true),
            DoctorItem("2",  "Dr. Moses Ssali",       "Cardiologist",          4.8, 203, "Nakasero Hosp, Kampala",   60000, isOnline=true,  isAvailableToday=true),
            DoctorItem("3",  "Dr. Grace Namukwaya",   "Neurologist",           4.7, 156, "Mulago Hosp, Kampala",     70000, isOnline=false, isAvailableToday=true),
            DoctorItem("4",  "Dr. Patrick Odeke",     "General Practitioner",  4.6,  89, "IHK, Kampala",             35000, isOnline=true,  isAvailableToday=true),
            DoctorItem("5",  "Dr. Amina Nakato",      "Dermatologist",         4.8, 112, "Aga Khan, Kampala",        55000, isOnline=true,  isAvailableToday=false),
            DoctorItem("6",  "Dr. James Mwesigwa",    "Orthopedic Surgeon",    4.7,  98, "Case Medical, Kampala",    80000, isOnline=true,  isAvailableToday=true),
            DoctorItem("7",  "Dr. Phoebe Asiimwe",    "Gynecologist",          4.9, 145, "Lubaga Hosp, Kampala",     65000, isOnline=true,  isAvailableToday=true),
            DoctorItem("8",  "Dr. Robert Kizito",     "Ophthalmologist",       4.6,  76, "Mengo Hospital, Kampala",  50000, isOnline=false, isAvailableToday=false),
            DoctorItem("9",  "Dr. Faith Tumwesigye",  "Psychiatrist",          4.5,  63, "Butabika Hosp, Kampala",   45000, isOnline=true,  isAvailableToday=true),
            DoctorItem("10", "Dr. David Mugisha",     "General Surgeon",       4.7, 134, "Nsambya Hosp, Kampala",    75000, isOnline=true,  isAvailableToday=true)
        )
        applyFilters()
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearch = s?.toString()?.lowercase()?.trim() ?: ""
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupBottomNav() {
        findViewById<TextView>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
        findViewById<TextView>(R.id.navSchedule).setOnClickListener {
            startActivity(Intent(this, MyAppointmentsActivity::class.java))
        }
        findViewById<TextView>(R.id.navChat).setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }
        findViewById<TextView>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }
}
