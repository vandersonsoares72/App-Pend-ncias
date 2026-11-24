package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ViewKanban
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.material.icons.rounded.ViewKanban
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.auth.signOut
import com.example.data.TaskDraft
import com.example.data.TaskRepository
import com.example.data.TaskStatus
import com.example.ui.BoardSnapshot
import com.example.ui.KanbanFeedback
import com.example.ui.KanbanViewModel
import com.example.ui.TaskFilter
import com.example.ui.UiState
import com.example.ui.components.TaskEditorSheet
import com.example.ui.theme.LocalKanbanColors
import com.google.firebase.firestore.FirebaseFirestore

private enum class MainTab {
    BOARD,
    SCHEDULE
}

@Composable
fun MainKanbanScreen(
    currentUserId: String,
    userDisplayName: String?,
    userEmail: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: KanbanViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                KanbanViewModel(TaskRepository(db), currentUserId)
            }
        }
    )

    val boardState by viewModel.boardState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeFilter by viewModel.activeFilter.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    var activeTab by rememberSaveable { mutableStateOf(MainTab.BOARD) }
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }
    var isBoardOverviewMode by rememberSaveable { mutableStateOf(false) }
    var editorDraft by remember { mutableStateOf<TaskDraft?>(null) }
    var showAccountDialog by remember { mutableStateOf(false) }

    // Back handling for secondary tab or search mode
    BackHandler(enabled = isSearchExpanded || activeTab != MainTab.BOARD) {
        when {
            isSearchExpanded -> {
                isSearchExpanded = false
                viewModel.onSearchQueryChanged("")
            }
            activeTab != MainTab.BOARD -> {
                activeTab = MainTab.BOARD
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.feedback.collect { event ->
            val msgRes = when (event) {
                KanbanFeedback.TASK_CREATED -> R.string.msg_task_created
                KanbanFeedback.TASK_UPDATED -> R.string.msg_task_updated
                KanbanFeedback.TASK_DELETED -> R.string.msg_task_deleted
                KanbanFeedback.MOVED_PENDING -> R.string.msg_moved_pending
                KanbanFeedback.MOVED_STARTED -> R.string.msg_moved_started
                KanbanFeedback.MOVED_DONE -> R.string.msg_moved_done
                KanbanFeedback.SAVE_ERROR -> R.string.error_save_task
            }
            snackbarHostState.showSnackbar(context.getString(msgRes))
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 800.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { editorDraft = TaskDraft() },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.fab_new_task),
                            style = MaterialTheme.typography.titleSmall
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.testTag("new_task_fab")
                )
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        NavigationBarItem(
                            selected = activeTab == MainTab.BOARD,
                            onClick = { activeTab = MainTab.BOARD },
                            icon = {
                                Icon(
                                    imageVector = if (activeTab == MainTab.BOARD) {
                                        Icons.Rounded.ViewKanban
                                    } else {
                                        Icons.Outlined.ViewKanban
                                    },
                                    contentDescription = stringResource(R.string.nav_board)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_board)) },
                            modifier = Modifier.testTag("nav_tab_board")
                        )
                        NavigationBarItem(
                            selected = activeTab == MainTab.SCHEDULE,
                            onClick = { activeTab = MainTab.SCHEDULE },
                            icon = {
                                Icon(
                                    imageVector = if (activeTab == MainTab.SCHEDULE) {
                                        Icons.Rounded.CalendarMonth
                                    } else {
                                        Icons.Outlined.CalendarMonth
                                    },
                                    contentDescription = stringResource(R.string.nav_schedule)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_schedule)) },
                            modifier = Modifier.testTag("nav_tab_schedule")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(Modifier.height(12.dp))
                        NavigationRailItem(
                            selected = activeTab == MainTab.BOARD,
                            onClick = { activeTab = MainTab.BOARD },
                            icon = {
                                Icon(
                                    imageVector = if (activeTab == MainTab.BOARD) {
                                        Icons.Rounded.ViewKanban
                                    } else {
                                        Icons.Outlined.ViewKanban
                                    },
                                    contentDescription = stringResource(R.string.nav_board)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_board)) },
                            modifier = Modifier.testTag("nav_rail_board")
                        )
                        NavigationRailItem(
                            selected = activeTab == MainTab.SCHEDULE,
                            onClick = { activeTab = MainTab.SCHEDULE },
                            icon = {
                                Icon(
                                    imageVector = if (activeTab == MainTab.SCHEDULE) {
                                        Icons.Rounded.CalendarMonth
                                    } else {
                                        Icons.Outlined.CalendarMonth
                                    },
                                    contentDescription = stringResource(R.string.nav_schedule)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_schedule)) },
                            modifier = Modifier.testTag("nav_rail_schedule")
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    val currentSnapshot = (boardState as? UiState.Success)?.data
                    StudioHeader(
                        snapshot = currentSnapshot,
                        activeTab = activeTab,
                        showOverviewToggle = !useNavRail && activeTab == MainTab.BOARD,
                        isBoardOverviewMode = isBoardOverviewMode,
                        isSearchExpanded = isSearchExpanded,
                        searchQuery = searchQuery,
                        activeFilter = activeFilter,
                        onToggleOverview = { isBoardOverviewMode = !isBoardOverviewMode },
                        onToggleSearch = {
                            isSearchExpanded = !isSearchExpanded
                            if (!isSearchExpanded) viewModel.onSearchQueryChanged("")
                        },
                        onSearchQueryChange = viewModel::onSearchQueryChanged,
                        onFilterSelected = viewModel::onFilterSelected,
                        onAccountClick = { showAccountDialog = true }
                    )

                    when (val state = boardState) {
                        is UiState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        is UiState.Error -> {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ErrorOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.error_load_tasks),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            text = state.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }

                        is UiState.Success -> {
                            when (activeTab) {
                                MainTab.BOARD -> {
                                    KanbanBoardContent(
                                        snapshot = state.data,
                                        isBoardOverviewMode = isBoardOverviewMode,
                                        onTaskClick = { task ->
                                            editorDraft = TaskDraft(
                                                id = task.id,
                                                title = task.title,
                                                description = task.description,
                                                status = task.statusEnum(),
                                                priority = task.priorityEnum(),
                                                category = task.category,
                                                scheduledAtMillis = task.scheduledAt?.toDate()?.time
                                            )
                                        },
                                        onMoveTask = viewModel::moveTask,
                                        onQuickAddForStatus = { status ->
                                            editorDraft = TaskDraft(status = status)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                MainTab.SCHEDULE -> {
                                    ScheduleContent(
                                        tasks = state.data.filteredTasks,
                                        onTaskClick = { task ->
                                            editorDraft = TaskDraft(
                                                id = task.id,
                                                title = task.title,
                                                description = task.description,
                                                status = task.statusEnum(),
                                                priority = task.priorityEnum(),
                                                category = task.category,
                                                scheduledAtMillis = task.scheduledAt?.toDate()?.time
                                            )
                                        },
                                        onMoveTask = viewModel::moveTask,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val activeDraft = editorDraft
    if (activeDraft != null) {
        TaskEditorSheet(
            initialDraft = activeDraft,
            isSaving = isSaving,
            onDismiss = { if (!isSaving) editorDraft = null },
            onSave = { updatedDraft ->
                viewModel.saveTask(updatedDraft) {
                    editorDraft = null
                }
            },
            onDelete = if (!activeDraft.id.isNullOrBlank()) {
                { id ->
                    viewModel.deleteTask(id) {
                        editorDraft = null
                    }
                }
            } else null
        )
    }

    if (showAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = { Text(stringResource(R.string.account_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.account_signed_in_as),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!userDisplayName.isNullOrBlank()) {
                        Text(
                            text = userDisplayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (!userEmail.isNullOrBlank()) {
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAccountDialog = false
                        signOut(credentialManager = credentialManager, scope = scope)
                    },
                    modifier = Modifier.testTag("sign_out_confirm_button")
                ) {
                    Text(
                        text = stringResource(R.string.account_sign_out),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccountDialog = false }) {
                    Text(stringResource(R.string.account_close))
                }
            }
        )
    }
}

@Composable
private fun StudioHeader(
    snapshot: BoardSnapshot?,
    activeTab: MainTab,
    showOverviewToggle: Boolean,
    isBoardOverviewMode: Boolean,
    isSearchExpanded: Boolean,
    searchQuery: String,
    activeFilter: TaskFilter,
    onToggleOverview: () -> Unit,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFilterSelected: (TaskFilter) -> Unit,
    onAccountClick: () -> Unit
) {
    val kanbanColors = LocalKanbanColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        // Top Bar Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        if (activeTab == MainTab.BOARD) R.string.app_name else R.string.nav_schedule
                    ),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (snapshot != null && snapshot.totalCount > 0) {
                    Text(
                        text = stringResource(
                            R.string.header_progress_label,
                            snapshot.completedCount,
                            snapshot.totalCount
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (showOverviewToggle) {
                IconButton(
                    onClick = onToggleOverview,
                    modifier = Modifier.testTag("toggle_board_layout_button")
                ) {
                    Icon(
                        imageVector = if (isBoardOverviewMode) {
                            Icons.Rounded.ViewCarousel
                        } else {
                            Icons.Rounded.ViewColumn
                        },
                        contentDescription = stringResource(R.string.header_layout_compact_desc),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            IconButton(
                onClick = onToggleSearch,
                modifier = Modifier.testTag("toggle_search_button")
            ) {
                Icon(
                    imageVector = if (isSearchExpanded) Icons.Rounded.Close else Icons.Rounded.Search,
                    contentDescription = stringResource(
                        if (isSearchExpanded) R.string.header_close_search_desc else R.string.header_search_desc
                    ),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Surface(
                onClick = onAccountClick,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(1.dp, kanbanColors.cardBorder),
                modifier = Modifier
                    .size(40.dp)
                    .testTag("account_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.AccountCircle,
                        contentDescription = stringResource(R.string.header_account_desc),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Progress Bar when tasks exist
        if (snapshot != null && snapshot.totalCount > 0) {
            val progress = snapshot.completedCount.toFloat() / snapshot.totalCount.toFloat()
            LinearProgressIndicator(
                progress = { progress },
                color = kanbanColors.doneAccent,
                trackColor = kanbanColors.columnSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(100.dp))
            )
        } else {
            Spacer(Modifier.height(6.dp))
        }

        // Collapsible Search Input
        AnimatedVisibility(visible = isSearchExpanded) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text(stringResource(R.string.header_search_placeholder)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.header_close_search_desc)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("search_tasks_input")
            )
        }

        // Priority & Due Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf(
                TaskFilter.ALL to stringResource(R.string.priority_all),
                TaskFilter.TODAY_OR_OVERDUE to stringResource(R.string.filter_overdue_today),
                TaskFilter.HIGH to stringResource(R.string.priority_high),
                TaskFilter.MEDIUM to stringResource(R.string.priority_medium),
                TaskFilter.LOW to stringResource(R.string.priority_low)
            )
            filters.forEach { (filter, label) ->
                FilterChip(
                    selected = activeFilter == filter,
                    onClick = { onFilterSelected(filter) },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (activeFilter == filter) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                )
            }
        }
    }
}
