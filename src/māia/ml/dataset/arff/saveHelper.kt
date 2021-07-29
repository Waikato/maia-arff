package māia.ml.dataset.arff

fun quoted(string : String) : String {
    return if ("'" in string)
        "\"$string\""
    else if ("\"" in string)
        "'$string'"
    else if (" " in string || string == MISSING_VALUE_SYMBOL || COMMENT_SYMBOL in string)
        "\"$string\""
    else
        string
}
