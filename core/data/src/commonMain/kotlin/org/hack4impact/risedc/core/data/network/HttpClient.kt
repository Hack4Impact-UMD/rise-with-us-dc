package org.hack4impact.risedc.core.data.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import kotlinx.serialization.json.Json
import io.ktor.serialization.kotlinx.json.json as jsonContent

/**
 * Where the backend (functions/) lives. The app never calls Google, WMATA or Gemini directly;
 * every external call goes through the backend, which holds the API keys.
 */
data class ApiConfig(val baseUrl: String) {
    companion object {
        // TODO(firebase): real URL once the rise-dc-dev Firebase project exists.
        val Dev = ApiConfig("https://example.invalid/api/")
    }
}

val RiseJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

fun riseHttpClient(config: ApiConfig): HttpClient = HttpClient {
    install(ContentNegotiation) { jsonContent(RiseJson) }
    defaultRequest { url(config.baseUrl) }
    expectSuccess = true
}
