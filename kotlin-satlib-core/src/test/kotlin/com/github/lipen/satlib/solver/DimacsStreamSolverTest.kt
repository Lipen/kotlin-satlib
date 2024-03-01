package com.github.lipen.satlib.solver

import com.github.lipen.satlib.test.`empty clause leads to UNSAT`
import com.github.lipen.satlib.test.`simple SAT`
import com.github.lipen.satlib.test.`simple UNSAT`
import com.github.lipen.satlib.test.`solving after reset`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class DimacsStreamSolverTest {
    private val solverCmd = "cryptominisat5"
    private val solver: Solver = DimacsStreamSolver (solverCmd)

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
    fun `assumptions are not supported`(): Unit = with(solver) {
        val x = newLiteral()
        addClause(x)
        assertThrows<UnsupportedOperationException> { solve(-x) }
    }
}
