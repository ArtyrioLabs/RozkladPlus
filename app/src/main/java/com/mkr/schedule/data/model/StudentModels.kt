package com.mkr.schedule.data.model

import com.google.gson.annotations.SerializedName

data class StudentData(
    val id: Int,
    @SerializedName("firstName") val firstName: String?,
    @SerializedName("secondName") val secondName: String?,
    @SerializedName("lastName") val lastName: String?,
    val birthday: String?,
    @SerializedName("facultyId") val facultyId: Int?,
    @SerializedName("facultyName") val facultyName: String?,
    @SerializedName("groupId") val groupId: Int?,
    @SerializedName("groupName") val groupName: String?,
    @SerializedName("specialityId") val specialityId: Int?,
    @SerializedName("specialityName") val specialityName: String?,
    val course: Int?,
    @SerializedName("educationFormId") val educationFormId: Int?,
    @SerializedName("educationFormName") val educationFormName: String?,
    @SerializedName("educationLevelId") val educationLevelId: Int?,
    @SerializedName("educationLevelName") val educationLevelName: String?
)

data class Semester(
    val id: Int,
    val year: Int,
    val semester: Int,
    val course: Int,
    val number: Int
)

data class SelectYearSemesterForm(
    val year: Int,
    val semester: Int
)

// Student entity inheriting person name fields (id, firstName, secondName, lastName)
data class Student(
    val id: Int,
    @SerializedName("firstName") val firstName: String?,
    @SerializedName("secondName") val secondName: String?,
    @SerializedName("lastName") val lastName: String?
) {
    private fun transliterateToCyrillic(text: String): String {
        if (text.isEmpty()) return text
        
        // Check if text already contains Cyrillic characters
        val hasCyrillic = text.any { it in 'А'..'Я' || it in 'а'..'я' || it in 'Є'..'Я' || it == 'І' || it == 'і' || it == 'Ї' || it == 'ї' || it == 'Є' || it == 'є' || it == 'Ґ' || it == 'ґ' }
        if (hasCyrillic) return text
        
        // Transliteration dictionary: longer combinations first, then single letters
        val replacements = listOf(
            // 4-letter combinations
            "shch" to "щ", "Shch" to "Щ", "SHCH" to "Щ",
            // 2-letter combinations
            "zh" to "ж", "Zh" to "Ж", "ZH" to "Ж",
            "sh" to "ш", "Sh" to "Ш", "SH" to "Ш",
            "ch" to "ч", "Ch" to "Ч", "CH" to "Ч",
            "ts" to "ц", "Ts" to "Ц", "TS" to "Ц",
            "kh" to "х", "Kh" to "Х", "KH" to "Х",
            "ya" to "я", "Ya" to "Я", "YA" to "Я",
            "ye" to "є", "Ye" to "Є", "YE" to "Є",
            "yo" to "йо", "Yo" to "Йо", "YO" to "Йо",
            "yu" to "ю", "Yu" to "Ю", "YU" to "Ю",
            "yi" to "ї", "Yi" to "Ї", "YI" to "Ї",
            "ia" to "я", "Ia" to "Я", "IA" to "Я",
            "ie" to "є", "Ie" to "Є", "IE" to "Є",
            "iu" to "ю", "Iu" to "Ю", "IU" to "Ю",
            "ii" to "ї", "Ii" to "Ї", "II" to "Ї",
            // Single letters
            "a" to "а", "A" to "А",
            "b" to "б", "B" to "Б",
            "v" to "в", "V" to "В",
            "g" to "г", "G" to "Г",
            "d" to "д", "D" to "Д",
            "e" to "е", "E" to "Е",
            "z" to "з", "Z" to "З",
            "i" to "і", "I" to "І",
            "y" to "и", "Y" to "И",
            "k" to "к", "K" to "К",
            "l" to "л", "L" to "Л",
            "m" to "м", "M" to "М",
            "n" to "н", "N" to "Н",
            "o" to "о", "O" to "О",
            "p" to "п", "P" to "П",
            "r" to "р", "R" to "Р",
            "s" to "с", "S" to "С",
            "t" to "т", "T" to "Т",
            "u" to "у", "U" to "У",
            "f" to "ф", "F" to "Ф",
            "h" to "г", "H" to "Г",
            "c" to "к", "C" to "К",
            "q" to "к", "Q" to "К",
            "w" to "в", "W" to "В",
            "x" to "кс", "X" to "Кс",
            "j" to "й", "J" to "Й"
        )
        
        var result = StringBuilder(text)
        var i = 0
        
        while (i < result.length) {
            var replaced = false
            // Match against longest replacement prefixes
            for ((pattern, replacement) in replacements) {
                if (i + pattern.length <= result.length) {
                    val substring = result.substring(i, i + pattern.length)
                    if (substring == pattern) {
                        result.replace(i, i + pattern.length, replacement)
                        i += replacement.length
                        replaced = true
                        break
                    }
                }
            }
            if (!replaced) {
                i++
            }
        }
        
        return result.toString()
    }
    
    private fun transliterateName(name: String?): String? {
        return name?.trim()?.takeIf { it.isNotEmpty() }?.let { 
            transliterateToCyrillic(it) 
        }
    }
    
    val transliteratedFirstName: String?
        get() = transliterateName(firstName)
    
    val transliteratedSecondName: String?
        get() = transliterateName(secondName)
    
    val transliteratedLastName: String?
        get() = transliterateName(lastName)
    
    val fullName: String
        get() = listOfNotNull(lastName, firstName, secondName).joinToString(" ")
    
    val shortName: String
        get() {
            val parts = mutableListOf<String>()
            
            // Last name (transliterated if needed)
            val transLastName = transliteratedLastName ?: lastName?.trim()
            transLastName?.takeIf { it.isNotEmpty() }?.let { parts.add(it) }
            
            // First name initial only
            val transFirstName = transliteratedFirstName ?: firstName?.trim()
            transFirstName?.takeIf { it.isNotEmpty() }?.firstOrNull()?.let { 
                parts.add("$it.") 
            }
            
            return if (parts.isNotEmpty()) {
                parts.joinToString(" ")
            } else {
                fullName
            }
        }
}

data class StudentsByGroupFilterForm(
    @SerializedName("groupId") val groupId: Int
)

