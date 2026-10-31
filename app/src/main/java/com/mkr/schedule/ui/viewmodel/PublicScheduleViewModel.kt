package com.mkr.schedule.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mkr.schedule.data.api.MkrApiService
import com.mkr.schedule.data.local.PreferencesManager
import com.mkr.schedule.data.model.*
import com.mkr.schedule.notification.ExactAlarmScheduler
import com.mkr.schedule.util.LogCollector
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class PublicScheduleUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val universities: List<University> = University.MKR_UNIVERSITIES,
    val selectedUniversity: University? = null,
    val structures: List<Structure> = emptyList(),
    val selectedStructure: Structure? = null,
    val faculties: List<Faculty> = emptyList(),
    val selectedFaculty: Faculty? = null,
    val courses: List<Course> = emptyList(),
    val selectedCourse: Course? = null,
    val groups: List<Group> = emptyList(),
    val selectedGroup: Group? = null,
    val students: List<Student> = emptyList(),
    val selectedStudent: Student? = null,
    val showSaveStudentDialog: Boolean = false,
    val scheduleType: ScheduleType = ScheduleType.GROUP,
    val schedule: List<TimeTableDate> = emptyList(),
    val callSchedule: List<CallSchedule> = emptyList(),
    val currentWeekOffset: Int = 0,
    val isLoadingMore: Boolean = false
)

enum class ScheduleType {
    GROUP,
    STUDENT
}

@HiltViewModel
class PublicScheduleViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: MkrApiService,
    private val preferencesManager: PreferencesManager,
    private val okHttpClient: OkHttpClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(PublicScheduleUiState())
    val uiState: StateFlow<PublicScheduleUiState> = _uiState.asStateFlow()

    private val _notesMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val notesMap: StateFlow<Map<String, String>> = _notesMap.asStateFlow()

    private val _isChillModeEnabled = MutableStateFlow(false)
    val isChillModeEnabled: StateFlow<Boolean> = _isChillModeEnabled

    private val _studentCardPhotoPath = MutableStateFlow<String?>(null)
    val studentCardPhotoPath: StateFlow<String?> = _studentCardPhotoPath

    private val _studentCodeType = MutableStateFlow("BARCODE")
    val studentCodeType: StateFlow<String> = _studentCodeType

    private var currentApiService: MkrApiService = apiService

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allNotes = preferencesManager.getNotesPrefs().all
                val mapped = mutableMapOf<String, String>()
                for ((k, v) in allNotes) {
                    if (v is String) mapped[k] = v
                }
                _notesMap.value = mapped
            } catch (e: Exception) {
                LogCollector.log("PublicScheduleViewModel", "Failed to load notes into memory: ${e.message}")
            }
        }

        viewModelScope.launch {
            val universities = University.MKR_UNIVERSITIES
            val savedUniversityId = preferencesManager.selectedUniversityId.first()
            val targetUniversity = universities.firstOrNull { it.id == savedUniversityId }

            if (targetUniversity != null) {
                val retrofit = Retrofit.Builder()
                    .baseUrl(targetUniversity.getRetrofitBaseUrl())
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                currentApiService = retrofit.create(MkrApiService::class.java)
                _uiState.value = _uiState.value.copy(selectedUniversity = targetUniversity)
                loadStructures()
            } else {
                _uiState.value = _uiState.value.copy(selectedUniversity = null)
            }
        }

        viewModelScope.launch {
            preferencesManager.isChillModeEnabled.collect { enabled ->
                _isChillModeEnabled.value = enabled
            }
        }

        viewModelScope.launch {
            preferencesManager.studentCardPhotoPath.collect { path ->
                _studentCardPhotoPath.value = path
            }
        }

        viewModelScope.launch {
            preferencesManager.studentCodeType.collect { type ->
                _studentCodeType.value = type
            }
        }
    }

    fun goBack() {
        val currentState = _uiState.value
        when {
            currentState.selectedStudent != null -> {
                viewModelScope.launch {
                    preferencesManager.saveSelectedStudent(0)
                }
                _uiState.value = currentState.copy(
                    selectedStudent = null,
                    schedule = emptyList()
                )
            }
            currentState.schedule.isNotEmpty() && currentState.students.isNotEmpty() -> {
                _uiState.value = currentState.copy(
                    schedule = emptyList()
                )
            }
            currentState.selectedGroup != null -> {
                viewModelScope.launch {
                    preferencesManager.saveSelectedGroup(0)
                    preferencesManager.saveSelectedStudent(0)
                }
                _uiState.value = currentState.copy(
                    selectedGroup = null,
                    students = emptyList(),
                    selectedStudent = null,
                    schedule = emptyList()
                )
            }
            currentState.selectedCourse != null -> {
                viewModelScope.launch {
                    preferencesManager.saveSelectedCourse(0)
                }
                _uiState.value = currentState.copy(
                    selectedCourse = null,
                    groups = emptyList()
                )
            }
            currentState.selectedFaculty != null -> {
                viewModelScope.launch {
                    preferencesManager.saveSelectedFaculty(0)
                }
                _uiState.value = currentState.copy(
                    selectedFaculty = null,
                    courses = emptyList()
                )
            }
            currentState.selectedStructure != null -> {
                viewModelScope.launch {
                    preferencesManager.saveSelectedStructure(0)
                }
                _uiState.value = currentState.copy(
                    selectedStructure = null,
                    faculties = emptyList()
                )
            }
            currentState.selectedUniversity != null -> {
                viewModelScope.launch {
                    preferencesManager.saveSelectedUniversity("")
                }
                _uiState.value = currentState.copy(
                    selectedUniversity = null,
                    structures = emptyList()
                )
            }
        }
    }

    fun resetToStart() {
        viewModelScope.launch {
            preferencesManager.saveSelectedUniversity("")
            preferencesManager.saveSelectedStructure(0)
            preferencesManager.saveSelectedFaculty(0)
            preferencesManager.saveSelectedCourse(0)
            preferencesManager.saveSelectedGroup(0)
            preferencesManager.saveSelectedStudent(0)
            preferencesManager.getScheduleCachePrefs().edit().clear().apply()
            ExactAlarmScheduler.cancelAllAlarms(context)
        }
        _uiState.value = PublicScheduleUiState()
    }

    fun selectUniversity(
        university: University,
        skipLoad: Boolean = false,
        preserveSavedSelection: Boolean = false
    ) {
        viewModelScope.launch {
            val previousUniversityId = preferencesManager.selectedUniversityId.first()
            preferencesManager.saveSelectedUniversity(university.id)
            if (!preserveSavedSelection && previousUniversityId != university.id) {
                preferencesManager.saveSelectedStructure(0)
                preferencesManager.saveSelectedFaculty(0)
                preferencesManager.saveSelectedCourse(0)
                preferencesManager.saveSelectedGroup(0)
                preferencesManager.saveSelectedStudent(0)
                preferencesManager.getScheduleCachePrefs().edit().clear().apply()
                ExactAlarmScheduler.cancelAllAlarms(context)
            }

            val retrofit = Retrofit.Builder()
                .baseUrl(university.getRetrofitBaseUrl())
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            currentApiService = retrofit.create(MkrApiService::class.java)

            _uiState.value = _uiState.value.copy(
                selectedUniversity = university,
                structures = emptyList(),
                selectedStructure = null,
                faculties = emptyList(),
                selectedFaculty = null,
                courses = emptyList(),
                selectedCourse = null,
                groups = emptyList(),
                selectedGroup = null,
                students = emptyList(),
                selectedStudent = null,
                schedule = emptyList(),
                callSchedule = emptyList(),
                error = null
            )
            if (!skipLoad) {
                loadStructures()
            }
        }
    }

    fun loadStructures() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val university = _uiState.value.selectedUniversity
            if (university == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Університет не вибрано"
                )
                return@launch
            }

            val urlsToTry = university.getAlternativeUrls()

            for (url in urlsToTry) {
                try {
                    val retrofit = Retrofit.Builder()
                        .baseUrl(url)
                        .client(okHttpClient)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                    val apiService = retrofit.create(MkrApiService::class.java)

                    val response = apiService.getStructures("")
                    if (response.isSuccessful && response.body() != null) {
                        currentApiService = apiService
                        val structures = response.body()!!
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            structures = structures
                        )

                        val savedStudentId = preferencesManager.selectedStudentId.first()?.toIntOrNull()
                        val savedGroupId = preferencesManager.selectedGroupId.first()?.toIntOrNull()
                        if ((savedGroupId != null && savedGroupId > 0) || (savedStudentId != null && savedStudentId > 0)) {
                            restoreSavedSelection(savedGroupId ?: 0, savedStudentId)
                        }

                        return@launch
                    }
                } catch (e: Exception) {
                    continue
                }
            }

            val cached = getScheduleFromCache()
            if (cached != null && cached.isNotEmpty()) {
                val savedGroupId = preferencesManager.selectedGroupId.first()?.toIntOrNull() ?: 0
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedGroup = if (savedGroupId > 0) Group(id = savedGroupId, name = "Збережений розклад", course = 1, priority = 0, educationForm = 1) else null,
                    schedule = filterAndSortSchedule(cached),
                    error = "Офлайн режим • дані з локального кешу"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Не вдалося підключитися до ${university.shortName}. Перевірте інтернет-з'єднання або спробуйте інший університет."
            )
        }
    }

    fun selectStructure(structure: Structure) {
        viewModelScope.launch {
            preferencesManager.saveSelectedStructure(structure.id)
            preferencesManager.saveSelectedFaculty(0)
            preferencesManager.saveSelectedCourse(0)
            preferencesManager.saveSelectedGroup(0)
            preferencesManager.saveSelectedStudent(0)
        }
        _uiState.value = _uiState.value.copy(
            selectedStructure = structure,
            faculties = emptyList(),
            selectedFaculty = null,
            courses = emptyList(),
            selectedCourse = null,
            groups = emptyList(),
            selectedGroup = null,
            students = emptyList(),
            selectedStudent = null,
            schedule = emptyList()
        )
        loadFaculties(structure.id)
    }

    fun loadFaculties(structureId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = currentApiService.getFaculties("", FacultiesFilterForm(structureId))
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        faculties = response.body()!!
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Не вдалося завантажити список факультетів: ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Помилка: ${e.message}"
                )
            }
        }
    }

    fun selectFaculty(faculty: Faculty) {
        viewModelScope.launch {
            preferencesManager.saveSelectedFaculty(faculty.id)
            preferencesManager.saveSelectedCourse(0)
            preferencesManager.saveSelectedGroup(0)
            preferencesManager.saveSelectedStudent(0)
        }
        _uiState.value = _uiState.value.copy(
            selectedFaculty = faculty,
            courses = emptyList(),
            selectedCourse = null,
            groups = emptyList(),
            selectedGroup = null,
            students = emptyList(),
            selectedStudent = null,
            schedule = emptyList()
        )
        loadCourses(faculty.id)
    }

    fun loadCourses(facultyId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = currentApiService.getCourses(CoursesFilterForm(facultyId))
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        courses = response.body()!!
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Не вдалося завантажити список курсів: ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Помилка: ${e.message}"
                )
            }
        }
    }

    fun selectCourse(course: Course) {
        viewModelScope.launch {
            preferencesManager.saveSelectedCourse(course.course)
            preferencesManager.saveSelectedGroup(0)
            preferencesManager.saveSelectedStudent(0)
        }
        _uiState.value = _uiState.value.copy(
            selectedCourse = course,
            groups = emptyList(),
            selectedGroup = null,
            students = emptyList(),
            selectedStudent = null,
            schedule = emptyList()
        )
        val facultyId = _uiState.value.selectedFaculty?.id
        if (facultyId != null) {
            loadGroups(facultyId, course.course)
        }
    }

    fun loadGroups(facultyId: Int, course: Int? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val university = _uiState.value.selectedUniversity
            try {
                LogCollector.log("PublicScheduleVM", "loadGroups: facultyId=$facultyId, course=$course, university=${university?.shortName}")

                val primaryResponse = currentApiService.getGroups("", GroupsFilterForm(facultyId, course))
                if (primaryResponse.isSuccessful && primaryResponse.body() != null && primaryResponse.body()!!.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        groups = primaryResponse.body()!!
                    )
                    return@launch
                }

                val noCourseResponse = currentApiService.getGroups("", GroupsFilterForm(facultyId, null))
                if (noCourseResponse.isSuccessful && noCourseResponse.body() != null && noCourseResponse.body()!!.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        groups = noCourseResponse.body()!!
                    )
                    return@launch
                }

                val alternativeUrls = university?.getAlternativeUrls().orEmpty()
                for (url in alternativeUrls) {
                    try {
                        val retrofit = Retrofit.Builder()
                            .baseUrl(url)
                            .client(okHttpClient)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build()
                        val apiService = retrofit.create(MkrApiService::class.java)

                        val responseWithCourse = apiService.getGroups("", GroupsFilterForm(facultyId, course))
                        if (responseWithCourse.isSuccessful && responseWithCourse.body() != null && responseWithCourse.body()!!.isNotEmpty()) {
                            currentApiService = apiService
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                groups = responseWithCourse.body()!!
                            )
                            return@launch
                        }

                        val responseWithoutCourse = apiService.getGroups("", GroupsFilterForm(facultyId, null))
                        if (responseWithoutCourse.isSuccessful && responseWithoutCourse.body() != null && responseWithoutCourse.body()!!.isNotEmpty()) {
                            currentApiService = apiService
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                groups = responseWithoutCourse.body()!!
                            )
                            return@launch
                        }
                    } catch (e: Exception) {
                        LogCollector.log("PublicScheduleVM", "loadGroups fallback #3 url=$url failed: ${e.message}")
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Не вдалося завантажити список груп для ${university?.shortName ?: "вибраного вузу"}. Спробуйте інший факультет або курс."
                )
            } catch (e: Exception) {
                LogCollector.log("PublicScheduleVM", "loadGroups exception: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Помилка: ${e.message}"
                )
            }
        }
    }

    fun selectGroup(group: Group) {
        viewModelScope.launch {
            preferencesManager.saveSelectedGroup(group.id)
            preferencesManager.saveSelectedStudent(0)
        }
        _uiState.value = _uiState.value.copy(
            selectedGroup = group,
            students = emptyList(),
            selectedStudent = null,
            schedule = emptyList()
        )
        loadStudentsByGroup(group.id)
    }

    fun loadStudentsByGroup(groupId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = currentApiService.getStudentsByGroup("", StudentsByGroupFilterForm(groupId))
                if (response.isSuccessful && response.body() != null) {
                    val students = response.body()!!
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        students = students
                    )
                    if (students.isEmpty()) {
                        loadGroupSchedule(groupId)
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        students = emptyList()
                    )
                    loadGroupSchedule(groupId)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    students = emptyList()
                )
                loadGroupSchedule(groupId)
            }
        }
    }

    private suspend fun restoreSavedSelection(groupId: Int, studentId: Int?) {
        try {
            val savedStructureId = preferencesManager.selectedStructureId.first()
            val savedFacultyId = preferencesManager.selectedFacultyId.first()
            val savedCourse = preferencesManager.selectedCourse.first()

            val structures = _uiState.value.structures
            savedStructureId?.toIntOrNull()?.let { structureId ->
                val structure = structures.find { it.id == structureId }
                structure?.let {
                    _uiState.value = _uiState.value.copy(selectedStructure = it)

                    val facultiesResponse = currentApiService.getFaculties("", FacultiesFilterForm(structureId))
                    if (facultiesResponse.isSuccessful && facultiesResponse.body() != null) {
                        val faculties = facultiesResponse.body()!!
                        _uiState.value = _uiState.value.copy(faculties = faculties)

                        savedFacultyId?.toIntOrNull()?.let { facultyId ->
                            val faculty = faculties.find { it.id == facultyId }
                            faculty?.let {
                                _uiState.value = _uiState.value.copy(selectedFaculty = it)

                                val coursesResponse = currentApiService.getCourses(CoursesFilterForm(facultyId))
                                if (coursesResponse.isSuccessful && coursesResponse.body() != null) {
                                    val courses = coursesResponse.body()!!
                                    _uiState.value = _uiState.value.copy(courses = courses)

                                    savedCourse?.toIntOrNull()?.let { courseNum ->
                                        val course = courses.find { it.course == courseNum }
                                        course?.let {
                                            _uiState.value = _uiState.value.copy(selectedCourse = it)

                                            val groupsResponse = currentApiService.getGroups("", GroupsFilterForm(facultyId, courseNum))
                                            if (groupsResponse.isSuccessful && groupsResponse.body() != null) {
                                                val groups = groupsResponse.body()!!
                                                _uiState.value = _uiState.value.copy(groups = groups)

                                                val group = groups.find { it.id == groupId }
                                                group?.let {
                                                    _uiState.value = _uiState.value.copy(selectedGroup = it)

                                                    val studentsResponse = currentApiService.getStudentsByGroup("", StudentsByGroupFilterForm(groupId))
                                                    if (studentsResponse.isSuccessful && studentsResponse.body() != null) {
                                                        val students = studentsResponse.body()!!
                                                        _uiState.value = _uiState.value.copy(students = students)

                                                        if (studentId != null && studentId > 0) {
                                                            val student = students.find { it.id == studentId }
                                                            if (student != null) {
                                                                _uiState.value = _uiState.value.copy(
                                                                    selectedStudent = student,
                                                                    scheduleType = ScheduleType.STUDENT
                                                                )
                                                                loadStudentSchedule(studentId)
                                                                return
                                                            }
                                                        }
                                                    }
                                                    // If no student saved or group has no students, load group schedule
                                                    _uiState.value = _uiState.value.copy(scheduleType = ScheduleType.GROUP)
                                                    loadGroupSchedule(groupId)
                                                    return
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // Fallback direct load if intermediate hierarchy changed
            if (_uiState.value.selectedGroup == null && groupId > 0) {
                _uiState.value = _uiState.value.copy(
                    selectedGroup = Group(id = groupId, name = "Розклад", course = 1, priority = 0, educationForm = 1)
                )
            }
            if (studentId != null && studentId > 0) {
                _uiState.value = _uiState.value.copy(scheduleType = ScheduleType.STUDENT)
                loadStudentSchedule(studentId)
            } else if (groupId > 0) {
                _uiState.value = _uiState.value.copy(scheduleType = ScheduleType.GROUP)
                loadGroupSchedule(groupId)
            }
        } catch (e: Exception) {
            try {
                if (_uiState.value.selectedGroup == null && groupId > 0) {
                    _uiState.value = _uiState.value.copy(
                        selectedGroup = Group(id = groupId, name = "Розклад", course = 1, priority = 0, educationForm = 1)
                    )
                }
                if (studentId != null && studentId > 0) {
                    loadStudentSchedule(studentId)
                } else if (groupId > 0) {
                    loadGroupSchedule(groupId)
                }
            } catch (_: Exception) {
            }
        }
    }

    fun selectScheduleType(type: ScheduleType) {
        _uiState.value = _uiState.value.copy(
            scheduleType = type,
            schedule = emptyList(),
            selectedStudent = null
        )
        val groupId = _uiState.value.selectedGroup?.id
        if (groupId != null) {
            when (type) {
                ScheduleType.GROUP -> {
                    viewModelScope.launch {
                        preferencesManager.saveSelectedStudent(0)
                    }
                    loadGroupSchedule(groupId)
                }
                ScheduleType.STUDENT -> { /* Wait for student selection */ }
            }
        }
    }

    fun selectStudent(student: Student) {
        _uiState.value = _uiState.value.copy(
            selectedStudent = student,
            scheduleType = ScheduleType.STUDENT,
            showSaveStudentDialog = true
        )
    }

    fun confirmSaveStudent(student: Student) {
        LogCollector.log("PublicScheduleVM", "confirmSaveStudent: ${student.id} - ${student.shortName}")
        viewModelScope.launch {
            preferencesManager.saveSelectedStudent(student.id)
            preferencesManager.saveSelectedGroup(_uiState.value.selectedGroup?.id ?: 0)
            _uiState.value = _uiState.value.copy(showSaveStudentDialog = false)
            loadStudentSchedule(student.id)
        }
    }

    fun cancelSaveStudent() {
        LogCollector.log("PublicScheduleVM", "cancelSaveStudent called")
        _uiState.value = _uiState.value.copy(showSaveStudentDialog = false)
        val student = _uiState.value.selectedStudent
        if (student != null) {
            loadStudentSchedule(student.id)
        }
    }

    fun loadGroupSchedule(groupId: Int, weekOffset: Int = 0) {
        viewModelScope.launch {
            val isFirstLoad = weekOffset == 0
            _uiState.value = _uiState.value.copy(
                isLoading = isFirstLoad,
                isLoadingMore = !isFirstLoad,
                error = null
            )
            try {
                val calendar = Calendar.getInstance()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                calendar.add(Calendar.WEEK_OF_YEAR, weekOffset)
                val dateStart = dateFormat.format(calendar.time)

                calendar.add(Calendar.DAY_OF_YEAR, 14)
                val dateEnd = dateFormat.format(calendar.time)

                val response = currentApiService.getGroupTimeTable("", GroupTimeTableForm(groupId, dateStart, dateEnd))
                if (response.isSuccessful && response.body() != null) {
                    val newSchedule = response.body()!!
                    val fullSchedule = if (isFirstLoad) {
                        newSchedule
                    } else {
                        (_uiState.value.schedule + newSchedule).distinctBy { it.date }
                    }

                    val filteredAndSorted = filterAndSortSchedule(fullSchedule)
                    val inferredGroupName = fullSchedule.asSequence()
                        .flatMap { it.lessons.asSequence() }
                        .flatMap { it.periods.asSequence() }
                        .mapNotNull { it.groups?.trim()?.takeIf { g -> g.isNotEmpty() } }
                        .firstOrNull()
                    val currentGroup = _uiState.value.selectedGroup
                    val updatedGroup = when {
                        currentGroup == null && inferredGroupName != null ->
                            Group(id = groupId, name = inferredGroupName, course = 1, priority = 0, educationForm = 1)
                        currentGroup != null && (currentGroup.name == "Розклад" || currentGroup.name == "Збережений розклад") && inferredGroupName != null ->
                            currentGroup.copy(name = inferredGroupName)
                        currentGroup == null ->
                            Group(id = groupId, name = "Група #$groupId", course = 1, priority = 0, educationForm = 1)
                        else -> currentGroup
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        selectedGroup = updatedGroup,
                        schedule = filteredAndSorted,
                        currentWeekOffset = weekOffset
                    )

                    if (isFirstLoad) {
                        saveScheduleToCache(newSchedule)
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = "Не вдалося завантажити розклад: ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                val cached = if (isFirstLoad) getScheduleFromCache() else null
                if (cached != null && cached.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        schedule = filterAndSortSchedule(cached),
                        error = "Офлайн режим (Дані з кешу)"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = "Помилка: ${e.message}"
                    )
                }
            }
        }
    }

    fun loadStudentSchedule(studentId: Int, weekOffset: Int = 0) {
        viewModelScope.launch {
            val isFirstLoad = weekOffset == 0
            _uiState.value = _uiState.value.copy(
                isLoading = isFirstLoad,
                isLoadingMore = !isFirstLoad,
                error = null
            )
            try {
                val calendar = Calendar.getInstance()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                calendar.add(Calendar.WEEK_OF_YEAR, weekOffset)
                val dateStart = dateFormat.format(calendar.time)

                calendar.add(Calendar.DAY_OF_YEAR, 14)
                val dateEnd = dateFormat.format(calendar.time)

                val response = currentApiService.getStudentTimeTablePublic("", StudentTimeTableForm(studentId, dateStart, dateEnd))

                if (response.isSuccessful && response.body() != null) {
                    val newSchedule = response.body()!!
                    val errorMessage = if (newSchedule.isEmpty() && isFirstLoad) {
                        "Розклад для цього студента не знайдено на обрані дати"
                    } else null

                    val fullSchedule = if (isFirstLoad) {
                        newSchedule
                    } else {
                        (_uiState.value.schedule + newSchedule).distinctBy { it.date }
                    }
                    val filteredAndSorted = filterAndSortSchedule(fullSchedule)
                    val inferredGroupName = fullSchedule.asSequence()
                        .flatMap { it.lessons.asSequence() }
                        .flatMap { it.periods.asSequence() }
                        .mapNotNull { it.groups?.trim()?.takeIf { g -> g.isNotEmpty() } }
                        .firstOrNull()
                    val currentGroup = _uiState.value.selectedGroup
                    val updatedGroup = when {
                        currentGroup == null && inferredGroupName != null ->
                            Group(id = 0, name = inferredGroupName, course = 1, priority = 0, educationForm = 1)
                        currentGroup != null && (currentGroup.name == "Розклад" || currentGroup.name == "Збережений розклад") && inferredGroupName != null ->
                            currentGroup.copy(name = inferredGroupName)
                        else -> currentGroup
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        selectedGroup = updatedGroup,
                        schedule = filteredAndSorted,
                        currentWeekOffset = weekOffset,
                        error = errorMessage
                    )

                    if (isFirstLoad && newSchedule.isNotEmpty()) {
                        saveScheduleToCache(newSchedule)
                    }
                } else {
                    val errorMsg = "Не вдалося завантажити розклад: ${response.code()} ${response.message()}"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = errorMsg
                    )
                }
            } catch (e: Exception) {
                val cached = if (isFirstLoad) getScheduleFromCache() else null
                if (cached != null && cached.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        schedule = filterAndSortSchedule(cached),
                        error = "Офлайн режим (Дані з кешу)"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = "Помилка завантаження: ${e.message}"
                    )
                }
            }
        }
    }

    fun loadNextWeek() {
        if (_uiState.value.isLoadingMore || _uiState.value.isLoading) {
            return
        }

        val selectedStudent = _uiState.value.selectedStudent
        val selectedGroup = _uiState.value.selectedGroup
        val nextWeekOffset = _uiState.value.currentWeekOffset + 2

        when {
            selectedStudent != null -> loadStudentSchedule(selectedStudent.id, nextWeekOffset)
            selectedGroup != null -> loadGroupSchedule(selectedGroup.id, nextWeekOffset)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    suspend fun getConferenceLink(groupId: Int, disciplineId: Int?): String? {
        return preferencesManager.getConferenceLink(groupId, disciplineId)
    }

    fun getConferenceLinkFlow(groupId: Int, disciplineId: Int?) =
        preferencesManager.getConferenceLinkFlow(groupId, disciplineId)

    fun saveConferenceLink(groupId: Int, disciplineId: Int?, link: String) {
        viewModelScope.launch {
            preferencesManager.saveConferenceLink(groupId, disciplineId, link)
        }
    }

    fun deleteConferenceLink(groupId: Int, disciplineId: Int?) {
        viewModelScope.launch {
            preferencesManager.deleteConferenceLink(groupId, disciplineId)
        }
    }

    suspend fun loadCallSchedule() {
        try {
            val response = currentApiService.getCallSchedule("")
            if (response.isSuccessful && response.body() != null) {
                _uiState.value = _uiState.value.copy(
                    callSchedule = response.body()!!
                )
            }
        } catch (e: Exception) {
            LogCollector.log("PublicScheduleViewModel", "Error loading call schedule: ${e.message}")
        }
    }

    fun resetToInitialState() {
        viewModelScope.launch {
            _uiState.value = PublicScheduleUiState(
                selectedUniversity = _uiState.value.selectedUniversity
            )
            val university = _uiState.value.selectedUniversity
            if (university != null) {
                loadStructures()
            }
        }
    }

    private fun saveScheduleToCache(schedule: List<TimeTableDate>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prefs = preferencesManager.getScheduleCachePrefs()
                val gson = com.google.gson.Gson()

                val daySchedules = schedule.map { timeTableDate ->
                    DaySchedule(
                        date = timeTableDate.date,
                        lessons = timeTableDate.lessons.map { lesson ->
                            NotificationLesson(
                                numberPair = lesson.number.toString(),
                                periods = lesson.periods
                            )
                        }
                    )
                }

                val scheduleJson = gson.toJson(daySchedules)
                val rawJson = gson.toJson(schedule)
                prefs.edit()
                    .putString("cached_schedule", scheduleJson)
                    .putString("cached_schedule_raw", rawJson)
                    .apply()

                LogCollector.log("PublicScheduleViewModel", "Schedule saved to cache")

                try {
                    androidx.glance.appwidget.GlanceAppWidgetManager(context)
                        .getGlanceIds(com.mkr.schedule.ui.widget.ScheduleWidget::class.java)
                        .forEach { glanceId ->
                            com.mkr.schedule.ui.widget.ScheduleWidget().update(context, glanceId)
                        }
                } catch (we: Exception) {
                    LogCollector.log("PublicScheduleViewModel", "Widget update skipped: ${we.message}")
                }

                if (preferencesManager.getNotificationsEnabled()) {
                    ExactAlarmScheduler.scheduleAllAlarms(context)
                }

                // Automatic background synchronization with Wear OS companion watch (Pixel Watch)
                val state = _uiState.value
                val groupName = state.selectedGroup?.name ?: "ФІТ 2-5"
                val studentObj = state.selectedStudent
                val studentLabel = if (studentObj != null) {
                    listOfNotNull(studentObj.lastName, studentObj.firstName, studentObj.secondName)
                        .joinToString(" ")
                        .trim()
                } else ""
                val uniShort = state.selectedUniversity?.shortName ?: "ДТЕУ"
                val subtitle = if (studentLabel.isNotBlank()) studentLabel else uniShort
                com.mkr.schedule.wear.PhoneWearSyncManager.syncScheduleFromViewModel(
                    context = context,
                    profileTitle = groupName,
                    profileSubtitle = subtitle,
                    groupId = state.selectedGroup?.id ?: 3344,
                    studentId = studentObj?.id ?: 0,
                    isStudentMode = studentObj != null,
                    schedule = schedule
                )
            } catch (e: Exception) {
                LogCollector.log("PublicScheduleViewModel", "Error saving schedule: ${e.message}")
            }
        }
    }

    fun saveNote(lessonKey: String, note: String) {
        preferencesManager.saveNote(lessonKey, note)
        val currentMap = _notesMap.value.toMutableMap()
        if (note.isBlank()) {
            currentMap.remove(lessonKey)
        } else {
            currentMap[lessonKey] = note
        }
        _notesMap.value = currentMap
    }

    fun getNote(lessonKey: String): String? {
        return _notesMap.value[lessonKey]
    }

    private fun getScheduleFromCache(): List<TimeTableDate>? {
        return try {
            val prefs = preferencesManager.getScheduleCachePrefs()
            val rawJson = prefs.getString("cached_schedule_raw", null)
            if (rawJson.isNullOrEmpty()) return null
            val type = object : com.google.gson.reflect.TypeToken<List<TimeTableDate>>() {}.type
            com.google.gson.Gson().fromJson(rawJson, type)
        } catch (e: Exception) { null }
    }

    private fun filterAndSortSchedule(schedule: List<TimeTableDate>): List<TimeTableDate> {
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        val todayDate = today.time

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayString = dateFormat.format(todayDate)

        val filtered = schedule.filter { day ->
            try {
                val dayDate = dateFormat.parse(day.date.take(10))
                dayDate != null && !dayDate.before(todayDate)
            } catch (e: Exception) {
                true
            }
        }

        return filtered.sortedWith(compareBy<TimeTableDate> { day ->
            if (day.date.startsWith(todayString)) {
                0L
            } else {
                try {
                    dateFormat.parse(day.date.take(10))?.time ?: Long.MAX_VALUE
                } catch (e: Exception) {
                    Long.MAX_VALUE
                }
            }
        })
    }

    fun saveStudentCardPhotoPath(path: String?) {
        viewModelScope.launch {
            preferencesManager.saveStudentCardPhotoPath(path)
        }
    }

    fun saveStudentCodeType(type: String) {
        viewModelScope.launch {
            preferencesManager.saveStudentCodeType(type)
        }
    }
}
