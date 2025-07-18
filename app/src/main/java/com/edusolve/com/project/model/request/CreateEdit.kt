

package com.edusolve.com.project.model.request

import com.squareup.moshi.Json
import com.edusolve.com.project.util.Constants

data class CreateEdit(
    val instruction: String,
    val n: Int = 1,
    val temperature: Float = 1f,
    @field:Json(name = "top_p")
    val topP: Float? = 1f,
    val model: String = Constants.EDIT_MODELS.first(),
    val input: String = "",
)
