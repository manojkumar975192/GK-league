package com.example.data

object RewardCatalog {
    val items: List<RewardCatalogItem> = listOf(
        // Gift Cards
        RewardCatalogItem(
            id = "gc_bookstore_5",
            title = "₹250 தமிழ் & கல்வி புத்தக வவுச்சர்",
            subtitle = "போட்டித் தேர்வு, இலக்கியம் & மின்-புத்தகங்களுக்கு பயன்படுத்தலாம்",
            category = RewardCategory.GIFT_CARD,
            pointsCost = 200L,
            badgeText = "பிரபலம்"
        ),
        RewardCatalogItem(
            id = "gc_student_store_10",
            title = "₹500 மாணவர் கிஃப்ட் கார்டு",
            subtitle = "எழுதுபொருட்கள் மற்றும் ஆன்லைன் பாடநெறிகளுக்கான பரிசு அட்டை",
            category = RewardCategory.GIFT_CARD,
            pointsCost = 400L,
            badgeText = "சிறந்த மதிப்பு"
        ),
        RewardCatalogItem(
            id = "gc_mega_scholar_25",
            title = "₹1500 சிறப்பு கல்வி கிஃப்ட் கார்டு",
            subtitle = "புத்தகங்கள், செயலிகள் & கல்வி உபகரணங்களுக்கான உயர்தர வவுச்சர்",
            category = RewardCategory.GIFT_CARD,
            pointsCost = 900L,
            badgeText = "சிறப்பு"
        ),

        // Tech Accessories
        RewardCatalogItem(
            id = "tech_usb_lamp",
            title = "LED ரீசார்ஜ் படிப்பு விளக்கு",
            subtitle = "இரவு நேரப் படிப்பிற்கான கண் பாதுகாப்பு LED விளக்கு",
            category = RewardCategory.TECH_ACCESSORY,
            pointsCost = 350L,
            badgeText = "படிப்பு கருவி"
        ),
        RewardCatalogItem(
            id = "tech_wireless_earbuds",
            title = "வயர்லெஸ் இயர்பட்ஸ் (Studio Lite)",
            subtitle = "ஆன்லைன் வகுப்புகள் மற்றும் கவனம் செலுத்தி படிக்க உதவும்",
            category = RewardCategory.TECH_ACCESSORY,
            pointsCost = 750L,
            badgeText = "சிறப்புத் தேர்வு"
        ),
        RewardCatalogItem(
            id = "tech_smart_stylus",
            title = "டேப்லெட் ஸ்மார்ட் ஸ்டைலஸ் பேனா",
            subtitle = "குறிப்புகள் மற்றும் வரைபடங்கள் வரைய துல்லியமான ஸ்டைலஸ்",
            category = RewardCategory.TECH_ACCESSORY,
            pointsCost = 1100L,
            badgeText = "முதன்மை"
        ),

        // Winner Certificates
        RewardCatalogItem(
            id = "cert_verified_scholar",
            title = "GK League தமிழ் அறிஞர் சான்றிதழ்",
            subtitle = "உங்கள் மாணவர் ID உடன் சரிபார்க்கப்பட்ட டிஜிட்டல் சான்றிதழ்",
            category = RewardCategory.WINNER_CERTIFICATE,
            pointsCost = 120L,
            badgeText = "உடனடி வழங்கல்"
        ),
        RewardCatalogItem(
            id = "cert_monthly_champion",
            title = "மாதாந்திர வெற்றியாளர் சான்றிதழ்",
            subtitle = "மாதத்தின் முதல் 3 இடங்களைப் பிடிக்கும் மாணவர்களுக்கான அதிகாரப்பூர்வ சான்றிதழ்",
            category = RewardCategory.WINNER_CERTIFICATE,
            pointsCost = 0L,
            badgeText = "மாத டாப் 3",
            requiresMonthlyTopRank = true
        )
    )

    val practiceCategories: List<String> = listOf(
        "தமிழ் இலக்கியம் & திருக்குறள்",
        "தமிழ்நாடு வரலாறு & பண்பாடு",
        "அறிவியல் & தொழில்நுட்பம்",
        "இந்தியா & உலக புவியியல்",
        "அரசியல் & நடப்பு நிகழ்வுகள்",
        "விண்வெளி & இயற்கை"
    )
}
