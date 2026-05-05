package com.healthbridge.util

/**
 * Hospital, Healthcare Facility and Pharmacy List for Uganda
 * Mandatory for Doctor Registration
 */
object HealthcareConstants {

    val HOSPITALS = arrayOf(
        "Select Hospital, Clinic or Pharmacy",

        // TIER 4: National Referral
        "Mulago National Referral Hospital - Kampala",
        "Butabika National Mental Health Hospital - Kampala",
        "Mulago specialized Women and Neonatal Hospital - Kampala",

        // TIER 3: Regional Referral
        "Mbarara Regional Referral Hospital",
        "Masaka Regional Referral Hospital",
        "Soroti Regional Referral Hospital",
        "Arua Regional Referral Hospital",
        "Mbale Regional Referral Hospital",
        "Fort Portal Regional Referral Hospital",
        "Jinja Regional Referral Hospital",

        // PRIVATE HOSPITALS
        "International Hospital Kampala (IHK)",
        "Nsambya Hospital - Kampala",
        "Nakasero Hospital - Kampala",
        "The Surgery Hospital - Kampala",
        "Case Medical Centre - Kampala",
        "Aga Khan Health Services",
        "Medical City - Kampala",
        "Kiwoko Hospital",
        "St. Mary's Hospital Lacor - Gulu",

        // PHARMACIES
        "Abacus Pharmacy - Multiple Locations",
        "First Pharmacy - Kampala",
        "Vine Pharmacy - Multiple Locations",
        "Guardian Health Pharmacy - Multiple Locations",
        "Good Life Pharmacy - Multiple Locations",
        "Alliance Pharmacy - Kampala",

        // PRIMARY CARE
        "Health Centre IV",
        "Health Centre III",
        "Private Clinic",
        
        "Other - Please Specify Below"
    )

    fun getLocationFromHospital(hospital: String): String {
        return when {
            hospital.contains("Kampala") -> "Kampala"
            hospital.contains("Mbarara") -> "Mbarara"
            hospital.contains("Gulu") -> "Gulu"
            hospital.contains("Jinja") -> "Jinja"
            else -> "Uganda"
        }
    }
}
