package māia.ml.dataset.arff.error

/**
 * Exception thrown for valid ARFF attribute types that aren't supported
 * in MĀIA yet.
 *
 * @param attributeType
 *          The attribute type that isn't supported.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
class UnsupportedAttributeTypeException(
    attributeType: String
): ARFFException(
    "Unsupported attribute type: $attributeType"
)
