package com.github.lipen.satlib.solver

import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be false`
import org.amshove.kluent.`should be true`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class DimacsFileSolverTest {
    private val solverCmd = "cryptominisat5 %s"
    private val solver: Solver = DimacsFileSolver(solverCmd, createTempFile())

    @Test
    fun `simple SAT`(): Unit = with(solver) {
        val x = newVariable()
        val y = newVariable()

        addClause(x)
        addClause(-y)

        solve().`should be true`()
        getValue(x).`should be true`()
        getValue(y).`should be false`()
    }

    @Test
    fun `simple UNSAT`(): Unit = with(solver) {
        val x = newVariable()
        val y = newVariable()

        addClause(x)
        addClause(-y)
        addClause(-x, y)

        solve().`should be false`()
    }

    @Test
    fun `assumptions are not supported`(): Unit = with(solver) {
        val x = newVariable()

        addClause(x)

        solve().`should be true`()
        assertThrows<UnsupportedOperationException> { solve(-x) }
    }

    @Test
    fun `empty clause leads to UNSAT`(): Unit = with(solver) {
        val x = newVariable()

        addClause(x)
        @Suppress("deprecation")
        addClause()

        solve().`should be false`()
    }

    @Test
    fun `solving after reset`() {
        with(solver) {
            val x = newVariable()
            addClause(x)
            numberOfVariables `should be equal to` 1
            numberOfClauses `should be equal to` 1
            solve().`should be true`()
            getValue(x).`should be true`()
        }
        solver.reset()
        with(solver) {
            val x = newVariable()
            addClause(-x)
            numberOfVariables `should be equal to` 1
            numberOfClauses `should be equal to` 1
            solve().`should be true`()
            getValue(x).`should be false`()
        }
    }
}
