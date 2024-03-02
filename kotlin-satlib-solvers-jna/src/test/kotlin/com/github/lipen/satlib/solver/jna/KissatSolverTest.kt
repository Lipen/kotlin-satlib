package com.github.lipen.satlib.solver.jna

import com.github.lipen.satlib.test.`empty clause leads to UNSAT`
import com.github.lipen.satlib.test.`simple SAT`
import com.github.lipen.satlib.test.`simple UNSAT`
import com.github.lipen.satlib.test.`solving after reset`
import com.github.lipen.satlib.test.`solving with timeout`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class KissatSolverTest {
    private val solver = KissatSolver()

    @Test
    fun `simple SAT`() {
        solver.`simple SAT`()
    }

    @Test
    fun `simple UNSAT`() {
        solver.`simple UNSAT`()
    }

    @Test
    fun `empty clause leads to UNSAT`() {
        solver.`empty clause leads to UNSAT`()
    }

    @Test
    fun `solving after reset`() {
        solver.`solving after reset`()
    }

    @Test
    fun `solving with timeout`() {
        solver.`solving with timeout`(continueSolving = false)
    }

    @Test
    fun `incremental solving is not supported`() {
        // The following is forbidden in Kissat:
        // solver.solve()
        // solver.solve()
        // Note: we cannot `assertThrows` it, since kissat_solve just halts.
    }

    @Test
    fun `assumptions are not supported`() {
        //
    }
}
