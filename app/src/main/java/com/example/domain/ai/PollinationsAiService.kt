package com.example.domain.ai

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.TimeUnit

enum class AiStylePreset(val label: String, val promptSuffix: String) {
    PHOTOREALISTIC("Photorealistic", ", photorealistic 8k sharp focus studio lighting cinematic"),
    DIGITAL_ART("Vibrant Digital Art", ", vibrant digital art masterpiece trending on artstation detailed"),
    CYBERPUNK("Cyberpunk Neon", ", cyberpunk neon lighting synthwave futuristic high detail"),
    ANIME("Anime Studio", ", anime aesthetic vivid colors detailed background studio ghibli style"),
    CINEMATIC_OIL("Oil Painting", ", classical oil painting rich impasto brushstrokes dramatic lighting"),
    MINIMALIST("Minimalist Vector", ", clean minimalist modern vector graphic flat design elegant")
}

enum class AiAspectRatio(val label: String, val width: Int, val height: Int) {
    SQUARE("Square 1:1", 1024, 1024),
    LANDSCAPE("Landscape 16:9", 1280, 720),
    PORTRAIT("Portrait 9:16", 720, 1280)
}

object PollinationsAiService {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(45, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    fun buildImageUrl(prompt: String, style: AiStylePreset, ratio: AiAspectRatio, seed: Long = System.currentTimeMillis()): String {
        val fullPrompt = prompt.trim() + style.promptSuffix
        val encodedPrompt = URLEncoder.encode(fullPrompt, StandardCharsets.UTF_8.toString())
        return "https://image.pollinations.ai/prompt/$encodedPrompt?width=${ratio.width}&height=${ratio.height}&seed=$seed&nologo=true"
    }

    suspend fun downloadImageToLocalUri(
        context: Context,
        imageUrl: String
    ): Uri = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(imageUrl).build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to download generated image: HTTP ${response.code}")
        }

        val body = response.body ?: throw Exception("Empty image response")
        val cacheDir = File(context.cacheDir, "ai_images").apply { mkdirs() }
        val file = File(cacheDir, "lumina_ai_${UUID.randomUUID()}.jpg")

        FileOutputStream(file).use { out ->
            body.byteStream().copyTo(out)
        }

        Uri.fromFile(file)
    }
}
