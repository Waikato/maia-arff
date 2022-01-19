package maia.ml.dataset.arff.error

/**
 * Error for when the file doesn't contain a valid relation name.
 *
 * @param line
 *          The line containing the @relation keyword.
 */
class RelationNameNotValidException(
    line : String
) : ARFFException(
    "Couldn't parse relation name from: $line"
)
