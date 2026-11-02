package org.hack4impact.risedc.core.data.snap

import org.hack4impact.risedc.core.data.model.Ingredient
import org.hack4impact.risedc.core.data.model.RecipeStep

/** RISE org wide never-use ingredients and allowed tools. */
data class SafetyPolicy(val neverUseIngredients: Set<String>, val allowedTools: Set<String>) {
    init {
        require(neverUseIngredients.none { it.isBlank() })
        require(allowedTools.isNotEmpty() && allowedTools.none { it.isBlank() })
    }
}

/** Structural/policy checks plus conservative English hazard checks, not a food-safety guarantee. */
internal class RecipeValidator(private val policy: SafetyPolicy) {
    private val heat = Regex("\\b(microwav|toast|heat|warm|hot|boil|broil|melt|steam|bak|fry|grill)[a-z]*")
    private val sharp = Regex(
        "\\b(knife|knives|cut|slic|chop|dic|minc|peel|grate|shred|" +
            "can opener|open(ing)?( (the|a))? (cans?|canned)\\b)[a-z]*",
    )
    private val restrictedTools = Regex("\\b(stove|stovetop|oven|skillet|grill)\\b")
    private val rawFood = Regex("\\braw\\s+(meat|chicken|beef|pork|turkey|lamb|fish|salmon|tuna|shrimp|egg)")
    private val neverUse = policy.neverUseIngredients.map { Regex("\\b${Regex.escape(it.trim().lowercase())}") }

    fun accepts(recipe: GeneratedRecipe): Boolean = recipe.title.isNotBlank() &&
        recipe.title.length <= SnapPrompt.MAX_TITLE_LENGTH &&
        !mentionsForbiddenFood(recipe) &&
        recipe.minutes in 1..SnapPrompt.MAX_MINUTES &&
        recipe.ingredients.size in 1..SnapPrompt.MAX_INGREDIENTS &&
        recipe.ingredients.all(::acceptsIngredient) &&
        recipe.utensils.size in 1..SnapPrompt.MAX_UTENSILS &&
        recipe.utensils.all { it in policy.allowedTools && it.length <= SnapPrompt.MAX_NAME_LENGTH } &&
        recipe.steps.size in 1..SnapPrompt.MAX_STEPS &&
        recipe.steps.all(::acceptsStep)

    private fun mentionsForbiddenFood(recipe: GeneratedRecipe): Boolean {
        val text = (
            recipe.ingredients.map { it.name } +
                recipe.steps.flatMap { listOf(it.shortLabel, it.instruction) } +
                recipe.title
            ).joinToString(" ").lowercase()
        return rawFood.containsMatchIn(text) || neverUse.any { it.containsMatchIn(text) }
    }

    private fun acceptsIngredient(ingredient: Ingredient): Boolean = ingredient.name.isNotBlank() &&
        ingredient.name.length <= SnapPrompt.MAX_NAME_LENGTH &&
        ingredient.amount.isNotBlank() &&
        ingredient.amount.length <= SnapPrompt.MAX_AMOUNT_LENGTH

    private fun acceptsStep(step: RecipeStep): Boolean {
        val text = "${step.shortLabel} ${step.instruction}".lowercase()
        return step.shortLabel.isNotBlank() &&
            step.shortLabel.length <= SnapPrompt.MAX_LABEL_LENGTH &&
            step.instruction.isNotBlank() &&
            step.instruction.length <= SnapPrompt.MAX_INSTRUCTION_LENGTH &&
            step.photo == null &&
            step.safety.all { it in SnapPrompt.SAFETY_TAGS } &&
            (!heat.containsMatchIn(text) || "HOT" in step.safety) &&
            (!sharp.containsMatchIn(text) || "SHARP" in step.safety) &&
            restrictedTools.findAll(text).all { it.value in policy.allowedTools }
    }
}
