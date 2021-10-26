package māia.ml.dataset.arff

import māia.ml.dataset.DataRow
import māia.ml.dataset.error.MissingValue
import māia.ml.dataset.headers.MutableDataColumnHeaders
import māia.ml.dataset.headers.ensureOwnership
import māia.ml.dataset.type.DataRepresentation

/**
 * The type of data-row that the ARFF loader returns.
 *
 * @param headersInternal
 *          The headers parsed from the ARFF file.
 * @param valueMap
 *          The map of prepared values.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
class ARFFDataRow internal constructor(
    private val headersInternal : MutableDataColumnHeaders,
    private val valueMap: Map<DataRepresentation<*, *, *>, Any?>
): DataRow {
    override val headers = headersInternal.readOnlyView

    override fun <T> getValue(
        representation : DataRepresentation<*, *, out T>
    ) : T = headersInternal.ensureOwnership(representation) {
        @Suppress("UNCHECKED_CAST")
        return valueMap[representation] as T ?: throw MissingValue()
    }
}
