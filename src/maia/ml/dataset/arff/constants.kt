package maia.ml.dataset.arff

/*
 * Constants defined by the ARFF file format.
 */

// The keywords for the different sections of the file

/** Keyword specifying the start of the relation section. */
const val RELATION_SECTION_KEYWORD = "@relation"

/** Keyword specifying the start of the attribute section. */
const val ATTRIBUTE_SECTION_KEYWORD = "@attribute"

/** Keyword specifying the start of the data section. */
const val DATA_SECTION_KEYWORD = "@data"

// Keywords for the different attribute data-types

/** Keyword specifying that an attribute takes numeric values. */
const val NUMERIC_ATTRIBUTE_KEYWORD = "numeric"

/** Alternative to [NUMERIC_ATTRIBUTE_KEYWORD]. Although it says integer, this is not enforced. */
const val INTEGER_ATTRIBUTE_KEYWORD = "integer"

/** Alternative to [NUMERIC_ATTRIBUTE_KEYWORD]. */
const val REAL_ATTRIBUTE_KEYWORD = "real"

// Nominal attributes aren't specified by keyword, but a {}-enclosed
// list of class names.
// const val NOMINAL_ATTRIBUTE_KEYWORD = "nominal"

/** Currently unsupported. */
const val DATE_ATTRIBUTE_KEYWORD = "date"

/** Currently unsupported. */
const val STRING_ATTRIBUTE_KEYWORD = "string"

/** Currently unsupported. */
const val RELATIONAL_ATTRIBUTE_KEYWORD = "relational"

// Misc. symbols

/** The symbol that represents a missing value. */
const val MISSING_VALUE_SYMBOL = "?"

/** The symbol that represents the beginning of a comment. */
const val COMMENT_SYMBOL = "%"
