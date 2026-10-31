package com.mkr.schedule.util

import java.text.SimpleDateFormat
import java.util.*

object LogCollector {
    private val logs = mutableListOf<String>()
    private val maxLogs = 500
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    
    fun log(tag: String, message: String) {
        val timestamp = synchronized(dateFormat) { dateFormat.format(Date()) }
        val logEntry = "[$timestamp] $tag: $message"
        
        synchronized(logs) {
            logs.add(logEntry)
            if (logs.size > maxLogs) {
                logs.removeAt(0)
            }
        }
        
        // Mirror entry to standard Android Log
        android.util.Log.d(tag, message)
    }
    
    fun getLogs(): List<String> {
        synchronized(logs) {
            return logs.toList()
        }
    }
    
    fun clear() {
        synchronized(logs) {
            logs.clear()
        }
    }
}
