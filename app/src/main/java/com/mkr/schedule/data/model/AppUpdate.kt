package com.mkr.schedule.data.model

data class AppUpdate(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val changelog: String,
    val forceUpdate: Boolean = false
)
