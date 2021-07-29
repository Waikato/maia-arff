package māia.ml.dataset.arff

import māia.util.buildSet
import java.lang.Exception

val DATA_DELIMITERS = buildSet<Char> { add('\t'); add(','); add('\n') }

val QUOTES = buildSet<Char> { add('"'); add('\'') }

const val NAME_PATTERN = "(?<quote>['\"])?(?![{},%])(((?<=['\"]).+?\\k<quote>)|(\\S+))"

const val NOMINAL_VALUE_PATTERN = "(?<quote>['\"])?(((?<=['\"]).+?\\k<quote>)|([^\\s,}]+))"

const val ATTRIBUTE_TYPE_PATTERN = "(numeric|integer|real|string|date|\\{.*?\\})"

const val DATA_VALUE_PATTERN = "(?<quote>['\"])?(((?<=['\"]).*?\\k<quote>)|(.*?))\\s*(\\t|,|\\n)"

const val DATE_FORMAT_PATTERN = ".*"

fun consume(line : String, pattern : String, ignoreLeadingWhitespace : Boolean = true, caseInsensitive : Boolean = true) : Pair<String?, String> {

    val actualLine = if (ignoreLeadingWhitespace) line.trimStart() else line

    val re = if (caseInsensitive) Regex(pattern, RegexOption.IGNORE_CASE) else Regex(pattern)

    val match = re.find(actualLine) ?: return Pair(null, line)

    return Pair(actualLine.substring(match.range), actualLine.substring(match.range.last + 1))
}

fun readTillFound(lines: Iterator<String>, words : Set<String>, searchComments : Boolean = false, caseInsensitive : Boolean = true) : String {

    var casedWords = words
    if (caseInsensitive) {
        casedWords = buildSet {
            words.forEach { word ->
                add(word.lowercase())
            }
        }
    }

    var line : String? = null
    while (line == null || lines.hasNext()) {
        line = lines.next()

        if (isWhitespaceOnly(line)) continue

        if (!searchComments && isCommentLine(line)) continue

        val searchLine = if (caseInsensitive) line.lowercase() else line

        casedWords.forEach {word ->
            if (searchLine.contains(word)) return line
        }

    }

    throw Exception("Keywords not found: $words")

}

fun removeQuotes(string : String) : String {
    return if (string.startsWith("\"") && string.endsWith("\""))
        string.substring(1, string.length - 1)
    else if (string.startsWith("'") && string.endsWith("'"))
        string.substring(1, string.length - 1)
    else
        string
}

fun isCommentLine(line : String) : Boolean {
    return lineStartsWith(line, COMMENT_SYMBOL, false)
}

fun removeKeyword(line : String, keyword : String) : String {
    if (!lineStartsWith(line, keyword)) throw Exception("$line does not start with keyword: $keyword")

    return line.substring(keyword.length)
}

fun isWhitespaceOnly(line: String) : Boolean {
    return line.trim() == ""
}

fun lineStartsWith(line : String, string : String, caseInsensitive: Boolean = true) : Boolean {
    val actualLine = if (caseInsensitive) line.lowercase() else line
    val actualString = if (caseInsensitive) string.lowercase() else string
    return actualLine.startsWith(actualString)
}

class KeywordNotFoundError(keywords : Iterable<String>) : ARFFError("Couldn't find line containing any of: ${keywords.joinToString()}")

class UnrecognisedContentError(content : String, line : String) : ARFFError("Unrecognised content '$content' in line: $line")
