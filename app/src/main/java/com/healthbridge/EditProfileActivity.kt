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

class EditProfileActivity : AppCompatActivity() {

    private lateinit var ivProfile: ImageView
    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etBio: EditText
    private lateinit var etPhone: EditText
    private lateinit var etBloodType: EditText

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = result.data?.data
            ivProfile.setImageURI(imageUri)
            Toast.makeText(this, "Photo updated", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        ivProfile = findViewById(R.id.ivProfile)
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etBio = findViewById(R.id.etBio)
        etPhone = findViewById(R.id.etPhone)
        etBloodType = findViewById(R.id.etBloodType)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnSave = findViewById<TextView>(R.id.btnSave)
        val btnChangePhoto = findViewById<CardView>(R.id.btnChangePhoto)

        // Load current data
        val prefs = getSharedPreferences("HealthBridge", MODE_PRIVATE)
        etFullName.setText(prefs.getString("userName", ""))
        etEmail.setText(prefs.getString("userEmail", ""))
        etPhone.setText(prefs.getString("userPhone", ""))
        etBloodType.setText(prefs.getString("bloodType", ""))

        btnBack.setOnClickListener { finish() }

        btnSave.setOnClickListener {
            val name = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val bloodType = etBloodType.text.toString().trim().uppercase()

            if (name.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Name and Email are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save locally first
            prefs.edit().apply {
                putString("userName", name)
                putString("userEmail", email)
                putString("userPhone", phone)
                putString("bloodType", bloodType)
                apply()
            }

            // Try to sync with API
            lifecycleScope.launch {
                try {
                    val body = mapOf(
                        "name" to name,
                        "email" to email,
                        "phone" to phone,
                        "bloodType" to bloodType
                    )
                    val response = ApiClient.instance.updateProfile(body)
                    if (response.success) {
                        Toast.makeText(this@EditProfileActivity, "✅ Profile Updated", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@EditProfileActivity, "⚠️ Saved locally (backend offline)", Toast.LENGTH_SHORT).show()
                    }
                } catch (_: Exception) {
                    Toast.makeText(this@EditProfileActivity, "✅ Saved locally (backend offline)", Toast.LENGTH_SHORT).show()
                }
                finish()
            }
        }

        btnChangePhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImage.launch(intent)
        }
    }
}
