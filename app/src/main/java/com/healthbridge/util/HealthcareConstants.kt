package com.healthbridge.util

/**
 * Hospital, Healthcare Facility and Pharmacy List for Uganda
 * Doctors MUST register with a valid account and select from this list or choose "Other"
 *
 * ALL LOCATIONS ARE IN UGANDA
 * ACCOUNT REGISTRATION IS MANDATORY FOR ALL DOCTORS
 */
object HealthcareConstants {

    val HOSPITALS = arrayOf(
        "Select Hospital or Healthcare Facility",

        // ═══════════════════════════════════════════════════════════════
        // TIER 4: National Referral Hospitals (Kampala)
        // ═══════════════════════════════════════════════════════════════
        "Mulago National Referral Hospital - Kampala",
        "Butabika National Mental Health Hospital - Kampala",

        // ═══════════════════════════════════════════════════════════════
        // TIER 3: Regional Referral Hospitals
        // ═══════════════════════════════════════════════════════════════
        "Mbarara Regional Referral Hospital - Mbarara",
        "Masaka Regional Referral Hospital - Masaka",
        "Soroti Regional Referral Hospital - Soroti",
        "Arua Regional Referral Hospital - Arua",
        "Mbale Regional Referral Hospital - Mbale",
        "Fort Portal Regional Referral Hospital - Fort Portal",
        "Lira Regional Referral Hospital - Lira",
        "Moroto Regional Referral Hospital - Moroto",
        "Kotido Regional Referral Hospital - Kotido",
        "Kasese Regional Referral Hospital - Kasese",
        "Kabale Regional Referral Hospital - Kabale",
        "Jinja Regional Referral Hospital - Jinja",

        // ═══════════════════════════════════════════════════════════════
        // TIER 2B: General Hospitals
        // ═══════════════════════════════════════════════════════════════
        "Tororo Hospital - Tororo",
        "Palisa Hospital - Pallisa",
        "Mukono Hospital - Mukono",
        "Wakiso Hospital - Wakiso",
        "Mubende Hospital - Mubende",

        // ═══════════════════════════════════════════════════════════════
        // PRIVATE HOSPITALS AND CLINICS (Kampala & Major Cities)
        // ═══════════════════════════════════════════════════════════════
        "International Hospital Kampala (IHK) - Kampala",
        "Nsambya Hospital - Kampala",
        "The Surgery Hospital - Kampala",
        "Case Hospital - Kampala",
        "Medical City - Kampala",
        "Kampala Heart Institute - Kampala",
        "Aga Khan Hospital - Kampala",
        "Nairobi Hospital Uganda (NHU) - Kampala",
        "Kiwoko Hospital - Nakasero, Kampala",
        "Nile Hospital - Kampala",
        "St. Mary's Hospital Lacor - Gulu",
        "Mbarara University Teaching Hospital - Mbarara",
        "Mulago Specialized Women and Neonatal Hospital - Kampala",
        "Kiruddu Referral Hospital - Kampala",

        // ═══════════════════════════════════════════════════════════════
        // PHARMACIES IN MAJOR CITIES (Uganda)
        // ═══════════════════════════════════════════════════════════════
        "Kampala Pharmacy - Kampala City Centre",
        "Protector Pharmacy - Old Kampala, Kampala",
        "Nairobi Pharmacy - Kampala",
        "Good Life Pharmacy - Ntinda, Kampala",
        "Alliance Pharmacy - Kikaya, Kampala",
        "Noah Pharmacy - Mukono",
        "City Pharmacy - Jinja City Centre",
        "Main Pharmacy - Masaka",
        "Central Pharmacy - Mbarara",
        "Unity Pharmacy - Fort Portal",
        "Hope Pharmacy - Gulu",
        "Crown Pharmacy - Arua",
        "Universal Pharmacy - Soroti",
        "Star Pharmacy - Mbale",

        // ═══════════════════════════════════════════════════════════════
        // PRIMARY HEALTH CARE CENTERS & CLINICS
        // ═══════════════════════════════════════════════════════════════
        "Health Centre IV - Kampala",
        "Health Centre III - Kampala",
        "Private Clinic - Kampala",
        "Community Health Clinic - Jinja",
        "Essential Health Clinic - Masaka",
        "Family Care Clinic - Mbarara",
        "Wellness Clinic - Fort Portal",
        "Community Care Centre - Gulu",
        "Primary Health Clinic - Arua",
        "Health Post - Soroti",

        // ═══════════════════════════════════════════════════════════════
        // CLINICS AND DIAGNOSTIC CENTERS
        // ═══════════════════════════════════════════════════════════════
        "Uganda Health Services Development Agency (UHSDA) - Kampala",
        "Health and Medical Services Laboratory - Kampala",
        "Genesis Diagnostic Centre - Kampala",
        "Path Health - Multiple Locations",
        "Medical Investigations Laboratory - Kampala",
        "Rainbow Diagnostics - Kampala",
        "Pioneer Diagnostic Centre - Jinja",

        // ═══════════════════════════════════════════════════════════════
        // OTHER - MUST SPECIFY LOCATION
        // ═══════════════════════════════════════════════════════════════
        "Other - Please Specify Location in Uganda"
    )

    fun isValidHospital(hospital: String): Boolean {
        return HOSPITALS.contains(hospital)
    }

    fun getHospitalCategory(hospital: String): String {
        return when {
            hospital.contains("National") -> "National Referral Hospital"
            hospital.contains("Regional") -> "Regional Referral Hospital"
            hospital.contains("Referral") -> "Referral Hospital"
            hospital.contains("Hospital") -> "Hospital"
            hospital.contains("Pharmacy") || hospital.contains("pharmacy") -> "Pharmacy"
            hospital.contains("Clinic") || hospital.contains("clinic") -> "Clinic"
            hospital.contains("Diagnostic") || hospital.contains("diagnostic") -> "Diagnostic Centre"
            hospital.contains("Health") -> "Healthcare Facility"
            hospital.contains("Other") -> "Other - Custom Location"
            else -> "Healthcare Facility"
        }
    }

    fun getLocationFromHospital(hospital: String): String {
        // Extract location from hospital string
        val parts = hospital.split(" - ")
        return if (parts.size > 1) parts[1] else "Uganda"
    }
}

