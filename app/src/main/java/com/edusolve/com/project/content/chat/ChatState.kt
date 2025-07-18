
package com.edusolve.com.project.content.chat

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.edusolve.com.project.R
import com.edusolve.com.project.content.main.settingsDataStore
import com.edusolve.com.project.db.entity.HistoryEntity
import com.edusolve.com.project.db.entity.HistoryItemEntity
import com.edusolve.com.project.model.Chat
import com.edusolve.com.project.model.request.CreateChatCompletion
import com.edusolve.com.project.model.response.ChatCompletion
import com.edusolve.com.project.util.Constants
import com.edusolve.com.project.util.Constants.db
import com.edusolve.com.project.util.DataStoreHelper
import com.edusolve.com.project.util.DateTimeUtils
import com.edusolve.com.project.util.log
import com.edusolve.com.project.web.APIs
import com.edusolve.com.project.web.Web
import com.edusolve.com.project.web.Web.apiOf

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.LocalDateTime
import java.time.ZoneOffset

class ChatState(
    context: Context,
    private val historyId: MutableLongState,
    val scope: LifecycleCoroutineScope,
    val chatInput: MutableState<String>,
    val chat: MutableState<List<Chat>>,
    val model: MutableState<String>,
    val title: MutableState<String>,
    val isOnline: MutableState<Boolean>,
    val isWaitingForResponse: MutableState<Boolean>,
    val inputVisibility: MutableState<Boolean>,
    val forceScroll: MutableState<Int>
) {
    private val retrofit = Web.getRetrofit()

    private val modelNotSupported = context.getString(R.string.model_not_supported)

    private val locallyRateLimited = context.getString(R.string.locally_rate_limited)

    private val aiFailedToAnswer = context.getString(R.string.ai_failed_answering)

    private val aiCancelledAnswer = context.getString(R.string.ai_was_cancelled)

    private val isModelSupported: Boolean
        get() = model.value in Constants.CHAT_MODELS

    private var isUpdatable = false

    val snackbarHost = SnackbarHostState()

    private val settings = DataStoreHelper(context.settingsDataStore)

    private var historyChat = listOf<Chat>()

    private lateinit var chatJob: Job

    init {
        scope.launch {
            model.value = settings.getString(Constants.API_MODEL) ?: Constants.DEFAULT_API_MODEL

            if (historyId.longValue != -1L)
                handleHistoryLoading()
        }
    }

    private suspend fun isLocallyRateLimited(): Boolean {
        val current = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        val last = settings.getLong(Constants.LAST_TRY)
        val tries = settings.getInt(Constants.TRIES) ?: 0
        return if (last != null && tries != 0) {
            if (current - last < Constants.RATE_LIMIT_TIME) {
                if (tries <= Constants.RATE_LIMIT_TRIES) {
                    updateUsage(tries + 1, current)
                    false
                } else true
            } else {
                updateUsage(0, current)
                false
            }
        } else {
            updateUsage(tries + 1, current)
            false
        }
    }

    private suspend fun updateUsage(
        tries: Int,
        now: Long
    ) {
        settings.setLong(Constants.LAST_TRY, now)
        settings.setInt(Constants.TRIES, tries)
    }

    private suspend fun createNewHistoryEntity() {
        historyId.longValue = db.historyDao().insert(
            HistoryEntity(title.value, DateTimeUtils.zonedNow())
        )
    }

    private suspend fun handleHistoryLoading() {
        isUpdatable = true
        chat.value = loadFromHistory(historyId.longValue)
        historyChat = chat.value
        title.value = db.historyDao().getById(historyId.longValue)?.title ?: ""
    }

    suspend fun newChatHandler(
        chatInput: String
    ) {
        if (!isLocallyRateLimited()) {
            if (isModelSupported) {
                val newChat = Chat(role = "user", content = chatInput.trim())
                chat.value += newChat
                if (chat.value.size == 1)
                    createNewHistoryEntity()
                addChatItemToHistory(newChat, historyId.longValue)

                resetInput()
                changeInputAllowance(false)
                chatJob = scope.launch {
                    val (completion, exception) = chatCompletionRequest()
                    val aiAnswer = if (completion != null && exception == null) {
                        completion.choices.first().message
                    } else createChatForExceptions(exception)
                    changeInputAllowance(true)

                    chat.value += aiAnswer
                    addChatItemToHistory(aiAnswer, historyId.longValue)

                    if (chat.value.size > 5)
                        title.value = createHistoryTitle(predictTitle())
                    updateChatHistoryTitle()
                }
            } else snackbarHost.showSnackbar(modelNotSupported)
        } else snackbarHost.showSnackbar(locallyRateLimited)
    }

    private fun createChatForExceptions(
        exception: Exception?
    ) = Chat(
        role = "assistant",
        content = when (exception) {
            is CancellationException -> aiCancelledAnswer
            is HttpException -> "Ai was rate limited"
            else -> aiFailedToAnswer
        }
    )

    private suspend fun chatCompletionRequest(): Pair<ChatCompletion?, Exception?> {
        return try {
            retrofit.apiOf<APIs.ChatCompletionsAPIs>().createChatCompletions(
                CreateChatCompletion(
                    model = model.value,
                    messages = chat.value
                )
            ) to null
        } catch (e: CancellationException) {
            log(e)
            null to e
        } catch (e: Exception) {
            log(e)
            null to e
        }
    }

    private fun resetInput() {
        this.chatInput.value = ""
    }

    private fun changeInputAllowance(
        isAllowed: Boolean
    ) {
        isWaitingForResponse.value = !isAllowed
        inputVisibility.value = isAllowed
    }

    private suspend fun addChatItemToHistory(
        chat: Chat,
        id: Long
    ) = db.historyItemDao().insert(
        HistoryItemEntity(
            content = chat.content,
            owner = ChatBubbleOwner.of(chat.role),
            historyId = id
        )
    )

    private suspend fun updateChatHistoryTitle(): Int? {
        val dao = db.historyDao()
        return dao
            .getById(historyId.longValue)?.let { historyEntity ->
                dao.update(
                    historyEntity.copy(title = title.value, date = DateTimeUtils.zonedNow())
                )
            }
    }

    private fun createHistoryTitle(
        predictedTitle: String
    ): String = predictedTitle.ifBlank { chat.value.first().content }

    private suspend fun loadFromHistory(
        historyId: Long
    ) = db.historyItemDao()
        .getByParam("historyId", historyId)
        .map { Chat(it.owner.toString().lowercase(), it.content) }

    private suspend fun predictTitle() = try {
        Web.getRetrofit()
            .apiOf<APIs.ChatCompletionsAPIs>()
            .createChatCompletions(
                CreateChatCompletion(
                    model = model.value,
                    messages = listOf(
                        Chat(
                            role = "user",
                            content = assembleChatForPrediction()
                        )
                    )
                )
            ).choices.first().message.content.trim()
            .replace("\\n", "")
            .removeSurrounding("\"")
    } catch (e: Exception) {
        log(e)
        title.value
    }

    private fun assembleChatForPrediction(): String {
        return buildString {
            append(Constants.TITLE_PREDICTION_PROMPT)
            chat.value.forEach { message ->
                append(message.content)
                append("\n")
            }
        }
    }

    fun cancel() {
        chatJob.cancel()
    }
}

@Composable
fun rememberChatState(
    context: Context = LocalContext.current,
    historyId: MutableLongState = rememberSaveable { mutableLongStateOf(-1L) },
    scope: LifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycleScope,
    chatInput: MutableState<String> = rememberSaveable { mutableStateOf("") },
    chat: MutableState<List<Chat>> = rememberSaveable { mutableStateOf(listOf()) },
    model: MutableState<String> = rememberSaveable { mutableStateOf(Constants.DEFAULT_API_MODEL) },
    title: MutableState<String> = rememberSaveable { mutableStateOf("") },
    isOnline: MutableState<Boolean> = rememberSaveable { mutableStateOf(true) },
    isWaitingForResponse: MutableState<Boolean> = rememberSaveable { mutableStateOf(false) },
    inputVisibility: MutableState<Boolean> = rememberSaveable { mutableStateOf(true) },
    forceScroll: MutableState<Int> = rememberSaveable { mutableIntStateOf(0) }
) = remember(
    context,
    historyId,
    scope,
    chatInput,
    chat,
    model,
    title,
    isOnline,
    isWaitingForResponse,
    inputVisibility,
    forceScroll
) {
    ChatState(
        context,
        historyId,
        scope,
        chatInput,
        chat,
        model,
        title,
        isOnline,
        isWaitingForResponse,
        inputVisibility,
        forceScroll
    )
}