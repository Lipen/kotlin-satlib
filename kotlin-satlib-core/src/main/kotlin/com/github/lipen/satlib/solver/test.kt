package com.github.lipen.satlib.solver

import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.op.exactlyOne

private fun Boolean.toInt(): Int = if (this) 1 else 0

private fun Solver.checkExactlyOne(vararg lits: Lit) {
    check(lits.sumBy { getValue(it).toInt() } == 1)
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
