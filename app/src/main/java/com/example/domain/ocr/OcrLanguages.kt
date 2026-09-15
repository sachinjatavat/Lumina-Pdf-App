package com.example.domain.ocr

enum class OcrEngineType {
    LATIN,
    DEVANAGARI,
    CHINESE,
    JAPANESE,
    KOREAN,
    UNIVERSAL_MULTI
}

data class SupportedOcrLanguage(
    val id: Int,
    val name: String,
    val nativeName: String,
    val script: String,
    val engineType: OcrEngineType,
    val flag: String = ""
)

object OcrLanguages {

    val AUTO_DETECT = SupportedOcrLanguage(
        id = 0,
        name = "Auto-Detect (All 70 Languages)",
        nativeName = "Auto Multi-Script",
        script = "Universal",
        engineType = OcrEngineType.UNIVERSAL_MULTI,
        flag = "🌐"
    )

    val ALL_70_LANGUAGES: List<SupportedOcrLanguage> = listOf(
        SupportedOcrLanguage(1, "English", "English", "Latin", OcrEngineType.LATIN, "🇬🇧"),
        SupportedOcrLanguage(2, "Mandarin Chinese", "中文 (普通话)", "Chinese (Hanzi)", OcrEngineType.CHINESE, "🇨🇳"),
        SupportedOcrLanguage(3, "Hindi", "हिन्दी", "Devanagari", OcrEngineType.DEVANAGARI, "🇮🇳"),
        SupportedOcrLanguage(4, "Spanish", "Español", "Latin", OcrEngineType.LATIN, "🇪🇸"),
        SupportedOcrLanguage(5, "Arabic", "العربية", "Arabic / Universal", OcrEngineType.UNIVERSAL_MULTI, "🇸🇦"),
        SupportedOcrLanguage(6, "French", "Français", "Latin", OcrEngineType.LATIN, "🇫🇷"),
        SupportedOcrLanguage(7, "Bengali", "বাংলা", "Bengali / Universal", OcrEngineType.UNIVERSAL_MULTI, "🇧🇩"),
        SupportedOcrLanguage(8, "Portuguese", "Português", "Latin", OcrEngineType.LATIN, "🇵🇹"),
        SupportedOcrLanguage(9, "Russian", "Русский", "Cyrillic / Universal", OcrEngineType.UNIVERSAL_MULTI, "🇷🇺"),
        SupportedOcrLanguage(10, "Indonesian", "Bahasa Indonesia", "Latin", OcrEngineType.LATIN, "🇮🇩"),
        SupportedOcrLanguage(11, "Urdu", "اردو", "Perso-Arabic / Universal", OcrEngineType.UNIVERSAL_MULTI, "🇵🇰"),
        SupportedOcrLanguage(12, "German", "Deutsch", "Latin", OcrEngineType.LATIN, "🇩🇪"),
        SupportedOcrLanguage(13, "Japanese", "日本語", "Kanji / Kana", OcrEngineType.JAPANESE, "🇯🇵"),
        SupportedOcrLanguage(14, "Nigerian Pidgin", "Naijá", "Latin", OcrEngineType.LATIN, "🇳🇬"),
        SupportedOcrLanguage(15, "Egyptian Arabic", "عربي مصري", "Arabic / Universal", OcrEngineType.UNIVERSAL_MULTI, "🇪🇬"),
        SupportedOcrLanguage(16, "Marathi", "मराठी", "Devanagari", OcrEngineType.DEVANAGARI, "🇮🇳"),
        SupportedOcrLanguage(17, "Vietnamese", "Tiếng Việt", "Latin", OcrEngineType.LATIN, "🇻🇳"),
        SupportedOcrLanguage(18, "Telugu", "తెలుగు", "Telugu / Universal", OcrEngineType.UNIVERSAL_MULTI, "🇮🇳"),
        SupportedOcrLanguage(19, "Hausa", "Harshen Hausa", "Latin", OcrEngineType.LATIN, "🇳🇬"),
        SupportedOcrLanguage(20, "Turkish", "Türkçe", "Latin", OcrEngineType.LATIN, "🇹🇷"),
        SupportedOcrLanguage(21, "Punjabi", "ਪੰਜਾਬੀ / پنجابی", "Gurmukhi / Universal", OcrEngineType.UNIVERSAL_MULTI, "🇮🇳"),
        SupportedOcrLanguage(22, "Swahili", "Kiswahili", "Latin", OcrEngineType.LATIN, "🇰🇪"),
        SupportedOcrLanguage(23, "Tagalog", "Wikang Tagalog", "Latin", OcrEngineType.LATIN, "🇵🇭"),
        SupportedOcrLanguage(24, "Tamil", "தமிழ்", "Tamil / Universal", OcrEngineType.UNIVERSAL_MULTI, "🇮🇳"),
        SupportedOcrLanguage(25, "Cantonese", "粵語", "Chinese (Traditional)", OcrEngineType.CHINESE, "🇭🇰"),
        SupportedOcrLanguage(26, "Wu Chinese", "吴语", "Chinese (Hanzi)", OcrEngineType.CHINESE, "🇨🇳"),
        SupportedOcrLanguage(27, "Persian", "فارسی", "Perso-Arabic / Universal", OcrEngineType.UNIVERSAL_MULTI, "🇮🇷"),
        SupportedOcrLanguage(28, "Korean", "한국어", "Hangul", OcrEngineType.KOREAN, "🇰🇷"),
        SupportedOcrLanguage(29, "Amharic", "አማርኛ", "Ge'ez / Universal", OcrEngineType.UNIVERSAL_MULTI, "🇪🇹"),
        SupportedOcrLanguage(30, "Thai", "ไทย", "Thai / Universal", OcrEngineType.UNIVERSAL_MULTI, "🇹🇭"),
        SupportedOcrLanguage(31, "Javanese", "Basa Jawa", "Latin", OcrEngineType.LATIN, "🇮🇩"),
        SupportedOcrLanguage(32, "Italian", "Italiano", "Latin", OcrEngineType.LATIN, "🇮🇹"),
        SupportedOcrLanguage(33, "Gujarati", "ગુજરાતી", "Gujarati / Universal", OcrEngineType.UNIVERSAL_MULTI, "🇮🇳"),
        SupportedOcrLanguage(34, "Kannada", "ಕನ್ನಡ", "Kannada / Universal", OcrEngineType.UNIVERSAL_MULTI, "🇮🇳"),
        SupportedOcrLanguage(35, "Levantine Arabic", "شامي", "Arabic / Universal", OcrEngineType.UNIVERSAL_MULTI, "🇱🇧"),
        SupportedOcrLanguage(36, "Sudanese Arabic", "لهجة سودانية", "Arabic / Universal", OcrEngineType.UNIVERSAL_MULTI, "🇸🇩"),
        SupportedOcrLanguage(37, "Yoruba", "Èdè Yorùbá", "Latin", OcrEngineType.LATIN, "🇳🇬"),
        SupportedOcrLanguage(38, "Bhojpuri", "भोजपुरी", "Devanagari", OcrEngineType.DEVANAGARI, "🇮🇳"),
        SupportedOcrLanguage(39, "Malay", "Bahasa Melayu", "Latin", OcrEngineType.LATIN, "🇲🇾"),
        SupportedOcrLanguage(40, "Odia", "ଓଡ଼ିଆ", "Odia / Universal", OcrEngineType.UNIVERSAL_MULTI, "🇮🇳"),
        SupportedOcrLanguage(41, "Burmese", "မြန်မာဘာသာ", "Burmese / Universal", OcrEngineType.UNIVERSAL_MULTI, "🇲🇲"),
        SupportedOcrLanguage(42, "Polish", "Polski", "Latin", OcrEngineType.LATIN, "🇵🇱"),
        SupportedOcrLanguage(43, "Ukrainian", "Українська", "Cyrillic / Universal", OcrEngineType.UNIVERSAL_MULTI, "🇺🇦"),
        SupportedOcrLanguage(44, "Malayalam", "മലയാളം", "Malayalam / Universal", OcrEngineType.UNIVERSAL_MULTI, "🇮🇳"),
        SupportedOcrLanguage(45, "Sindhi", "سنڌي", "Perso-Arabic / Universal", OcrEngineType.UNIVERSAL_MULTI, "🇵🇰"),
        SupportedOcrLanguage(46, "Sundanese", "Basa Sunda", "Latin", OcrEngineType.LATIN, "🇮🇩"),
        SupportedOcrLanguage(47, "Moroccan Arabic", "الدارجة المغربية", "Arabic / Universal", OcrEngineType.UNIVERSAL_MULTI, "🇲🇦"),
        SupportedOcrLanguage(48, "Igbo", "Asụsụ Igbo", "Latin", OcrEngineType.LATIN, "🇳🇬"),
        SupportedOcrLanguage(49, "Nepali", "नेपाली", "Devanagari", OcrEngineType.DEVANAGARI, "🇳🇵"),
        SupportedOcrLanguage(50, "Uzbek", "Oʻzbekcha", "Latin", OcrEngineType.LATIN, "🇺🇿"),
        SupportedOcrLanguage(51, "Romanian", "Română", "Latin", OcrEngineType.LATIN, "🇷🇴"),
        SupportedOcrLanguage(52, "Dutch", "Nederlands", "Latin", OcrEngineType.LATIN, "🇳🇱"),
        SupportedOcrLanguage(53, "Sa'idi Arabic", "صعيدي", "Arabic / Universal", OcrEngineType.UNIVERSAL_MULTI, "🇪🇬"),
        SupportedOcrLanguage(54, "Pashto", "پښتو", "Perso-Arabic / Universal", OcrEngineType.UNIVERSAL_MULTI, "🇦🇫"),
        SupportedOcrLanguage(55, "Saraiki", "سرائیکی", "Perso-Arabic / Universal", OcrEngineType.UNIVERSAL_MULTI, "🇵🇰"),
        SupportedOcrLanguage(56, "Xhosa", "isiXhosa", "Latin", OcrEngineType.LATIN, "🇿🇦"),
        SupportedOcrLanguage(57, "Malagasy", "Fiteny Malagasy", "Latin", OcrEngineType.LATIN, "🇲🇬"),
        SupportedOcrLanguage(58, "Zulu", "isiZulu", "Latin", OcrEngineType.LATIN, "🇿🇦"),
        SupportedOcrLanguage(59, "Somali", "Af-Soomaali", "Latin", OcrEngineType.LATIN, "🇸🇴"),
        SupportedOcrLanguage(60, "Khmer", "ភាសាខ្មែរ", "Khmer / Universal", OcrEngineType.UNIVERSAL_MULTI, "🇰🇭"),
        SupportedOcrLanguage(61, "Afrikaans", "Afrikaans", "Latin", OcrEngineType.LATIN, "🇿🇦"),
        SupportedOcrLanguage(62, "Sinhala", "සිංහල", "Sinhala / Universal", OcrEngineType.UNIVERSAL_MULTI, "🇱🇰"),
        SupportedOcrLanguage(63, "Czech", "Čeština", "Latin", OcrEngineType.LATIN, "🇨🇿"),
        SupportedOcrLanguage(64, "Hungarian", "Magyar", "Latin", OcrEngineType.LATIN, "🇭🇺"),
        SupportedOcrLanguage(65, "Greek", "Ελληνικά", "Greek / Universal", OcrEngineType.UNIVERSAL_MULTI, "🇬🇷"),
        SupportedOcrLanguage(66, "Swedish", "Svenska", "Latin", OcrEngineType.LATIN, "🇸🇪"),
        SupportedOcrLanguage(67, "Finnish", "Suomi", "Latin", OcrEngineType.LATIN, "🇫🇮"),
        SupportedOcrLanguage(68, "Danish", "Dansk", "Latin", OcrEngineType.LATIN, "🇩🇰"),
        SupportedOcrLanguage(69, "Hebrew", "עברית", "Hebrew / Universal", OcrEngineType.UNIVERSAL_MULTI, "🇮🇱"),
        SupportedOcrLanguage(70, "Azerbaijani", "Azərbaycan dili", "Latin", OcrEngineType.LATIN, "🇦🇿")
    )

    fun findById(id: Int): SupportedOcrLanguage {
        return ALL_70_LANGUAGES.find { it.id == id } ?: AUTO_DETECT
    }

    fun findByName(query: String): List<SupportedOcrLanguage> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return ALL_70_LANGUAGES
        return ALL_70_LANGUAGES.filter {
            it.name.lowercase().contains(q) ||
            it.nativeName.lowercase().contains(q) ||
            it.script.lowercase().contains(q) ||
            it.id.toString() == q
        }
    }
}
