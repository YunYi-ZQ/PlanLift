package app.planlift.fit.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionTest {

    private val t = ProgressionTarget(repMin = 5, repMax = 8, rir = 2, plannedSets = 3)

    private fun perf(vararg r: Triple<Double, Int, Int>) =
        r.map { SetPerformance(it.first, it.second, it.third) }

    @Test
    fun `no history starts from given weight`() {
        val res = ProgressionEngine.next(emptyList(), t, currentWeight = 40.0, increment = 5.0)
        assertEquals(ProgressionAction.START, res.action)
        assertEquals(40.0, res.nextWeightKg, 0.001)
        assertEquals(5, res.nextReps)
    }

    @Test
    fun `all sets hit rep max with rir left - go up one step`() {
        val last = perf(Triple(60.0, 8, 2), Triple(60.0, 8, 2), Triple(60.0, 8, 3))
        val res = ProgressionEngine.next(last, t, 60.0, 5.0)
        assertEquals(ProgressionAction.UP, res.action)
        assertEquals(65.0, res.nextWeightKg, 0.001)
        assertEquals(5, res.nextReps)
    }

    @Test
    fun `hit max reps but grinding to failure - hold instead of up`() {
        val last = perf(Triple(60.0, 8, 0), Triple(60.0, 8, 0), Triple(60.0, 7, 0))
        val res = ProgressionEngine.next(last, t, 60.0, 5.0)
        assertEquals(ProgressionAction.HOLD, res.action)
        assertEquals(60.0, res.nextWeightKg, 0.001)
    }

    @Test
    fun `one rep short of top - hold and chase one more rep`() {
        val last = perf(Triple(60.0, 8, 2), Triple(60.0, 8, 2), Triple(60.0, 7, 2))
        val res = ProgressionEngine.next(last, t, 60.0, 5.0)
        assertEquals(ProgressionAction.HOLD, res.action)
        assertEquals(8, res.nextReps)
        assertEquals(60.0, res.nextWeightKg, 0.001)
    }

    @Test
    fun `below rep minimum - drop half a step`() {
        val last = perf(Triple(60.0, 5, 1), Triple(60.0, 4, 0), Triple(60.0, 4, 0))
        val res = ProgressionEngine.next(last, t, 60.0, 5.0)
        assertEquals(ProgressionAction.DOWN, res.action)
        assertEquals(57.5, res.nextWeightKg, 0.001)
    }

    @Test
    fun `drop never goes below zero`() {
        val last = perf(Triple(1.0, 3, 0), Triple(1.0, 3, 0), Triple(1.0, 3, 0))
        val res = ProgressionEngine.next(last, t, 1.0, 5.0)
        assertEquals(ProgressionAction.DOWN, res.action)
        assertEquals(0.0, res.nextWeightKg, 0.001)
    }

    @Test
    fun `overshooting rep max only adds one step`() {
        val last = perf(Triple(50.0, 15, 4), Triple(50.0, 15, 4), Triple(50.0, 12, 3))
        val res = ProgressionEngine.next(last, t, 50.0, 2.5)
        assertEquals(ProgressionAction.UP, res.action)
        assertEquals(52.5, res.nextWeightKg, 0.001)
    }

    @Test
    fun `zero weight is treated as start`() {
        val last = perf(Triple(0.0, 0, 0))
        val res = ProgressionEngine.next(last, t, 0.0, 2.5)
        assertEquals(ProgressionAction.START, res.action)
        assertEquals(0.0, res.nextWeightKg, 0.001)
    }

    @Test
    fun `halt sets with zero reps are ignored`() {
        val last = perf(Triple(0.0, 0, 0), Triple(0.0, 0, 0))
        val res = ProgressionEngine.next(last, t, 30.0, 2.5)
        assertEquals(ProgressionAction.START, res.action)
    }

    @Test
    fun `increment rules by body part and sex`() {
        assertEquals(5.0, ProgressionEngine.increment(lowerBody = true, isMachine = false, sex = "male"), 0.001)
        assertEquals(2.5, ProgressionEngine.increment(lowerBody = true, isMachine = false, sex = "female"), 0.001)
        assertEquals(2.5, ProgressionEngine.increment(lowerBody = false, isMachine = false, sex = "male"), 0.001)
        assertEquals(1.25, ProgressionEngine.increment(lowerBody = false, isMachine = false, sex = "female"), 0.001)
        assertEquals(2.5, ProgressionEngine.increment(lowerBody = false, isMachine = true, sex = "female"), 0.001)
    }

    @Test
    fun `e1rm follows epley`() {
        assertEquals(100.0, ProgressionEngine.e1rm(100.0, 1), 0.001)
        // 80kg x 10 -> 80 * (1 + 10/30) = 106.67
        assertEquals(106.67, ProgressionEngine.e1rm(80.0, 10), 0.05)
        assertEquals(0.0, ProgressionEngine.e1rm(0.0, 10), 0.001)
        assertEquals(0.0, ProgressionEngine.e1rm(80.0, 0), 0.001)
    }

    @Test
    fun `reason text is human readable`() {
        val last = perf(Triple(60.0, 8, 2), Triple(60.0, 8, 2), Triple(60.0, 8, 2))
        val res = ProgressionEngine.next(last, t, 60.0, 5.0)
        assertTrue(res.reason.contains("加"))
        assertTrue(res.reason.contains("65"))
    }
}
