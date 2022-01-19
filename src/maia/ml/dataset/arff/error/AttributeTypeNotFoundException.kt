package maia.ml.dataset.arff.error

/**
 * Exception for when an attribute's type cannot be parsed.
 *
 * @param line
 *          The line that was being parsed.
 */
class AttributeTypeNotFoundException(
    line : String
) : ARFFException(
    "Couldn't parse attribute type from: $line"
)
