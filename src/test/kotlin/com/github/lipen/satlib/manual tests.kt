@file:Suppress("ClassName")

package com.github.lipen.satlib

import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.op.exactlyOne
import com.github.lipen.satlib.solver.DimacsFileSolver
import com.github.lipen.satlib.solver.DimacsStreamSolver
import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.solver.addClause
import com.github.lipen.satlib.solver.jni.CadicalSolver
import com.github.lipen.satlib.solver.jni.CryptoMiniSatSolver
import com.github.lipen.satlib.solver.jni.GlucoseSolver
import com.github.lipen.satlib.solver.jni.MiniSatSolver
import com.github.lipen.satlib.solver.solve
import com.github.lipen.satlib.utils.useWith
import java.io.File

private object test_MiniSatSolver {
    @JvmStatic
    fun main(args: Array<String>) {
        MiniSatSolver().useWith {
            testSolverWithAssumptions()
        }
    }
}

private object test_GlucoseSolver {
    @JvmStatic
    fun main(args: Array<String>) {
        GlucoseSolver().useWith {
            testSolverWithAssumptions()
        }
    }
}

private object test_CadicalSolver {
    @JvmStatic
    fun main(args: Array<String>) {
        CadicalSolver().useWith {
            testSolverWithAssumptions()
        }
    }
}

private object test_CryptoMiniSatSolver {
    @JvmStatic
    fun main(args: Array<String>) {
        CryptoMiniSatSolver().useWith {
            testSolverWithAssumptions()
        }
    }
}

private object test_DimacsFileSolver {
    @JvmStatic
    fun main(args: Array<String>) {
        DimacsFileSolver(
            file = { File("dimacs.cnf") },
            command = { "cryptominisat5 $it" }
        ).useWith {
            testSolverWithoutAssumptions()
        }
    }
}

private object test_DimacsStreamSolver {
    @JvmStatic
    fun main(args: Array<String>) {
        DimacsStreamSolver("cryptominisat5").useWith {
            testSolverWithoutAssumptions()
        }
    }
}

private fun Boolean.toInt(): Int = if (this) 1 else 0

private fun Solver.checkExactlyOne(vararg lits: Lit) {
    check(lits.sumOf { getValue(it).toInt() } == 1)
}

internal fun Solver.testSolverWithAssumptions() {
    val x = newLiteral()
    val y = newLiteral()
    val z = newLiteral()

    println("Encoding exactlyOne(x, y, z)")
    exactlyOne(x, y, z)

    println("nVars = $numberOfVariables")
    println("nClauses = $numberOfClauses")

    check(solve())
    println("model = ${getModel()}")
    checkExactlyOne(x, y, z)

    println("Solving with assumptions...")
    check(solve(x)); println("model = ${getModel()}"); check(getValue(x))
    check(solve(y)); println("model = ${getModel()}"); check(getValue(y))
    check(solve(z)); println("model = ${getModel()}"); check(getValue(z))

    println("Everything OK.")
}

internal fun Solver.testSolverWithoutAssumptions() {
    val x = newLiteral()
    val y = newLiteral()
    val z = newLiteral()

    println("Encoding exactlyOne(x, y, z)")
    exactlyOne(x, y, z)

    println("nVars = $numberOfVariables")
    println("nClauses = $numberOfClauses")

    check(solve())
    println("model = ${getModel()}")
    checkExactlyOne(x, y, z)

    println("Adding ~x unit-clause")
    addClause(-x)
    check(solve())
    println("model = ${getModel()}")
    checkExactlyOne(x, y, z)

    println("Adding z unit-clause")
    addClause(z)
    check(solve())
    println("model = ${getModel()}")
    checkExactlyOne(x, y, z)

    println("Everything OK.")
}
