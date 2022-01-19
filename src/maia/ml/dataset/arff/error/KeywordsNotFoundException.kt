package maia.ml.dataset.arff.error

/**
 * Exception indicating that none of a set of required keywords was found.
 *
 * @param keywords
 *          The keywords that were being searched for.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
class KeywordsNotFoundException(
    keywords : Iterable<String>
) : ARFFException(
    "Couldn't find line containing any of: ${keywords.joinToString()}"
)
