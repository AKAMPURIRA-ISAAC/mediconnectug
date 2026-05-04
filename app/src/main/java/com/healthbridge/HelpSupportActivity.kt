package com.healthbridge

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class HelpSupportActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help_support)
        
        val tvFAQ1 = findViewById<TextView>(R.id.tvFAQ1)
        val tvFAQ2 = findViewById<TextView>(R.id.tvFAQ2)
        val tvFAQ3 = findViewById<TextView>(R.id.tvFAQ3)
        val btnEmail = findViewById<Button>(R.id.btnEmail)
        val btnCall = findViewById<Button>(R.id.btnCall)
        
        tvFAQ1.setOnClickListener {
            Toast.makeText(this, "Go to Find Doctors → Select doctor → Book", Toast.LENGTH_SHORT).show()
        }
        
        tvFAQ2.setOnClickListener {
            Toast.makeText(this, "Go to My Appointments → Select appointment → Cancel", Toast.LENGTH_SHORT).show()
        }
        
        tvFAQ3.setOnClickListener {
            Toast.makeText(this, "Go to Emergency screen or call 999", Toast.LENGTH_SHORT).show()
        }
        
        btnEmail.setOnClickListener {
            Toast.makeText(this, "support@healthbridge.com", Toast.LENGTH_SHORT).show()
        }
        
        btnCall.setOnClickListener {
            Toast.makeText(this, "Call +256 700 123456", Toast.LENGTH_SHORT).show()
        }
    }
}
