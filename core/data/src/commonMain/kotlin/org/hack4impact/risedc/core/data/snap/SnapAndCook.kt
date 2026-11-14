package org.hack4impact.risedc.core.data.snap

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface SnapAndCook {
    // false until both the API key and a confirmed safety policy are supplied.
    val isAvailable: Boolean

    // generates a recipe from a photo if the key and safety policy are configured
    suspend fun recipesFrom(photo: ByteArray): SnapResult
}

internal class GeminiSnapAndCook(
    private val client: GeminiClient,
    private val budget: GenerationBudget,
    private val policy: SafetyPolicy?,
    private val hasKey: Boolean,
    // allows prepare to be overridden for testing
    private val prepare: suspend (ByteArray) -> PreparedPhoto? = ::preparePhoto,
) : SnapAndCook {
    // Snap and Cook is unavailable if either the API key is missing or the safety policy is not configured
    override val isAvailable: Boolean get() = hasKey && policy != null

    override suspend fun recipesFrom(photo: ByteArray): SnapResult {
        if (!hasKey || policy == null) return SnapResult.Failed(FailureReason.NOT_CONFIGURED)

        // prepare the photo and generate the recipes within a timeout
        return withTimeoutOrNull(DEADLINE) {
            val prepared = prepare(photo) ?: return@withTimeoutOrNull SnapResult.BadPhoto
            generate(prepared, policy)
        } ?: SnapResult.TimedOut
    }

    @Suppress("ReturnCount") // Each terminal outcome exits the bounded retry loop.
    private suspend fun generate(photo: PreparedPhoto, policy: SafetyPolicy): SnapResult {
        val validator = RecipeValidator(policy)
        var failure = FailureReason.INVALID_REPLY

        // retry if the first call fails, with a delay between calls
        repeat(2) { attempt ->
            // delay if this isn't the first attempt
            if (attempt > 0) delay(RETRY_DELAY)

            // reserve a call in the budget before making the API call, or return LimitReached if over 50 calls today
            if (!budget.reserve()) return SnapResult.LimitReached

            // call the Gemini API to generate recipes
            when (val result = client.generate(photo, policy)) {
                // if it's not retryable return the failure reason, otherwise retry
                is GeminiReply.Failure -> {
                    failure = result.reason
                    if (!result.retryable) return SnapResult.Failed(failure)
                }

                is GeminiReply.Success -> {
                    val reply = result.reply

                    // return BadPhoto if no food is found and no recipes are returned
                    if (!reply.foodFound && reply.recipes.isEmpty()) return SnapResult.BadPhoto

                    if (!reply.foodFound || reply.recipes.size > SnapPrompt.MAX_RECIPES) {
                        failure = FailureReason.INVALID_REPLY
                        return@repeat
                    }

                    // Filter through the policy after validating the response envelope.
                    val safe = reply.recipes.filter(validator::accepts)

                    // Even one safe recipe is better than showing nothing, so don't spend a retry on more.
                    if (safe.isNotEmpty()) {
                        return SnapResult.Recipes(safe.map { it.toRecipe(newId()) })
                    }
                    failure = FailureReason.INVALID_REPLY
                }
            }
        }
        return SnapResult.Failed(failure)
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun newId() = Uuid.random().toString()

    // timeout and retry constants for the GeminiSnapAndCook implementation
    private companion object {
        val DEADLINE = 30.seconds
        val RETRY_DELAY = 0.5.seconds
    }
}
