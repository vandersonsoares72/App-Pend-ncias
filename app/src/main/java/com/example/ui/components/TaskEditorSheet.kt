package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.TaskDraft
import com.example.data.TaskPriority
import com.example.data.TaskStatus
import com.example.ui.theme.LocalKanbanColors
import com.example.ui.theme.PriorityLow
import com.example.ui.theme.PriorityMedium
import com.example.ui.util.combineUtcDateWithLocalTime
import com.example.ui.util.formatDateOnly
import com.example.ui.util.formatTimeOnly
import com.example.ui.util.quickScheduleMillis
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditorSheet(
    initialDraft: TaskDraft,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (TaskDraft) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val kanbanColors = LocalKanbanColors.current
    val isEditing = !initialDraft.id.isNullOrBlank()

    var title by remember(initialDraft) { mutableStateOf(initialDraft.title) }
    var description by remember(initialDraft) { mutableStateOf(initialDraft.description) }
    var status by remember(initialDraft) { mutableStateOf(initialDraft.status) }
    var priority by remember(initialDraft) { mutableStateOf(initialDraft.priority) }
    var category by remember(initialDraft) { mutableStateOf(initialDraft.category) }
    var scheduledAtMillis by remember(initialDraft) { mutableStateOf(initialDraft.scheduledAtMillis) }
    var showTitleError by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val categories = listOf(
        stringResource(R.string.cat_work),
        stringResource(R.string.cat_personal),
        stringResource(R.string.cat_study),
        stringResource(R.string.cat_project),
        stringResource(R.string.cat_finance),
        stringResource(R.string.cat_health)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(
                        if (isEditing) R.string.sheet_title_edit else R.string.sheet_title_new
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isEditing && onDelete != null) {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            enabled = !isSaving,
                            modifier = Modifier.testTag("delete_task_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_sheet_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.action_cancel)
                        )
                    }
                }
            }

            // Title Input
            OutlinedTextField(
                value = title,
                onValueChange = {
                    if (it.length <= 120) {
                        title = it
                        if (showTitleError && it.isNotBlank()) showTitleError = false
                    }
                },
                label = { Text(stringResource(R.string.field_title_label)) },
                placeholder = { Text(stringResource(R.string.field_title_placeholder)) },
                isError = showTitleError,
                supportingText = {
                    if (showTitleError) {
                        Text(stringResource(R.string.field_title_error))
                    } else {
                        Text("${title.length}/120")
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input")
            )

            // Description Input
            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 500) description = it },
                label = { Text(stringResource(R.string.field_desc_label)) },
                placeholder = { Text(stringResource(R.string.field_desc_placeholder)) },
                minLines = 2,
                maxLines = 4,
                supportingText = { Text("${description.length}/500") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_description_input")
            )

            // Kanban Status Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.field_status_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val stages = listOf(
                        Triple(TaskStatus.PENDENTE, stringResource(R.string.status_pending), Icons.Rounded.Schedule),
                        Triple(TaskStatus.INICIADO, stringResource(R.string.status_started), Icons.Rounded.PlayCircle),
                        Triple(TaskStatus.CONCLUIDO, stringResource(R.string.status_done), Icons.Rounded.CheckCircle)
                    )
                    stages.forEach { (stage, label, icon) ->
                        val selected = status == stage
                        val (container, content, accent) = when (stage) {
                            TaskStatus.PENDENTE -> Triple(kanbanColors.pendingContainer, kanbanColors.onPendingContainer, kanbanColors.pendingAccent)
                            TaskStatus.INICIADO -> Triple(kanbanColors.startedContainer, kanbanColors.onStartedContainer, kanbanColors.startedAccent)
                            TaskStatus.CONCLUIDO -> Triple(kanbanColors.doneContainer, kanbanColors.onDoneContainer, kanbanColors.doneAccent)
                        }
                        FilterChip(
                            selected = selected,
                            onClick = { status = stage },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = container,
                                selectedLabelColor = content
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("status_chip_${stage.wireValue.lowercase()}")
                        )
                    }
                }
            }

            // Priority Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.field_priority_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val priorities = listOf(
                        Triple(TaskPriority.BAIXA, stringResource(R.string.priority_low), PriorityLow),
                        Triple(TaskPriority.MEDIA, stringResource(R.string.priority_medium), PriorityMedium),
                        Triple(TaskPriority.ALTA, stringResource(R.string.priority_high), kanbanColors.highPriority)
                    )
                    priorities.forEach { (item, label, accent) ->
                        val selected = priority == item
                        FilterChip(
                            selected = selected,
                            onClick = { priority = item },
                            label = { Text(label, style = MaterialTheme.typography.labelLarge) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accent.copy(alpha = 0.18f),
                                selectedLabelColor = accent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("priority_chip_${item.wireValue.lowercase()}")
                        )
                    }
                }
            }

            // Category Chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.field_category_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, style = MaterialTheme.typography.labelLarge) },
                            modifier = Modifier.testTag("category_chip_$cat")
                        )
                    }
                }
            }

            // Schedule Date & Time Section
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.field_schedule_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Quick Schedule Shortcuts
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { scheduledAtMillis = quickScheduleMillis(0) },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, kanbanColors.cardBorder),
                        modifier = Modifier.testTag("quick_schedule_today")
                    ) {
                        Text(stringResource(R.string.schedule_quick_today))
                    }
                    OutlinedButton(
                        onClick = { scheduledAtMillis = quickScheduleMillis(1, defaultHour = 9) },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, kanbanColors.cardBorder),
                        modifier = Modifier.testTag("quick_schedule_tomorrow")
                    ) {
                        Text(stringResource(R.string.schedule_quick_tomorrow))
                    }
                    OutlinedButton(
                        onClick = { scheduledAtMillis = quickScheduleMillis(7, defaultHour = 9) },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, kanbanColors.cardBorder),
                        modifier = Modifier.testTag("quick_schedule_week")
                    ) {
                        Text(stringResource(R.string.schedule_quick_next_week))
                    }
                    if (scheduledAtMillis != null) {
                        TextButton(
                            onClick = { scheduledAtMillis = null },
                            modifier = Modifier.testTag("clear_schedule_button")
                        ) {
                            Text(
                                text = stringResource(R.string.schedule_clear),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Date & Time Picker Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        onClick = { showDatePicker = true },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, kanbanColors.cardBorder),
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("pick_date_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = scheduledAtMillis?.let { formatDateOnly(it) }
                                    ?: stringResource(R.string.schedule_pick_date),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            if (scheduledAtMillis == null) {
                                scheduledAtMillis = quickScheduleMillis(0)
                            }
                            showTimePicker = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, kanbanColors.cardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_time_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = scheduledAtMillis?.let { formatTimeOnly(it) }
                                    ?: stringResource(R.string.schedule_pick_time),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Primary Submit Action
            Button(
                onClick = {
                    val trimmed = title.trim()
                    if (trimmed.isEmpty() || trimmed.length > 120) {
                        showTitleError = true
                        return@Button
                    }
                    onSave(
                        TaskDraft(
                            id = initialDraft.id,
                            title = trimmed,
                            description = description.trim(),
                            status = status,
                            priority = priority,
                            category = category,
                            scheduledAtMillis = scheduledAtMillis
                        )
                    )
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_task_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(
                            if (isEditing) R.string.action_save else R.string.action_create
                        ),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = scheduledAtMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedUtc = datePickerState.selectedDateMillis
                        if (selectedUtc != null) {
                            val existingCal = Calendar.getInstance().apply {
                                timeInMillis = scheduledAtMillis ?: quickScheduleMillis(0)
                            }
                            scheduledAtMillis = combineUtcDateWithLocalTime(
                                utcDateMillis = selectedUtc,
                                hour = existingCal.get(Calendar.HOUR_OF_DAY),
                                minute = existingCal.get(Calendar.MINUTE)
                            )
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val cal = remember(scheduledAtMillis) {
            Calendar.getInstance().apply {
                timeInMillis = scheduledAtMillis ?: quickScheduleMillis(0)
            }
        }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.schedule_pick_time)) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updated = Calendar.getInstance().apply {
                            timeInMillis = scheduledAtMillis ?: quickScheduleMillis(0)
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        scheduledAtMillis = updated.timeInMillis
                        showTimePicker = false
                    }
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showDeleteConfirm && !initialDraft.id.isNullOrBlank() && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.confirm_delete_title)) },
            text = { Text(stringResource(R.string.confirm_delete_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(initialDraft.id)
                    },
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
