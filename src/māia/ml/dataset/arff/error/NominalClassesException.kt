package māia.ml.dataset.arff.error

/**
 * Exception for when the nominal classes can't be parsed from
 * a nominal attribute type definition.
 *
 * @param string
 *          The string being parsed for nominal classes.
 */
class NominalClassesException(
    string : String
) : ARFFException(
    "Error parsing nominal values from: $string"
)
