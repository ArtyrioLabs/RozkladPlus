package com.mkr.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mkr.schedule.data.model.TimeTableDate
import com.mkr.schedule.data.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class ScheduleUiState(
    val isLoading: Boolean = false,
    val schedule: List<TimeTableDate> = emptyList(),
    val error: String? = null,
    val selectedDate: Date = Date()
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    fun loadStudentSchedule(token: String, studentId: Int, days: Int = 7) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val startDate = Calendar.getInstance().apply {
                time = _uiState.value.selectedDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            
            val endDate = Calendar.getInstance().apply {
                time = startDate.time
                add(Calendar.DAY_OF_YEAR, days)
            }
            
            scheduleRepository.getStudentTimeTable(
                token,
                studentId,
                dateFormat.format(startDate.time),
                dateFormat.format(endDate.time)
            )
                .onSuccess { schedule ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        schedule = schedule
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Помилка завантаження розкладу"
                    )
                }
        }
    }
    
    fun loadGroupSchedule(token: String, groupId: Int, days: Int = 7) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val startDate = Calendar.getInstance().apply {
                time = _uiState.value.selectedDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            
            val endDate = Calendar.getInstance().apply {
                time = startDate.time
                add(Calendar.DAY_OF_YEAR, days)
            }
            
            scheduleRepository.getGroupTimeTable(
                token,
                groupId,
                dateFormat.format(startDate.time),
                dateFormat.format(endDate.time)
            )
                .onSuccess { schedule ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        schedule = schedule
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Помилка завантаження розкладу"
                    )
                }
        }
    }
    
    fun setSelectedDate(date: Date) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

