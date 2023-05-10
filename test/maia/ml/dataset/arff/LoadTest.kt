package maia.ml.dataset.arff

import maia.ml.dataset.DataMetadata
import maia.ml.dataset.headers.DataColumnHeaders
import maia.ml.dataset.headers.header.DataColumnHeader
import maia.ml.dataset.type.DataType
import maia.ml.dataset.type.standard.Nominal
import maia.ml.dataset.type.standard.Numeric
import maia.ml.dataset.util.formatString
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

/**
 * Tests the ARFF loading functionality.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
internal class LoadTest {

    fun getIrisARFFFilename(): String {
        val url = assertNotNull(this.javaClass.getResource("/iris.arff"))
        val filename = url.file
        assertNotEquals("", filename)
        return filename
    }

    fun testRelationName(metadata : DataMetadata) {
        assertEquals("iris", metadata.name)
    }

    fun testHeaders(headers: DataColumnHeaders) {
        assertEquals(5, headers.size)
        testHeader<Numeric<*, *>>(0, "sepallength", headers[0])
        testHeader<Numeric<*, *>>(1, "sepalwidth", headers[1])
        testHeader<Numeric<*, *>>(2, "petallength", headers[2])
        testHeader<Numeric<*, *>>(3, "petalwidth", headers[3])
        testHeader<Nominal<*, *, *, *, *>>(4, "class", headers[4])
    }

    inline fun <reified T: DataType<*, *>> testHeader(
        index: Int,
        name: String,
        header : DataColumnHeader
    ) {
        assertEquals(index, header.index)
        assertEquals(name, header.name)
        assertIs<T>(header.type)
    }

    @Test
    fun testStream() {
        val filename = getIrisARFFFilename()

        val arff = assertDoesNotThrow {
            load(filename)
        }

        testRelationName(arff.metadata)
        testHeaders(arff.headers)

        assertDoesNotThrow {
            arff.rowIterator().forEach {
                println(it.formatString())
            }
        }
    }

    @Test
    fun testBatch() {
        val filename = getIrisARFFFilename()

        val arff = assertDoesNotThrow {
            load(filename, true)
        }

        assertIs<ARFFDataBatch>(arff)
        assertEquals(5, arff.numColumns)
        assertEquals(150, arff.numRows)
        testRelationName(arff.metadata)
        testHeaders(arff.headers)
    }

}
