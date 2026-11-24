package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.KanbanTask
import com.example.data.TaskStatus
import com.example.ui.BoardSnapshot
import com.example.ui.components.KanbanTaskCard
import com.example.ui.theme.LocalKanbanColors
import kotlinx.coroutines.launch

@Composable
fun KanbanBoardContent(
    snapshot: BoardSnapshot,
    isBoardOverviewMode: Boolean,
    onTaskClick: (KanbanTask) -> Unit,
    onMoveTask: (String, TaskStatus) -> Unit,
    onQuickAddForStatus: (TaskStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val availableWidth = maxWidth
        val isExpanded = availableWidth >= 800.dp

        when {
            isExpanded -> {
                // Canonical 3-column side-by-side desktop/tablet Kanban board
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    KanbanColumnPane(
                        status = TaskStatus.PENDENTE,
                        tasks = snapshot.pendingTasks,
                        onTaskClick = onTaskClick,
                        onMoveTask = onMoveTask,
                        onQuickAdd = { onQuickAddForStatus(TaskStatus.PENDENTE) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    KanbanColumnPane(
                        status = TaskStatus.INICIADO,
                        tasks = snapshot.startedTasks,
                        onTaskClick = onTaskClick,
                        onMoveTask = onMoveTask,
                        onQuickAdd = { onQuickAddForStatus(TaskStatus.INICIADO) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    KanbanColumnPane(
                        status = TaskStatus.CONCLUIDO,
                        tasks = snapshot.doneTasks,
                        onTaskClick = onTaskClick,
                        onMoveTask = onMoveTask,
                        onQuickAdd = { onQuickAddForStatus(TaskStatus.CONCLUIDO) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }

            isBoardOverviewMode -> {
                // Horizontally scrollable side-by-side 3-column Kanban board on phones
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val colWidth = (availableWidth * 0.84f).coerceIn(280.dp, 360.dp)
                    KanbanColumnPane(
                        status = TaskStatus.PENDENTE,
                        tasks = snapshot.pendingTasks,
                        onTaskClick = onTaskClick,
                        onMoveTask = onMoveTask,
                        onQuickAdd = { onQuickAddForStatus(TaskStatus.PENDENTE) },
                        modifier = Modifier
                            .width(colWidth)
                            .fillMaxHeight()
                    )
                    KanbanColumnPane(
                        status = TaskStatus.INICIADO,
                        tasks = snapshot.startedTasks,
                        onTaskClick = onTaskClick,
                        onMoveTask = onMoveTask,
                        onQuickAdd = { onQuickAddForStatus(TaskStatus.INICIADO) },
                        modifier = Modifier
                            .width(colWidth)
                            .fillMaxHeight()
                    )
                    KanbanColumnPane(
                        status = TaskStatus.CONCLUIDO,
                        tasks = snapshot.doneTasks,
                        onTaskClick = onTaskClick,
                        onMoveTask = onMoveTask,
                        onQuickAdd = { onQuickAddForStatus(TaskStatus.CONCLUIDO) },
                        modifier = Modifier
                            .width(colWidth)
                            .fillMaxHeight()
                    )
                }
            }

            else -> {
                // Focused swipeable 3-column Kanban board with segmented stage tabs
                CompactSwipeableKanban(
                    snapshot = snapshot,
                    onTaskClick = onTaskClick,
                    onMoveTask = onMoveTask,
                    onQuickAddForStatus = onQuickAddForStatus
                )
            }
        }
    }
}

@Composable
private fun CompactSwipeableKanban(
    snapshot: BoardSnapshot,
    onTaskClick: (KanbanTask) -> Unit,
    onMoveTask: (String, TaskStatus) -> Unit,
    onQuickAddForStatus: (TaskStatus) -> Unit
) {
    val stages = TaskStatus.entries
    val pagerState = rememberPagerState(pageCount = { stages.size })
    val scope = rememberCoroutineScope()
    val kanbanColors = LocalKanbanColors.current

    Column(modifier = Modifier.fillMaxSize()) {
        // 3-Column Kanban Segmented Switcher
        Surface(
            color = kanbanColors.columnSurface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, kanbanColors.cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                stages.forEachIndexed { index, stage ->
                    val selected = pagerState.currentPage == index
                    val count = when (stage) {
                        TaskStatus.PENDENTE -> snapshot.pendingTasks.size
                        TaskStatus.INICIADO -> snapshot.startedTasks.size
                        TaskStatus.CONCLUIDO -> snapshot.doneTasks.size
                    }
                    val (label, accent, container, onContainer) = stagePresentation(stage)

                    Surface(
                        color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = if (selected) 1.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            }
                            .testTag("kanban_tab_${stage.wireValue.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(accent)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                            Spacer(Modifier.width(5.dp))
                            Surface(
                                color = if (selected) container else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) onContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Swipeable Kanban Columns
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            val stage = stages[page]
            val tasks = when (stage) {
                TaskStatus.PENDENTE -> snapshot.pendingTasks
                TaskStatus.INICIADO -> snapshot.startedTasks
                TaskStatus.CONCLUIDO -> snapshot.doneTasks
            }

            KanbanColumnPane(
                status = stage,
                tasks = tasks,
                onTaskClick = onTaskClick,
                onMoveTask = onMoveTask,
                onQuickAdd = { onQuickAddForStatus(stage) },
                showHeader = false,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun KanbanColumnPane(
    status: TaskStatus,
    tasks: List<KanbanTask>,
    onTaskClick: (KanbanTask) -> Unit,
    onMoveTask: (String, TaskStatus) -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true
) {
    val kanbanColors = LocalKanbanColors.current
    val (label, accent, container, onContainer) = stagePresentation(status)

    Surface(
        color = if (showHeader) kanbanColors.columnSurface.copy(alpha = 0.72f) else Color.Transparent,
        shape = RoundedCornerShape(22.dp),
        border = if (showHeader) BorderStroke(1.dp, kanbanColors.cardBorder) else null,
        modifier = modifier.testTag("kanban_column_${status.wireValue.lowercase()}")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (showHeader) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = container,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = tasks.size.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = onContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = onQuickAdd,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("column_add_${status.wireValue.lowercase()}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.fab_new_task),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (tasks.isEmpty()) {
                EmptyColumnPlaceholder(
                    status = status,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = if (showHeader) 10.dp else 0.dp,
                        end = if (showHeader) 10.dp else 0.dp,
                        top = 6.dp,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items = tasks, key = { it.id }) { task ->
                        KanbanTaskCard(
                            task = task,
                            onCardClick = { onTaskClick(task) },
                            onMoveStatus = { next -> onMoveTask(task.id, next) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyColumnPlaceholder(
    status: TaskStatus,
    modifier: Modifier = Modifier
) {
    val (_, accent, container, _) = stagePresentation(status)
    val icon: ImageVector = when (status) {
        TaskStatus.PENDENTE -> Icons.Rounded.Schedule
        TaskStatus.INICIADO -> Icons.Rounded.PlayCircle
        TaskStatus.CONCLUIDO -> Icons.Rounded.CheckCircle
    }
    val titleRes = when (status) {
        TaskStatus.PENDENTE -> R.string.empty_pending_title
        TaskStatus.INICIADO -> R.string.empty_started_title
        TaskStatus.CONCLUIDO -> R.string.empty_done_title
    }
    val bodyRes = when (status) {
        TaskStatus.PENDENTE -> R.string.empty_pending_body
        TaskStatus.INICIADO -> R.string.empty_started_body
        TaskStatus.CONCLUIDO -> R.string.empty_done_body
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = container,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(bodyRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private data class StagePresentation(
    val label: String,
    val accent: Color,
    val container: Color,
    val onContainer: Color
)

@Composable
private fun stagePresentation(status: TaskStatus): StagePresentation {
    val kanbanColors = LocalKanbanColors.current
    return when (status) {
        TaskStatus.PENDENTE -> StagePresentation(
            label = stringResource(R.string.status_pending),
            accent = kanbanColors.pendingAccent,
            container = kanbanColors.pendingContainer,
            onContainer = kanbanColors.onPendingContainer
        )
        TaskStatus.INICIADO -> StagePresentation(
            label = stringResource(R.string.status_started),
            accent = kanbanColors.startedAccent,
            container = kanbanColors.startedContainer,
            onContainer = kanbanColors.onStartedContainer
        )
        TaskStatus.CONCLUIDO -> StagePresentation(
            label = stringResource(R.string.status_done),
            accent = kanbanColors.doneAccent,
            container = kanbanColors.doneContainer,
            onContainer = kanbanColors.onDoneContainer
        )
    }
}
