package org.hack4impact.risedc.core.data.snap

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.hack4impact.risedc.core.data.model.Ingredient
import org.hack4impact.risedc.core.data.model.MealType
import org.hack4impact.risedc.core.data.model.RecipeStep
import org.hack4impact.risedc.core.data.model.StoreSection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SnapAndCookTest {
    private val policy = SafetyPolicy(setOf("peanut"), setOf("spoon"))
    private val recipe = GeneratedRecipe(
        title = "Beans",
        mealType = MealType.LUNCH,
        minutes = 2,
        ingredients = listOf(Ingredient("canned beans", "1 can", StoreSection.PANTRY)),
        utensils = listOf("spoon"),
        steps = listOf(RecipeStep("Mix beans", "Mix the beans.")),
    )

    @Test
    fun retriesReserveSeparateCallsAndPersistTheCap() = runTest {
        val settings = MapSettings()
        val budget = GenerationBudget(settings) { LocalDate(2026, 10, 26) }
        repeat(49) { assertTrue(budget.reserve()) }
        var requests = 0
        val service = service(budget) {
            requests++
            GeminiReply.Failure(FailureReason.NETWORK, retryable = true)
        }
        assertEquals(SnapResult.LimitReached, service.recipesFrom(byteArrayOf()))
        assertEquals(1, requests)
        assertFalse(GenerationBudget(settings) { LocalDate(2026, 10, 26) }.reserve())
    }

    @Test
    fun concurrentReservationsNeverExceedFifty() = runTest {
        val budget = budget()
        val results = List(80) { async { budget.reserve() } }.awaitAll()
        assertEquals(50, results.count { it })
    }

    @Test
    fun differentDateResetsTheDailyAllowance() = runTest {
        var date = LocalDate(2026, 10, 26)
        val budget = GenerationBudget(MapSettings()) { date }
        repeat(50) { assertTrue(budget.reserve()) }
        assertFalse(budget.reserve())
        for (newDate in listOf(LocalDate(2026, 10, 25), LocalDate(2026, 10, 27))) {
            date = newDate
            repeat(50) { assertTrue(budget.reserve()) }
            assertFalse(budget.reserve())
        }
    }

    @Test
    fun returnsValidatedRecipesWithGeneratedSteps() = runTest {
        val result = service(budget()) {
            GeminiReply.Success(GeneratedReply(true, listOf(recipe, recipe)))
        }.recipesFrom(byteArrayOf())
        val recipes = assertIs<SnapResult.Recipes>(result).recipes
        assertEquals(2, recipes.size)
        assertEquals("Mix the beans.", recipes.first().steps.single().instruction)
        assertTrue(recipes.first().steps.single().safety.isEmpty())
    }

    @Test
    fun validatorRejectsForbiddenFoodToolsAndInvalidSteps() {
        val validator = RecipeValidator(policy)
        assertTrue(validator.accepts(recipe))
        val invalid = listOf(
            recipe.copy(title = ""),
            recipe.copy(minutes = 16),
            recipe.copy(ingredients = emptyList()),
            recipe.copy(ingredients = listOf(recipe.ingredients.single().copy(name = ""))),
            recipe.copy(ingredients = listOf(recipe.ingredients.single().copy(name = "raw chicken with cooked rice"))),
            recipe.copy(ingredients = listOf(recipe.ingredients.single().copy(name = "Raw Eggs"))),
            recipe.copy(ingredients = listOf(recipe.ingredients.single().copy(amount = ""))),
            recipe.copy(utensils = listOf("oven")),
            recipe.copy(steps = List(7) { recipe.steps.single() }),
            recipe.copy(steps = listOf(RecipeStep("", ""))),
            recipe.copy(steps = listOf(RecipeStep("Use oven", "Heat in the oven.", safety = listOf("HOT")))),
        )
        invalid.forEach { assertFalse(validator.accepts(it)) }
    }

    @Test
    fun rawMeatFishAndEggsAreRejectedWithoutFlaggingOtherFood() {
        val validator = RecipeValidator(policy)
        val ingredient = recipe.ingredients.single()
        val rejected = listOf(
            "uncooked chicken",
            "undercooked beef",
            "raw whole egg",
            "Raw Eggs",
            "raw ground beef",
            "raw free-range chicken",
            "raw wild caught salmon",
            "raw carrots and chicken",
            "raw duck",
            "raw poultry",
            "raw seafood",
            "raw shellfish",
            "raw squid",
            "raw sardines",
            "raw trout",
            "raw liver",
            "raw ribs",
            "raw hamburger",
            "raw meatballs",
            "ceviche",
        )
        rejected.forEach { name ->
            assertFalse(validator.accepts(recipe.copy(ingredients = listOf(ingredient.copy(name = name)))), name)
        }
        assertFalse(validator.accepts(recipe.copy(steps = listOf(RecipeStep("Add meat", "Add the uncooked shrimp.")))))
        val allowed = listOf(
            "raw eggplant",
            "raw carrots",
            "raw hamburger buns",
            "raw carrots, cooked chicken",
            "tartare sauce",
        )
        allowed.forEach { name ->
            assertTrue(validator.accepts(recipe.copy(ingredients = listOf(ingredient.copy(name = name)))), name)
        }
        val separateFields = listOf(ingredient.copy(name = "raw carrots"), ingredient.copy(name = "cooked chicken"))
        assertTrue(validator.accepts(recipe.copy(ingredients = separateFields)))
    }

    @Test
    fun neverUseIngredientsAreRejectedAndOtherFoodIsAllowed() {
        val validator = RecipeValidator(policy)
        val ingredient = recipe.ingredients.single()
        val rejected = listOf(
            recipe.copy(ingredients = listOf(ingredient.copy(name = "Peanut Butter"))),
            recipe.copy(ingredients = listOf(ingredient.copy(name = "salted peanuts"))),
            recipe.copy(title = "Peanut beans"),
            recipe.copy(steps = listOf(RecipeStep("Add topping", "Add peanuts to the beans."))),
        )
        rejected.forEach { assertFalse(validator.accepts(it)) }
        val allowed = listOf("canned corn", "apples", "raw carrots", "cooked chicken")
        allowed.forEach { name ->
            assertTrue(validator.accepts(recipe.copy(ingredients = listOf(ingredient.copy(name = name)))))
        }
    }

    @Test
    fun emptyNeverUseListStillRejectsRawFoodButBlankEntriesAreInvalid() {
        val validator = RecipeValidator(SafetyPolicy(emptySet(), setOf("spoon")))
        val ingredient = recipe.ingredients.single()
        assertTrue(validator.accepts(recipe))
        assertFalse(validator.accepts(recipe.copy(ingredients = listOf(ingredient.copy(name = "raw chicken")))))
        assertFailsWith<IllegalArgumentException> { SafetyPolicy(setOf(" "), setOf("spoon")) }
    }

    @Test
    fun openingACanRequiresSharpButMentioningCannedFoodDoesNot() {
        val validator = RecipeValidator(policy.copy(allowedTools = setOf("can opener")))
        val canRecipe = recipe.copy(utensils = listOf("can opener"))
        val openingSteps = listOf(
            RecipeStep("Open can", "Open the can of beans."),
            RecipeStep("Open beans", "Open the canned beans."),
            RecipeStep("Open beans", "Use the can opener on the beans."),
        )
        openingSteps.forEach { step ->
            assertFalse(validator.accepts(canRecipe.copy(steps = listOf(step))))
            assertTrue(validator.accepts(canRecipe.copy(steps = listOf(step.copy(safety = listOf("SHARP"))))))
        }
        assertTrue(validator.accepts(canRecipe.copy(steps = listOf(RecipeStep("Add beans", "Add the canned beans.")))))
        assertFalse(RecipeValidator(policy).accepts(canRecipe))
    }

    @Test
    fun allowedToolsIgnoreCase() {
        val validator = RecipeValidator(policy.copy(allowedTools = setOf("Spoon", "Oven")))
        val ovenStep = RecipeStep("Bake", "Bake in the oven.", safety = listOf("HOT"))
        assertTrue(validator.accepts(recipe.copy(utensils = listOf("spoon"), steps = listOf(ovenStep))))
        assertTrue(validator.accepts(recipe.copy(utensils = listOf("SPOON"))))
        assertFalse(validator.accepts(recipe.copy(utensils = listOf("fork"))))
    }

    @Test
    fun shortLabelsAreLimitedByLengthNotWordCount() {
        val validator = RecipeValidator(policy)
        val instruction = "Mix the beans."
        assertTrue(validator.accepts(recipe.copy(steps = listOf(RecipeStep("Add the canned beans", instruction)))))
        assertFalse(validator.accepts(recipe.copy(steps = listOf(RecipeStep("a".repeat(26), instruction)))))
    }

    @Test
    fun heatWordsRequireHotTagWithoutFlaggingLookalikes() {
        val tools = setOf("spoon", "oven", "stove", "skillet", "grill")
        val validator = RecipeValidator(policy.copy(allowedTools = tools))
        val heatSteps = listOf(
            "Cook the beans.", "Simmer the beans.", "Roast the beans.", "Sear the beans.", "Poach the beans.",
            "Saute the beans.", "Sauté the beans.", "Reheat the beans.", "Preheat the oven.", "Fry the beans.",
            "Stir-fry the beans.", "Stir-fried beans.", "Braise the beans.", "Stew the beans.", "Scald the milk.",
            "Put the pan on the burner.", "Use the stove.", "Warm the skillet.",
        )
        heatSteps.forEach { instruction ->
            val step = RecipeStep("Next", instruction)
            assertFalse(validator.accepts(recipe.copy(steps = listOf(step))), instruction)
            assertTrue(validator.accepts(recipe.copy(steps = listOf(step.copy(safety = listOf("HOT"))))), instruction)
        }
        val coldSteps = listOf("Add the cookies.", "Follow the cookbook.", "Search the pantry.", "Add the chard.")
        coldSteps.forEach { instruction ->
            assertTrue(validator.accepts(recipe.copy(steps = listOf(RecipeStep("Next", instruction)))), instruction)
        }
    }

    @Test
    fun heatAndKnifeStepsRequireTagsButAreNotRestrictedToColdActions() {
        val validator = RecipeValidator(policy.copy(allowedTools = setOf("microwave", "knife")))
        val heatStep = RecipeStep("Heat beans", "Microwave the beans.", safety = listOf("HOT"))
        val knifeStep = RecipeStep("Cut food", "Slice the food.", safety = listOf("SHARP"))
        val warmRecipe = recipe.copy(utensils = listOf("microwave", "knife"), steps = listOf(heatStep, knifeStep))
        assertTrue(validator.accepts(warmRecipe))
        assertFalse(validator.accepts(warmRecipe.copy(steps = listOf(heatStep.copy(safety = emptyList())))))
        assertFalse(validator.accepts(warmRecipe.copy(steps = listOf(knifeStep.copy(safety = emptyList())))))
        assertFalse(validator.accepts(warmRecipe.copy(steps = listOf(heatStep.copy(safety = listOf("UNKNOWN"))))))
    }

    @Test
    fun invalidReplyRetriesButPermanentErrorsDoNot() = runTest {
        var requests = 0
        val result = service(budget()) {
            requests++
            GeminiReply.Success(GeneratedReply(true, listOf(recipe)))
        }.recipesFrom(byteArrayOf())
        assertEquals(SnapResult.Failed(FailureReason.INVALID_REPLY), result)
        assertEquals(2, requests)
        requests = 0
        val permanent = service(budget()) {
            requests++
            GeminiReply.Failure(FailureReason.REQUEST, retryable = false)
        }.recipesFrom(byteArrayOf())
        assertEquals(SnapResult.Failed(FailureReason.REQUEST), permanent)
        assertEquals(1, requests)
    }

    @Test
    fun oversizedRepliesDoNotBypassLocalChecks() = runTest {
        val invalid = recipe.copy(minutes = 16)
        val oversized = service(budget()) {
            GeminiReply.Success(GeneratedReply(true, listOf(recipe, recipe, recipe, invalid)))
        }.recipesFrom(byteArrayOf())
        assertEquals(SnapResult.Failed(FailureReason.INVALID_REPLY), oversized)
    }

    @Test
    fun deadlineAndCancellationDoNotBecomeGenericFailures() = runTest {
        val result = service(budget()) {
            kotlinx.coroutines.delay(31_000)
            GeminiReply.Success(GeneratedReply(true, listOf(recipe, recipe)))
        }.recipesFrom(byteArrayOf())
        assertEquals(SnapResult.TimedOut, result)
        val job = async {
            service(budget()) { throw kotlinx.coroutines.CancellationException() }.recipesFrom(byteArrayOf())
        }
        job.join()
        assertTrue(job.isCancelled)
    }

    @Test
    fun missingConfigurationAndBadPhotoMakeNoRequests() = runTest {
        val client = GeminiClient { _, _ -> error("Unexpected request") }
        val unavailable = GeminiSnapAndCook(client, budget(), null, hasKey = true)
        assertFalse(unavailable.isAvailable)
        assertEquals(SnapResult.Failed(FailureReason.NOT_CONFIGURED), unavailable.recipesFrom(byteArrayOf()))
        val badPhoto = GeminiSnapAndCook(client, budget(), policy, hasKey = true, prepare = { null })
        assertEquals(SnapResult.BadPhoto, badPhoto.recipesFrom(byteArrayOf()))
    }

    private fun budget() = GenerationBudget(MapSettings()) { LocalDate(2026, 10, 26) }

    private fun service(budget: GenerationBudget, reply: suspend () -> GeminiReply) = GeminiSnapAndCook(
        client = GeminiClient { _, _ -> reply() },
        budget = budget,
        policy = policy,
        hasKey = true,
        prepare = { PreparedPhoto(byteArrayOf(1), "image/jpeg") },
    )
}
