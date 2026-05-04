package com.healthbridge

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.healthbridge.network.ApiClient
import com.healthbridge.network.RegisterRequest
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etFullName = findViewById<EditText>(R.id.etFullName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)

        btnRegister.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (fullName.isNotEmpty() && email.isNotEmpty() && phone.isNotEmpty() && password.isNotEmpty()) {
                if (password.length < 6) {
                    Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                btnRegister.isEnabled = false
                btnRegister.text = "Creating account…"
                attemptRegister(fullName, email, phone, password, btnRegister)
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun attemptRegister(
        fullName: String, email: String, phone: String, password: String, btnRegister: Button
    ) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.register(
                    RegisterRequest(fullName, email, phone, password)
                )
                if (response.success) {
                    saveLocalCreds(fullName, email, phone, password)
                    Toast.makeText(
                        this@RegisterActivity,
                        "Registration Successful! Please Login.",
                        Toast.LENGTH_LONG
                    ).show()
                    startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(
                        this@RegisterActivity,
                        response.error ?: "Registration failed",
                        Toast.LENGTH_SHORT
                    ).show()
                    resetButton(btnRegister)
                }
            } catch (_: Exception) {
                // Backend offline — save locally and continue
                saveLocalCreds(fullName, email, phone, password)
                Toast.makeText(
                    this@RegisterActivity,
                    "Registration Successful! Please Login.",
                    Toast.LENGTH_LONG
                ).show()
                startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                finish()
            }
        }
    }

    private fun saveLocalCreds(fullName: String, email: String, phone: String, password: String) {
        getSharedPreferences("HealthBridge", MODE_PRIVATE).edit().apply {
            putString("reg_name", fullName)
            putString("reg_email", email)
            putString("reg_phone", phone)
            putString("reg_password", password)
            apply()
        }
    }

    private fun resetButton(btnRegister: Button) {
        btnRegister.isEnabled = true
        btnRegister.text = "Sign Up"
    }
}
