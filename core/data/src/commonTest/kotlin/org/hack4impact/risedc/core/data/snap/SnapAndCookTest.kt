package org.hack4impact.risedc.core.data.snap

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.hack4impact.risedc.core.data.model.Ingredient
import org.hack4impact.risedc.core.data.model.MealType
import org.hack4impact.risedc.core.data.model.RecipeSource
import org.hack4impact.risedc.core.data.model.StepItem
import org.hack4impact.risedc.core.data.model.StoreSection
import org.hack4impact.risedc.core.data.model.Utensil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SnapAndCookTest {
    private val policy = SafetyPolicy(setOf("peanut"), setOf("spoon"))
    private val recipe = GeneratedRecipe(
        title = "Beans",
        mealType = MealType.LUNCH,
        minutes = 2,
        servings = 1,
        ingredients = listOf(GeneratedIngredient("canned beans", "1 can", StoreSection.PANTRY)),
        utensils = tools("spoon"),
        steps = listOf(step("Mix the beans.")),
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
    fun corruptUsageRecordResetsTheDailyAllowance() = runTest {
        val settings = MapSettings()
        settings.putString("snap.dailyCallUsage", "not json")
        val budget = GenerationBudget(settings) { LocalDate(2026, 10, 26) }
        repeat(50) { assertTrue(budget.reserve()) }
        assertFalse(budget.reserve())
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
            recipe.copy(ingredients = listOf(recipe.ingredients.single().copy(amount = "some"))),
            recipe.copy(utensils = tools("oven")),
            recipe.copy(steps = List(SnapPrompt.MAX_STEPS + 1) { recipe.steps.single() }),
            recipe.copy(steps = listOf(step(""))),
            recipe.copy(steps = listOf(step("Heat in the oven.", "HOT"))),
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
        assertFalse(validator.accepts(recipe.copy(steps = listOf(step("Add the uncooked shrimp.")))))
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
            recipe.copy(steps = listOf(step("Add peanuts to the beans."))),
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
        val canRecipe = recipe.copy(utensils = tools("can opener"))
        val openingSteps = listOf(
            step("Open the can of beans."),
            step("Open the canned beans."),
            step("Use the can opener on the beans."),
        )
        openingSteps.forEach { step ->
            assertFalse(validator.accepts(canRecipe.copy(steps = listOf(step))))
            assertTrue(validator.accepts(canRecipe.copy(steps = listOf(step.copy(safety = listOf("SHARP"))))))
        }
        assertTrue(validator.accepts(canRecipe.copy(steps = listOf(step("Add the canned beans.")))))
        assertFalse(RecipeValidator(policy).accepts(canRecipe))
    }

    @Test
    fun allowedToolsIgnoreCase() {
        val validator = RecipeValidator(policy.copy(allowedTools = setOf("Spoon", "Oven")))
        val ovenStep = step("Bake in the oven.", "HOT")
        assertTrue(validator.accepts(recipe.copy(utensils = tools("spoon"), steps = listOf(ovenStep))))
        assertTrue(validator.accepts(recipe.copy(utensils = tools("SPOON"))))
        assertFalse(validator.accepts(recipe.copy(utensils = tools("fork"))))
    }

    @Test
    fun servingsMustBeBetweenOneAndTheLimit() {
        val validator = RecipeValidator(policy)
        listOf(1, SnapPrompt.MAX_SERVINGS).forEach { assertTrue(validator.accepts(recipe.copy(servings = it)), "$it") }
        listOf(0, -1, SnapPrompt.MAX_SERVINGS + 1).forEach {
            assertFalse(validator.accepts(recipe.copy(servings = it)), "$it")
        }
    }

    @Test
    fun storeAmountsAreANumberAndAShortPackage() {
        val validator = RecipeValidator(policy)
        val ingredient = recipe.ingredients.single()
        val short = listOf("1 bag", "1 jar", "1 loaf", "1 dozen", "2 cans", "1 small box", "3", "1 Bunch", " 1 bag ")
        short.forEach { amount ->
            assertTrue(validator.accepts(recipe.copy(ingredients = listOf(ingredient.copy(amount = amount)))), amount)
        }
        val long = listOf(
            "",
            " ",
            "a jar",
            "some",
            "one bag",
            "1 bag, any brand",
            "1 can (15 oz)",
            "1 loaf of whole wheat bread",
            "enough for the recipe",
        )
        long.forEach { amount ->
            assertFalse(validator.accepts(recipe.copy(ingredients = listOf(ingredient.copy(amount = amount)))), amount)
        }
    }

    @Test
    fun stepsMayOnlyUseItemsTheRecipeLists() {
        val validator = RecipeValidator(policy)
        val listed = step("Mix the beans.").copy(ingredients = listOf(" Canned Beans "), utensils = listOf("SPOON"))
        assertTrue(validator.accepts(recipe.copy(steps = listOf(listed))))
        assertFalse(validator.accepts(recipe.copy(steps = listOf(listed.copy(ingredients = listOf("rice"))))))
        assertFalse(validator.accepts(recipe.copy(steps = listOf(listed.copy(utensils = listOf("fork"))))))
    }

    @Test
    fun toRecipeAttachesPicturesAndCopiesThemOntoSteps() {
        val generated = recipe.copy(
            title = " Toast with jelly ",
            servings = 2,
            imageId = "D01",
            ingredients = listOf(
                GeneratedIngredient("Bread", "2 slices", StoreSection.PANTRY, imageId = "R01"),
                GeneratedIngredient("Jelly", "1 tbsp", StoreSection.PANTRY, imageId = "made-up"),
            ),
            utensils = listOf(GeneratedUtensil("Knife", "K46"), GeneratedUtensil("Plate", SnapPrompt.NO_IMAGE)),
            steps = listOf(
                GeneratedStep(
                    instruction = "Spread 1 tbsp of jelly on the bread.",
                    imageId = "STEP02",
                    ingredients = listOf("jelly", "BREAD"),
                    utensils = listOf("knife"),
                ),
            ),
        )
        val mapped = generated.toRecipe("id-1")
        assertEquals("id-1", mapped.id)
        assertEquals("Toast with jelly", mapped.title)
        assertEquals(2, mapped.servings)
        assertEquals("D01", mapped.imageId)
        assertEquals(RecipeSource.GENERATED, mapped.source)
        assertEquals("R01", mapped.ingredients[0].imageId)
        assertNull(mapped.ingredients[1].imageId, "an id that isn't in the catalog becomes null")
        assertEquals(listOf(Utensil("Knife", "K46"), Utensil("Plate", null)), mapped.utensils)
        val step = mapped.steps.single()
        assertEquals("STEP02", step.imageId)
        assertEquals(listOf(StepItem("Jelly", null), StepItem("Bread", "R01")), step.ingredients)
        assertEquals(listOf(StepItem("Knife", "K46")), step.utensils)
    }

    @Test
    fun imageIdsMustComeFromTheMatchingList() {
        // A real id from the wrong list is still no picture: a dish isn't an ingredient, a step tile isn't a dish.
        val generated = recipe.copy(
            imageId = "STEP02",
            ingredients = listOf(GeneratedIngredient("Toast", "1 slice", imageId = "D01")),
            steps = listOf(step("Mix the beans.").copy(imageId = "K45")),
        )
        val mapped = generated.toRecipe("id")
        assertNull(mapped.imageId)
        assertNull(mapped.ingredients.single().imageId)
        assertNull(mapped.steps.single().imageId)
    }

    @Test
    fun storeSectionComesFromTheStickerWhenItHasOne() {
        val ingredients = listOf(
            // Whole milk is Dairy & eggs in the manifest, whatever Gemini says.
            GeneratedIngredient("Milk", "1 cup", StoreSection.PANTRY, imageId = "308"),
            // No sticker: Gemini's section is kept.
            GeneratedIngredient("Mystery", "1 cup", StoreSection.DELI),
            // R09 has no section in the manifest, so Gemini's is kept.
            GeneratedIngredient("Berries", "1 cup", StoreSection.FROZEN, imageId = "R09"),
        )
        val sections = recipe.copy(ingredients = ingredients).toRecipe("id").ingredients.map(Ingredient::storeSection)
        assertEquals(listOf(StoreSection.DAIRY_AND_EGGS, StoreSection.DELI, StoreSection.FROZEN), sections)
    }

    @Test
    fun heatWordsRequireHotTagWithoutFlaggingLookalikes() {
        val tools = setOf("spoon", "oven", "stove", "skillet", "grill")
        val validator = RecipeValidator(policy.copy(allowedTools = tools))
        val heatSteps = listOf(
            "Cook the beans.", "Simmer the beans.", "Roast the beans.", "Sear the beans.", "Poach the beans.",
            "Saute the beans.", "Sauté the beans.", "Reheat the beans.", "Preheat the oven.", "Fry the beans.",
            "Stir-fry the beans.", "Stir-fried beans.", "Braise the beans.", "Stew the beans.", "Scald the milk.",
            "Put the pan on the burner.", "Use the stove.", "Warm the skillet.", "Bake the beans.",
            "Add baking powder, then bake.", "Baking the beans.", "Melt the butter.", "Melting the butter.",
            "Add the melted butter, then melt the cheese.", "Bake on the baking sheet.",
            "Microwave the bowl.", "Put the microwave-safe bowl in the microwave.",
            "Microwave the microwave-safe bowl.", "Microwaving the peas.",
        )
        heatSteps.forEach { instruction ->
            val step = step(instruction)
            assertFalse(validator.accepts(recipe.copy(steps = listOf(step))), instruction)
            assertTrue(validator.accepts(recipe.copy(steps = listOf(step.copy(safety = listOf("HOT"))))), instruction)
        }
        val coldSteps = listOf(
            "Add the cookies.",
            "Follow the cookbook.",
            "Search the pantry.",
            "Add the chard.",
            "Add the baking powder.",
            "Stir in Baking Powder.",
            "Add the melted butter.",
            "Stir until Melted.",
            "Place the bagels on a baking sheet.",
            "Line two Baking Sheets with paper.",
            "Add green peas to the microwave-safe bowl.",
            "Mix the peas in the Microwave-Safe Bowl.",
            "Pour the milk into the microwave safe bowl.",
        )
        coldSteps.forEach { instruction ->
            assertTrue(validator.accepts(recipe.copy(steps = listOf(step(instruction)))), instruction)
        }
    }

    @Test
    fun knifeActionsRequireSharpTagButPreparedFoodDoesNot() {
        val validator = RecipeValidator(policy.copy(allowedTools = setOf("spoon", "knife")))
        val sharpSteps = listOf(
            "Slice the bagel.",
            "Slicing the bagel.",
            "Cut the bagel into slices.",
            "Cut the bagel.",
            "Cutting the bagel.",
            "Chop the onion.",
            "Dice the tomato.",
            "Mince the garlic.",
            "Peel the potato with the peeler.",
            "Grate the cheese.",
            "Shred the cheese.",
            "Shredding the cheese.",
            "Use the knife.",
            "Add the slices, then slice the bagel.",
            "Add the shredded cheese, then shred the lettuce.",
            "Add the pre-cut carrots, then cut the bagel.",
            "Place the bagel cut side up, then cut the apple.",
            "Put 1 slice of bread on the plate, then slice the tomato.",
        )
        sharpSteps.forEach { instruction ->
            val step = step(instruction)
            assertFalse(validator.accepts(recipe.copy(steps = listOf(step))), instruction)
            assertTrue(validator.accepts(recipe.copy(steps = listOf(step.copy(safety = listOf("SHARP"))))), instruction)
        }
        val plainSteps = listOf(
            "Top with pepperoni slices.",
            "Add the Apple Slices.",
            "Put 1 slice of bread on the plate.",
            "Put a slice of cheese on top of the bread.",
            "Put 12 slice of cheese on the plate.",
            "Sprinkle the shredded mozzarella cheese on top.",
            "Use sliced bagels.",
            "Add the chopped onion.",
            "Add the Diced tomatoes.",
            "Add the minced garlic.",
            "Add the peeled banana.",
            "Add the grated cheese.",
            "Add the pre-cut carrots.",
            "Add the precut carrots.",
            "Place the bagel cut side up.",
            "Place the bagel cut-side up.",
        )
        plainSteps.forEach { instruction ->
            assertTrue(validator.accepts(recipe.copy(steps = listOf(step(instruction)))), instruction)
        }
    }

    @Test
    fun heatAndKnifeStepsRequireTagsButAreNotRestrictedToColdActions() {
        val validator = RecipeValidator(policy.copy(allowedTools = setOf("microwave", "knife")))
        val heatStep = step("Microwave the beans.", "HOT")
        val knifeStep = step("Slice the food.", "SHARP")
        val warmRecipe = recipe.copy(utensils = tools("microwave", "knife"), steps = listOf(heatStep, knifeStep))
        assertTrue(validator.accepts(warmRecipe))
        assertFalse(validator.accepts(warmRecipe.copy(steps = listOf(heatStep.copy(safety = emptyList())))))
        assertFalse(validator.accepts(warmRecipe.copy(steps = listOf(knifeStep.copy(safety = emptyList())))))
        assertFalse(validator.accepts(warmRecipe.copy(steps = listOf(heatStep.copy(safety = listOf("UNKNOWN"))))))
    }

    @Test
    fun oneSafeRecipeIsReturnedWithoutRetrying() = runTest {
        val invalid = recipe.copy(minutes = 16)
        for (reply in listOf(listOf(recipe), listOf(recipe, invalid))) {
            var requests = 0
            val result = service(budget()) {
                requests++
                GeminiReply.Success(GeneratedReply(true, reply))
            }.recipesFrom(byteArrayOf())
            assertEquals(1, assertIs<SnapResult.Recipes>(result).recipes.size)
            assertEquals(1, requests)
        }
    }

    @Test
    fun foodWithNoSafeRecipesRetriesThenFails() = runTest {
        val invalid = recipe.copy(minutes = 16)
        for (reply in listOf(emptyList(), listOf(invalid, invalid))) {
            var requests = 0
            val result = service(budget()) {
                requests++
                GeminiReply.Success(GeneratedReply(true, reply))
            }.recipesFrom(byteArrayOf())
            assertEquals(SnapResult.Failed(FailureReason.INVALID_REPLY), result)
            assertEquals(2, requests)
        }
    }

    @Test
    fun invalidReplyRetriesButPermanentErrorsDoNot() = runTest {
        var requests = 0
        val result = service(budget()) {
            requests++
            GeminiReply.Success(GeneratedReply(true, listOf(recipe.copy(minutes = 16))))
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

    private fun step(instruction: String, vararg safety: String) = GeneratedStep(instruction, safety = safety.toList())

    private fun tools(vararg names: String) = names.map { GeneratedUtensil(it) }

    private fun budget() = GenerationBudget(MapSettings()) { LocalDate(2026, 10, 26) }

    private fun service(budget: GenerationBudget, reply: suspend () -> GeminiReply) = GeminiSnapAndCook(
        client = GeminiClient { _, _ -> reply() },
        budget = budget,
        policy = policy,
        hasKey = true,
        prepare = { PreparedPhoto(byteArrayOf(1), "image/jpeg") },
    )
}
