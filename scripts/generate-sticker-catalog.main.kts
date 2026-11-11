#!/usr/bin/env kotlin

// Generates core/data/.../sticker/StickerCatalog.kt from the wiki's sticker manifest.
//
//   kotlin scripts/generate-sticker-catalog.main.kts            # write the catalog
//   kotlin scripts/generate-sticker-catalog.main.kts --check    # exit 1 if it is out of date
//   kotlin scripts/generate-sticker-catalog.main.kts --manifest <path/to/manifest.csv>
//
// Needs the Kotlin CLI (`brew install kotlin`). Rerun whenever manifest.csv changes and commit the result.

import java.io.File
import kotlin.system.exitProcess

/** Manifest folder prefixes that are ingredients. 11 household, 13 app icons, 14 dishes, 15 step tiles are left out. */
val ingredientFolders = setOf("01", "02", "03", "04", "05", "06", "07", "08", "09", "12", "17")

/** Manifest folder prefixes that are utensils. */
val utensilFolders = setOf("10", "16")

/** Manifest store_section text -> StoreSection enum constant. Unknown text fails the run. */
val sections = mapOf(
    "Produce" to "PRODUCE",
    "Bakery" to "BAKERY",
    "Deli" to "DELI",
    "Meat & seafood" to "MEAT_AND_SEAFOOD",
    "Dairy & eggs" to "DAIRY_AND_EGGS",
    "Frozen" to "FROZEN",
    "Pantry" to "PANTRY",
    "Snacks & drinks" to "SNACKS_AND_DRINKS",
    "Household" to "HOUSEHOLD",
)

/** Section for an ingredient whose manifest row has no store_section. */
val fallbackSection = "OTHER"

val repoRoot: File = File(__FILE__.absolutePath).parentFile.parentFile
val outFile = File(
    repoRoot,
    "core/data/src/commonMain/kotlin/org/hack4impact/risedc/core/data/sticker/StickerCatalog.kt",
)

val check = "--check" in args
val manifest: File = args.indexOf("--manifest").takeIf { it >= 0 }
    ?.let { File(args.getOrNull(it + 1) ?: fail("--manifest needs a path")) }
    ?: File(repoRoot, "../rise-dc-wiki/raw/design/stickers/manifest.csv")

fun fail(message: String): Nothing {
    System.err.println("error: $message")
    exitProcess(1)
}

/** RFC 4180 fields for one line: commas split, double quotes wrap, "" is a literal quote. No multi-line fields. */
fun parseCsvLine(line: String): List<String> {
    val fields = mutableListOf<String>()
    val field = StringBuilder()
    var quoted = false
    var i = 0
    while (i < line.length) {
        val c = line[i]
        when {
            quoted && c == '"' && line.getOrNull(i + 1) == '"' -> { field.append('"'); i++ }
            c == '"' -> quoted = !quoted
            !quoted && c == ',' -> { fields += field.toString(); field.clear() }
            else -> field.append(c)
        }
        i++
    }
    if (quoted) fail("unterminated quote in: $line")
    fields += field.toString()
    return fields
}

fun kotlinString(s: String): String =
    "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$") + "\""

data class Row(val id: String, val name: String, val section: String)

if (!manifest.isFile) fail("manifest not found: ${manifest.path}")
val lines = manifest.readLines().filter { it.isNotBlank() }
val header = parseCsvLine(lines.first())
val expected = listOf("file", "id", "name", "category", "store_section")
if (header != expected) fail("unexpected manifest header $header, expected $expected")

val ingredients = mutableListOf<Row>()
val utensils = mutableListOf<Row>()
val seenIds = mutableSetOf<String>()
val unsectioned = mutableListOf<String>()

for ((index, line) in lines.drop(1).withIndex()) {
    val lineNo = index + 2
    val fields = parseCsvLine(line)
    if (fields.size != expected.size) fail("line $lineNo has ${fields.size} fields, expected ${expected.size}")
    val (file, id, name, _, section) = fields.map { it.trim() }
    val folder = file.substringBefore('/').substringBefore('-')
    val isIngredient = folder in ingredientFolders
    if (!isIngredient && folder !in utensilFolders) continue

    if (id.isEmpty()) fail("line $lineNo has no id")
    if (name.isEmpty()) fail("line $lineNo ($id) has no name")
    if (!seenIds.add(id)) fail("duplicate id $id on line $lineNo")

    if (isIngredient) {
        val enumName = if (section.isEmpty()) {
            unsectioned += id
            fallbackSection
        } else {
            sections[section] ?: fail("line $lineNo ($id) has unknown store_section \"$section\"")
        }
        ingredients += Row(id, name, enumName)
    } else {
        utensils += Row(id, name, "")
    }
}

/** Natural order: S2 before S10, 60 before 60b. */
fun naturalKey(id: String): List<Comparable<*>> =
    Regex("""\d+|\D+""").findAll(id).map { it.value.toIntOrNull() ?: it.value }.toList()

val naturalOrder = Comparator<Row> { a, b ->
    val ka = naturalKey(a.id)
    val kb = naturalKey(b.id)
    for (i in 0 until minOf(ka.size, kb.size)) {
        val x = ka[i]
        val y = kb[i]
        val cmp = when {
            x is Int && y is Int -> x.compareTo(y)
            else -> x.toString().compareTo(y.toString())
        }
        if (cmp != 0) return@Comparator cmp
    }
    ka.size.compareTo(kb.size)
}

val manifestPath = "../rise-dc-wiki/raw/design/stickers/manifest.csv"
val output = buildString {
    appendLine("// GENERATED by scripts/generate-sticker-catalog.main.kts. Do not edit.")
    appendLine("// Source: $manifestPath")
    appendLine("@file:Suppress(\"LargeClass\")")
    appendLine()
    appendLine("package org.hack4impact.risedc.core.data.sticker")
    appendLine()
    appendLine("import org.hack4impact.risedc.core.data.model.StoreSection")
    appendLine()
    appendLine("/** An ingredient sticker. [id] is the manifest id and doubles as the image id. */")
    appendLine("data class IngredientSticker(val id: String, val name: String, val storeSection: StoreSection)")
    appendLine()
    appendLine("/** A utensil sticker. [id] is the manifest id and doubles as the image id. */")
    appendLine("data class UtensilSticker(val id: String, val name: String)")
    appendLine()
    appendLine("/** Every sticker the app can show for an ingredient or a utensil, keyed by manifest id. */")
    appendLine("object StickerCatalog {")
    appendLine("    val ingredients: Map<String, IngredientSticker> = listOf(")
    for (r in ingredients.sortedWith(naturalOrder)) {
        appendLine(
            "        IngredientSticker(${kotlinString(r.id)}, ${kotlinString(r.name)}, StoreSection.${r.section}),",
        )
    }
    appendLine("    ).associateBy { it.id }")
    appendLine()
    appendLine("    val utensils: Map<String, UtensilSticker> = listOf(")
    for (r in utensils.sortedWith(naturalOrder)) {
        appendLine("        UtensilSticker(${kotlinString(r.id)}, ${kotlinString(r.name)}),")
    }
    appendLine("    ).associateBy { it.id }")
    appendLine("}")
}

if (check) {
    if (!outFile.isFile || outFile.readText() != output) {
        fail("${outFile.relativeTo(repoRoot)} is out of date; rerun scripts/generate-sticker-catalog.main.kts")
    }
    println("StickerCatalog.kt is up to date.")
} else {
    outFile.parentFile.mkdirs()
    outFile.writeText(output)
    println("Wrote ${outFile.relativeTo(repoRoot)}: ${ingredients.size} ingredients, ${utensils.size} utensils.")
}
if (unsectioned.isNotEmpty()) {
    println("${unsectioned.size} ingredients have no store_section in the manifest, using $fallbackSection: ${unsectioned.joinToString()}")
}
