package org.hack4impact.risedc.core.data.snap

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import org.hack4impact.risedc.core.data.network.RiseJson
import kotlin.io.encoding.Base64

internal sealed interface GeminiReply {
    data class Success(val reply: GeneratedReply) : GeminiReply
    data class Failure(val reason: FailureReason, val retryable: Boolean) : GeminiReply
}

// Interface for sending a photo and safety policy to the Gemini API
internal fun interface GeminiClient {
    suspend fun generate(photo: PreparedPhoto, policy: SafetyPolicy): GeminiReply
}

internal class HttpGeminiClient(
    httpClient: HttpClient,
    private val apiKey: String,
    private val model: String = "gemini-4-hydrogen",
) : GeminiClient {
    // Gemini sends nothing until the whole reply is ready, which can take 15-50 s. OkHttp's default
    // 10 s read timeout would report every slow reply as a network error, so wait past SnapAndCook's
    // own deadline and let that deadline decide.
    private val http = httpClient.config {
        install(HttpTimeout) {
            requestTimeoutMillis = TIMEOUT_MILLIS
            socketTimeoutMillis = TIMEOUT_MILLIS
        }
    }

    override suspend fun generate(photo: PreparedPhoto, policy: SafetyPolicy): GeminiReply = try {
        // Encode the safety policy as JSON to send to the Gemini API
        val policyText = RiseJson.encodeToString(PolicyText(policy.neverUseIngredients, policy.allowedTools))

        // Create a part object for the image with inline data
        val image = Part(inlineData = InlineData(photo.mimeType, Base64.encode(photo.bytes)))

        // Send a POST request to the Gemini API with the system instruction, policy text, and image
        val response = http.post("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent") {
            expectSuccess = false
            header("x-goog-api-key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(
                GenerateRequest(
                    systemInstruction = Content(
                        listOf(
                            Part(text = SnapPrompt.SYSTEM_INSTRUCTION),
                            Part(text = SnapPrompt.STICKER_LIST),
                            Part(text = policyText),
                        ),
                    ),
                    contents = listOf(Content(listOf(image))),
                    generationConfig = GenerationConfig(
                        responseMimeType = "application/json",
                        responseSchema = SnapPrompt.RESPONSE_SCHEMA,
                        thinkingConfig = ThinkingConfig(thinkingLevel = "LOW"),
                        maxOutputTokens = MAX_OUTPUT_TOKENS,
                    ),
                ),
            )
        }

        when {
            // Don't retry since the 429 means the user has exceeded Gemini's limit for a given time period
            response.status == HttpStatusCode.TooManyRequests -> GeminiReply.Failure(
                FailureReason.RATE_LIMITED,
                retryable = false,
            )

            // Retryable since an internal server error could be a temporary issue with the Gemini API
            response.status.value >= HttpStatusCode.InternalServerError.value -> GeminiReply.Failure(
                FailureReason.SERVICE_UNAVAILABLE,
                retryable = true,
            )

            // Decode the response if the call is successful, otherwise return a failure with the appropriate reason
            response.status.isSuccess() -> decode(response.body())
            else -> GeminiReply.Failure(FailureReason.REQUEST, retryable = false)
        }
    } catch (e: CancellationException) {
        throw e // The timeout in SnapAndCook relies on cancellation reaching it.
    } catch (_: IOException) {
        GeminiReply.Failure(FailureReason.NETWORK, retryable = true)
    } catch (_: Exception) {
        // Malformed JSON or an unreadable reply, such as a non-JSON content type.
        GeminiReply.Failure(FailureReason.INVALID_REPLY, retryable = true)
    }

    @Suppress("ReturnCount") // Provider terminal states are guard clauses.
    private fun decode(response: GenerateResponse): GeminiReply {
        // Take the first candidate from the response. If there are none, Gemini blocked the request.
        val candidate = response.candidates.firstOrNull()
            ?: return GeminiReply.Failure(FailureReason.BLOCKED, retryable = false)

        // Reject incomplete responses, only token-limit failures get another attempt.
        if (candidate.finishReason != "STOP") {
            return GeminiReply.Failure(FailureReason.INVALID_REPLY, retryable = candidate.finishReason == "MAX_TOKENS")
        }

        // Combine the parts of the candidate into a single string
        val text = candidate.content?.parts?.mapNotNull { it.text }?.joinToString("")
            ?: return GeminiReply.Failure(FailureReason.INVALID_REPLY, retryable = true)
        return GeminiReply.Success(RiseJson.decodeFromString(text))
    }

    // Data class for the safety policy to be JSONified and later sent to the Gemini API
    @kotlinx.serialization.Serializable
    private data class PolicyText(val neverUseIngredients: Set<String>, val allowedTools: Set<String>)

    private companion object {
        const val MAX_OUTPUT_TOKENS = 4096
        const val TIMEOUT_MILLIS = 35_000L
    }
}
