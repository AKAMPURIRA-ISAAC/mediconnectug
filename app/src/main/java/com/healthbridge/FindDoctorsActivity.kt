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

    private var allDoctors: List<DoctorItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_doctors)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        recyclerView = findViewById(R.id.recyclerViewDoctors)
        searchEditText = findViewById(R.id.searchEditText)

        btnBack.setOnClickListener { finish() }

        setupRecyclerView()
        setupSearch()
        setupBottomNav()
        loadDoctors()
    }

    private fun setupRecyclerView() {
        adapter = DoctorAdapter(mutableListOf()) { doctor ->
            val intent = Intent(this, DoctorProfileActivity::class.java).apply {
                putExtra("DOCTOR_NAME", doctor.name)
                putExtra("DOCTOR_SPECIALTY", doctor.specialty)
                putExtra("DOCTOR_RATING", doctor.rating)
                putExtra("DOCTOR_FEE", doctor.fee)
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
                    allDoctors = doctors.map { d ->
                        DoctorItem(
                            id = d.id.toString(),
                            name = d.name,
                            specialty = d.specialty,
                            rating = d.rating,
                            reviewCount = d.reviewCount,
                            distance = "Nearby",
                            fee = d.consultationFee,
                            isOnline = true
                        )
                    }
                    adapter.updateData(allDoctors)
                }
                result.onFailure {
                    showSampleDoctors()
                    Toast.makeText(
                        this@FindDoctorsActivity,
                        "Backend offline — showing cached doctors",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (_: Exception) {
                showSampleDoctors()
                Toast.makeText(
                    this@FindDoctorsActivity,
                    "Backend offline — showing sample doctors",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showSampleDoctors() {
        allDoctors = listOf(
            DoctorItem("1", "Dr. Sarah Nakigozi",   "Pediatrician",         4.9, 120, "Mulago Hosp, Kampala",     45000, true),
            DoctorItem("2", "Dr. Moses Ssali",       "Cardiologist",         4.8, 203, "Nakasero Hosp, Kampala",   60000, true),
            DoctorItem("3", "Dr. Grace Namukwaya",   "Neurologist",          4.7, 156, "Mulago Hosp, Kampala",     70000, false),
            DoctorItem("4", "Dr. Patrick Odeke",     "General Practitioner", 4.6,  89, "IHK, Kampala",             35000, true),
            DoctorItem("5", "Dr. Amina Nakato",      "Dermatologist",        4.8, 112, "Aga Khan, Kampala",        55000, true),
            DoctorItem("6", "Dr. James Mwesigwa",    "Orthopedic Surgeon",   4.7,  98, "Case Medical, Kampala",    80000, true),
            DoctorItem("7", "Dr. Phoebe Asiimwe",    "Gynecologist",         4.9, 145, "Lubaga Hosp, Kampala",     65000, true),
            DoctorItem("8", "Dr. Robert Kizito",     "Ophthalmologist",      4.6,  76, "Mengo Hospital, Kampala",  50000, false),
            DoctorItem("9", "Dr. Faith Tumwesigye",  "Psychiatrist",         4.5,  63, "Butabika Hosp, Kampala",   45000, true),
            DoctorItem("10","Dr. David Mugisha",     "General Surgeon",      4.7, 134, "Nsambya Hosp, Kampala",    75000, true)
        )
        adapter.updateData(allDoctors)
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.lowercase()?.trim() ?: ""
                val filtered = if (query.isEmpty()) {
                    allDoctors
                } else {
                    allDoctors.filter {
                        it.name.lowercase().contains(query) ||
                        it.specialty.lowercase().contains(query)
                    }
                }
                adapter.updateData(filtered)
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

