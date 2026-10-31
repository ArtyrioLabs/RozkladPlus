package com.mkr.schedule.data.model

import com.google.gson.annotations.SerializedName

data class LoginForm(
    val username: String,
    val password: String,
    @SerializedName("app_key") val appKey: String
)

data class AccessToken(
    @SerializedName("access_token") val accessToken: String
)

data class User(
    val id: Int,
    val username: String,
    val email: String,
    val blocked: Int,
    val type: Int,
    @SerializedName("currentId") val currentId: Int,
    val model: Person?,
    val accounts: List<Account>?
)

data class Person(
    val id: Int,
    @SerializedName("firstName") val firstName: String?,
    @SerializedName("secondName") val secondName: String?,
    @SerializedName("lastName") val lastName: String?
)

data class Account(
    val int: Int,
    val type: Int,
    val title: String
)

