package com.github.lipen.satlib.solver

import com.github.lipen.satlib.op.runWithTimeout
import org.amshove.kluent.`should be false`
import org.amshove.kluent.`should be true`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class GlucoseSolverTest {
    private val solver = GlucoseSolver()

    @Test
    fun `assumptions are supported`(): Unit = with(solver) {
        val x = newLiteral()

        addClause(x)

        solve().`should be true`()
        solve(x).`should be true`()
        solve(-x).`should be false`()
    }

    @Test
    fun `solving with timeout`(): Unit = with(solver) {
        declare_sgen_n120_sat()
        // The problem is satisfiable, but 1 millisecond is definitely not enough to solve it
        runWithTimeout(1) { solve() }.`should be false`()
        backend.clearInterrupt()
        // Continue solving without a timeout (this may take a while, ~10-60 seconds)
        solve().`should be true`()
    }
}
