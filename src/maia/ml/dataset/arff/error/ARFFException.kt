package maia.ml.dataset.arff.error

import java.lang.Exception

/**
 * Base class for all exceptions coming from the ARFF package.
 *
 * @param message [Exception.message]
 * @param cause [Exception.cause]
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
open class ARFFException(
    message : String?,
    cause: Throwable? = null
) : Exception(message, cause)
