package māia.ml.dataset.arff

import māia.ml.dataset.DataColumn
import māia.ml.dataset.DataRow
import māia.ml.dataset.WithColumnHeaders
import māia.ml.dataset.mutable.MutableDataBatch
import māia.ml.dataset.mutable.WithMutableColumnStructure
import māia.ml.dataset.mutable.WithMutableMetadata
import māia.ml.dataset.mutable.WithMutableRowStructure
import māia.ml.dataset.DataColumnHeader
import māia.ml.dataset.primitive.PrimitiveDataBatch
import māia.ml.dataset.primitive.PrimitiveDataColumn
import māia.ml.dataset.primitive.PrimitiveDataColumnHeader
import māia.ml.dataset.primitive.toPrimitive
import māia.ml.dataset.type.DataType
import māia.ml.dataset.type.Numeric
import māia.ml.dataset.type.standard.NominalIndexImpl
import māia.ml.dataset.type.standard.NumericDoubleImpl
import māia.ml.dataset.util.appendColumn
import māia.ml.dataset.util.appendRow
import māia.ml.dataset.util.buildRow
import māia.ml.dataset.util.clear
import māia.ml.dataset.util.convertToInternalUnchecked
import māia.util.EmptyIterator
import māia.util.buildSet
import māia.util.collect
import māia.util.map
import māia.util.zip
import java.io.FileReader


fun readLines(filename : String) : Iterator<String> {
    val reader = FileReader(filename)
    return reader.readLines().iterator()
}

fun load(filename : String) : PrimitiveDataBatch {
    return load(filename, PrimitiveDataBatch())
}

fun <T> load(filename : String, dataset : T) :
        T
        where T : MutableDataBatch<*, *>,
              T : WithMutableColumnStructure<DataColumn, DataColumn>,
              T : WithMutableRowStructure<DataRow, DataRow>,
              T : WithMutableMetadata
{

    val lines = readLines(filename)

    val relation = getRelationSection(lines)
    val arff = dataset.also { it.clear() }
    arff.name = relation

    // Delete all data
    while (arff.numColumns > 0) arff.deleteColumn(0)

    val attributes = getAttributeSection(lines)

    val headers = attributes.map { PrimitiveDataColumnHeader(it.first, it.second, false) }

    for (header in headers) {
        arff.appendColumn(PrimitiveDataColumn(header, EmptyIterator))
    }

    getDataSection(lines, arff)

    return arff
}

fun <T> getDataSection(lines : Iterator<String>, arff : T)
        where T : WithMutableRowStructure<DataRow, *>,
              T : WithColumnHeaders {
    var line : String? = null
    while (line == null || lines.hasNext()) {
        line = lines.next()
        if (isWhitespaceOnly(line) || isCommentLine(line)) continue
        appendDataRow(line, arff)
    }
}

fun <T> appendDataRow(line : String, arff : T)
        where T : WithMutableRowStructure<DataRow, *>,
              T : WithColumnHeaders {
    val numAttributes = arff.numColumns
    var i = 0
    var j = 0
    val lineLength = line.length
    var start : Int
    var end : Int
    val headers = List(arff.numColumns) { arff.getColumnHeader(it).toPrimitive() }
    val values = ArrayList<String>()
    while (i < numAttributes) {
        if (j == lineLength) break
        while (line[j] == ' ') j++
        if (line[j] in QUOTES) {
            val quote = line[j]
            j++
            start = j
            while (line[j] != quote) j++
            end = j
            j += 2
        } else {
            start = j
            while (j < lineLength && line[j] !in DATA_DELIMITERS) j++
            end = j
            j++
        }
        val s = line.substring(start, end)
        values.add(s)
        i++
    }

    // TODO: Add missing values
    val valuesTyped = zip(headers, values).map { (header, value) ->
        val type = header.type
        val externalValue = when {
            type is Numeric<*> -> value.toDouble()
            else -> value
        }
        type.convertToInternalUnchecked(externalValue)
    }.collect(ArrayList())

    arff.appendRow(
        buildRow(
            headers.size,
            headers::get,
            valuesTyped::get
        )
    )

    if (j != lineLength + 1 || i != numAttributes) throw DataSizeMismatchError(numAttributes, line)
}

class DataSizeMismatchError(numAttributes : Int, line : String) : ARFFError("Wrong number of values (require $numAttributes) in: $line")

fun getRelationSection(lines : Iterator<String>) : String {
    val line = readTillFound(lines, buildSet { add(RELATION_SECTION_KEYWORD) })
    return parseRelationLine(line)
}

fun parseRelationLine(line : String) : String {
    val lineWithoutKeyword = removeKeyword(line, RELATION_SECTION_KEYWORD).trimStart()
    val (relation, unconsumedLine) = consume(lineWithoutKeyword, NAME_PATTERN)
    if (relation == null) throw RelationNameNotFoundException(line)
    if (!isWhitespaceOnly(unconsumedLine)) throw UnrecognisedContentError(unconsumedLine, line)
    return removeQuotes(relation)
}

class RelationNameNotFoundException(line : String) : ARFFError("Couldn't parse relation name from: $line")

fun getAttributeSection(lines : Iterator<String>) : List<Pair<String, DataType<*, *>>> {
    val attributes = ArrayList<Pair<String, DataType<*, *>>>()
    val stopwords = buildSet<String> { add(ATTRIBUTE_SECTION_KEYWORD); add(DATA_SECTION_KEYWORD) }
    var line = readTillFound(lines, stopwords)
    while (!lineStartsWith(line, DATA_SECTION_KEYWORD)) {
        val attribute = parseAttribute(line)
        attributes.add(attribute)
        line = readTillFound(lines, stopwords)
    }
    return attributes
}

fun parseAttribute(line : String) : Pair<String, DataType<*, *>> {

    val lineNoKeyword = removeKeyword(line, ATTRIBUTE_SECTION_KEYWORD)

    val (name, unconsumedLine) = consume(lineNoKeyword, NAME_PATTERN)

    if (name == null) throw AttributeNameNotFoundError(line)

    val (attributeType, additional, moreUnconsumedLine) = consumeAttributeTypeInformation(unconsumedLine)

    if (attributeType == null) throw AttributeTypeNotFoundError(line)

    if (!isWhitespaceOnly(moreUnconsumedLine)) throw UnrecognisedContentError(moreUnconsumedLine, line)

    // TODO: Add missing values
    return Pair(
            name,
            if (attributeType == NOMINAL_ATTRIBUTE_KEYWORD) {
                NominalIndexImpl(*additional!!.toTypedArray())
            } else {
                NumericDoubleImpl
            }
    )
}

class AttributeNameNotFoundError(line : String) : ARFFError("Couldn't parse attribute name from: $line")

class AttributeTypeNotFoundError(line : String) : ARFFError("Couldn't parse attribute type from: $line")

fun consumeAttributeTypeInformation(line : String) : Triple<String?, List<String>?, String> {

    val (attributeType, unconsumedLine) = consume(line, ATTRIBUTE_TYPE_PATTERN)

    if (attributeType == null) return Triple(null, null, line)

    val actualType = attributeType.lowercase()

    return if (actualType.startsWith("{")) {
        Triple(NOMINAL_ATTRIBUTE_KEYWORD, parseNominalValues(attributeType), unconsumedLine)
    } else if (actualType == REAL_ATTRIBUTE_KEYWORD || actualType == INTEGER_ATTRIBUTE_KEYWORD || actualType == NUMERIC_ATTRIBUTE_KEYWORD) {
        Triple(NUMERIC_ATTRIBUTE_KEYWORD, null, unconsumedLine)
    } else {
        throw ARFFError("Unsupported attribute type: $attributeType")
    }
}

fun parseNominalValues(type : String) : List<String> {
    val values = ArrayList<String>()
    var string = type.substring(1)
    while (string != "}") {
        string = string.trimStart()
        val (value, unconsumedString) = consume(string, NOMINAL_VALUE_PATTERN)
        if (value == null) throw NominalValuesError(type)
        values.add(removeQuotes(value))
        string = unconsumedString.trimStart()
        if (string.startsWith(","))
            string = string.substring(1)
        else if (!string.startsWith("}"))
            throw NominalValuesError(type)
    }
    return values
}

class NominalValuesError(string : String) : ARFFError("Error parsing nominal values from : $string")
