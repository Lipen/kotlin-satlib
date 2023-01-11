package com.github.lipen.satlib.solver

import com.github.lipen.satlib.jni.solver.GlucoseSolver
import com.github.lipen.satlib.jni.solver.MiniSatSolver
import org.amshove.kluent.`should be false`
import org.amshove.kluent.`should be true`
import org.junit.jupiter.api.DisplayNameGeneration
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayNameGeneration(MyDisplayNameGenerator::class)
class MiniSatAndGlucoseTests {
    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("solvers")
    fun Solver.`assumptions are supported`() {
        val x = newLiteral()

        addClause(x)

        solve().`should be true`()
        solve(x).`should be true`()
        solve(-x).`should be false`()
    }

    @Suppress("unused")
    private fun solvers(): List<Solver> =
        listOf(
            MiniSatSolver(),
            GlucoseSolver(),
        )
}
