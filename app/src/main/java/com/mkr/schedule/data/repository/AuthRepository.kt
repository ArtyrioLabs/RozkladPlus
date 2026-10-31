package com.mkr.schedule.data.repository

import com.mkr.schedule.data.api.MkrApiService
import com.mkr.schedule.data.local.PreferencesManager
import com.mkr.schedule.data.model.AccessToken
import com.mkr.schedule.data.model.LoginForm
import com.mkr.schedule.data.model.User
import javax.inject.Inject
import javax.inject.Singleton

interface AuthRepository {
    suspend fun login(username: String, password: String, appKey: String): Result<AccessToken>
    suspend fun getUserInfo(token: String): Result<User>
    suspend fun logout()
    fun getAccessToken(): kotlinx.coroutines.flow.Flow<String?>
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: MkrApiService,
    private val preferencesManager: PreferencesManager
) : AuthRepository {
    override suspend fun login(username: String, password: String, appKey: String): Result<AccessToken> {
        return try {
            val response = apiService.login(LoginForm(username, password, appKey))
            if (response.isSuccessful && response.body() != null) {
                val token = response.body()!!
                preferencesManager.saveAccessToken("Bearer ${token.accessToken}")
                preferencesManager.saveUsername(username)
                preferencesManager.saveAppKey(appKey)
                Result.success(token)
            } else {
                Result.failure(Exception("Login failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getUserInfo(token: String): Result<User> {
        return try {
            val response = apiService.getUserInfo(token)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get user info: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun logout() {
        preferencesManager.clear()
    }
    
    override fun getAccessToken(): kotlinx.coroutines.flow.Flow<String?> {
        return preferencesManager.accessToken
    }
}

