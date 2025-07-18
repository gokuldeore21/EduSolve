

package com.edusolve.com.project.model.response

import com.squareup.moshi.Json

data class Models(
    @field:Json(name = "object") val obj: String,
    val data: List<Model>,
    val root: String,
    val parent: String?
)
