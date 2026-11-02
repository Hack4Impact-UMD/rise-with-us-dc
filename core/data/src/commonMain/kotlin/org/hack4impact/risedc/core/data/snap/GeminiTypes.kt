package org.hack4impact.risedc.core.data.snap

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import org.hack4impact.risedc.core.data.model.Ingredient
import org.hack4impact.risedc.core.data.model.MealType
import org.hack4impact.risedc.core.data.model.RecipeStep

// --- A. Gemini generateContent request/response (https://ai.google.dev/api/generate-content) ---

@Serializable
internal data class GenerateRequest(
    val systemInstruction: Content, // the prompt
    val contents: List<Content>, // the ingredient photo the user took
    val generationConfig: GenerationConfig,
)

@Serializable
internal data class Content(val parts: List<Part>)

/** A message part should contain either text or an image (not both) */
@Serializable
internal data class Part(val text: String? = null, val inlineData: InlineData? = null)

/** The image data including the media type descriptor and the image itself*/
@Serializable
internal data class InlineData(val mimeType: String, val data: String)

@Serializable
internal data class GenerationConfig(
    val responseMimeType: String,
    val responseSchema: JsonObject,
    val thinkingConfig: ThinkingConfig,
    val maxOutputTokens: Int,
)

@Serializable
internal data class ThinkingConfig(val thinkingLevel: String)

/* Gemini can return multiple options of responses to one request, so each candidate
    represents one potential response. For now use the default of setting one candidate per request */
@Serializable
internal data class GenerateResponse(val candidates: List<Candidate> = emptyList())

@Serializable
internal data class Candidate(val content: Content? = null, val finishReason: String? = null)

// --- B. The recipe JSON we ask Gemini to write (must match SnapPrompt's schema) ---

@Serializable
internal data class GeneratedReply(val foodFound: Boolean, val recipes: List<GeneratedRecipe> = emptyList())

@Serializable
internal data class GeneratedRecipe(
    val title: String,
    val mealType: MealType,
    val minutes: Int,
    val ingredients: List<Ingredient>,
    val utensils: List<String>,
    val steps: List<RecipeStep>,
)
