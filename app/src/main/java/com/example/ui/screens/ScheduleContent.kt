package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.KanbanTask
import com.example.data.TaskStatus
import com.example.ui.components.KanbanTaskCard
import com.example.ui.theme.LocalKanbanColors
import com.example.ui.util.ScheduleBucket
import com.example.ui.util.classifyScheduleBucket

@Composable
fun ScheduleContent(
    tasks: List<KanbanTask>,
    onTaskClick: (KanbanTask) -> Unit,
    onMoveTask: (String, TaskStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val kanbanColors = LocalKanbanColors.current
    val grouped = remember(tasks) {
        val now = System.currentTimeMillis()
        val byBucket = tasks.groupBy { classifyScheduleBucket(it, now) }
        ScheduleBucket.entries.associateWith { bucket ->
            val list = byBucket[bucket].orEmpty()
            if (bucket == ScheduleBucket.DONE) {
                list.sortedByDescending { it.updatedAt?.seconds ?: 0L }
            } else {
                list.sortedBy { it.scheduledAt?.toDate()?.time ?: Long.MAX_VALUE }
            }
        }
    }

    if (tasks.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 360.dp)
            ) {
                Surface(
                    color = kanbanColors.startedContainer,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = kanbanColors.startedAccent,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.empty_schedule_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.empty_schedule_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val sections = listOf(
                Triple(ScheduleBucket.OVERDUE, R.string.schedule_section_overdue, Icons.Rounded.WarningAmber),
                Triple(ScheduleBucket.TODAY, R.string.schedule_section_today, Icons.Rounded.Today),
                Triple(ScheduleBucket.UPCOMING, R.string.schedule_section_upcoming, Icons.Rounded.CalendarMonth),
                Triple(ScheduleBucket.UNSCHEDULED, R.string.schedule_section_unscheduled, Icons.Rounded.EventNote),
                Triple(ScheduleBucket.DONE, R.string.schedule_section_done, Icons.Rounded.CheckCircle)
            )

            sections.forEach { (bucket, titleRes, icon) ->
                val bucketTasks = grouped[bucket].orEmpty()
                if (bucketTasks.isNotEmpty()) {
                    item(key = "header_${bucket.name}") {
                        val accent: Color = when (bucket) {
                            ScheduleBucket.OVERDUE -> kanbanColors.highPriority
                            ScheduleBucket.TODAY -> kanbanColors.pendingAccent
                            ScheduleBucket.UPCOMING -> kanbanColors.startedAccent
                            ScheduleBucket.UNSCHEDULED -> MaterialTheme.colorScheme.onSurfaceVariant
                            ScheduleBucket.DONE -> kanbanColors.doneAccent
                        }
                        ScheduleSectionHeader(
                            title = stringResource(titleRes),
                            count = bucketTasks.size,
                            icon = icon,
                            accent = accent
                        )
                    }
                    items(items = bucketTasks, key = { "sched_${it.id}" }) { task ->
                        KanbanTaskCard(
                            task = task,
                            onCardClick = { onTaskClick(task) },
                            onMoveStatus = { next -> onMoveTask(task.id, next) },
                            showStatusPill = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleSectionHeader(
    title: String,
    count: Int,
    icon: ImageVector,
    accent: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(8.dp))
        Surface(
            color = accent.copy(alpha = 0.14f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
            )
        }
    }
}
