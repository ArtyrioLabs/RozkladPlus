@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.mkr.schedule.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mkr.schedule.data.local.PreferencesManager
import com.mkr.schedule.data.model.*
import com.mkr.schedule.ui.components.ScheduleGridView
import com.mkr.schedule.ui.settings.SettingsScreen
import com.mkr.schedule.ui.viewmodel.PublicScheduleUiState
import com.mkr.schedule.ui.viewmodel.PublicScheduleViewModel
import com.mkr.schedule.ui.viewmodel.ScheduleType
import com.mkr.schedule.util.LogCollector
import com.mkr.schedule.util.localization.LocalStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun PublicScheduleScreen(
    viewModel: PublicScheduleViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val isChillModeEnabled by viewModel.isChillModeEnabled.collectAsState()
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context.applicationContext) }
    val strings = LocalStrings.current

    var swipeOffset by remember { mutableStateOf(0f) }
    var swipeStartedFromEdge by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    var isGridView by rememberSaveable { mutableStateOf(false) }
    var universitySearchQuery by rememberSaveable { mutableStateOf("") }
    var studentSearchQuery by rememberSaveable { mutableStateOf("") }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val blockingLoading = uiState.isLoading &&
        uiState.structures.isEmpty() &&
        uiState.faculties.isEmpty() &&
        uiState.courses.isEmpty() &&
        uiState.groups.isEmpty() &&
        uiState.students.isEmpty() &&
        uiState.schedule.isEmpty()

    val blockingError = uiState.error != null &&
        uiState.schedule.isEmpty() &&
        !uiState.isLoading &&
        uiState.groups.isEmpty() &&
        uiState.students.isEmpty()

    val persistedXp by preferencesManager.totalStudentXp.collectAsState(initial = 0)
    val persistedMaxCompletedLessons by preferencesManager.maxCompletedLessons.collectAsState(initial = 0)
    var progressNow by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            progressNow = LocalDateTime.now()
            delay(30_000)
        }
    }

    val liveProgressStats = remember(uiState.schedule, progressNow) {
        calculateScheduleProgress(uiState.schedule, progressNow)
    }

    LaunchedEffect(uiState.selectedGroup?.id) {
        uiState.selectedGroup?.let { group ->
            com.mkr.schedule.util.AppAnalytics.logGroupSelected(group.name)
            com.mkr.schedule.util.AppAnalytics.logScheduleView(group.name, uiState.selectedUniversity?.shortName)
        }
    }

    LaunchedEffect(liveProgressStats.completedLessons, persistedMaxCompletedLessons, persistedXp) {
        if (liveProgressStats.completedLessons > persistedMaxCompletedLessons) {
            val gainedLessons = liveProgressStats.completedLessons - persistedMaxCompletedLessons
            val gainedXp = gainedLessons * XP_PER_COMPLETED_LESSON
            preferencesManager.saveStudentLevelProgress(
                totalXp = persistedXp + gainedXp,
                maxCompletedLessons = liveProgressStats.completedLessons
            )
        }
    }

    val showLoadMoreButton by remember {
        derivedStateOf {
            uiState.schedule.isNotEmpty() &&
                !isGridView &&
                listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == listState.layoutInfo.totalItemsCount - 1
        }
    }

    val canGoBack = remember(uiState) {
        uiState.selectedStudent != null ||
            uiState.schedule.isNotEmpty() ||
            uiState.selectedGroup != null ||
            uiState.selectedCourse != null ||
            uiState.selectedFaculty != null ||
            uiState.selectedStructure != null ||
            uiState.selectedUniversity != null
    }

    BackHandler(enabled = canGoBack && !showSettings && !showResetConfirmation && drawerState.isClosed) {
        if (uiState.schedule.isNotEmpty()) {
            showResetConfirmation = true
        } else {
            viewModel.goBack()
        }
    }

    BackHandler(enabled = showSettings) {
        showSettings = false
    }

    BackHandler(enabled = showResetConfirmation) {
        showResetConfirmation = false
    }

    BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch {
            drawerState.close()
        }
    }

    val isShowingSchedule = uiState.schedule.isNotEmpty() ||
        (uiState.selectedGroup != null && uiState.students.isEmpty() && !uiState.isLoading)

    val topTitle = when {
        uiState.schedule.isNotEmpty() -> uiState.selectedGroup?.name ?: strings.schedule
        uiState.selectedUniversity == null -> selectionStageTitle(strings.selectUniversity)
        uiState.selectedStructure == null -> selectionStageTitle(strings.selectStructure)
        uiState.selectedFaculty == null -> selectionStageTitle(strings.selectFaculty)
        uiState.selectedCourse == null -> selectionStageTitle(strings.selectCourse)
        uiState.selectedGroup == null -> selectionStageTitle(strings.selectGroup)
        uiState.schedule.isEmpty() && uiState.students.isNotEmpty() && uiState.selectedStudent == null ->
            uiState.selectedGroup?.name ?: selectionStageTitle(strings.selectStudent)
        uiState.selectedGroup != null -> uiState.selectedGroup?.name ?: strings.schedule
        else -> strings.schedule
    }

    val topSubtitle = when {
        uiState.selectedStudent != null -> uiState.selectedStudent?.fullName
        uiState.selectedGroup != null && uiState.schedule.isNotEmpty() -> uiState.selectedUniversity?.shortName
        uiState.selectedUniversity != null -> uiState.selectedUniversity?.shortName
        else -> null
    }

    val menuIconRotation by animateFloatAsState(
        targetValue = if (drawerState.isOpen) 90f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "menu_icon_rotation"
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.86f),
                drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
            ) {
                ProfileDrawerContent(
                    uiState = uiState,
                    isChillModeEnabled = isChillModeEnabled,
                    isGridView = isGridView,
                    onToggleGridView = {
                        isGridView = !isGridView
                        coroutineScope.launch { drawerState.close() }
                    },
                    onOpenSettings = {
                        showSettings = true
                        coroutineScope.launch { drawerState.close() }
                    },
                    onChangeSchedule = {
                        coroutineScope.launch { drawerState.close() }
                        showResetConfirmation = true
                    },
                    callSchedule = uiState.callSchedule,
                    isLoadingCallSchedule = uiState.isLoading && uiState.callSchedule.isEmpty(),
                    totalXp = persistedXp,
                    maxCompletedLessons = persistedMaxCompletedLessons
                )
            }
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = topTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!topSubtitle.isNullOrBlank()) {
                                Text(
                                    text = topSubtitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    if (drawerState.isOpen) {
                                        drawerState.close()
                                    } else {
                                        if (uiState.callSchedule.isEmpty() && uiState.selectedUniversity != null) {
                                            viewModel.loadCallSchedule()
                                        }
                                        drawerState.open()
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Меню",
                                modifier = Modifier.rotate(menuIconRotation)
                            )
                        }
                    },
                    actions = {
                        if (uiState.schedule.isNotEmpty()) {
                            FilledTonalIconButton(
                                onClick = { isGridView = !isGridView },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Icon(
                                    imageVector = if (isGridView) Icons.Default.List else Icons.Default.GridView,
                                    contentDescription = if (isGridView) "Список" else "Сітка"
                                )
                            }
                        }
                        if (canGoBack && uiState.schedule.isEmpty()) {
                            IconButton(onClick = { viewModel.goBack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = strings.back
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .then(
                        if (canGoBack && !showSettings) {
                            Modifier.pointerInput(canGoBack, showSettings) {
                                detectHorizontalDragGestures(
                                    onDragStart = { offset ->
                                        swipeStartedFromEdge = offset.x < 64.dp.toPx()
                                        if (swipeStartedFromEdge) {
                                            swipeOffset = 0f
                                        }
                                    },
                                    onDragEnd = {
                                        if (swipeStartedFromEdge && swipeOffset > 90f) {
                                            if (uiState.schedule.isNotEmpty()) {
                                                showResetConfirmation = true
                                            } else {
                                                viewModel.goBack()
                                            }
                                        }
                                        swipeOffset = 0f
                                        swipeStartedFromEdge = false
                                    },
                                    onDragCancel = {
                                        swipeOffset = 0f
                                        swipeStartedFromEdge = false
                                    }
                                ) { change, dragAmount ->
                                    if (swipeStartedFromEdge && dragAmount > 0 && swipeOffset >= 0f) {
                                        change.consume()
                                        swipeOffset += dragAmount
                                    }
                                }
                            }
                        } else {
                            Modifier
                        }
                    )
            ) {
                AnimatedVisibility(
                    visible = blockingLoading,
                    enter = fadeIn(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
                    exit = fadeOut(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec())
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingStateCard(
                            title = loadingTitleFor(uiState),
                            subtitle = loadingSubtitleFor(uiState),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                AnimatedVisibility(
                    visible = blockingError,
                    enter = fadeIn(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()) +
                        scaleIn(initialScale = 0.95f, animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()),
                    exit = fadeOut(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(padding)
                ) {
                    ErrorStateCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        title = "Не вдалося завантажити дані",
                        message = uiState.error.orEmpty(),
                        compact = false,
                        onRetry = { retryCurrentStep(viewModel, uiState) },
                        onDismiss = { viewModel.clearError() }
                    )
                }

                if (!blockingError && !blockingLoading) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 88.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        uiState.error?.let { error ->
                            item(key = "non_blocking_error") {
                                ErrorStateCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    title = "Увага",
                                    message = error,
                                    compact = true,
                                    onRetry = { retryCurrentStep(viewModel, uiState) },
                                    onDismiss = { viewModel.clearError() }
                                )
                            }
                        }

                        // Step 0: University Selection
                        if (uiState.selectedUniversity == null) {
                            item(key = "step_university") {
                                SelectionStepCard(
                                    title = selectionStageTitle(strings.selectUniversity),
                                    subtitle = "Оберіть ваш заклад вищої освіти зі списку",
                                    icon = Icons.Default.AccountBalance
                                ) {
                                    OutlinedTextField(
                                        value = universitySearchQuery,
                                        onValueChange = { universitySearchQuery = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp),
                                        singleLine = true,
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = null
                                            )
                                        },
                                        trailingIcon = if (universitySearchQuery.isNotEmpty()) {
                                            {
                                                IconButton(onClick = { universitySearchQuery = "" }) {
                                                    Icon(Icons.Default.Close, contentDescription = "Очистити")
                                                }
                                            }
                                        } else null,
                                        placeholder = { Text("Пошук за назвою або абревіатурою") },
                                        shape = RoundedCornerShape(16.dp)
                                    )

                                    val filteredUniversities = if (universitySearchQuery.isBlank()) {
                                        uiState.universities
                                    } else {
                                        uiState.universities.filter { university ->
                                            university.name.contains(universitySearchQuery, ignoreCase = true) ||
                                                university.shortName.contains(universitySearchQuery, ignoreCase = true)
                                        }
                                    }

                                    if (filteredUniversities.isEmpty()) {
                                        Text(
                                            text = "Нічого не знайдено",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(vertical = 12.dp)
                                        )
                                    }

                                    filteredUniversities.forEach { university ->
                                        ModernSelectableCard(
                                            title = university.shortName,
                                            subtitle = university.name,
                                            onClick = { viewModel.selectUniversity(university) }
                                        )
                                    }
                                }
                            }
                        }

                        // Step 1: Structure Selection
                        if (!isShowingSchedule && uiState.selectedUniversity != null && uiState.structures.isNotEmpty() && uiState.selectedStructure == null) {
                            item(key = "step_structure") {
                                SelectionStepCard(
                                    title = selectionStageTitle(strings.selectStructure),
                                    subtitle = uiState.selectedUniversity?.name ?: "Оберіть підрозділ",
                                    icon = Icons.Default.School
                                ) {
                                    uiState.structures.forEach { structure ->
                                        ModernSelectableCard(
                                            title = structure.fullName ?: structure.shortName ?: "Структура",
                                            subtitle = structure.shortName?.takeIf { it != structure.fullName },
                                            onClick = { viewModel.selectStructure(structure) }
                                        )
                                    }
                                }
                            }
                        }

                        // Step 2: Faculty Selection
                        if (!isShowingSchedule && uiState.selectedStructure != null && uiState.faculties.isNotEmpty() && uiState.selectedFaculty == null) {
                            item(key = "step_faculty") {
                                SelectionStepCard(
                                    title = selectionStageTitle(strings.selectFaculty),
                                    subtitle = uiState.selectedStructure?.shortName ?: "Оберіть факультет або інститут",
                                    icon = Icons.Default.School
                                ) {
                                    uiState.faculties.forEach { faculty ->
                                        ModernSelectableCard(
                                            title = faculty.shortName ?: faculty.fullName ?: "Факультет",
                                            subtitle = faculty.fullName?.takeIf { it != faculty.shortName },
                                            onClick = { viewModel.selectFaculty(faculty) }
                                        )
                                    }
                                }
                            }
                        }

                        // Step 3: Course Selection
                        if (!isShowingSchedule && uiState.selectedFaculty != null && uiState.courses.isNotEmpty() && uiState.selectedCourse == null) {
                            item(key = "step_course") {
                                SelectionStepCard(
                                    title = selectionStageTitle(strings.selectCourse),
                                    subtitle = uiState.selectedFaculty?.shortName ?: "Оберіть курс навчання",
                                    icon = Icons.Default.Class
                                ) {
                                    uiState.courses.forEach { course ->
                                        ModernSelectableCard(
                                            title = "${course.course} курс",
                                            onClick = { viewModel.selectCourse(course) }
                                        )
                                    }
                                }
                            }
                        }

                        // Step 4: Group Selection
                        if (!isShowingSchedule && uiState.selectedCourse != null && uiState.groups.isNotEmpty() && uiState.selectedGroup == null) {
                            item(key = "step_group") {
                                SelectionStepCard(
                                    title = selectionStageTitle(strings.selectGroup),
                                    subtitle = "${uiState.selectedFaculty?.shortName.orEmpty()} • ${uiState.selectedCourse?.course} курс",
                                    icon = Icons.Default.Groups
                                ) {
                                    uiState.groups.forEach { group ->
                                        ModernSelectableCard(
                                            title = group.name,
                                            onClick = { viewModel.selectGroup(group) }
                                        )
                                    }
                                }
                            }
                        }

                        // Step 5: Student / Group Schedule Selection
                        if (uiState.selectedGroup != null && uiState.students.isNotEmpty() && uiState.schedule.isEmpty()) {
                            item(key = "step_student_or_group") {
                                SelectionStepCard(
                                    title = "Режим розкладу",
                                    subtitle = "Група ${uiState.selectedGroup?.name} • оберіть загальний розклад групи або персональний",
                                    icon = Icons.Default.Person
                                ) {
                                    FilledTonalButton(
                                        onClick = { viewModel.selectScheduleType(ScheduleType.GROUP) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        contentPadding = PaddingValues(vertical = 14.dp, horizontal = 16.dp)
                                    ) {
                                        Icon(Icons.Default.Groups, contentDescription = null)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Відкрити загальний розклад групи ${uiState.selectedGroup?.name}",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    )

                                    OutlinedTextField(
                                        value = studentSearchQuery,
                                        onValueChange = { studentSearchQuery = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 4.dp),
                                        singleLine = true,
                                        leadingIcon = {
                                            Icon(Icons.Default.Search, contentDescription = null)
                                        },
                                        trailingIcon = if (studentSearchQuery.isNotEmpty()) {
                                            {
                                                IconButton(onClick = { studentSearchQuery = "" }) {
                                                    Icon(Icons.Default.Close, contentDescription = "Очистити")
                                                }
                                            }
                                        } else null,
                                        placeholder = { Text("Пошук студента за прізвищем") },
                                        shape = RoundedCornerShape(16.dp)
                                    )

                                    val filteredStudents = if (studentSearchQuery.isBlank()) {
                                        uiState.students
                                    } else {
                                        uiState.students.filter {
                                            it.fullName.contains(studentSearchQuery, ignoreCase = true)
                                        }
                                    }

                                    filteredStudents.forEach { student ->
                                        ModernSelectableCard(
                                            title = student.fullName,
                                            onClick = { viewModel.selectStudent(student) }
                                        )
                                    }
                                }
                            }
                        }

                        // Step 6: Show Schedule
                        if (isShowingSchedule) {
                            if (uiState.schedule.isEmpty() && !uiState.isLoading && uiState.error == null) {
                                item(key = "empty_schedule") {
                                    EmptyScheduleCard(
                                        onLoadNextWeek = { viewModel.loadNextWeek() }
                                    )
                                }
                            } else if (uiState.schedule.isNotEmpty()) {
                                if (isGridView) {
                                    item(key = "grid_view") {
                                        ScheduleGridView(schedule = uiState.schedule)
                                    }
                                } else {
                                    itemsIndexed(
                                        items = uiState.schedule,
                                        key = { index, day -> "${day.date}_$index" }
                                    ) { _, daySchedule ->
                                        ModernDayScheduleCard(
                                            daySchedule = daySchedule,
                                            groupId = uiState.selectedGroup?.id ?: 0,
                                            viewModel = viewModel
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Load Next 2 Weeks Extended FAB (clean, no infinite pulse)
                AnimatedVisibility(
                    visible = showLoadMoreButton && !uiState.isLoadingMore && !uiState.isLoading && uiState.selectedGroup != null,
                    enter = slideInVertically(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()) { it } +
                        fadeIn(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
                    exit = slideOutVertically(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()) { it } +
                        fadeOut(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp)
                ) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            try {
                                if (!uiState.isLoadingMore && !uiState.isLoading) {
                                    viewModel.loadNextWeek()
                                }
                            } catch (e: Exception) {
                                LogCollector.log("PublicSchedule", "Error loading next week: ${e.message}")
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        },
                        text = {
                            Text(
                                text = "Наступні тижні",
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                if (uiState.isLoadingMore) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 20.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 4.dp
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(10.dp)
                                .size(28.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Dialog: Save Student
        uiState.selectedStudent?.let { selectedStudent ->
            if (uiState.showSaveStudentDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.cancelSaveStudent() },
                    title = {
                        Text(
                            text = "Зберегти вибір?",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = "Відкривати розклад студента ${selectedStudent.fullName} автоматично під час запуску?",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.confirmSaveStudent(selectedStudent) }
                        ) {
                            Text("Зберегти")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { viewModel.cancelSaveStudent() }
                        ) {
                            Text("Тільки переглянути")
                        }
                    },
                    shape = RoundedCornerShape(28.dp)
                )
            }
        }

        // Dialog: Reset / Change Group or University
        if (showResetConfirmation) {
            AlertDialog(
                onDismissRequest = { showResetConfirmation = false },
                title = {
                    Text(
                        text = "Змінити групу або ЗВО?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Ви можете повернутися до вибору студента/групи або скинути вибір до списку університетів.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showResetConfirmation = false
                            viewModel.resetToStart()
                        }
                    ) {
                        Text("Змінити ЗВО")
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (uiState.students.isNotEmpty() || uiState.groups.isNotEmpty()) {
                            FilledTonalButton(
                                onClick = {
                                    showResetConfirmation = false
                                    viewModel.goBack()
                                }
                            ) {
                                Text("Крок назад")
                            }
                        }
                        TextButton(onClick = { showResetConfirmation = false }) {
                            Text("Скасувати")
                        }
                    }
                },
                shape = RoundedCornerShape(28.dp)
            )
        }

        // Settings Screen Overlay with Expressive Motion
        AnimatedVisibility(
            visible = showSettings,
            enter = slideInHorizontally(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()) { it / 3 } +
                fadeIn(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
            exit = slideOutHorizontally(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()) { it / 3 } +
                fadeOut(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec())
        ) {
            SettingsScreen(
                onBackClick = { showSettings = false }
            )
        }
    }
}

@Composable
private fun ProfileDrawerContent(
    uiState: PublicScheduleUiState,
    isChillModeEnabled: Boolean,
    isGridView: Boolean,
    onToggleGridView: () -> Unit,
    onOpenSettings: () -> Unit,
    onChangeSchedule: () -> Unit,
    callSchedule: List<CallSchedule>,
    isLoadingCallSchedule: Boolean,
    totalXp: Int,
    maxCompletedLessons: Int
) {
    val strings = LocalStrings.current
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(30_000)
        }
    }

    val stats = remember(uiState.schedule, now) {
        calculateScheduleProgress(uiState.schedule, now)
    }
    val progress = stats.progress
    val effectiveXp = totalXp +
        (stats.completedLessons - maxCompletedLessons).coerceAtLeast(0) * XP_PER_COMPLETED_LESSON
    val levelProgress = calculateLevelProgress(effectiveXp)

    val profileTitle = when {
        uiState.selectedStudent != null -> uiState.selectedStudent.fullName
        uiState.selectedGroup != null -> "${strings.groupPrefix} ${uiState.selectedGroup.name}"
        else -> strings.noProfileSelected
    }

    val profileBadge = buildString {
        uiState.selectedUniversity?.shortName?.let { append(it) }
        uiState.selectedGroup?.name?.let {
            if (isNotEmpty()) append(" • ")
            append(it)
        }
    }.ifEmpty { "Розклад+" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Header Card (Material 3 Expressive)
        item(key = "drawer_profile_header") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = profileBadge,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                        if (isChillModeEnabled) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = strings.formatLevel(levelProgress.level, effectiveXp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = profileTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Navigation & Quick Actions
        item(key = "drawer_quick_actions") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strings.quickActions,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                )

                NavigationDrawerItem(
                    label = {
                        Text(
                            text = if (isGridView) strings.viewDayList else strings.viewWeekGrid,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.List else Icons.Default.GridView,
                            contentDescription = null
                        )
                    },
                    selected = false,
                    onClick = onToggleGridView,
                    shape = RoundedCornerShape(18.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        unselectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )

                NavigationDrawerItem(
                    label = {
                        Text(
                            text = strings.changeGroupOrUniversity,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    icon = {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null)
                    },
                    selected = false,
                    onClick = onChangeSchedule,
                    shape = RoundedCornerShape(18.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                NavigationDrawerItem(
                    label = {
                        Text(
                            text = strings.settings,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    icon = {
                        Icon(Icons.Default.Settings, contentDescription = null)
                    },
                    selected = false,
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(18.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unselectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }

        // Bell Schedule Grouped Container
        item(key = "drawer_bell_header") {
            Text(
                text = strings.callSchedule,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }

        if (isLoadingCallSchedule) {
            item(key = "drawer_bell_loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                }
            }
        } else if (callSchedule.isEmpty()) {
            item(key = "drawer_bell_empty") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = strings.bellScheduleUnavailable,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            val currentTimeStr = String.format(Locale.getDefault(), "%02d:%02d", now.hour, now.minute)
            item(key = "drawer_bell_list") {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    callSchedule.forEachIndexed { index, call ->
                        val startClean = call.timeStart.take(5)
                        val endClean = call.timeEnd.take(5)
                        val isCurrentBell = currentTimeStr in startClean..endClean
                        val shape = groupedListShape(index, callSchedule.size)

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = shape,
                            color = if (isCurrentBell) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isCurrentBell) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${call.number}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCurrentBell) {
                                                    MaterialTheme.colorScheme.onPrimary
                                                } else {
                                                    MaterialTheme.colorScheme.onSecondaryContainer
                                                }
                                            )
                                        }
                                    }
                                    Text(
                                        text = strings.formatOrdinalClass(call.number),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isCurrentBell) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isCurrentBell) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Text(
                                        text = startClean,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrentBell) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    Text(
                                        text = "– $endClean",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isCurrentBell) {
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.padding(bottom = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerActionTile(
    label: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun groupedListShape(index: Int, count: Int): Shape {
    val outer = 20.dp
    val inner = 6.dp
    return when {
        count <= 1 -> RoundedCornerShape(outer)
        index == 0 -> RoundedCornerShape(topStart = outer, topEnd = outer, bottomStart = inner, bottomEnd = inner)
        index == count - 1 -> RoundedCornerShape(topStart = inner, topEnd = inner, bottomStart = outer, bottomEnd = outer)
        else -> RoundedCornerShape(inner)
    }
}

private data class ScheduleProgressStats(
    val totalLessons: Int,
    val completedLessons: Int,
    val ongoingLessons: Int,
    val remainingLessons: Int,
    val progress: Float
)

private fun calculateScheduleProgress(
    schedule: List<TimeTableDate>,
    now: LocalDateTime
): ScheduleProgressStats {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    var totalLessons = 0
    var completedLessons = 0
    var ongoingLessons = 0
    var remainingLessons = 0

    val today = now.toLocalDate()

    schedule.forEach { day ->
        val lessonDate = runCatching {
            LocalDate.parse(day.date.take(10), DateTimeFormatter.ISO_LOCAL_DATE)
        }.getOrNull() ?: return@forEach

        if (lessonDate != today) return@forEach

        day.lessons.forEach { lesson ->
            lesson.periods.forEach { period ->
                val start = period.timeStart?.take(5) ?: return@forEach
                val startTime = runCatching { LocalTime.parse(start, timeFormatter) }.getOrNull() ?: return@forEach
                val endTime = runCatching {
                    period.timeEnd?.take(5)?.let { LocalTime.parse(it, timeFormatter) } ?: startTime.plusMinutes(80)
                }.getOrDefault(startTime.plusMinutes(80))

                val startDateTime = LocalDateTime.of(lessonDate, startTime)
                val endDateTime = LocalDateTime.of(lessonDate, endTime)
                totalLessons += 1

                when {
                    now.isAfter(endDateTime) || now.isEqual(endDateTime) -> completedLessons += 1
                    now.isBefore(startDateTime) -> remainingLessons += 1
                    else -> ongoingLessons += 1
                }
            }
        }
    }

    val completedEquivalent = completedLessons + (ongoingLessons * 0.5f)
    val progress = if (totalLessons > 0) (completedEquivalent / totalLessons).coerceIn(0f, 1f) else 0f

    return ScheduleProgressStats(
        totalLessons = totalLessons,
        completedLessons = completedLessons,
        ongoingLessons = ongoingLessons,
        remainingLessons = remainingLessons,
        progress = progress
    )
}

private fun selectionStageTitle(raw: String): String {
    val normalized = raw
        .removePrefix("Оберіть ")
        .removePrefix("Виберіть ")
        .removePrefix("Select ")
        .trim()

    return when (normalized.lowercase(Locale.getDefault())) {
        "університет", "university" -> "Університет"
        "структуру", "structure" -> "Структура"
        "факультет", "faculty" -> "Факультет"
        "курс", "course" -> "Курс"
        "групу", "group" -> "Група"
        "студента", "student" -> "Студент"
        else -> normalized.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }
}

private const val XP_PER_COMPLETED_LESSON = 120
private const val XP_PER_LEVEL = 1_000

private data class LevelProgress(
    val level: Int,
    val xpIntoCurrentLevel: Int,
    val progressToNextLevel: Float
)

private fun calculateLevelProgress(totalXp: Int): LevelProgress {
    val normalizedXp = totalXp.coerceAtLeast(0)
    val level = (normalizedXp / XP_PER_LEVEL) + 1
    val xpIntoCurrentLevel = normalizedXp % XP_PER_LEVEL
    return LevelProgress(
        level = level,
        xpIntoCurrentLevel = xpIntoCurrentLevel,
        progressToNextLevel = xpIntoCurrentLevel.toFloat() / XP_PER_LEVEL.toFloat()
    )
}

@Composable
fun SelectionStepCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            content()
        }
    }
}

@Composable
fun ModernSelectableCard(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "selectable_card_scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .scale(cardScale),
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun EmptyScheduleCard(
    onLoadNextWeek: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(68.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(34.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Text(
                text = "Немає занять на обраний період",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Можливо, пари заплановані на наступних тижнях.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (onLoadNextWeek != null) {
                FilledTonalButton(
                    onClick = onLoadNextWeek,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.ExpandMore, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Завантажити наступні тижні")
                }
            }
        }
    }
}

@Composable
private fun LoadingStateCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(44.dp),
                strokeWidth = 4.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun ErrorStateCard(
    modifier: Modifier = Modifier,
    title: String,
    message: String,
    compact: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(if (compact) 20.dp else 28.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = if (compact) 14.dp else 22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Повторити")
                }
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Закрити",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

private fun loadingTitleFor(uiState: PublicScheduleUiState): String {
    return when {
        uiState.selectedStudent != null -> "Завантажуємо розклад студента"
        uiState.selectedGroup != null -> "Підбираємо дані групи"
        uiState.selectedCourse != null -> "Показуємо групи курсу"
        uiState.selectedFaculty != null -> "Показуємо курси факультету"
        uiState.selectedStructure != null -> "Показуємо факультети"
        else -> "Підключаємося до сервера"
    }
}

private fun loadingSubtitleFor(uiState: PublicScheduleUiState): String {
    return when {
        uiState.selectedStudent != null -> "Готуємо заняття, час і аудиторії на найближчі дні."
        uiState.selectedGroup != null -> "Отримуємо список студентів і розклад."
        uiState.selectedCourse != null -> "Шукаємо всі групи цього курсу."
        uiState.selectedFaculty != null -> "Підтягуємо доступні курси факультету."
        uiState.selectedStructure != null -> "Оновлюємо перелік факультетів і напрямів."
        else -> "Перевіряємо доступність сервера університету."
    }
}

private fun retryCurrentStep(
    viewModel: PublicScheduleViewModel,
    uiState: PublicScheduleUiState
) {
    viewModel.clearError()
    when {
        uiState.selectedStudent != null -> viewModel.loadStudentSchedule(
            uiState.selectedStudent.id,
            uiState.currentWeekOffset
        )
        uiState.selectedGroup != null && uiState.students.isNotEmpty() && uiState.schedule.isEmpty() -> {
            viewModel.loadStudentsByGroup(uiState.selectedGroup.id)
        }
        uiState.selectedGroup != null -> viewModel.loadGroupSchedule(
            uiState.selectedGroup.id,
            uiState.currentWeekOffset
        )
        uiState.selectedCourse != null && uiState.selectedFaculty != null -> viewModel.loadGroups(
            uiState.selectedFaculty.id,
            uiState.selectedCourse.course
        )
        uiState.selectedFaculty != null -> viewModel.loadCourses(uiState.selectedFaculty.id)
        uiState.selectedStructure != null -> viewModel.loadFaculties(uiState.selectedStructure.id)
        else -> viewModel.loadStructures()
    }
}

private data class FlatPeriodEntry(
    val lessonNumber: Int,
    val period: TimeTablePeriod
)

@Composable
fun ModernDayScheduleCard(
    daySchedule: TimeTableDate,
    groupId: Int,
    viewModel: PublicScheduleViewModel
) {
    val strings = LocalStrings.current
    val currentLocale = remember(strings) {
        if (strings is com.mkr.schedule.util.localization.EnglishStrings) Locale.ENGLISH else Locale("uk", "UA")
    }
    val dateFormat = remember(currentLocale) { SimpleDateFormat("d MMMM yyyy", currentLocale) }
    val dayFormat = remember(currentLocale) { SimpleDateFormat("EEEE", currentLocale) }
    val isoFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    val cleanDateStr = remember(daySchedule.date) { daySchedule.date.take(10) }
    val date = remember(cleanDateStr) {
        try {
            isoFormat.parse(cleanDateStr)
        } catch (e: Exception) {
            null
        }
    }
    val todayStr = remember { isoFormat.format(Date()) }
    val isToday = cleanDateStr == todayStr

    val flatPeriods = remember(daySchedule.lessons) {
        daySchedule.lessons.flatMap { lesson ->
            lesson.periods.map { period ->
                FlatPeriodEntry(lessonNumber = lesson.number, period = period)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Day Section Header (No outer card or border)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (date != null) {
                                dayFormat.format(date).replaceFirstChar { it.uppercase() }
                            } else {
                                cleanDateStr
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (isToday) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = strings.today,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    if (date != null) {
                        Text(
                            text = dateFormat.format(date),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = strings.formatPairsCount(flatPeriods.size),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        // Grouped Lesson Cards for the Day with Smart Window / Break Indicators (#3)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            flatPeriods.forEachIndexed { idx, entry ->
                if (idx > 0) {
                    val prevEntry = flatPeriods[idx - 1]
                    val gapInfo = remember(prevEntry, entry, strings) {
                        computeLessonGapInfo(prevEntry, entry, strings)
                    }
                    if (gapInfo != null) {
                        LessonGapIndicator(gapInfo = gapInfo, strings = strings)
                    }
                }
                val itemShape = groupedCardShape(idx, flatPeriods.size)
                key("${cleanDateStr}_${entry.lessonNumber}_${entry.period.disciplineId}_${entry.period.timeStart}_$idx") {
                    ModernLessonCard(
                        period = entry.period,
                        lessonNumber = entry.lessonNumber,
                        date = cleanDateStr,
                        groupId = groupId,
                        shape = itemShape,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

private data class LessonGapInfo(
    val isWindow: Boolean,
    val title: String,
    val durationText: String,
    val untilTime: String
)

private fun parseTimeMinutes(timeStr: String?): Int? {
    val clean = timeStr?.take(5) ?: return null
    val parts = clean.split(":")
    if (parts.size < 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    return h * 60 + m
}

private fun formatGapDuration(minutes: Int, strings: com.mkr.schedule.util.localization.Strings): String {
    val h = minutes / 60
    val m = minutes % 60
    val isEn = strings is com.mkr.schedule.util.localization.EnglishStrings
    val hWord = if (isEn) "h" else "год"
    val mWord = if (isEn) "min" else "хв"
    return when {
        h > 0 && m > 0 -> "$h $hWord $m $mWord"
        h > 0 -> "$h $hWord"
        else -> "$m $mWord"
    }
}

private fun computeLessonGapInfo(prev: FlatPeriodEntry, curr: FlatPeriodEntry, strings: com.mkr.schedule.util.localization.Strings): LessonGapInfo? {
    val prevEndMin = parseTimeMinutes(prev.period.timeEnd) ?: return null
    val currStartMin = parseTimeMinutes(curr.period.timeStart) ?: return null
    val gapMinutes = currStartMin - prevEndMin
    if (gapMinutes < 30) return null

    val currStartClean = curr.period.timeStart?.take(5) ?: ""
    val skippedPairs = if (prev.lessonNumber > 0 && curr.lessonNumber > prev.lessonNumber) {
        (curr.lessonNumber - prev.lessonNumber - 1).coerceAtLeast(0)
    } else 0

    return if (skippedPairs >= 1 || gapMinutes >= 50) {
        LessonGapInfo(
            isWindow = true,
            title = strings.formatWindowWithPairs(skippedPairs),
            durationText = formatGapDuration(gapMinutes, strings),
            untilTime = currStartClean
        )
    } else {
        LessonGapInfo(
            isWindow = false,
            title = strings.longBreak,
            durationText = formatGapDuration(gapMinutes, strings),
            untilTime = currStartClean
        )
    }
}

@Composable
private fun LessonGapIndicator(gapInfo: LessonGapInfo, strings: com.mkr.schedule.util.localization.Strings) {
    val containerColor = if (gapInfo.isWindow) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f)
    }
    val contentColor = if (gapInfo.isWindow) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = contentColor
                )
                Text(
                    text = gapInfo.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
            }
            Text(
                text = buildString {
                    append(gapInfo.durationText)
                    if (gapInfo.untilTime.isNotEmpty()) {
                        append(" • ${strings.untilTimePrefix} ${gapInfo.untilTime}")
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = contentColor.copy(alpha = 0.88f)
            )
        }
    }
}

private fun formatLessonsCount(count: Int): String {
    val rem100 = count % 100
    val rem10 = count % 10
    val word = when {
        rem100 in 11..14 -> "пар"
        rem10 == 1 -> "пара"
        rem10 in 2..4 -> "пари"
        else -> "пар"
    }
    return "$count $word"
}

private fun groupedCardShape(index: Int, count: Int): Shape {
    val outer = 24.dp
    val inner = 8.dp
    return when {
        count <= 1 -> RoundedCornerShape(outer)
        index == 0 -> RoundedCornerShape(topStart = outer, topEnd = outer, bottomStart = inner, bottomEnd = inner)
        index == count - 1 -> RoundedCornerShape(topStart = inner, topEnd = inner, bottomStart = outer, bottomEnd = outer)
        else -> RoundedCornerShape(inner)
    }
}

@Composable
fun ModernLessonCard(
    period: TimeTablePeriod,
    lessonNumber: Int,
    date: String,
    groupId: Int,
    shape: Shape = RoundedCornerShape(22.dp),
    viewModel: PublicScheduleViewModel
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val shortName = remember(period, strings) { strings.translateDiscipline(period.disciplineShortName?.trim()) }
    val fullName = remember(period, strings) { strings.translateDiscipline(period.disciplineFullName?.trim()) }
    val primaryTitle = remember(shortName, fullName) {
        when {
            fullName.isNotEmpty() && fullName.length <= 68 -> fullName
            shortName.isNotEmpty() -> shortName
            fullName.isNotEmpty() -> fullName
            else -> strings.disciplineFallback
        }
    }
    val secondaryTitle = remember(shortName, fullName, primaryTitle) {
        if (primaryTitle == shortName && fullName.isNotEmpty() && fullName != shortName) {
            fullName
        } else {
            null
        }
    }

    val classroom = remember(period) { period.classroom?.trim() ?: "" }
    val lessonKind = remember(period, shortName, fullName) {
        resolveLessonKind(period, shortName, fullName)
    }

    val isOnline = remember(classroom) {
        classroom.contains("ONLINE", ignoreCase = true) ||
            classroom.contains("ON-LINE", ignoreCase = true) ||
            classroom.contains("ON LINE", ignoreCase = true) ||
            classroom.contains("ДИСТАНЦ", ignoreCase = true)
    }

    val typeBadgeBg = when (lessonKind) {
        LessonKind.LECTURE -> MaterialTheme.colorScheme.tertiaryContainer
        LessonKind.PRACTICE -> MaterialTheme.colorScheme.primaryContainer
        LessonKind.LAB, LessonKind.OTHER -> MaterialTheme.colorScheme.secondaryContainer
    }
    val typeBadgeFg = when (lessonKind) {
        LessonKind.LECTURE -> MaterialTheme.colorScheme.onTertiaryContainer
        LessonKind.PRACTICE -> MaterialTheme.colorScheme.onPrimaryContainer
        LessonKind.LAB, LessonKind.OTHER -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    val lessonKindLabel = when (lessonKind) {
        LessonKind.LECTURE -> strings.lecture
        LessonKind.PRACTICE -> strings.practice
        LessonKind.LAB -> strings.laboratory
        LessonKind.OTHER -> strings.lessonDefault
    }

    var progress by remember { mutableStateOf(0f) }
    var isActive by remember { mutableStateOf(false) }

    val startClean = remember(period.timeStart) { period.timeStart?.take(5) ?: "--:--" }
    val endClean = remember(period.timeEnd) { period.timeEnd?.take(5) ?: "" }

    LaunchedEffect(date, startClean, endClean) {
        if (period.timeStart == null || period.timeEnd == null) return@LaunchedEffect
        while (true) {
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayStr = dateFormat.format(Date())
                if (date == todayStr) {
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val start = timeFormat.parse(startClean)
                    val end = timeFormat.parse(endClean)
                    val current = timeFormat.parse(timeFormat.format(Date()))

                    if (start != null && end != null && current != null) {
                        if (current.time in start.time..end.time) {
                            isActive = true
                            val totalDuration = (end.time - start.time).coerceAtLeast(1L)
                            val elapsed = current.time - start.time
                            progress = (elapsed.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
                        } else {
                            isActive = false
                        }
                    }
                } else {
                    isActive = false
                }
            } catch (_: Exception) {
            }
            delay(30_000L)
        }
    }

    // Notes & Conference Link State
    val lessonKey = "note_${date}_${period.disciplineId ?: period.disciplineShortName}_${startClean}"
    val notesMap by viewModel.notesMap.collectAsState()
    val savedNote = notesMap[lessonKey] ?: ""
    val conferenceLink by remember(groupId, period.disciplineId) {
        viewModel.getConferenceLinkFlow(groupId, period.disciplineId)
    }.collectAsState(initial = null)

    var isExpanded by rememberSaveable(lessonKey) { mutableStateOf(false) }
    var noteDraft by remember(savedNote) { mutableStateOf(savedNote) }
    var linkDraft by remember(conferenceLink) { mutableStateOf(conferenceLink ?: "") }
    var showLinkEditor by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "lesson_card_scale"
    )

    val targetCardBg = if (isActive) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        lerp(
            MaterialTheme.colorScheme.surfaceContainer,
            MaterialTheme.colorScheme.surfaceContainerHigh,
            0.55f
        )
    }

    val cardContainerColor by animateColorAsState(
        targetValue = targetCardBg,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "lesson_card_bg"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .animateContentSize(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()),
        onClick = { isExpanded = !isExpanded },
        interactionSource = interactionSource,
        shape = shape,
        color = cardContainerColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                // LEFT TIME BLOCK: Prominent Start Time, Subtle Secondary End Time
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    modifier = Modifier.widthIn(min = 76.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Start Time — Hero Accent
                        Text(
                            text = startClean,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isActive) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        // End Time — Subtle & Unobtrusive
                        if (endClean.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${strings.untilTimePrefix} $endClean",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = if (isActive) {
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }

                // RIGHT CONTENT BLOCK: Badges, Subject Title, Clean Metadata
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Top Row: Type Badge + Pair Number + Active/Note/Link Indicators
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = typeBadgeBg
                        ) {
                            Text(
                                text = lessonKindLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = typeBadgeFg,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        if (lessonNumber > 0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest
                            ) {
                                Text(
                                    text = "$lessonNumber ${strings.pairLabel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        if (isOnline) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VideoCall,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = strings.onlineBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        } else if (classroom.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = classroom,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Subject Name: Clear & Readable
                    Text(
                        text = primaryTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (isExpanded) 5 else 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (secondaryTitle != null && isExpanded) {
                        Text(
                            text = secondaryTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Teacher Name: Clean Inline Row without Box Clutter
                    val teacherName = period.teachersNameFull?.takeIf { it.isNotBlank() }
                        ?: period.teachersName?.takeIf { it.isNotBlank() }
                    if (teacherName != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = teacherName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (isExpanded) 3 else 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Smart Chips for Links, Deadlines & Notes (#5)
                    val smartNote = remember(savedNote, conferenceLink) {
                        parseSmartNoteData(savedNote, conferenceLink)
                    }
                    if (!isExpanded && (smartNote.links.isNotEmpty() || smartNote.deadlines.isNotEmpty() || smartNote.cleanText.isNotBlank())) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            smartNote.links.forEach { linkChip ->
                                Surface(
                                    onClick = { openUrlSafely(context, linkChip.url) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = linkChip.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = linkChip.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                            smartNote.deadlines.forEach { deadlineLabel ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            text = deadlineLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                            if (smartNote.cleanText.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.75f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        Text(
                                            text = smartNote.cleanText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // EXPANDED PANEL: Notes & Online Conference Link (Zero Clutter When Collapsed!)
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()) +
                    expandVertically(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()),
                exit = fadeOut(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()) +
                    shrinkVertically(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Conference Link Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (!conferenceLink.isNullOrBlank()) conferenceLink!! else strings.conferenceLinkPlaceholder,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (!conferenceLink.isNullOrBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (!conferenceLink.isNullOrBlank()) {
                                FilledTonalButton(
                                    onClick = { openUrlSafely(context, conferenceLink!!) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Відкрити", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            IconButton(
                                onClick = {
                                    linkDraft = conferenceLink ?: ""
                                    showLinkEditor = !showLinkEditor
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (conferenceLink.isNullOrBlank()) Icons.Default.AddLink else Icons.Default.Edit,
                                    contentDescription = "Редагувати посилання",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (showLinkEditor) {
                        OutlinedTextField(
                            value = linkDraft,
                            onValueChange = { linkDraft = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("https://zoom.us/j/... або meet.google.com/...", style = MaterialTheme.typography.bodySmall) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            trailingIcon = {
                                Row {
                                    if (linkDraft.isNotBlank()) {
                                        IconButton(onClick = {
                                            viewModel.deleteConferenceLink(groupId, period.disciplineId)
                                            linkDraft = ""
                                            showLinkEditor = false
                                        }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Видалити")
                                        }
                                    }
                                    IconButton(onClick = {
                                        viewModel.saveConferenceLink(groupId, period.disciplineId, linkDraft)
                                        showLinkEditor = false
                                    }) {
                                        Icon(Icons.Default.Check, contentDescription = "Зберегти")
                                    }
                                }
                            },
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Encrypted Note Field with Smart Chips recognition (#5)
                    val draftSmartNote = remember(noteDraft, conferenceLink) {
                        parseSmartNoteData(noteDraft, conferenceLink)
                    }
                    if (draftSmartNote.links.isNotEmpty() || draftSmartNote.deadlines.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            draftSmartNote.links.forEach { linkChip ->
                                Surface(
                                    onClick = { openUrlSafely(context, linkChip.url) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = linkChip.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = linkChip.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                            draftSmartNote.deadlines.forEach { deadlineLabel ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            text = deadlineLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = noteDraft,
                        onValueChange = { noteDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "Нотатка, посилання (Zoom/Meet/Drive/TG) або дедлайн (до 15.11)...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        trailingIcon = {
                            if (noteDraft != savedNote) {
                                IconButton(
                                    onClick = {
                                        viewModel.saveNote(lessonKey, noteDraft)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Зберегти нотатку",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else if (savedNote.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        noteDraft = ""
                                        viewModel.saveNote(lessonKey, "")
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Очистити нотатку"
                                    )
                                }
                            }
                        },
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private data class SmartLinkChip(
    val label: String,
    val url: String,
    val icon: ImageVector
)

private data class SmartNoteData(
    val links: List<SmartLinkChip>,
    val deadlines: List<String>,
    val cleanText: String
)

private val URL_EXTRACT_REGEX = Regex(
    """(?:https?://[^\s,;)"]+)|(?:(?:meet\.google\.com|zoom\.us|us\d+web\.zoom\.us|teams\.microsoft\.com|teams\.live\.com|t\.me|telegram\.me|drive\.google\.com|docs\.google\.com|classroom\.google\.com|dl\.knute\.edu\.ua|github\.com|figma\.com)/[^\s,;)"]*)""",
    RegexOption.IGNORE_CASE
)

private val DEADLINE_DATE_REGEX = Regex(
    """(?:(до|на|дедлайн|здати|захист|екзамен|залік|тест|кр|контрольна|модуль|лаба|лр)\s*:?\s*)?(\b\d{1,2}[./]\d{1,2}(?:[./]\d{2,4})?\b)""",
    RegexOption.IGNORE_CASE
)

private val TASK_KEYWORD_REGEX = Regex(
    """\b(дедлайн|контрольна|модуль|захист|екзамен|залік|колоквіум)\b""",
    RegexOption.IGNORE_CASE
)

private fun classifySmartLink(rawUrl: String): SmartLinkChip {
    val lower = rawUrl.lowercase(Locale.getDefault())
    return when {
        lower.contains("meet.google.com") -> SmartLinkChip("Google Meet", rawUrl, Icons.Default.VideoCall)
        lower.contains("zoom.us") -> SmartLinkChip("Zoom", rawUrl, Icons.Default.VideoCall)
        lower.contains("teams.microsoft.com") || lower.contains("teams.live.com") -> SmartLinkChip("MS Teams", rawUrl, Icons.Default.VideoCall)
        lower.contains("t.me") || lower.contains("telegram.me") -> SmartLinkChip("Telegram", rawUrl, Icons.AutoMirrored.Filled.OpenInNew)
        lower.contains("drive.google.com") || lower.contains("docs.google.com") -> SmartLinkChip("Google Диск", rawUrl, Icons.Default.Link)
        lower.contains("classroom.google.com") -> SmartLinkChip("Classroom", rawUrl, Icons.Default.School)
        lower.contains("knute.edu.ua") || lower.contains("moodle") -> SmartLinkChip("Moodle / ДН", rawUrl, Icons.Default.School)
        lower.contains("github.com") -> SmartLinkChip("GitHub", rawUrl, Icons.AutoMirrored.Filled.OpenInNew)
        else -> SmartLinkChip("Посилання", rawUrl, Icons.AutoMirrored.Filled.OpenInNew)
    }
}

private fun parseSmartNoteData(note: String, conferenceLink: String?): SmartNoteData {
    val links = mutableListOf<SmartLinkChip>()
    val seenUrls = mutableSetOf<String>()

    if (!conferenceLink.isNullOrBlank()) {
        val trimmed = conferenceLink.trim()
        seenUrls.add(trimmed.lowercase(Locale.getDefault()))
        val classified = classifySmartLink(trimmed)
        val label = if (classified.label == "Посилання") "Приєднатися" else classified.label
        links.add(classified.copy(label = label))
    }

    if (note.isNotBlank()) {
        URL_EXTRACT_REGEX.findAll(note).forEach { match ->
            val url = match.value.trimEnd('.', ',', ';', ')')
            if (seenUrls.add(url.lowercase(Locale.getDefault()))) {
                links.add(classifySmartLink(url))
            }
        }
    }

    val deadlines = mutableListOf<String>()
    if (note.isNotBlank()) {
        val textWithoutUrls = URL_EXTRACT_REGEX.replace(note, " ")
        DEADLINE_DATE_REGEX.findAll(textWithoutUrls).forEach { match ->
            val prefix = match.groupValues[1].trim()
            val datePart = match.groupValues[2].trim()
            val chipText = when {
                prefix.isEmpty() -> "Дедлайн $datePart"
                prefix.equals("до", ignoreCase = true) -> "Дедлайн до $datePart"
                prefix.equals("на", ignoreCase = true) -> "На $datePart"
                else -> "${prefix.replaceFirstChar { it.uppercase() }} $datePart"
            }
            if (!deadlines.contains(chipText)) {
                deadlines.add(chipText)
            }
        }
        if (deadlines.isEmpty()) {
            TASK_KEYWORD_REGEX.findAll(textWithoutUrls).forEach { match ->
                val kw = match.value.replaceFirstChar { it.uppercase() }
                if (!deadlines.contains(kw)) {
                    deadlines.add(kw)
                }
            }
        }
    }

    val cleanText = if (note.isNotBlank()) {
        URL_EXTRACT_REGEX.replace(note, "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    } else ""

    return SmartNoteData(
        links = links,
        deadlines = deadlines,
        cleanText = cleanText
    )
}

private fun openUrlSafely(context: android.content.Context, rawUrl: String) {
    val trimmed = rawUrl.trim()
    if (trimmed.isEmpty()) return
    val normalized = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        "https://$trimmed"
    }
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: Exception) {
        LogCollector.log("PublicScheduleScreen", "Failed to open URL: ${e.message}")
    }
}

private enum class LessonKind {
    LECTURE,
    PRACTICE,
    LAB,
    OTHER
}

private fun resolveLessonKind(
    period: TimeTablePeriod,
    shortName: String,
    fullName: String
): LessonKind {
    val source = "$shortName $fullName".lowercase(Locale.getDefault())

    if (source.contains("лк") || source.contains("лекц")) return LessonKind.LECTURE
    if (source.contains("пз") || source.contains("практ") || source.contains("сем")) return LessonKind.PRACTICE
    if (source.contains("лб") || source.contains("лаб")) return LessonKind.LAB

    return when (period.type) {
        1 -> LessonKind.LECTURE
        2 -> LessonKind.PRACTICE
        3 -> LessonKind.LAB
        else -> LessonKind.OTHER
    }
}
