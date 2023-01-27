package com.github.lipen.satlib.jni.solver

import com.github.lipen.satlib.solver.DimacsFileSolver
import com.github.lipen.satlib.test.`empty clause leads to UNSAT`
import com.github.lipen.satlib.test.`simple SAT`
import com.github.lipen.satlib.test.`simple UNSAT`
import com.github.lipen.satlib.test.`solving after reset`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class DimacsFileSolverTest {
    private val solver = DimacsFileSolver { "cryptominisat5 $it" }

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
}
