package com.mkr.schedule.util.localization

/**
 * Intelligent translator for academic university disciplines and lesson types.
 * Provides Ukrainian to English translation for course schedules.
 */
object DisciplineTranslator {

    private val EXACT_DICTIONARY: Map<String, String> = mapOf(
        // Computer Science & Software Engineering
        "комп'ютерні мережі" to "Computer Networks",
        "комп’ютерні мережі" to "Computer Networks",
        "компьютерні мережі" to "Computer Networks",
        "операційні системи" to "Operating Systems",
        "бази даних" to "Database Systems",
        "організація баз даних" to "Database Management Systems",
        "організація баз даних та знань" to "Databases & Knowledge Systems",
        "алгоритми та структури даних" to "Algorithms & Data Structures",
        "алгоритми і структури даних" to "Algorithms & Data Structures",
        "асд" to "Algorithms & Data Structures",
        "об'єктно-орієнтоване програмування" to "Object-Oriented Programming",
        "об’єктно-орієнтоване програмування" to "Object-Oriented Programming",
        "ооп" to "Object-Oriented Programming",
        "основи програмування" to "Programming Fundamentals",
        "програмування" to "Programming",
        "прикладне програмування" to "Applied Programming",
        "інженерія програмного забезпечення" to "Software Engineering",
        "іпз" to "Software Engineering",
        "веб-технології" to "Web Technologies",
        "web-технології" to "Web Technologies",
        "веб-розробка" to "Web Development",
        "технології веб-розробки" to "Web Development Technologies",
        "архітектура комп'ютерів" to "Computer Architecture",
        "архітектура обчислювальних систем" to "Computer Architecture",
        "комп'ютерна архітектура" to "Computer Architecture",
        "безпека інформаційних систем" to "Information Systems Security",
        "кібербезпека" to "Cybersecurity",
        "основи кібербезпеки" to "Fundamentals of Cybersecurity",
        "інформаційна безпека" to "Information Security",
        "штучний інтелект" to "Artificial Intelligence",
        "основи штучного інтелекту" to "Artificial Intelligence Fundamentals",
        "машинне навчання" to "Machine Learning",
        "комп'ютерна графіка" to "Computer Graphics",
        "чисельні методи" to "Numerical Methods",
        "методи оптимізації" to "Optimization Methods",
        "теорія алгоритмів" to "Theory of Algorithms",
        "теорія інформації та кодування" to "Information & Coding Theory",
        "теорія інформації" to "Information Theory",
        "розподілені системи" to "Distributed Systems",
        "хмарні технології" to "Cloud Technologies",
        "хмарні обчислення" to "Cloud Computing",
        "мобільна розробка" to "Mobile App Development",
        "розробка мобільних застосунків" to "Mobile App Development",
        "розробка мобільних додатків" to "Mobile App Development",
        "тестування програмного забезпечення" to "Software Testing & QA",
        "тестування пз" to "Software Testing & QA",
        "контроль якості пз" to "Software Quality Assurance",
        "системний аналіз" to "Systems Analysis",
        "криптографія" to "Cryptography",
        "прикладна криптографія" to "Applied Cryptography",
        "інтернет речей" to "Internet of Things (IoT)",
        "iot" to "Internet of Things (IoT)",
        "аналіз великих даних" to "Big Data Analytics",
        "big data" to "Big Data Analytics",
        "devops технології" to "DevOps Practices & Tools",
        "мікропроцесорні системи" to "Microprocessor Systems",
        "автоматизація процесів" to "Process Automation",
        "проєктний практикум" to "Project Practicum",
        "курсовий проєкт" to "Course Project",
        "курсова робота" to "Term Paper / Project",

        // Mathematics & Physics
        "вища математика" to "Higher Mathematics",
        "математичний аналіз" to "Mathematical Analysis / Calculus",
        "мат. аналіз" to "Calculus",
        "лінійна алгебра та аналітична геометрія" to "Linear Algebra & Analytic Geometry",
        "лінійна алгебра" to "Linear Algebra",
        "аналітична геометрія" to "Analytic Geometry",
        "дискретна математика" to "Discrete Mathematics",
        "диференціальні рівняння" to "Differential Equations",
        "теорія ймовірностей та математична статистика" to "Probability & Mathematical Statistics",
        "теорія ймовірностей і математична статистика" to "Probability & Mathematical Statistics",
        "теорія ймовірностей" to "Probability Theory",
        "математична статистика" to "Mathematical Statistics",
        "тй та мс" to "Probability & Statistics",
        "тйтамс" to "Probability & Statistics",
        "фізика" to "Physics",
        "загальна фізика" to "General Physics",
        "прикладна фізика" to "Applied Physics",
        "електротехніка" to "Electrical Engineering",
        "електроніка" to "Electronics",
        "схемотехніка" to "Circuit Design & Electronics",

        // Languages & Humanities
        "іноземна мова" to "Foreign Language (English)",
        "іноземна мова за професійним спрямуванням" to "English for Specific Purposes",
        "англійська мова" to "English Language",
        "англійська мова за професійним спрямуванням" to "English for Specific Purposes",
        "ділова англійська мова" to "Business English",
        "українська мова" to "Ukrainian Language",
        "українська мова за професійним спрямуванням" to "Ukrainian for Professional Purposes",
        "ділова українська мова" to "Business Ukrainian",
        "історія україни" to "History of Ukraine",
        "історія української культури" to "History of Ukrainian Culture",
        "філософія" to "Philosophy",
        "соціологія" to "Sociology",
        "політологія" to "Political Science",
        "психологія" to "Psychology",
        "психологія спілкування" to "Psychology of Communication",
        "правознавство" to "Jurisprudence",
        "основи правознавства" to "Fundamentals of Law",
        "правові основи" to "Legal Foundations",
        "етика та естетика" to "Ethics & Aesthetics",
        "логіка" to "Logic",

        // Economics & Management
        "економіка" to "Economics",
        "основи економіки" to "Fundamentals of Economics",
        "економічна теорія" to "Economic Theory",
        "економіка підприємства" to "Enterprise Economics",
        "менеджмент" to "Management",
        "основи менеджменту" to "Fundamentals of Management",
        "маркетинг" to "Marketing",
        "бухгалтерський облік" to "Accounting",
        "статистика" to "Statistics",

        // General University Courses
        "фізичне виховання" to "Physical Education",
        "фізвиховання" to "Physical Education",
        "фізична культура" to "Physical Culture",
        "безпека життєдіяльності" to "Life Safety",
        "бжд" to "Life Safety",
        "основи охорони праці" to "Occupational Safety Fundamentals",
        "охорона праці" to "Occupational Safety",
        "цивільний захист" to "Civil Protection",
        "екологія" to "Ecology",
        "основи екології" to "Fundamentals of Ecology"
    )

    private val WORD_REPLACEMENTS: Map<String, String> = mapOf(
        "основи" to "Fundamentals of",
        "теорія" to "Theory of",
        "методи" to "Methods of",
        "системи" to "Systems",
        "систем" to "Systems",
        "технології" to "Technologies",
        "технологій" to "Technologies",
        "прикладна" to "Applied",
        "прикладне" to "Applied",
        "комп'ютерна" to "Computer",
        "комп'ютерні" to "Computer",
        "інформаційні" to "Information",
        "інформаційна" to "Information",
        "програмні" to "Software",
        "програмне" to "Software",
        "забезпечення" to "Engineering",
        "безпека" to "Security",
        "мережі" to "Networks",
        "даних" to "Data",
        "аналіз" to "Analysis",
        "моделювання" to "Modeling",
        "проектування" to "Design",
        "проєктування" to "Design",
        "розробка" to "Development",
        "керування" to "Management",
        "управління" to "Management"
    )

    /**
     * Translates discipline title if language is English, otherwise returns original.
     */
    fun translateDiscipline(title: String?, language: Language): String {
        if (title.isNullOrBlank()) return ""
        if (language != Language.ENGLISH) return title

        val cleaned = title.trim()
        val normalized = cleaned.lowercase()
            .replace(Regex("""\((лек|пр|лаб|сем|конс|зал|екз)\.?\)""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\[(лекція|практика|лабораторна)\]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+"""), " ")
            .trim()

        // 1. Direct exact match
        EXACT_DICTIONARY[normalized]?.let { return it }

        // 2. Check without trailing punctuation or roman numbers
        val stripped = normalized.replace(Regex("""\s+(i|ii|iii|iv|v|1|2|3|ч\.\s*\d+|сем\.\s*\d+)$""", RegexOption.IGNORE_CASE), "").trim()
        val suffix = normalized.substring(stripped.length).trim()
        EXACT_DICTIONARY[stripped]?.let {
            return if (suffix.isNotEmpty()) "$it $suffix".uppercaseSuffix() else it
        }

        // 3. Substring check
        for ((key, value) in EXACT_DICTIONARY) {
            if (normalized.startsWith(key)) {
                val remainder = normalized.removePrefix(key).trim()
                return if (remainder.isEmpty()) value else "$value ${remainder.uppercaseSuffix()}"
            }
        }

        // 4. Word-by-word academic translation if multiple matching keywords exist
        val words = normalized.split(" ")
        if (words.size in 2..5 && words.any { WORD_REPLACEMENTS.containsKey(it) }) {
            val translatedWords = words.map { word ->
                WORD_REPLACEMENTS[word] ?: word.replaceFirstChar { it.uppercase() }
            }
            return translatedWords.joinToString(" ")
        }

        return cleaned
    }

    private fun String.uppercaseSuffix(): String {
        return this.split(" ").joinToString(" ") { token ->
            if (token.matches(Regex("""^(i|ii|iii|iv|v)$""", RegexOption.IGNORE_CASE))) {
                token.uppercase()
            } else {
                token
            }
        }
    }
}
