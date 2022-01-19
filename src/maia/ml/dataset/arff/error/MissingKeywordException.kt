package maia.ml.dataset.arff.error

/**
 * Exception indicating that a line from the file didn't start
 * with an expected keyword.
 *
 * @param line
 *          The line being checked.
 * @param keyword
 *          The keyword being searched for.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
class MissingKeywordException(
    line : String,
    keyword : String
): ARFFException(
    "Expected line to start with keyword '$keyword': $line"
)
