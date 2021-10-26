package māia.ml.dataset.arff

import māia.ml.dataset.DataMetadata
import māia.ml.dataset.DataStream
import māia.ml.dataset.headers.MutableDataColumnHeaders

/**
 * The type of data-stream returned by the ARFF loader.
 *
 * @param relationName
 *          The name of the ARFF relation.
 * @param headersInternal
 *          The ARFF attribute headers.
 * @param rows
 *          The ARFF data-rows.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
open class ARFFDataStream internal constructor(
    relationName: String,
    headersInternal : MutableDataColumnHeaders,
    private val rows: Iterator<ARFFDataRow>
): DataStream<ARFFDataRow> {
    override val headers = headersInternal.readOnlyView
    override val metadata : DataMetadata = object : DataMetadata {
        override val name : String = relationName
    }
    override fun rowIterator() : Iterator<ARFFDataRow> = rows
}
