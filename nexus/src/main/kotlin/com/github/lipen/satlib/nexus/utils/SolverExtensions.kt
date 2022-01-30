package com.github.lipen.satlib.nexus.utils

import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.solver.GlucoseSolver
import com.github.lipen.satlib.solver.MiniSatSolver
import com.github.lipen.satlib.solver.Solver
import mu.KLogger
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun Solver.maybeSetFrozen(lit: Lit, frozen: Boolean) {
    when (this) {
        is MiniSatSolver -> backend.setFrozen(lit, frozen)
        is GlucoseSolver -> backend.setFrozen(lit, frozen)
        else -> {}
    }
}

fun Solver.maybeFreeze(lit: Lit) {
    maybeSetFrozen(lit, true)
}

fun Solver.maybeMelt(lit: Lit) {
    maybeSetFrozen(lit, false)
}

fun Solver.maybeSetDecision(lit: Lit, decision: Boolean) {
    when (this) {
        is MiniSatSolver -> backend.setDecision(lit, decision)
        is GlucoseSolver -> backend.setDecision(lit, decision)
        else -> {}
    }
}

/** [lhs] => ([a] `XOR` [b]) */
fun Solver.implyXor2(lhs: Lit, a: Lit, b: Lit) {
    addClause(-lhs, a, b)
    addClause(-lhs, -a, -b)
}

/** [lhs] <=> ([a] `XOR` [b]) */
fun Solver.iffXor2(lhs: Lit, a: Lit, b: Lit) {
    implyXor2(lhs, a, b)
    addClause(lhs, a, -b)
    addClause(lhs, -a, b)
}

internal inline fun Solver.declare(
    externalLogger: KLogger? = null,
    block: () -> Unit,
) {
    val logger = externalLogger ?: logger

    val timeStartDeclare = timeNow()
    val startNumberOfVariables = numberOfVariables
    val startNumberOfClauses = numberOfClauses

    logger.info("Declaring variables and constraints...")
    block()

    val diffVars = numberOfVariables - startNumberOfVariables
    val diffClauses = numberOfClauses - startNumberOfClauses
    logger.info {
        "Declared $diffVars variables and $diffClauses clauses in %.3f s"
            .format(secondsSince(timeStartDeclare))
    }
}
