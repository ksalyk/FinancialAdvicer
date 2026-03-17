package kz.fearsom.financiallifev2.scenarios

internal fun story(vararg paragraphs: String): String =
    paragraphs.joinToString("\n\n") { it.trimIndent().trim() }

