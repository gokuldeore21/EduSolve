
package com.edusolve.com.project.model.response

import com.squareup.moshi.Json

data class CompletionChoice(
    val text: String,
    val index: Int,
    val logprobs: Int?,
    @field:Json(name = "finish_reason")
    val finishReason: String
)