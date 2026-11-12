package org.hack4impact.risedc.core.data.snap

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import org.hack4impact.risedc.core.data.model.MealType
import org.hack4impact.risedc.core.data.model.StoreSection

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

/*
 * Gemini's own shapes, kept apart from the app models so a new model field can't be filled by Gemini
 * unchecked. Image ids are Gemini's pick from the sticker lists, or NO_IMAGE; toRecipe() turns
 * anything that isn't a real catalog id into null.
 */
@Serializable
internal data class GeneratedRecipe(
    val title: String,
    val mealType: MealType,
    val minutes: Int,
    val servings: Int,
    val imageId: String = SnapPrompt.NO_IMAGE,
    val ingredients: List<GeneratedIngredient>,
    val utensils: List<GeneratedItem>,
    val steps: List<GeneratedStep>,
)

@Serializable
internal data class GeneratedIngredient(
    val name: String,
    val amount: String,
    val storeSection: StoreSection = StoreSection.OTHER,
    val imageId: String = SnapPrompt.NO_IMAGE,
)

/** A utensil: its simple name and sticker id. */
@Serializable
internal data class GeneratedItem(val name: String, val imageId: String = SnapPrompt.NO_IMAGE)

/** [ingredients] and [utensils] name items from the recipe's own lists. */
@Serializable
internal data class GeneratedStep(
    val instruction: String,
    val imageId: String = SnapPrompt.NO_IMAGE,
    val ingredients: List<String> = emptyList(),
    val utensils: List<String> = emptyList(),
    val safety: List<String> = emptyList(),
)
