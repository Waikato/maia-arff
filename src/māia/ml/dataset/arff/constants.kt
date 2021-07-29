package māia.ml.dataset.arff

/*
 * Constants defined by the ARFF file format.
 */

// The keywords for the different sections of the file
const val RELATION_SECTION_KEYWORD = "@relation"
const val ATTRIBUTE_SECTION_KEYWORD = "@attribute"
const val DATA_SECTION_KEYWORD = "@data"

// Keywords for the different attribute datatypes
const val NUMERIC_ATTRIBUTE_KEYWORD = "numeric"
const val INTEGER_ATTRIBUTE_KEYWORD = "integer"
const val REAL_ATTRIBUTE_KEYWORD = "real"
const val NOMINAL_ATTRIBUTE_KEYWORD = "nominal"
const val DATE_ATTRIBUTE_KEYWORD = "date"
const val STRING_ATTRIBUTE_KEYWORD = "string"
const val RELATIONAL_ATTRIBUTE_KEYWORD = "relational"

// The symbol that represents a missing value
const val MISSING_VALUE_SYMBOL = "?"

// The symbol that represents the beginning of a comment
const val COMMENT_SYMBOL = "%"
