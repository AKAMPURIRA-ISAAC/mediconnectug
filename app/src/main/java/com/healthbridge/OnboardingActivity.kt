package com.healthbridge

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class OnboardingActivity : AppCompatActivity() {
    
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var btnNext: Button
    private lateinit var btnSkip: TextView
    
    private val onboardingItems = listOf(
        OnboardingItem("🏥", "Find Quality Healthcare\nNear You", "Locate verified doctors, clinics, and pharmacies in Uganda with real patient reviews."),
        OnboardingItem("📅", "Book Appointments\nInstantly", "Schedule appointments with top doctors in Kampala, Mbarara, Gulu and beyond — in seconds."),
        OnboardingItem("🤖", "AI Health Assistant\n24/7", "Get instant symptom analysis, health tips, and medical guidance from our HealthBridge AI."),
        OnboardingItem("🚑", "Emergency Services\nat Your Fingertips", "One tap to call Uganda's ambulance (0800 100 066) and locate the nearest hospital.")
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        btnNext   = findViewById(R.id.btnNext)
        btnSkip   = findViewById(R.id.btnSkip)

        viewPager.adapter = OnboardingPagerAdapter(onboardingItems)
        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        // Update button text based on current page
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position == onboardingItems.size - 1) {
                    btnNext.text = "Get Started →"
                    btnSkip.text = ""
                } else {
                    btnNext.text = "Next →"
                    btnSkip.text = "Skip"
                }
            }
        })

        btnSkip.setOnClickListener { navigateToLogin() }

        btnNext.setOnClickListener {
            if (viewPager.currentItem < onboardingItems.size - 1) {
                viewPager.currentItem += 1
            } else {
                navigateToLogin()
            }
        }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
    
    data class OnboardingItem(val icon: String, val title: String, val description: String)
}
