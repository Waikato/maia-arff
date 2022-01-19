package maia.ml.dataset.arff.error

/**
 * Exception for when a line of data doesn't contain a value for each attribute.
 *
 * @param numAttributes
 *          The expected number of attributes.
 * @param line
 *          The line being parsed.
 * @param cause [ARFFException.cause]
 */
class DataSizeMismatchException(
    numAttributes : Int,
    line : String,
    cause: Throwable? = null
) : ARFFException(
    "Wrong number of values (require $numAttributes) in: $line",
    cause
)
