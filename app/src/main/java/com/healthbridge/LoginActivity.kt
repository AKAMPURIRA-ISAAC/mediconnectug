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
import com.healthbridge.util.SessionManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private var passwordVisible = false
    private var isDoctorLogin = false
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        sessionManager = SessionManager(this)

        // Check if redirected due to session timeout
        val sessionTimedOut = intent.getBooleanExtra("session_timeout", false)
        if (sessionTimedOut) {
            Toast.makeText(
                this,
                "⏱️ Your session has expired (15 minutes of inactivity). Please login again.",
                Toast.LENGTH_LONG
            ).show()
        }

        // Check if user is already logged in with valid session
        if (sessionManager.isLoggedIn() && sessionManager.isSessionValid()) {
            navigateToHome()
            return
        }

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        val tvTogglePassword = findViewById<TextView>(R.id.tvTogglePassword)
        
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
                    saveUserAndNavigate(
                        email,
                        response.user?.name,
                        response.token,
                        response.user?.userType,
                        response.user?.id,
                        response.user?.doctorId
                    )
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
                // No registration on record — MUST register first
                Toast.makeText(
                    this,
                    "❌ You must register first. Registration is required to use this app.",
                    Toast.LENGTH_LONG
                ).show()
                resetButton(btnLogin)
            }
            else -> {
                Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
                resetButton(btnLogin)
            }
        }
    }

    private fun saveUserAndNavigate(
        email: String,
        name: String?,
        token: String? = null,
        userType: String? = "patient",
        userId: Int? = null,
        doctorId: Int? = null
    ) {
        // Save to SessionManager (handles all session-related storage)
        sessionManager.saveUser(
            email = email,
            name = name ?: email.substringBefore('@'),
            token = token ?: getSharedPreferences("HealthBridge", MODE_PRIVATE).getString("auth_token", "") ?: "",
            userType = userType ?: "patient",
            userId = userId ?: -1,
            doctorId = doctorId
        )

        // Also save to SharedPreferences for backward compatibility
        val prefs = getSharedPreferences("HealthBridge", MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("isLoggedIn", true)
            putString("userEmail", email)
            putString("userType", userType ?: "patient")
            if (!name.isNullOrBlank()) putString("userName", name)
            if (!token.isNullOrBlank()) {
                putString("auth_token", token)
                ApiClient.authToken = token
            }
            apply()
        }
        
        val successMessage = if (userType == "doctor") "Doctor Login Successful" else "Login Successful"
        Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show()
        
        navigateToHome()
    }

    private fun navigateToHome() {
        val userType = sessionManager.getUserType()
        val targetActivity = if (userType == "doctor") DoctorHomeActivity::class.java else HomeActivity::class.java
        startActivity(Intent(this, targetActivity))
        finish()
    }

    private fun resetButton(btnLogin: Button) {
        btnLogin.isEnabled = true
        btnLogin.text = if (isDoctorLogin) "Login as Doctor" else "login"
    }
}
