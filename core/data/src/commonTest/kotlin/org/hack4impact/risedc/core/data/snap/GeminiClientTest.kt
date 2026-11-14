package org.hack4impact.risedc.core.data.snap

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.hack4impact.risedc.core.data.network.RiseJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class GeminiClientTest {
    @Test
    fun classifiesHttpErrorsAndMalformedOutput() = runTest {
        val cases = listOf(
            Triple(HttpStatusCode.BadRequest, "{}", GeminiReply.Failure(FailureReason.REQUEST, false)),
            Triple(HttpStatusCode.TooManyRequests, "{}", GeminiReply.Failure(FailureReason.RATE_LIMITED, false)),
            Triple(
                HttpStatusCode.ServiceUnavailable,
                "{}",
                GeminiReply.Failure(FailureReason.SERVICE_UNAVAILABLE, true),
            ),
            Triple(HttpStatusCode.OK, "not json", GeminiReply.Failure(FailureReason.INVALID_REPLY, true)),
            Triple(HttpStatusCode.OK, "{}", GeminiReply.Failure(FailureReason.BLOCKED, false)),
        )
        for ((status, body, expected) in cases) {
            withClient(status, body) { assertEquals(expected, it.generate(photo, policy)) }
        }
    }

    @Test
    fun unexpectedExceptionsBecomeFailuresButCancellationPropagates() = runTest {
        val html = HttpClient(
            MockEngine { respond("<html></html>", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/html")) },
        ) {
            install(ContentNegotiation) { json(RiseJson) }
        }
        val cancelled = HttpClient(MockEngine { throw CancellationException("cancelled") }) {
            install(ContentNegotiation) { json(RiseJson) }
        }
        try {
            assertEquals(
                GeminiReply.Failure(FailureReason.INVALID_REPLY, true),
                HttpGeminiClient(html, "test-key").generate(photo, policy),
            )
            assertFailsWith<CancellationException> { HttpGeminiClient(cancelled, "test-key").generate(photo, policy) }
        } finally {
            html.close()
            cancelled.close()
        }
    }

    @Test
    fun rejectsTruncatedCandidatesAndParsesCompleteOutput() = runTest {
        val truncated = GenerateResponse(listOf(Candidate(Content(listOf(Part(text = "{}"))), "MAX_TOKENS")))
        withClient(HttpStatusCode.OK, RiseJson.encodeToString(truncated)) {
            assertEquals(GeminiReply.Failure(FailureReason.INVALID_REPLY, true), it.generate(photo, policy))
        }
        val reply = GeneratedReply(foodFound = false)
        val complete = GenerateResponse(
            listOf(Candidate(Content(listOf(Part(text = RiseJson.encodeToString(reply)))), "STOP")),
        )
        withClient(HttpStatusCode.OK, RiseJson.encodeToString(complete)) {
            assertEquals(reply, assertIs<GeminiReply.Success>(it.generate(photo, policy)).reply)
        }
    }

    private suspend fun withClient(status: HttpStatusCode, body: String, test: suspend (GeminiClient) -> Unit) {
        val http =
            HttpClient(MockEngine { respond(body, status, headersOf(HttpHeaders.ContentType, "application/json")) }) {
                expectSuccess = true // The transport must override the shared client's setting.
                install(ContentNegotiation) { json(RiseJson) }
            }
        try {
            test(HttpGeminiClient(http, "test-key"))
        } finally {
            http.close()
        }
    }

    private companion object {
        val photo = PreparedPhoto(byteArrayOf(1), "image/jpeg")
        val policy = SafetyPolicy(setOf("peanut"), setOf("spoon"))
    }
}
