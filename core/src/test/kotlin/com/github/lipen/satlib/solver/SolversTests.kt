package com.github.lipen.satlib.solver

import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be false`
import org.amshove.kluent.`should be in`
import org.amshove.kluent.`should be true`
import org.junit.jupiter.api.DisplayNameGeneration
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayNameGeneration(MyDisplayNameGenerator::class)
class SolversTests {
    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("solvers")
    fun Solver.`simple SAT`() {
        val x = newLiteral()
        val y = newLiteral()

        addClause(x)
        addClause(-y)

        solve().`should be true`()
        getValue(x).`should be true`()
        getValue(y).`should be false`()
        getModel().data `should be equal to` listOf(true, false)
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("solvers")
    fun Solver.`simple UNSAT`() {
        val x = newLiteral()
        val y = newLiteral()

        addClause(x)
        addClause(-y)
        addClause(-x, y)

        solve().`should be false`()
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("solvers")
    fun Solver.`empty clause leads to UNSAT`() {
        val x = newLiteral()

        addClause(x)
        @Suppress("deprecation")
        addClause()

        solve().`should be false`()
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @MethodSource("solvers")
    fun Solver.`solving after reset`() {
        run {
            val x = newLiteral()
            val y = newLiteral()
            addClause(x, y)
            numberOfVariables `should be equal to` 2
            numberOfClauses `should be equal to` 1
            solve().`should be true`()
            (getValue(x) or getValue(y)).`should be true`()
            getModel().data `should be in` listOf(
                listOf(true, true),
                listOf(true, false),
                listOf(false, true),
            )
        }
        reset()
        run {
            val x = newLiteral()
            val y = newLiteral()
            addClause(-x)
            addClause(-y)
            numberOfVariables `should be equal to` 2
            numberOfClauses `should be equal to` 2
            solve().`should be true`()
            getValue(x).`should be false`()
            getValue(y).`should be false`()
            getModel().data `should be equal to` listOf(false, false)
        }
    }

    @Suppress("unused")
    private fun solvers(): List<Solver> =
        listOf(
            MiniSatSolver(),
            GlucoseSolver(),
            CadicalSolver(),
            CryptoMiniSatSolver(),
            DimacsFileSolver { "cryptominisat5 $it" },
            DimacsStreamSolver { "cryptominisat5" },
        )
}
