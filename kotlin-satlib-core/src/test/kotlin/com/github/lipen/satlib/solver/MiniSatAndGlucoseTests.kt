package com.github.lipen.satlib.solver

import com.github.lipen.satlib.core.AssumptionsProvider
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

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("solvers")
    fun Solver.`assumptions via observable`() {
        val x = newLiteral()
        val y = newLiteral()

        addClause(x, y)
        addClause(-x, -y)

        solve().`should be true`()
        solve(x).`should be true`()
        solve(y).`should be true`()
        solve(-x).`should be true`()
        solve(-y).`should be true`()
        solve(x, y).`should be false`()
        solve(-x, -y).`should be false`()

        val ax = AssumptionsProvider { listOf(x) }
        val ay = AssumptionsProvider { listOf(y) }
        val anxy = AssumptionsProvider { listOf(-x, -y) }

        solve().`should be true`()
        assumptionsObservable.register(ax)
        solve().`should be true`()
        assumptionsObservable.register(ay)
        solve().`should be false`()
        assumptionsObservable.unregister(ax)
        solve().`should be true`()
        assumptionsObservable.unregister(ay)
        solve().`should be true`()
        assumptionsObservable.register(anxy)
        solve().`should be false`()
        assumptionsObservable.clear()
        solve().`should be true`()
    }

    @Suppress("unused")
    private fun solvers(): List<Solver> =
        listOf(
            MiniSatSolver(),
            GlucoseSolver(),
        )
}
