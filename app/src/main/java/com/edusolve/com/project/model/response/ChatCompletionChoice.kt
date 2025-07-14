

package com.edusolve.com.project.model.response

import com.squareup.moshi.Json
import com.edusolve.com.project.model.Chat

data class ChatCompletionChoice(
    val message: Chat,
    val index: Int,
    val logprobs: Int?,
    @field:Json(name = "finish_reason")
    val finishReason: String
)