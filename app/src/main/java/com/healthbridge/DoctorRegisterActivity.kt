package com.healthbridge

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.healthbridge.network.ApiClient
import com.healthbridge.network.DoctorRegisterRequest
import kotlinx.coroutines.launch

class DoctorRegisterActivity : AppCompatActivity() {

    private var passwordVisible = false
    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var spinnerSpecialty: Spinner
    private lateinit var etHospital: EditText
    private lateinit var etLicenseNumber: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_register)

        // Initialize views
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        spinnerSpecialty = findViewById(R.id.spinnerSpecialty)
        etHospital = findViewById(R.id.etHospital)
        etLicenseNumber = findViewById(R.id.etLicenseNumber)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)

        val tvTogglePassword = findViewById<TextView>(R.id.tvTogglePassword)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)
        val btnBack = findViewById<ImageView>(R.id.btnBack)

        // Setup specialty spinner
        setupSpecialtySpinner()

        // Password visibility toggle
        tvTogglePassword.setOnClickListener {
            passwordVisible = !passwordVisible
            if (passwordVisible) {
                etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                etConfirmPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                tvTogglePassword.text = "🙈"
            } else {
                etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                etConfirmPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                tvTogglePassword.text = "👁"
            }
            etPassword.setSelection(etPassword.text.length)
            etConfirmPassword.setSelection(etConfirmPassword.text.length)
        }

        btnBack.setOnClickListener {
            finish()
        }

        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnRegister.setOnClickListener {
            validateAndRegister()
        }
    }

    private fun setupSpecialtySpinner() {
        val specialties = arrayOf(
            "Select Specialty",
            "General Practice",
            "Cardiology",
            "Dermatology",
            "Pediatrics",
            "Obstetrics & Gynecology",
            "Orthopedics",
            "Neurology",
            "Psychiatry",
            "Ophthalmology",
            "ENT (Ear, Nose & Throat)",
            "Dentistry",
            "Surgery",
            "Internal Medicine",
            "Radiology",
            "Anesthesiology"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, specialties)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSpecialty.adapter = adapter
    }

    private fun validateAndRegister() {
        val name = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val specialty = spinnerSpecialty.selectedItem.toString()
        val hospital = etHospital.text.toString().trim()
        val licenseNumber = etLicenseNumber.text.toString().trim()
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        when {
            name.isEmpty() -> {
                etFullName.error = "Enter your full name"
                etFullName.requestFocus()
                return
            }
            email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                etEmail.error = "Enter a valid email"
                etEmail.requestFocus()
                return
            }
            phone.isEmpty() -> {
                etPhone.error = "Enter phone number"
                etPhone.requestFocus()
                return
            }
            specialty == "Select Specialty" -> {
                Toast.makeText(this, "Please select a specialty", Toast.LENGTH_SHORT).show()
                return
            }
            hospital.isEmpty() -> {
                etHospital.error = "Enter hospital name"
                etHospital.requestFocus()
                return
            }
            licenseNumber.isEmpty() -> {
                etLicenseNumber.error = "Enter license number"
                etLicenseNumber.requestFocus()
                return
            }
            password.length < 6 -> {
                etPassword.error = "Password must be at least 6 characters"
                etPassword.requestFocus()
                return
            }
            password != confirmPassword -> {
                etConfirmPassword.error = "Passwords don't match"
                etConfirmPassword.requestFocus()
                return
            }
        }

        // All validations passed, proceed with registration
        btnRegister.isEnabled = false
        btnRegister.text = "Registering..."

        attemptDoctorRegistration(name, email, phone, specialty, hospital, licenseNumber, password)
    }

    private fun attemptDoctorRegistration(
        name: String,
        email: String,
        phone: String,
        specialty: String,
        hospital: String,
        licenseNumber: String,
        password: String
    ) {
        lifecycleScope.launch {
            try {
                val request = DoctorRegisterRequest(
                    name = name,
                    email = email,
                    phone = phone,
                    password = password,
                    specialty = specialty,
                    hospital = hospital,
                    licenseNumber = licenseNumber
                )

                val response = ApiClient.instance.registerDoctor(request)

                if (response.success && response.token != null) {
                    // Save credentials
                    val prefs = getSharedPreferences("HealthBridge", MODE_PRIVATE)
                    prefs.edit().apply {
                        putString("authToken", response.token)
                        putString("userEmail", email)
                        putString("userName", response.user?.name)
                        putString("userType", "doctor")
                        putInt("doctorId", response.user?.doctorId ?: 0)
                        apply()
                    }

                    ApiClient.authToken = response.token

                    Toast.makeText(this@DoctorRegisterActivity,
                        "Registration successful! Welcome, Dr. ${response.user?.name}",
                        Toast.LENGTH_LONG).show()

                    // Navigate to Doctor Home
                    startActivity(Intent(this@DoctorRegisterActivity, DoctorHomeActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(
                        this@DoctorRegisterActivity,
                        response.error ?: "Registration failed",
                        Toast.LENGTH_LONG
                    ).show()
                    resetButton()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@DoctorRegisterActivity,
                    "Network error: ${e.message}. Please check your connection and try again.",
                    Toast.LENGTH_LONG
                ).show()
                resetButton()
            }
        }
    }

    private fun resetButton() {
        btnRegister.isEnabled = true
        btnRegister.text = "Register as Doctor"
    }
}

