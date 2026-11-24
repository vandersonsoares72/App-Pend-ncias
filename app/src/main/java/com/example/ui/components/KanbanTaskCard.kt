package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.KanbanTask
import com.example.data.TaskPriority
import com.example.data.TaskStatus
import com.example.ui.theme.LocalKanbanColors
import com.example.ui.theme.PriorityLow
import com.example.ui.theme.PriorityMedium
import com.example.ui.util.formatScheduleBadge
import com.example.ui.util.isTaskOverdue

@Composable
fun KanbanTaskCard(
    task: KanbanTask,
    onCardClick: () -> Unit,
    onMoveStatus: (TaskStatus) -> Unit,
    modifier: Modifier = Modifier,
    showStatusPill: Boolean = false
) {
    val kanbanColors = LocalKanbanColors.current
    val status = task.statusEnum()
    val priority = task.priorityEnum()
    val overdue = remember(task.scheduledAt, task.status) { isTaskOverdue(task) }
    val scheduleText = remember(task.scheduledAt) { formatScheduleBadge(task.scheduledAt) }

    val stageAccent = when (status) {
        TaskStatus.PENDENTE -> kanbanColors.pendingAccent
        TaskStatus.INICIADO -> kanbanColors.startedAccent
        TaskStatus.CONCLUIDO -> kanbanColors.doneAccent
    }

    val priorityColor = when (priority) {
        TaskPriority.ALTA -> kanbanColors.highPriority
        TaskPriority.MEDIA -> PriorityMedium
        TaskPriority.BAIXA -> PriorityLow
    }

    val priorityLabel = when (priority) {
        TaskPriority.ALTA -> stringResource(R.string.priority_high)
        TaskPriority.MEDIA -> stringResource(R.string.priority_medium)
        TaskPriority.BAIXA -> stringResource(R.string.priority_low)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (overdue) kanbanColors.highPriority.copy(alpha = 0.45f) else kanbanColors.cardBorder
        ),
        shadowElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onCardClick)
            .testTag("task_card_${task.id}")
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Vertical Stage Accent Spine
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(stageAccent)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                // Top Meta Row: Category, Priority, optional Stage badge, Edit Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = task.category.ifBlank { stringResource(R.string.cat_work) },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Priority Dot + Label
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(priorityColor.copy(alpha = 0.12f))
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(priorityColor)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = priorityLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = priorityColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (showStatusPill) {
                        val (statusLabel, statusContainer, statusTextColor) = when (status) {
                            TaskStatus.PENDENTE -> Triple(
                                stringResource(R.string.status_pending),
                                kanbanColors.pendingContainer,
                                kanbanColors.onPendingContainer
                            )
                            TaskStatus.INICIADO -> Triple(
                                stringResource(R.string.status_started),
                                kanbanColors.startedContainer,
                                kanbanColors.onStartedContainer
                            )
                            TaskStatus.CONCLUIDO -> Triple(
                                stringResource(R.string.status_done),
                                kanbanColors.doneContainer,
                                kanbanColors.onDoneContainer
                            )
                        }
                        Surface(
                            color = statusContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusTextColor,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    IconButton(
                        onClick = onCardClick,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("edit_task_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.action_edit),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Title
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (status == TaskStatus.CONCLUIDO) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textDecoration = if (status == TaskStatus.CONCLUIDO) {
                        TextDecoration.LineThrough
                    } else {
                        TextDecoration.None
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Optional Description Preview
                if (task.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Scheduled Date & Time Row
                if (scheduleText != null) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        color = if (overdue) {
                            kanbanColors.highPriorityContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = if (overdue) Icons.Rounded.WarningAmber else Icons.Rounded.CalendarToday,
                                contentDescription = null,
                                tint = if (overdue) kanbanColors.highPriority else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (overdue) {
                                    "${stringResource(R.string.badge_overdue)} • $scheduleText"
                                } else {
                                    scheduleText
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (overdue) kanbanColors.highPriority else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Quick Kanban Stage Progression Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (status) {
                        TaskStatus.PENDENTE -> {
                            FilledTonalButton(
                                onClick = { onMoveStatus(TaskStatus.INICIADO) },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = kanbanColors.startedContainer,
                                    contentColor = kanbanColors.onStartedContainer
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("start_task_${task.id}")
                            ) {
                                Text(
                                    text = stringResource(R.string.action_start),
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        TaskStatus.INICIADO -> {
                            OutlinedButton(
                                onClick = { onMoveStatus(TaskStatus.PENDENTE) },
                                border = BorderStroke(1.dp, kanbanColors.cardBorder),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("revert_task_${task.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.action_back_pending),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            FilledTonalButton(
                                onClick = { onMoveStatus(TaskStatus.CONCLUIDO) },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = kanbanColors.doneContainer,
                                    contentColor = kanbanColors.onDoneContainer
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("complete_task_${task.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.action_complete),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        TaskStatus.CONCLUIDO -> {
                            OutlinedButton(
                                onClick = { onMoveStatus(TaskStatus.INICIADO) },
                                border = BorderStroke(1.dp, kanbanColors.cardBorder),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("reopen_task_${task.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Replay,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.action_reopen),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
