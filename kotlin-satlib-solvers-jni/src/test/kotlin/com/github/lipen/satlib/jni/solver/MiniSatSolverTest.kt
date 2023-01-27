package com.github.lipen.satlib.jni.solver

import com.github.lipen.satlib.op.runWithTimeout
import com.github.lipen.satlib.test.`assumptions are supported`
import com.github.lipen.satlib.test.declare_sgen_n120_sat
import com.github.lipen.satlib.test.`empty clause leads to UNSAT`
import com.github.lipen.satlib.test.`simple SAT`
import com.github.lipen.satlib.test.`simple UNSAT`
import com.github.lipen.satlib.test.`solving after reset`
import com.github.lipen.satlib.utils.useWith
import org.amshove.kluent.`should be false`
import org.amshove.kluent.`should be true`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class MiniSatSolverTest {
    private val solver = MiniSatSolver()

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
    fun `assumptions are supported`() {
        solver.`assumptions are supported`()
    }

    @Test
    fun `solving with timeout`(): Unit {
        solver.useWith {
            declare_sgen_n120_sat()
            // The problem is satisfiable, but 1 millisecond is definitely not enough to solve it
            runWithTimeout(1) { solve() }.`should be false`()
            backend.clearInterrupt()
            // Continue solving without a timeout (this may take a while, ~10-60 seconds)
            solve().`should be true`()
        }
    }
}
