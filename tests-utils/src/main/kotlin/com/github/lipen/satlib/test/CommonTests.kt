@file:Suppress("FunctionName")

package com.github.lipen.satlib.test

import com.github.lipen.satlib.op.runWithTimeout
import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.solver.addClause
import com.github.lipen.satlib.solver.solve
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be false`
import org.amshove.kluent.`should be in`
import org.amshove.kluent.`should be true`

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

fun Solver.`simple UNSAT`() {
    val x = newLiteral()
    val y = newLiteral()

    addClause(x)
    addClause(-y)
    addClause(-x, y)

    solve().`should be false`()
}

fun Solver.`empty clause leads to UNSAT`() {
    val x = newLiteral()

    addClause(x)
    addClause()

    solve().`should be false`()
}

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

fun Solver.`assumptions are supported`() {
    val x = newLiteral()

    addClause(x)

    solve(x).`should be true`()
    solve(-x).`should be false`()
    solve().`should be true`()
}

fun <S : Solver> S.`solving with timeout`(clearInterrupt: S.() -> Unit) {
    declare_sgen_n120_sat()
    // The problem is satisfiable, but 1 millisecond is definitely not enough to solve it
    runWithTimeout(1) { solve() }.`should be false`()
    clearInterrupt()
    // Continue solving without a timeout (this may take a while, ~10-60 seconds)
    solve().`should be true`()
}
