package org.hack4impact.risedc.core.data.snap

import org.hack4impact.risedc.core.data.model.Recipe

/** The result of the Photo to Recipe API call */
sealed interface SnapResult {
    /** The success case returns a list of recipes that are deemed safe according
     to the configured policy */
    data class Recipes(val recipes: List<Recipe>) : SnapResult

    /** Not a valid JPEG/PNG, or Gemini didn't identify any food in the photo */
    data object BadPhoto : SnapResult

    /** Gemini took longer than the timeout */
    data object TimedOut : SnapResult

    /** The user exceeded the daily API call limit */
    data object LimitReached : SnapResult

    /** No key, network error, or no usable recipes after a retry. */
    data class Failed(val reason: FailureReason) : SnapResult
}

enum class FailureReason { NOT_CONFIGURED, NETWORK, REQUEST, RATE_LIMITED, INVALID_REPLY, BLOCKED }
