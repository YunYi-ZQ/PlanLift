package app.planlift.fit.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncMergeTest {

    private data class Row(
        val id: String,
        val rev: Long,
        val updatedAt: Long,
        val deviceId: String,
        val value: String = ""
    )

    private fun merge(local: List<Row>, remote: List<Row>) =
        SyncMerge.merge(local, remote, { it.id }, { it.rev }, { it.updatedAt }, { it.deviceId })

    @Test
    fun `remote only records are downloaded`() {
        val out = merge(
            emptyList(),
            listOf(Row("a", 1, 100, "devB", "remote"))
        )
        assertEquals(1, out.merged.size)
        assertEquals("remote", out.merged[0].value)
        assertEquals(1, out.remoteWins)
    }

    @Test
    fun `local only records are kept`() {
        val out = merge(listOf(Row("a", 1, 100, "devA", "local")), emptyList())
        assertEquals("local", out.merged[0].value)
        assertEquals(1, out.localWins)
    }

    @Test
    fun `higher rev wins regardless of time`() {
        val out = merge(
            listOf(Row("a", 3, 100, "devA", "local")),
            listOf(Row("a", 1, 999, "devB", "remote"))
        )
        assertEquals("local", out.merged[0].value)
        assertEquals(1, out.localWins)
    }

    @Test
    fun `same rev falls back to updatedAt`() {
        val out = merge(
            listOf(Row("a", 2, 100, "devA", "old")),
            listOf(Row("a", 2, 200, "devB", "new"))
        )
        assertEquals("new", out.merged[0].value)
        assertEquals(1, out.remoteWins)
    }

    @Test
    fun `same rev and time falls back to deviceId deterministically`() {
        val a = listOf(Row("a", 2, 100, "devA", "A"))
        val b = listOf(Row("a", 2, 100, "devB", "B"))
        val forward = merge(a, b)
        val backward = merge(b, a)
        assertEquals(forward.merged[0].value, backward.merged[0].value)
        assertEquals("B", forward.merged[0].value)
    }

    @Test
    fun `union of keys keeps everything`() {
        val out = merge(
            listOf(Row("a", 1, 10, "A"), Row("b", 1, 10, "A")),
            listOf(Row("b", 2, 20, "B"), Row("c", 1, 20, "B"))
        )
        assertEquals(setOf("a", "b", "c"), out.merged.map { it.id }.toSet())
        assertEquals(3, out.merged.size)
    }

    @Test
    fun `identical records are counted as same`() {
        val row = Row("a", 5, 50, "A")
        val out = merge(listOf(row), listOf(row))
        assertEquals(1, out.sameRecord)
        assertEquals(0, out.localWins)
        assertEquals(0, out.remoteWins)
    }

    @Test
    fun `deleted flag travels with the winner`() {
        val out = merge(
            listOf(Row("a", 1, 10, "A", "alive")),
            listOf(Row("a", 2, 20, "B", "deleted"))
        )
        assertEquals("deleted", out.merged[0].value)
        assertTrue(out.remoteWins > 0)
    }
}
