package māia.ml.dataset.arff.error

/**
 * Exception for when an attribute's name cannot be parsed.
 *
 * @param line
 *          The line that was being parsed.
 */
class AttributeNameNotFoundException(
    line : String
) : ARFFException(
    "Couldn't parse attribute name from: $line"
)
