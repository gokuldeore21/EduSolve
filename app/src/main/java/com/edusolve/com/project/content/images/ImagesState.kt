
package com.edusolve.com.project.content.images

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.edusolve.com.project.model.request.CreateUrlImage
import com.edusolve.com.project.util.log
import com.edusolve.com.project.web.APIs
import com.edusolve.com.project.web.Web
import com.edusolve.com.project.web.Web.apiOf
import retrofit2.HttpException

class ImagesState(
    val imagePrompt: MutableState<String>,
    val imageUrls: MutableState<List<String>?>,
    val isWaitingForResponse: MutableState<Boolean>,
    val scope: LifecycleCoroutineScope
) {
    suspend fun generateImages(
        newPrompt: String
    ) {
        imageUrls.value = try {
            isWaitingForResponse.value = true
            Web.getRetrofit()
                .apiOf<APIs.ImagesAPIs>()
                .createUrlImage(createImage = CreateUrlImage(prompt = newPrompt, n = 2))
                .data
                .map { it.url }
        } catch (e: Exception) {
            val errorBody = (e as? HttpException)?.response()?.errorBody()
            log(e)
            log(errorBody?.string() ?: "Unknown generate image error")
            null
        } finally {
            isWaitingForResponse.value = false
        }
    }
}

@Composable
fun rememberImagesState(
    imagePrompt: MutableState<String> = rememberSaveable { mutableStateOf("") },
    imageUrls: MutableState<List<String>?> = rememberSaveable { mutableStateOf(listOf()) },
    isWaitingForResponse: MutableState<Boolean> = rememberSaveable { mutableStateOf(false) },
    scope: LifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycleScope
) = remember(imagePrompt, imageUrls, isWaitingForResponse, scope) {
    ImagesState(imagePrompt, imageUrls, isWaitingForResponse, scope)
}