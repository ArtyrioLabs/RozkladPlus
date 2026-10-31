package com.mkr.schedule.data.repository

import com.mkr.schedule.data.api.MkrApiService
import com.mkr.schedule.data.local.PreferencesManager
import com.mkr.schedule.data.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

interface ScheduleRepository {
    suspend fun getStudentTimeTable(
        token: String,
        studentId: Int,
        dateStart: String,
        dateEnd: String
    ): Result<List<TimeTableDate>>
    
    suspend fun getGroupTimeTable(
        token: String,
        groupId: Int,
        dateStart: String,
        dateEnd: String
    ): Result<List<TimeTableDate>>
    
    suspend fun getCallSchedule(token: String): Result<List<CallSchedule>>
    
    suspend fun getStudentInfo(token: String): Result<StudentData>
}

@Singleton
class ScheduleRepositoryImpl @Inject constructor(
    private val apiService: MkrApiService,
    private val preferencesManager: PreferencesManager
) : ScheduleRepository {
    
    private val gson = Gson()
    private val cachePrefs by lazy { preferencesManager.getScheduleCachePrefs() }
    
    override suspend fun getStudentTimeTable(
        token: String,
        studentId: Int,
        dateStart: String,
        dateEnd: String
    ): Result<List<TimeTableDate>> {
        val queryKey = "STUDENT_${studentId}_${dateStart}_${dateEnd}"
        return try {
            val response = apiService.getStudentTimeTable(
                token,
                StudentTimeTableForm(studentId, dateStart, dateEnd)
            )
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                // Persist response into offline cache
                cachePrefs.edit().putString(queryKey, gson.toJson(data)).apply()
                Result.success(data)
            } else {
                throw Exception("Failed to get schedule: ${response.message()}")
            }
        } catch (e: Exception) {
            // Attempt retrieving from offline disk cache on failure
            val cachedJson = cachePrefs.getString(queryKey, null)
            if (!cachedJson.isNullOrEmpty()) {
                val type = object : TypeToken<List<TimeTableDate>>() {}.type
                val data: List<TimeTableDate> = gson.fromJson(cachedJson, type)
                Result.success(data)
            } else {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun getGroupTimeTable(
        token: String,
        groupId: Int,
        dateStart: String,
        dateEnd: String
    ): Result<List<TimeTableDate>> {
        val queryKey = "GROUP_${groupId}_${dateStart}_${dateEnd}"
        return try {
            val response = apiService.getGroupTimeTable(
                token,
                GroupTimeTableForm(groupId, dateStart, dateEnd)
            )
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                cachePrefs.edit().putString(queryKey, gson.toJson(data)).apply()
                Result.success(data)
            } else {
                throw Exception("Failed to get schedule: ${response.message()}")
            }
        } catch (e: Exception) {
            val cachedJson = cachePrefs.getString(queryKey, null)
            if (!cachedJson.isNullOrEmpty()) {
                val type = object : TypeToken<List<TimeTableDate>>() {}.type
                val data: List<TimeTableDate> = gson.fromJson(cachedJson, type)
                Result.success(data)
            } else {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun getCallSchedule(token: String): Result<List<CallSchedule>> {
        val queryKey = "CALL_SCHEDULE"
        return try {
            val response = apiService.getCallSchedule(token)
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                cachePrefs.edit().putString(queryKey, gson.toJson(data)).apply()
                Result.success(data)
            } else {
                throw Exception("Failed to get call schedule: ${response.message()}")
            }
        } catch (e: Exception) {
            val cachedJson = cachePrefs.getString(queryKey, null)
            if (!cachedJson.isNullOrEmpty()) {
                val type = object : TypeToken<List<CallSchedule>>() {}.type
                val data: List<CallSchedule> = gson.fromJson(cachedJson, type)
                Result.success(data)
            } else {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun getStudentInfo(token: String): Result<StudentData> {
        val queryKey = "STUDENT_INFO"
        return try {
            val response = apiService.getStudentInfo(token)
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                cachePrefs.edit().putString(queryKey, gson.toJson(data)).apply()
                Result.success(data)
            } else {
                throw Exception("Failed to get student info: ${response.message()}")
            }
        } catch (e: Exception) {
            val cachedJson = cachePrefs.getString(queryKey, null)
            if (!cachedJson.isNullOrEmpty()) {
                val data: StudentData = gson.fromJson(cachedJson, StudentData::class.java)
                Result.success(data)
            } else {
                Result.failure(e)
            }
        }
    }
}

