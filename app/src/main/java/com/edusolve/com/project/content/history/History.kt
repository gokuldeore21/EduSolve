
package com.edusolve.com.project.content.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.edusolve.com.project.R
import com.edusolve.com.project.ui.composables.Lottie
import com.edusolve.com.project.ui.composables.PersianText
import com.edusolve.com.project.ui.composables.PersianTextField
import com.edusolve.com.project.ui.composables.ScaffoldWithTitle
import com.edusolve.com.project.util.DateTimeUtils.toIso
import java.time.ZonedDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryContent(
    onBackClick: () -> Unit,
    onItemClick: (Long) -> Unit
) {
    val state = rememberHistoryState()
    val hapticFeedback = LocalHapticFeedback.current

    ScaffoldWithTitle(
        title = stringResource(id = R.string.chat_history),
        onBackClick = onBackClick,
        content = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = {
                    if (state.history.value.isNotEmpty()) {
                        items(state.history.value) {
                            HistoryItem(
                                title = it.title,
                                date = it.date,
                                onClick = { onItemClick(it.id) },
                                onDelete = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    state.delete(it.id)
                                },
                                onEdit = { title ->
                                    state.edit(
                                        id = it.id,
                                        newTitle = title
                                    )
                                }
                            )
                        }
                    } else item { Lottie(R.raw.empty_list) }
                }
            )
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryItem(
    title: String,
    date: ZonedDateTime,
    onClick: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: () -> Unit
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }

    val hapticFeedback = LocalHapticFeedback.current

    ElevatedCard(
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = {
                isMenuExpanded = true
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        ),
        content = {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                content = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PersianText(
                            text = stringResource(R.string.title),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        PersianText(
                            text = title,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PersianText(
                            text = stringResource(R.string.date),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        PersianText(
                            text = date.toLocalDateTime().toIso().replace("T", "_"),
                            forcePersian = true
                        )
                    }

                    ItemDropDownMenu(
                        expanded = isMenuExpanded,
                        onDismiss = { isMenuExpanded = false },
                        onEdit = {
                            isMenuExpanded = false
                            isEditing = true
                        },
                        onDelete = {
                            isDeleting = true
                            isMenuExpanded = false
                        }
                    )
                }
            )
        }
    )
    if (isDeleting) {
        DeleteConfirmModal(
            onDismiss = { isDeleting = false },
            onNegative = { isDeleting = false },
            onPositive = {
                isDeleting = false
                onDelete()
            }
        )
    }

    if (isEditing) {
        EditDialog(
            title = title,
            onDismiss = { isEditing = false },
            onCancel = { isEditing = false },
            onConfirm = {
                isEditing = false
                onEdit(it)
            }
        )
    }
}

@Composable
fun ItemDropDownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val edit = stringResource(R.string.edit)
    val delete = stringResource(R.string.delete)
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        content = {
            DropdownMenuItem(
                onClick = onEdit,
                text = { PersianText(edit) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.TwoTone.Edit,
                        contentDescription = edit
                    )
                }
            )
            DropdownMenuItem(
                onClick = onDelete,
                text = { PersianText(delete) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.TwoTone.Delete,
                        contentDescription = delete
                    )
                }
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteConfirmModal(
    onDismiss: () -> Unit,
    onPositive: () -> Unit,
    onNegative: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        content = {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .padding(bottom = 64.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = {
                    PersianText(stringResource(R.string.delete_confirm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            16.dp,
                            Alignment.CenterHorizontally
                        )
                    ) {
                        Button(
                            content = { PersianText(stringResource(R.string.yes)) },
                            onClick = {
                                onPositive()
                                onDismiss()
                            }
                        )
                        Button(
                            content = { PersianText(stringResource(R.string.no)) },
                            onClick = {
                                onNegative()
                                onDismiss()
                            }
                        )
                    }
                }
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDialog(
    title: String,
    onDismiss: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newTitle by remember { mutableStateOf(title) }
    AlertDialog(
        onDismissRequest = onDismiss,
        content = {
            Surface {
                Column(
                    modifier = Modifier
                        .padding(32.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    content = {
                        PersianTextField(
                            singleLine = true,
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            placeholder = { PersianText(stringResource(R.string.history_title)) }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(
                                16.dp,
                                Alignment.CenterHorizontally
                            )
                        ) {
                            Button(
                                content = { PersianText(stringResource(R.string.accept)) },
                                onClick = {
                                    onConfirm(newTitle)
                                    onDismiss()
                                }
                            )
                            Button(
                                content = { PersianText(stringResource(R.string.cancel)) },
                                onClick = {
                                    onCancel()
                                    onDismiss()
                                }
                            )
                        }
                    }
                )
            }
        }
    )
}
