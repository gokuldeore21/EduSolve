package com.edusolve.com.project.content.chat

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Clear
import androidx.compose.material.icons.twotone.Send
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material.icons.twotone.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.edusolve.com.project.R
import com.edusolve.com.project.ui.animation.TypewriterText
import com.edusolve.com.project.ui.composables.PersianText
import com.edusolve.com.project.ui.composables.PersianTextField
import com.edusolve.com.project.ui.animation.AnimatedDots
import com.edusolve.com.project.ui.composables.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatContent(
    historyId: Long,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val defaultTitle = stringResource(R.string.new_chat)
    val state = rememberChatState(
        title = remember { mutableStateOf(defaultTitle) },
        historyId = remember { mutableLongStateOf(historyId) }
    )

    val listState = rememberScrollState()

    if (listState.isScrollInProgress)
        LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.TextHandleMove)

    val chatSize by remember { derivedStateOf { state.chat.value.size } }

    LaunchedEffect(chatSize) {
        val index = state.chat.value.size - 1
        if (index > 0)
            listState.animateScrollTo(listState.maxValue)
    }

    LaunchedEffect(state.forceScroll.value) {
        listState.animateScrollBy(100f)
    }

    InternetAwareComposable { state.isOnline.value = it }

    BackHandler { state.scope.launch { onBackClick() } }

    ScaffoldWithTitle(
        onBackClick = { state.scope.launch { onBackClick() } },
        title = {
            AnimatedContent(
                targetState = state.title.value,
                label = state.title.value,
                content = {
                    PersianText(
                        text = it,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        },
        snackbarHost = {
            SnackbarHost(state.snackbarHost) { data ->
                MySnackbar {
                    PersianText(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        text = data.visuals.message
                    )
                }
            }
        },
        actions = {
            IconButton(
                onClick = onSettingsClick,
                content = {
                    Icon(
                        imageVector = Icons.TwoTone.Settings,
                        contentDescription = stringResource(R.string.settings)
                    )
                }
            )
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(listState)
            ) {
                AnimatedVisibility(
                    visible = !state.isOnline.value,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    OutlinedCard {
                        PersianText(
                            text = stringResource(R.string.no_internet_connection),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        )
                    }
                }
                state.chat.value.forEach {
                    ChatBubble(
                        content = it.content,
                        owner = ChatBubbleOwner.of(it.role),
                        isTypewriter = historyId == -1L && it.role == "assistant",
                        typeWriterPass = { state.forceScroll.value = ++state.forceScroll.value }
                    )
                }
                if (state.isWaitingForResponse.value) {
                    ChatBubble(
                        owner = ChatBubbleOwner.Assistant,
                        content = {
                            Box(
                                content = { AnimatedDots() },
                                modifier = Modifier.padding(
                                    vertical = 4.dp,
                                    horizontal = 8.dp
                                )
                            )
                        }
                    )
                }
            }
        },
        bottomBar = {
            Crossfade(
                targetState = state.inputVisibility.value,
                label = "",
                content = { visibility ->
                    if (visibility) {
                        UserInput(
                            chatInput = state.chatInput.value,
                            onNewChatInputSubmit = { state.scope.launch { state.newChatHandler(state.chatInput.value) } },
                            onChatInputChange = { input -> state.chatInput.value = input }
                        )
                    } else {
                        BottomAppBar(
                            actions = { },
                            floatingActionButton = {
                                FloatingActionButton(
                                    onClick = { state.cancel() },
                                    content = {
                                        Icon(
                                            imageVector = Icons.TwoTone.Stop,
                                            contentDescription = stringResource(R.string.cancel)
                                        )
                                    }
                                )
                            }
                        )
                    }
                }
            )
        }
    )
}

@Composable
private fun UserInput(
    chatInput: String,
    onChatInputChange: (String) -> Unit,
    onNewChatInputSubmit: () -> Unit
) {
    PersianTextField(
        label = { PersianText(text = stringResource(R.string.chat_input)) },
        value = chatInput,
        onValueChange = { onChatInputChange(it) },
        minLines = 2,
        maxLines = 5,
        modifier = Modifier.fillMaxWidth(),
        keyboardActions = KeyboardActions(onSend = { onNewChatInputSubmit() }),
        keyboardOptions = KeyboardOptions(
            imeAction = if (chatInput.isNotBlank()) ImeAction.Send else ImeAction.None,
            keyboardType = KeyboardType.Text,
            capitalization = KeyboardCapitalization.Sentences
        ),
        leadingIcon = {
            IconButton(
                onClick = { onChatInputChange("") },
                content = {
                    Icon(
                        imageVector = Icons.TwoTone.Clear,
                        contentDescription = stringResource(R.string.clear)
                    )
                }
            )
        },
        trailingIcon = {
            IconButton(
                enabled = chatInput.isNotBlank(),
                onClick = onNewChatInputSubmit,
                content = {
                    Icon(
                        imageVector = Icons.TwoTone.Send,
                        contentDescription = stringResource(R.string.send)
                    )
                }
            )
        }
    )
}

@Composable
fun ChatBubble(
    content: String,
    owner: ChatBubbleOwner,
    isTypewriter: Boolean,
    typeWriterPass: () -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(true) }

    val haptic = LocalHapticFeedback.current
    val textCopied = stringResource(R.string.text_copied)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    ChatBubble(
        owner = owner,
        onClick = { isExpanded = !isExpanded },
        onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            clipboardManager.setText(AnnotatedString(content))
            Toast
                .makeText(context, textCopied, Toast.LENGTH_SHORT)
                .show()
        },
        content = {
            if (isTypewriter) {
                TypewriterText(
                    modifier = Modifier.padding(8.dp),
                    text = content.trim(),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 10,
                    overrideDirection = true,
                    typeWriterPass = typeWriterPass
                )
            } else {
                PersianText(
                    modifier = Modifier.padding(8.dp),
                    text = content.trim(),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 10,
                    overrideDirection = true
                )
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubble(
    content: @Composable (ColumnScope.() -> Unit),
    owner: ChatBubbleOwner,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val container = if (owner == ChatBubbleOwner.User) {
        MaterialTheme.colorScheme.secondaryContainer
    } else MaterialTheme.colorScheme.tertiaryContainer

    val shape = if (owner == ChatBubbleOwner.User) {
        RoundedCornerShape(
            topStart = 2.dp,
            topEnd = 16.dp,
            bottomEnd = 16.dp,
            bottomStart = 16.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomEnd = 2.dp,
            bottomStart = 16.dp
        )
    }

    val arrangement = if (owner == ChatBubbleOwner.User) Arrangement.End else Arrangement.Start

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = arrangement
    ) {
        ElevatedCard(
            colors = CardDefaults.cardColors(containerColor = container),
            shape = shape,
            content = content,
            modifier = Modifier
                .padding(4.dp)
                .clip(shape)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        )
    }
}