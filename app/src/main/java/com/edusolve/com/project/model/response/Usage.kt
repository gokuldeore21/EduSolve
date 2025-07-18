
package com.edusolve.com.project.model.response

import com.squareup.moshi.Json

data class Usage(
    @field:Json(name = "prompt_tokens")
    val promptTokens: Int,
    @field:Json(name = "completion_tokens")
    val completionTokens: Int,
    @field:Json(name = "total_tokens")
    val totalTokens: Int
)
