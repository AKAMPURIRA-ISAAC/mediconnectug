package com.healthbridge

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.healthbridge.network.ApiClient
import com.healthbridge.network.LoginRequest
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private var passwordVisible = false
    private var isDoctorLogin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        val tvTogglePassword = findViewById<TextView>(R.id.tvTogglePassword)
        val btnFacebook = findViewById<LinearLayout>(R.id.btnFacebook)
        
        // Add toggle for doctor/patient login
        val tvToggleUserType = TextView(this).apply {
            text = "👨‍⚕️ Login as Doctor"
            textSize = 14f
            setTextColor(0xFF2196F3.toInt())
            setPadding(0, 20, 0, 20)
            gravity = android.view.Gravity.CENTER
            setOnClickListener {
                isDoctorLogin = !isDoctorLogin
                text = if (isDoctorLogin) "👤 Login as Patient" else "👨‍⚕️ Login as Doctor"
                btnLogin.text = if (isDoctorLogin) "Login as Doctor" else "Login"
            }
        }
        findViewById<LinearLayout>(R.id.rootLayout)?.addView(tvToggleUserType, 2) // Insert after title

        tvTogglePassword.setOnClickListener {
            passwordVisible = !passwordVisible
            if (passwordVisible) {
                etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                tvTogglePassword.text = "🙈"
            } else {
                etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                tvTogglePassword.text = "👁"
            }
            etPassword.setSelection(etPassword.text.length)
        }

        btnFacebook.setOnClickListener {
            Toast.makeText(this, "Facebook login coming soon", Toast.LENGTH_SHORT).show()
        }

        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                btnLogin.isEnabled = false
                btnLogin.text = "Logging in…"
                attemptLogin(email, password, btnLogin)
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        tvRegister.setOnClickListener {
            if (isDoctorLogin) {
                startActivity(Intent(this, DoctorRegisterActivity::class.java))
            } else {
                startActivity(Intent(this, RegisterActivity::class.java))
            }
        }
    }

    private fun attemptLogin(email: String, password: String, btnLogin: Button) {
        lifecycleScope.launch {
            try {
                val userType = if (isDoctorLogin) "doctor" else "patient"
                val response = ApiClient.instance.login(LoginRequest(email, password, userType))
                if (response.success) {
                    saveUserAndNavigate(email, response.user?.name, response.token, response.user?.userType)
                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        response.error ?: "Invalid credentials",
                        Toast.LENGTH_SHORT
                    ).show()
                    resetButton(btnLogin)
                }
            } catch (e: Exception) {
                // Backend unavailable — fall back to locally stored credentials
                fallbackLocalLogin(email, password, btnLogin)
            }
        }
    }

    private fun fallbackLocalLogin(email: String, password: String, btnLogin: Button) {
        val prefs = getSharedPreferences("HealthBridge", MODE_PRIVATE)
        val savedEmail = prefs.getString("reg_email", null)
        val savedPassword = prefs.getString("reg_password", null)
        val savedName = prefs.getString("reg_name", null)

        when {
            savedEmail != null && savedEmail == email && savedPassword == password -> {
                saveUserAndNavigate(email, savedName)
            }
            savedEmail == null -> {
                // No registration on record — accept any credentials (demo / first run)
                val name = email.substringBefore('@')
                    .replaceFirstChar { it.uppercase() }
                saveUserAndNavigate(email, name)
            }
            else -> {
                Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
                resetButton(btnLogin)
            }
        }
    }

    private fun saveUserAndNavigate(email: String, name: String?, token: String? = null, userType: String? = "patient") {
        val prefs = getSharedPreferences("HealthBridge", MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("isLoggedIn", true)
            putString("userEmail", email)
            putString("userType", userType ?: "patient")
            if (!name.isNullOrBlank()) putString("userName", name)
            if (!token.isNullOrBlank()) {
                putString("auth_token", token)
                ApiClient.authToken = token   // set in-memory for this session
            } else {
                // Fallback / offline login — always restore any previously saved token
                val existing = prefs.getString("auth_token", null)
                if (!existing.isNullOrBlank()) {
                    ApiClient.authToken = existing
                }
            }
            apply()
        }
        
        val successMessage = if (userType == "doctor") "Doctor Login Successful" else "Login Successful"
        Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show()
        
        // Navigate to appropriate home screen
        val targetActivity = if (userType == "doctor") DoctorHomeActivity::class.java else HomeActivity::class.java
        startActivity(Intent(this, targetActivity))
        finish()
    }

    private fun resetButton(btnLogin: Button) {
        btnLogin.isEnabled = true
        btnLogin.text = if (isDoctorLogin) "Login as Doctor" else "login"
    }
}
