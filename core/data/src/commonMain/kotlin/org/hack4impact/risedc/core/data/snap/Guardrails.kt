package org.hack4impact.risedc.core.data.snap

/** Org wide safety policy which defines what ingredients the recipes should not include
 and what tools are allowed for the apps users */
data class SafetyPolicy(val neverUseIngredients: Set<String>, val allowedTools: Set<String>) {
    init {
        require(neverUseIngredients.none { it.isBlank() })
        require(allowedTools.isNotEmpty() && allowedTools.none { it.isBlank() })
    }
}

/** Validates the recipes provided by Gemini follow the orgs defines safety policy */
internal class RecipeValidator(private val policy: SafetyPolicy) {
    private val heat = Regex(
        "\\b(microwav(?!e[- ]safe\\b)|toast|(pre|re)?heat|hot|boil|broil|melt(?!ed\\b)|steam|" +
            "bak(?!ing\\s+(powder|sheets?)\\b)|fry|stir-fr|grill|cook(?!ie|book|ed\\b)|simmer|roast|sear(?!ch)|" +
            "poach|saut|brais|stew|scald|sizzl|" +
            "oven|stove|burner|skillet)[a-z]*",
    )
    private val sharp = Regex(
        // "the cut tomato" is already cut, and "cutting board" names a tool, like "microwave-safe bowl".
        // "Slice" counts only as an action: starting a sentence, after "then" or "and", before what gets
        // sliced ("slice the", "slice 1"), or as "slicing". "1 slice of bread" and "the other slice" don't.
        "\\b((knife|knives|(?<!pre-|\\bthe )cut(?![- ]side\\b|ting[- ]boards?\\b)|" +
            "(?<=\\bthe )cut(?=[a-z])(?!ting[- ]boards?\\b)|" +
            "(?<=^|\\. |\\bthen |\\band )slic(?!es\\b)|" +
            "slic(?=ing\\b|e (the|a|an|it|them|into|up|each|both|\\d)\\b)|" +
            "chop|dic|minc|peeler|grate|shred)" +
            "(\\b|[a-z]*(?<!ed)\\b)|" +
            "can openers?|open(ing)?( (the|a))? (cans?|canned)\\b)",
    )
    private val restrictedTools = Regex("\\b(stove|stovetop|oven|skillet|grill)\\b")
    private val rawFood = Regex(
        "\\b(raw|uncooked|undercooked)\\s+([a-z-]+\\s+){0,2}(meats?|chicken|beef|pork|turkey|lamb|veal|ham|bacon|" +
            "sausages?|steaks?|fish|salmon|tuna|cod|tilapia|shrimp|prawns?|crabs?|lobsters?|clams?|mussels?|" +
            "oysters?|scallops?|eggs?|ducks?|venison|poultry|seafood|shellfish|squids?|sardines?|trout|livers?|" +
            "ribs?|hamburgers?(?! buns?)|meatballs?)\\b|" +
            "\\b(ceviche|(steak|beef|tuna|salmon) tartare)\\b",
    )

    /** A number and at most two words: "1 bag", "1 jar", "2 cans", "1 small box", "3". */
    private val amount = Regex("(\\d+( \\d+/\\d+)?|\\d+/\\d+)( [a-z.]+){0,2}")
    private val neverUse = policy.neverUseIngredients.map { Regex("\\b${Regex.escape(it.trim().lowercase())}") }
    private val allowedTools = policy.allowedTools.map { it.trim().lowercase() }.toSet()

    fun accepts(recipe: GeneratedRecipe): Boolean {
        val ingredientNames = recipe.ingredients.map { it.name.key() }.toSet()
        val utensilNames = recipe.utensils.map { it.name.key() }.toSet()
        return recipe.title.isNotBlank() &&
            recipe.title.length <= SnapPrompt.MAX_TITLE_LENGTH &&
            !mentionsForbiddenFood(recipe) &&
            recipe.minutes in 1..SnapPrompt.MAX_MINUTES &&
            recipe.servings in 1..SnapPrompt.MAX_SERVINGS &&
            recipe.ingredients.size in 1..SnapPrompt.MAX_INGREDIENTS &&
            recipe.ingredients.all(::acceptsIngredient) &&
            recipe.utensils.size in 1..SnapPrompt.MAX_UTENSILS &&
            recipe.utensils.all { it.name.key() in allowedTools } &&
            recipe.steps.size in 1..SnapPrompt.MAX_STEPS &&
            recipe.steps.all(::acceptsStep) &&
            // A step may only point at items the recipe lists, so the app can find their pictures.
            recipe.steps.all { step ->
                step.ingredients.all { it.key() in ingredientNames } && step.utensils.all { it.key() in utensilNames }
            }
    }

    private fun mentionsForbiddenFood(recipe: GeneratedRecipe): Boolean {
        val text = (
            recipe.ingredients.map { it.name } +
                recipe.steps.map { it.instruction } +
                recipe.title
            ).joinToString(". ").lowercase() // Keeps separate fields from matching as one phrase.
        return rawFood.containsMatchIn(text) || neverUse.any { it.containsMatchIn(text) }
    }

    private fun acceptsIngredient(ingredient: GeneratedIngredient): Boolean = ingredient.name.isNotBlank() &&
        ingredient.name.length <= SnapPrompt.MAX_NAME_LENGTH &&
        ingredient.amount.length <= SnapPrompt.MAX_AMOUNT_LENGTH &&
        amount.matches(ingredient.amount.trim().lowercase())

    private fun acceptsStep(step: GeneratedStep): Boolean {
        val text = step.instruction.lowercase()
        return step.instruction.isNotBlank() &&
            step.instruction.length <= SnapPrompt.MAX_INSTRUCTION_LENGTH &&
            step.safety.size <= SnapPrompt.SAFETY_TAGS.size &&
            step.safety.all { it in SnapPrompt.SAFETY_TAGS } &&
            (!heat.containsMatchIn(text) || "HOT" in step.safety) &&
            (!sharp.containsMatchIn(text) || "SHARP" in step.safety) &&
            restrictedTools.findAll(text).all { it.value in allowedTools }
    }
}

/** standardizing item names for simpler regex comparison */
internal fun String.key(): String = trim().lowercase()
