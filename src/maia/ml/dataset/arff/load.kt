package maia.ml.dataset.arff

import maia.ml.dataset.arff.error.AttributeNameNotFoundException
import maia.ml.dataset.arff.error.AttributeTypeNotFoundException
import maia.ml.dataset.arff.error.DataSizeMismatchException
import maia.ml.dataset.arff.error.NominalClassesException
import maia.ml.dataset.arff.error.RelationNameNotValidException
import maia.ml.dataset.arff.error.UnrecognisedContentException
import maia.ml.dataset.arff.error.UnsupportedAttributeTypeException
import maia.ml.dataset.headers.MutableDataColumnHeaders
import maia.ml.dataset.type.DataRepresentation
import maia.ml.dataset.type.DataType
import maia.ml.dataset.type.standard.Nominal
import maia.ml.dataset.type.standard.Numeric
import maia.util.buildSet
import maia.util.itemIterator
import maia.util.process
import maia.util.zip


/**
 * Returns a data-stream over the contents of the given ARFF file.
 *
 * @param filename
 *          The name of the ARFF file to load.
 * @param batch
 *          Whether to batch the lines at read-time. If false, the @data
 *          section of the file is streamed as rows are consumed.
 * @return
 *          The ARFF data-set.
 */
fun load(
    filename : String,
    batch: Boolean = false
) : ARFFDataStream {
    // Get the file content iterator
    val lines = readLines(filename)

    // Parse the relation section
    val relationName = parseRelationSection(lines)

    // Parse the attributes section
    val attributes = parseAttributeSection(lines)

    // Create the headers for the types
    val headers = MutableDataColumnHeaders(attributes.size)
    attributes.forEach { (name, type) ->
        headers.append(name, type, false)
    }

    // Convert the remaining lines into data-rows on the fly
    val rowIterator = createStreamingDataParser(lines, headers)

    // Return the requested type of data-set
    return if (batch)
        ARFFDataBatch(relationName, headers, rowIterator)
    else
        ARFFDataStream(relationName, headers, rowIterator)
}

/**
 * Transforms the iterator of lines from the @data section into
 * an iterator of data-rows.
 *
 * @param lines
 *          The lines of the @data section.
 * @param headers
 *          The headers parsed from the attributes.
 * @return
 *          An iterator over the parsed data-rows.
 */
fun createStreamingDataParser(
    lines : Iterator<String>,
    headers: MutableDataColumnHeaders
): Iterator<ARFFDataRow> {
    return lines.process { line ->
        if (isWhitespaceOnly(line) || isCommentLine(line))
            null
        else
            itemIterator(parseDataLine(line, headers))
    }
}

/**
 * Parses a single line from the @data section into a data-row.
 *
 * @param line
 *          The line of data to parse.
 * @param headers
 *          The headers parsed from the attributes.
 * @return
 *          The parsed data as a data-row.
 */
fun parseDataLine(
    line : String,
    headers: MutableDataColumnHeaders
): ARFFDataRow {
    // Split the line into individual values
    val values = splitValues(line, headers.size)

    // Create a map of prepared values
    // TODO: Choose a more efficient data-structure
    val valuesMap = HashMap<DataRepresentation<*, *, *>, Any?>()
    zip(headers, values).forEach { (header, value) ->
        val type = header.type
        when (type) {
            is Numeric<*, *> -> {
                val representation = type.canonicalRepresentation
                valuesMap[representation] = if (value == MISSING_VALUE_SYMBOL)
                    null
                else
                    value.toDouble().also { representation.validate(it) }
            }
            is Nominal<*, *, *, *, *> -> {
                if (value == MISSING_VALUE_SYMBOL) {
                    valuesMap[type.canonicalRepresentation] = null
                    valuesMap[type.labelRepresentation] = null
                    valuesMap[type.indexRepresentation] = null
                    valuesMap[type.entropicRepresentation] = null
                } else {
                    type.labelRepresentation.validate(value)
                    val index = type.indexOf(value)
                    valuesMap[type.canonicalRepresentation] = type.oneHot(index)
                    valuesMap[type.labelRepresentation] = value
                    valuesMap[type.indexRepresentation] = index
                    valuesMap[type.entropicRepresentation] = index.toBigInteger()
                }
            }
            else -> throw UnsupportedAttributeTypeException("$type")
        }
    }

    return ARFFDataRow(headers, valuesMap)
}

/**
 * Scans the given data-line for values and splits them into a list.
 *
 * @param line
 *          The data-line to scan.
 * @param numAttributes
 *          The expected number of attributes to split.
 * @return
 *          A list of the separated values.
 */
fun splitValues(
    line: String,
    numAttributes: Int
): List<String> {
    var j = 0
    val lineLength = line.length
    val values = ArrayList<String>()
    try {
        repeat(numAttributes) {
            // Skip any leading whitespace
            while (line[j] == ' ') j++

            // Scan for the start and end of the next value, removing quotes
            val value = if (line[j] in QUOTES) {
                // Record what quote character was used
                val quote = line[j]

                // Record the start-index of the value
                val start = ++j

                // Scan until the matching end-quote
                while (line[j] != quote) j++

                // Record the end-index of the value
                val end = j++

                // Scan to the next delimiter to remove trailing whitespace
                if (j < lineLength) {
                    var char = line[j]
                    while (char == ' ') char = line[++j]
                    if (char !in DATA_DELIMITERS)
                        throw UnrecognisedContentException(line.substring(j), line)
                }

                // Move to the next character to begin the next value
                j++

                line.substring(start, end)
            } else {
                // Record the start-index of the value
                val start = j

                // Scan to the next delimiter
                while (j < lineLength && line[j] !in DATA_DELIMITERS) j++

                // Record the end-index of the value
                val end = j++

                // Remove any gathered trailing whitespace
                line.substring(start, end).trimEnd()
            }

            // Add the value to the list
            values.add(value)
        }
    } catch (e: IndexOutOfBoundsException) {
        // Trying to access past the end of the line means there
        // wasn't enough to complete the requisite number of values
        throw DataSizeMismatchException(numAttributes, line, e)
    }

    // Check that the line parsing completed
    if (--j != lineLength)
        throw UnrecognisedContentException(line.substring(j), line)

    return values
}

/**
 * Parses the @relation section of the ARFF file.
 *
 * @param lines
 *          The file line iterator.
 * @return
 *          The name of the relation as specified in the file.
 */
fun parseRelationSection(lines : Iterator<String>) : String {
    val relationLine = readTillFound(lines, buildSet { add(RELATION_SECTION_KEYWORD) })
    return parseRelationLine(relationLine)
}

/**
 * Parses the relation name from the @relation line.
 *
 * @param relationLine
 *          The line containing the @relation keyword.
 * @return
 *          The name of the relation.
 */
fun parseRelationLine(
    relationLine : String
) : String {
    // Remove the @relation keyword from the start of the line
    val lineWithoutKeyword = removeKeyword(relationLine, RELATION_SECTION_KEYWORD).trimStart()

    // Directly following the keyword should be the name (plus some whitespace)
    val (relationName, unconsumedLine) = splitPatternFromLineStart(lineWithoutKeyword, NAME_PATTERN)

    // If a name wasn't found, error
    if (relationName == null)
        throw RelationNameNotValidException(relationLine)

    // Remainder of line after the relation name should be empty
    if (!isWhitespaceOnly(unconsumedLine))
        throw UnrecognisedContentException(unconsumedLine, relationLine)

    // Return the relation name without quotes
    return removeQuotes(relationName)
}

/**
 * Parses the @attribute section into data-types for use with MĀIA.
 *
 * @param lines
 *          The file content iterator.
 * @return
 *          A list of pairs of:
 *           - Attribute name
 *           - MĀIA data-type equivalent to the ARFF attribute type.
 */
fun parseAttributeSection(
    lines : Iterator<String>
) : List<Pair<String, DataType<*, *>>> {
    // Create a list to gather the attributes in
    val attributes = ArrayList<Pair<String, DataType<*, *>>>()

    // Need to stop at each attribute line, and finally at the data section
    val stopWords = buildSet<String> {
        add(ATTRIBUTE_SECTION_KEYWORD)
        add(DATA_SECTION_KEYWORD)
    }

    do {
        // Read until the next attribute (or the data section) is found
        val line = readTillFound(lines, stopWords)

        // If we've reached the data section, all attributes have been parsed
        if (line.startsWith(DATA_SECTION_KEYWORD,true)) break

        // Otherwise, parse this attribute and add it to the list
        val attribute = parseAttribute(line)
        attributes.add(attribute)

    } while (true)

    return attributes
}

/**
 * Parses an attribute name and MĀIA data-type from an @attribute definition.
 *
 * @param line
 *          The line from the ARFF file containing the attribute definition.
 * @return
 *          A pair of:
 *           - The attribute name.
 *           - Its MĀIA data-type.
 */
fun parseAttribute(
    line : String
) : Pair<String, DataType<*, *>> {
    // Remove the @attribute keyword from the start of the line
    val lineWithoutKeyword = removeKeyword(line, ATTRIBUTE_SECTION_KEYWORD)

    // Extract the name of the attribute from the line
    val (attributeName, unconsumedLine) = splitPatternFromLineStart(lineWithoutKeyword, NAME_PATTERN)

    // Make sure a name was found
    if (attributeName == null)
        throw AttributeNameNotFoundException(line)

    // Consume the attribute type and any type metadata from the line
    val (attributeType, moreUnconsumedLine) = parseAttributeType(unconsumedLine)

    // Error if we can't parse the attribute's type
    if (attributeType == null)
        throw AttributeTypeNotFoundException(line)

    // Rest of the line should just be whitespace
    if (!isWhitespaceOnly(moreUnconsumedLine))
        throw UnrecognisedContentException(moreUnconsumedLine, line)

    return Pair(attributeName, attributeType)
}

/**
 * Parses the line for attribute type information.
 *
 * @param line
 *          The line to parse.
 * @return
 *          A pair of:
 *           - The MĀIA data-type for the attribute, or null if it couldn't be determined.
 *           - The remainder of the line that wasn't consumed during parsing.
 */
fun parseAttributeType(
    line : String
) : Pair<DataType<*, *>?, String> {
    // Parse the attribute type-name
    val (attributeType, unconsumedLine) = splitPatternFromLineStart(line, ATTRIBUTE_TYPE_PATTERN)

    // Return the null indicator if the type-name couldn't be parsed
    if (attributeType == null) return Pair(null, line)

    // Perform case-insensitive matching on attribute type
    val attributeTypeLowerCase = attributeType.lowercase()

    // Parse any extra type meta-data (e.g. nominal classes) and create
    // a placeholder for the MĀIA data-type
    val dataType =  if (attributeTypeLowerCase.startsWith("{")) {
        // Need to parse nominal classes from the non-lower-cased version
        val nominalClasses = parseNominalClasses(attributeType)

        Nominal.PlaceHolder(true, *nominalClasses.toTypedArray())
    } else if (
        attributeTypeLowerCase == REAL_ATTRIBUTE_KEYWORD
        || attributeTypeLowerCase == INTEGER_ATTRIBUTE_KEYWORD
        || attributeTypeLowerCase == NUMERIC_ATTRIBUTE_KEYWORD
    ) {
        Numeric.PlaceHolder(true)
    } else {
        throw UnsupportedAttributeTypeException(attributeType)
    }

    return Pair(
        dataType,
        unconsumedLine
    )
}

/**
 * Parses the nominal classes from the attribute type-specification.
 *
 * @param type
 *          The attribute type-specification.
 * @return
 *          A list of nominal classes for the type.
 */
fun parseNominalClasses(
    type : String
) : List<String> {
    // Create a list to collect the classes into
    val values = ArrayList<String>()

    // Remove the opening curly-brace
    var string = type.substring(1)

    // Keep reading classes until the closing curly-brace
    while (string != "}") {
        // Remove whitespace
        string = string.trimStart()

        // Parse a name for the class
        val (value, unconsumedString) = splitPatternFromLineStart(string, NOMINAL_VALUE_PATTERN)

        // Error if a name couldn't be parsed
        if (value == null)
            throw NominalClassesException(type)

        // Add the class name to the collection
        values.add(removeQuotes(value))

        // Remove any whitespace trailing the class name
        string = unconsumedString.trimStart()

        // Next character should be a comma or the closing curly-brace, remove
        // commas and go around again for the next class name
        if (string.startsWith(","))
            string = string.substring(1)
        else if (!string.startsWith("}"))
            throw NominalClassesException(type)
    }

    return values
}

