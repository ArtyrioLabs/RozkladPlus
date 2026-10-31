package com.mkr.schedule.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mkr.schedule.util.LogCollector
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mkr_preferences")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val USERNAME_KEY = stringPreferencesKey("username")
        private val APP_KEY_KEY = stringPreferencesKey("app_key")
        private val CREDENTIALS_REVISION_KEY = longPreferencesKey("credentials_revision")
        private val SELECTED_UNIVERSITY_ID_KEY = stringPreferencesKey("selected_university_id")
        private val SELECTED_STUDENT_ID_KEY = stringPreferencesKey("selected_student_id")
        private val SELECTED_GROUP_ID_KEY = stringPreferencesKey("selected_group_id")
        private val SELECTED_STRUCTURE_ID_KEY = stringPreferencesKey("selected_structure_id")
        private val SELECTED_FACULTY_ID_KEY = stringPreferencesKey("selected_faculty_id")
        private val SELECTED_COURSE_KEY = stringPreferencesKey("selected_course")
        private val NOTIFICATIONS_ENABLED_KEY = stringPreferencesKey("notifications_enabled")
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private val DYNAMIC_COLORS_KEY = booleanPreferencesKey("dynamic_colors")
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val STUDENT_LEVEL_CHILL_MODE_KEY = booleanPreferencesKey("student_level_chill_mode")
        private val STUDENT_CARD_PHOTO_KEY = stringPreferencesKey("student_card_photo_path")
        private val STUDENT_CODE_TYPE_KEY = stringPreferencesKey("student_code_type")
        private val STUDENT_TOTAL_XP_KEY = intPreferencesKey("student_total_xp")
        private val STUDENT_MAX_COMPLETED_LESSONS_KEY = intPreferencesKey("student_max_completed_lessons")
    }

    val dynamicColors: Flow<Boolean> = context.dataStore.data.map { it[DYNAMIC_COLORS_KEY] ?: true }
    val themeMode: Flow<String> = context.dataStore.data.map { it[THEME_MODE_KEY] ?: "system" }

    suspend fun setDynamicColors(enabled: Boolean) {
        context.dataStore.edit { it[DYNAMIC_COLORS_KEY] = enabled }
    }

    suspend fun setThemeMode(mode: String) {
        require(mode in listOf("system", "light", "dark"))
        context.dataStore.edit { it[THEME_MODE_KEY] = mode }
    }

    val accessToken: Flow<String?> = context.dataStore.data.map { preferences ->
        readSecret(preferences, ACCESS_TOKEN_KEY)
    }

    val username: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USERNAME_KEY]
    }

    val appKey: Flow<String?> = context.dataStore.data.map { preferences ->
        readSecret(preferences, APP_KEY_KEY)
    }

    suspend fun saveAccessToken(token: String) {
        saveSecret(ACCESS_TOKEN_KEY, token)
    }

    suspend fun saveUsername(username: String) {
        context.dataStore.edit { preferences ->
            preferences[USERNAME_KEY] = username
        }
    }

    suspend fun saveAppKey(appKey: String) {
        saveSecret(APP_KEY_KEY, appKey)
    }

    private fun createEncryptedPrefsSafely(fileName: String): SharedPreferences {
        return try {
            val masterKey = androidx.security.crypto.MasterKey.Builder(context)
                .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
                .build()
            androidx.security.crypto.EncryptedSharedPreferences.create(
                context,
                fileName,
                masterKey,
                androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            LogCollector.log("PreferencesManager", "EncryptedSharedPreferences ($fileName) corrupted, resetting: ${e.message}")
            try {
                context.deleteSharedPreferences(fileName)
                val masterKey = androidx.security.crypto.MasterKey.Builder(context)
                    .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
                    .build()
                androidx.security.crypto.EncryptedSharedPreferences.create(
                    context,
                    fileName,
                    masterKey,
                    androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (fallbackEx: Exception) {
                LogCollector.log("PreferencesManager", "Fallback to private SharedPreferences for $fileName: ${fallbackEx.message}")
                context.getSharedPreferences("${fileName}_fallback", Context.MODE_PRIVATE)
            }
        }
    }

    private val credentials: SharedPreferences by lazy {
        createEncryptedPrefsSafely("credentials")
    }

    private suspend fun saveSecret(key: Preferences.Key<String>, value: String) {
        withContext(Dispatchers.IO) {
            val committed = credentials.edit().putString(key.name, value).commit()
            if (!committed) {
                LogCollector.log("PreferencesManager", "Warning: commit returned false for ${key.name}, using apply()")
                credentials.edit().putString(key.name, value).apply()
            }
            context.dataStore.edit {
                it.remove(key)
                it[CREDENTIALS_REVISION_KEY] = (it[CREDENTIALS_REVISION_KEY] ?: 0L) + 1L
            }
        }
    }

    private suspend fun readSecret(preferences: Preferences, key: Preferences.Key<String>): String? =
        withContext(Dispatchers.IO) {
            // Read revision so DataStore Flow re-evaluates when secret is updated
            @Suppress("UNUSED_VARIABLE")
            val rev = preferences[CREDENTIALS_REVISION_KEY]
            val legacy = preferences[key]
            if (legacy != null) {
                saveSecret(key, legacy)
                return@withContext legacy
            }
            try {
                credentials.getString(key.name, null)
            } catch (e: Exception) {
                LogCollector.log("PreferencesManager", "Error reading secret ${key.name}: ${e.message}")
                null
            }
        }

    suspend fun saveSelectedUniversity(universityId: String) {
        context.dataStore.edit { preferences ->
            if (universityId.isBlank()) {
                preferences.remove(SELECTED_UNIVERSITY_ID_KEY)
            } else {
                preferences[SELECTED_UNIVERSITY_ID_KEY] = universityId
            }
        }
    }

    suspend fun saveSelectedStudent(studentId: Int) {
        context.dataStore.edit { preferences ->
            if (studentId <= 0) {
                preferences.remove(SELECTED_STUDENT_ID_KEY)
            } else {
                preferences[SELECTED_STUDENT_ID_KEY] = studentId.toString()
            }
        }
    }

    suspend fun saveSelectedGroup(groupId: Int) {
        context.dataStore.edit { preferences ->
            if (groupId <= 0) {
                preferences.remove(SELECTED_GROUP_ID_KEY)
            } else {
                preferences[SELECTED_GROUP_ID_KEY] = groupId.toString()
            }
        }
    }

    val selectedUniversityId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[SELECTED_UNIVERSITY_ID_KEY]?.takeIf { it.isNotBlank() }
    }

    val selectedStudentId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[SELECTED_STUDENT_ID_KEY]?.takeIf { it.isNotBlank() && it != "0" }
    }

    val selectedGroupId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[SELECTED_GROUP_ID_KEY]?.takeIf { it.isNotBlank() && it != "0" }
    }

    suspend fun saveSelectedStructure(structureId: Int) {
        context.dataStore.edit { preferences ->
            if (structureId <= 0) {
                preferences.remove(SELECTED_STRUCTURE_ID_KEY)
            } else {
                preferences[SELECTED_STRUCTURE_ID_KEY] = structureId.toString()
            }
        }
    }

    suspend fun saveSelectedFaculty(facultyId: Int) {
        context.dataStore.edit { preferences ->
            if (facultyId <= 0) {
                preferences.remove(SELECTED_FACULTY_ID_KEY)
            } else {
                preferences[SELECTED_FACULTY_ID_KEY] = facultyId.toString()
            }
        }
    }

    suspend fun saveSelectedCourse(course: Int) {
        context.dataStore.edit { preferences ->
            if (course <= 0) {
                preferences.remove(SELECTED_COURSE_KEY)
            } else {
                preferences[SELECTED_COURSE_KEY] = course.toString()
            }
        }
    }

    val selectedStructureId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[SELECTED_STRUCTURE_ID_KEY]?.takeIf { it.isNotBlank() && it != "0" }
    }

    val selectedFacultyId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[SELECTED_FACULTY_ID_KEY]?.takeIf { it.isNotBlank() && it != "0" }
    }

    val selectedCourse: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[SELECTED_COURSE_KEY]?.takeIf { it.isNotBlank() && it != "0" }
    }

    private fun getConferenceLinkKey(groupId: Int, disciplineId: Int?): Preferences.Key<String> {
        val key = "conference_link_${groupId}_${disciplineId ?: "unknown"}"
        return stringPreferencesKey(key)
    }

    suspend fun saveConferenceLink(groupId: Int, disciplineId: Int?, link: String) {
        context.dataStore.edit { preferences ->
            if (link.isBlank()) {
                preferences.remove(getConferenceLinkKey(groupId, disciplineId))
            } else {
                preferences[getConferenceLinkKey(groupId, disciplineId)] = link.trim()
            }
        }
    }

    suspend fun getConferenceLink(groupId: Int, disciplineId: Int?): String? {
        return try {
            val key = getConferenceLinkKey(groupId, disciplineId)
            context.dataStore.data.map { preferences ->
                preferences[key]
            }.first()
        } catch (e: Exception) {
            null
        }
    }

    fun getConferenceLinkFlow(groupId: Int, disciplineId: Int?): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[getConferenceLinkKey(groupId, disciplineId)]
        }
    }

    suspend fun deleteConferenceLink(groupId: Int, disciplineId: Int?) {
        context.dataStore.edit { preferences ->
            preferences.remove(getConferenceLinkKey(groupId, disciplineId))
        }
    }

    val notificationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_ENABLED_KEY]?.toBoolean() ?: false
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED_KEY] = enabled.toString()
        }
    }

    suspend fun getNotificationsEnabled(): Boolean {
        return try {
            notificationsEnabledFlow.first()
        } catch (e: Exception) {
            false
        }
    }

    val isChillModeEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[STUDENT_LEVEL_CHILL_MODE_KEY] ?: false
    }

    suspend fun setChillModeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[STUDENT_LEVEL_CHILL_MODE_KEY] = enabled
        }
    }

    val studentCardPhotoPath: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[STUDENT_CARD_PHOTO_KEY]
    }

    suspend fun saveStudentCardPhotoPath(path: String?) {
        context.dataStore.edit { preferences ->
            if (path == null) preferences.remove(STUDENT_CARD_PHOTO_KEY)
            else preferences[STUDENT_CARD_PHOTO_KEY] = path
        }
    }

    val studentCodeType: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[STUDENT_CODE_TYPE_KEY] ?: "BARCODE"
    }

    suspend fun saveStudentCodeType(type: String) {
        context.dataStore.edit { preferences ->
            preferences[STUDENT_CODE_TYPE_KEY] = type
        }
    }

    val totalStudentXp: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[STUDENT_TOTAL_XP_KEY] ?: 0
    }

    val maxCompletedLessons: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[STUDENT_MAX_COMPLETED_LESSONS_KEY] ?: 0
    }

    suspend fun saveStudentLevelProgress(totalXp: Int, maxCompletedLessons: Int) {
        context.dataStore.edit { preferences ->
            preferences[STUDENT_TOTAL_XP_KEY] = totalXp.coerceAtLeast(0)
            preferences[STUDENT_MAX_COMPLETED_LESSONS_KEY] = maxCompletedLessons.coerceAtLeast(0)
        }
    }

    suspend fun getStudentId(): String {
        return try {
            selectedStudentId.first() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    val language: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LANGUAGE_KEY] ?: "uk"
    }

    suspend fun saveLanguage(languageCode: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = languageCode
        }
    }

    suspend fun clear() {
        withContext(Dispatchers.IO) {
            credentials.edit().clear().apply()
        }
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    private val _scheduleCachePrefs: SharedPreferences by lazy {
        context.getSharedPreferences("schedule_cache", Context.MODE_PRIVATE)
    }

    private val _notesPrefs: SharedPreferences by lazy {
        createEncryptedPrefsSafely("secret_notes")
    }

    fun getScheduleCachePrefs(): SharedPreferences {
        return _scheduleCachePrefs
    }

    fun getNotesPrefs(): SharedPreferences {
        return _notesPrefs
    }

    fun saveNote(lessonKey: String, note: String) {
        if (note.isBlank()) {
            getNotesPrefs().edit().remove(lessonKey).apply()
        } else {
            getNotesPrefs().edit().putString(lessonKey, note).apply()
        }
    }

    fun getNote(lessonKey: String): String? {
        return getNotesPrefs().getString(lessonKey, null)
    }
}
