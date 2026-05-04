package com.healthbridge

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.healthbridge.network.ApiClient
import kotlinx.coroutines.launch

class EditDoctorProfileActivity : AppCompatActivity() {

    private lateinit var ivDoctorAvatar: ImageView
    private lateinit var etDoctorName: EditText
    private lateinit var etSpecialty: EditText
    private lateinit var etExperience: EditText
    private lateinit var etPhone: EditText
    private lateinit var etHospital: EditText
    private lateinit var etFee: EditText
    private lateinit var etBio: EditText

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = result.data?.data
            ivDoctorAvatar.setImageURI(imageUri)
            Toast.makeText(this, "Photo updated", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_doctor_profile)

        ivDoctorAvatar = findViewById(R.id.ivDoctorAvatar)
        etDoctorName   = findViewById(R.id.etDoctorName)
        etSpecialty    = findViewById(R.id.etSpecialty)
        etExperience   = findViewById(R.id.etExperience)
        etPhone        = findViewById(R.id.etPhone)
        etHospital     = findViewById(R.id.etHospital)
        etFee          = findViewById(R.id.etFee)
        etBio          = findViewById(R.id.etBio)

        val btnBack        = findViewById<ImageView>(R.id.btnBack)
        val btnSave        = findViewById<TextView>(R.id.btnSave)
        val btnChangePhoto = findViewById<CardView>(R.id.btnChangePhoto)

        // Pre-fill from intent extras (existing data passed from DoctorProfileActivity)
        etDoctorName.setText(intent.getStringExtra("DOCTOR_NAME") ?: "")
        etSpecialty.setText(intent.getStringExtra("DOCTOR_SPECIALTY") ?: "")
        etPhone.setText(intent.getStringExtra("DOCTOR_PHONE") ?: "")
        val fee = intent.getIntExtra("DOCTOR_FEE", 0)
        if (fee > 0) etFee.setText(fee.toString())

        // Also load from prefs for fields not in intent
        val prefs = getSharedPreferences("HealthBridge", MODE_PRIVATE)
        etExperience.setText(prefs.getString("doctorExperience", ""))
        etHospital.setText(prefs.getString("doctorHospital", ""))
        etBio.setText(prefs.getString("doctorBio", ""))

        btnBack.setOnClickListener { finish() }

        btnChangePhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImage.launch(intent)
        }

        btnSave.setOnClickListener {
            val name       = etDoctorName.text.toString().trim()
            val specialty  = etSpecialty.text.toString().trim()
            val experience = etExperience.text.toString().trim()
            val phone      = etPhone.text.toString().trim()
            val hospital   = etHospital.text.toString().trim()
            val fee        = etFee.text.toString().trim()
            val bio        = etBio.text.toString().trim()

            if (name.isEmpty() || specialty.isEmpty()) {
                Toast.makeText(this, "Name and Specialty are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save locally
            prefs.edit().apply {
                putString("doctorName",       name)
                putString("doctorSpecialty",  specialty)
                putString("doctorExperience", experience)
                putString("doctorPhone",      phone)
                putString("doctorHospital",   hospital)
                putString("doctorFee",        fee)
                putString("doctorBio",        bio)
                apply()
            }

            // Try to sync with API
            lifecycleScope.launch {
                try {
                    val body = mapOf(
                        "name"       to name,
                        "specialty"  to specialty,
                        "experience" to experience,
                        "phone"      to phone,
                        "hospital"   to hospital,
                        "fee"        to fee,
                        "bio"        to bio
                    )
                    ApiClient.instance.updateProfile(body)
                    Toast.makeText(this@EditDoctorProfileActivity, "✅ Profile Updated", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {
                    Toast.makeText(this@EditDoctorProfileActivity, "✅ Saved locally (backend offline)", Toast.LENGTH_SHORT).show()
                }
                finish()
            }
        }
    }
}

