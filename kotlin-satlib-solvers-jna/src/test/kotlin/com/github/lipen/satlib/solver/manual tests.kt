@file:Suppress("ClassName")

package com.github.lipen.satlib.solver

import com.github.lipen.satlib.solver.jna.CadicalSolver
import com.github.lipen.satlib.solver.jna.GlucoseSolver
import com.github.lipen.satlib.solver.jna.MiniSatSolver
import com.github.lipen.satlib.utils.useWith

private object test_MinisatSolver {
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

fun Solver.testSolverWithAssumptions() {
    val x = newLiteral()
    val y = newLiteral()
    val z = newLiteral()
    println("Variables: $numberOfVariables")

    println("Adding clauses...")
    addClause(listOf(-x))
    addClause(listOf(-z))
    addClause(listOf(x, y, z))
    println("Clauses: $numberOfClauses")

    println("Solving...")
    check(solve()) { "Unexpected UNSAT" }
    println("x = ${getValue(x)}, y = ${getValue(y)}, z = ${getValue(z)}")
    println("model = ${getModel()}")

    println("Solving with assumptions...")
    check(solve(y))
    check(!solve(-y))

    val t = newLiteral()
    check(solve(t))
    check(solve(-t))
    println("Solving with assumptions: OK")

    println("Everything OK.")
}
