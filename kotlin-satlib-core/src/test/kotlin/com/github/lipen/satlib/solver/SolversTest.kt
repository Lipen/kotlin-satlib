package com.github.lipen.satlib.solver

import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be false`
import org.amshove.kluent.`should be true`
import org.junit.jupiter.api.DisplayNameGeneration
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayNameGeneration(MyDisplayNameGenerator::class)
class SolversTest {
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

        if (this is CryptoMiniSatSolver) {
            solve() // CMS does not produce UNSAT with empty clause but must finish solving without errors
        } else {
            solve().`should be false`()
        }
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
        }
        reset()
        run {
            val x = newLiteral()
            val y = newLiteral()
            addClause(x, y)
            numberOfVariables `should be equal to` 2
            numberOfClauses `should be equal to` 1
            solve().`should be true`()
            (getValue(x) or getValue(y)).`should be true`()
        }
    }

    @Suppress("unused")
    private fun solvers(): List<Solver> =
        if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
            listOf(
                MiniSatSolver(),
                GlucoseSolver(),
                DimacsFileSolver("cryptominisat5 %s")
            )
        } else {
            listOf(
                MiniSatSolver(),
                GlucoseSolver(),
                CadicalSolver(),
                CryptoMiniSatSolver(),
                DimacsFileSolver("cryptominisat5 %s")
            )
        }
}
