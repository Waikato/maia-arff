package māia.ml.dataset.arff

import māia.ml.dataset.arff.error.KeywordsNotFoundException
import māia.ml.dataset.arff.error.MissingKeywordException
import māia.util.buildSet
import java.io.FileReader

/** Valid set of characters that can delimit data-values. */
val DATA_DELIMITERS = buildSet<Char> { add('\t'); add(','); add('\n') }

/** Valid quoting characters. */
val QUOTES = buildSet<Char> { add('"'); add('\'') }

/** Pattern which matches valid names in an ARFF file. */
const val NAME_PATTERN = "(?<quote>['\"])?(?![{},%])(((?<=['\"]).+?\\k<quote>)|(\\S+))"

/** Pattern which matches valid nominal strings in an ARFF file. */
const val NOMINAL_VALUE_PATTERN = "(?<quote>['\"])?(((?<=['\"]).+?\\k<quote>)|([^\\s,}]+))"

/** Pattern which matches valid attribute type specifiers. */
const val ATTRIBUTE_TYPE_PATTERN = "(numeric|integer|real|string|date|\\{.*?\\})"

/** Pattern which matches a valid date value. TODO: Implement. */
const val DATE_FORMAT_PATTERN = ".*"

/**
 * Attempts to consume a portion of the line that matches the provided
 * pattern.
 *
 * @param line
 *          The line to scan.
 * @param pattern
 *          The pattern to consume.
 * @param ignoreLeadingWhitespace
 *          Whether leading whitespace should be stripped from the line
 *          before attempting to match the pattern.
 * @param caseInsensitive
 *          Whether to ignore case when matching.
 * @return
 *          A pair of:
 *           - The portion from the beginning of the line that matched
 *             the pattern, or null if no match was found.
 *           - The remainder of the line that was not part of a match.
 */
fun splitPatternFromLineStart(
    line : String,
    pattern : String,
    ignoreLeadingWhitespace : Boolean = true,
    caseInsensitive : Boolean = true
) : Pair<String?, String> {
    // Trim of any leading whitespace if we're ignoring it
    val whitespaceHandledLine = if (ignoreLeadingWhitespace) line.trimStart() else line

    // Compile the regex, taking the case-sensitivity into account
    val regex = if (caseInsensitive) Regex(pattern, RegexOption.IGNORE_CASE) else Regex(pattern)

    // Perform the search, returning the unhandled case if no match is found
    val match = regex.find(whitespaceHandledLine) ?: return Pair(null, line)

    // Split the string around the matched portion
    return Pair(
        whitespaceHandledLine.substring(match.range),
        whitespaceHandledLine.substring(match.range.last + 1)
    )
}

/**
 * Reads lines from the file until a given keyword is found.
 *
 * @param lines
 *          The iterator over the ARFF file contents.
 * @param keywords
 *          The keywords to search for.
 * @param searchComments
 *          Whether comment lines should be searched as well.
 * @param caseInsensitive
 *          Whether to ignore the case of words when matching.
 */
fun readTillFound(
    lines: Iterator<String>,
    keywords : Set<String>,
    searchComments : Boolean = false,
    caseInsensitive : Boolean = true
) : String {
    // Keep searching until all lines are exhausted
    while (lines.hasNext()) {
        // Get the next line from the file
        val line = lines.next()

        // Can skip blank lines
        if (isWhitespaceOnly(line)) continue

        // Skip comments if told to
        if (!searchComments && isCommentLine(line)) continue

        // Search foe each word in the line, returning it if one is found
        keywords.forEach { keyword ->
            if (line.contains(keyword, caseInsensitive)) return line
        }
    }

    // We ran out of lines without finding any keywords, so error
    throw KeywordsNotFoundException(keywords)

}

/**
 * Removes any quotes around a string. If there are none, returns
 * the string as is.
 *
 * @param string
 *          The string to remove quotes from. Should not contain
 *          leading/trailing whitespace.
 * @return
 *          The string without quotes.
 */
fun removeQuotes(string : String) : String {
    return if (string.startsWith("\"") && string.endsWith("\""))
        string.substring(1, string.length - 1)
    else if (string.startsWith("'") && string.endsWith("'"))
        string.substring(1, string.length - 1)
    else
        string
}

/**
 * Returns whether the given line is a comment.
 *
 * @param line
 *          The line from the ARFF file to check.
 * @return
 *          True if it's a comment, false if not.
 */
fun isCommentLine(line : String) : Boolean =
    line.startsWith(COMMENT_SYMBOL)

/**
 * Removes the given keyword from the start of the line.
 *
 * @param line
 *          The line starting with the keyword.
 * @param keyword
 *          The keyword to remove.
 * @return
 *          The line sans the keyword.
 */
fun removeKeyword(
    line : String,
    keyword : String
) : String {
    // Throw an error if the line doesn't start with the keyword
    if (!line.startsWith(keyword, true))
        throw MissingKeywordException(line, keyword)

    return line.substring(keyword.length)
}

/**
 * Checks if the given line only contains whitespace.
 *
 * @param line
 *          The line from the ARFF file to check.
 * @return
 *          True if the line only contains whitespace, false if not.
 */
fun isWhitespaceOnly(line: String) : Boolean =
    line.trim() == ""

/**
 * Gets an iterator over the contents of the named file.
 *
 * @param filename
 *          The name of the file to read.
 * @return
 *          The line-by-line content iterator.
 */
fun readLines(filename : String) : Iterator<String> {
    val reader = FileReader(filename).buffered()
    val iterator = reader.lineSequence().iterator()
    return object : Iterator<String> by iterator {
        override fun hasNext() : Boolean {
            return iterator.hasNext().also {
                if (!it) reader.close()
            }
        }
    }
}

