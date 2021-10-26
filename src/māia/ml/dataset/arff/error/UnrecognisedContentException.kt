package māia.ml.dataset.arff.error

/**
 * Exception for when a line is successfully parsed, but has additional
 * content tacked on the end.
 *
 * @param content
 *          The unexpected content.
 * @param line
 *          The full line that contains the extra content.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
class UnrecognisedContentException(
    content : String,
    line : String
) : ARFFException(
    "Unrecognised content '$content' in line: $line"
)
