package com.github.lipen.satlib.nexus.utils

import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.solver.CadicalSolver
import com.github.lipen.satlib.solver.GlucoseSolver
import com.github.lipen.satlib.solver.MiniSatSolver
import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.solver.addClause
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

/** [lhs] => `NAND`([rhs]) */
fun Solver.implyNand(lhs: Lit, vararg rhs: Lit) {
    addClause {
        yield(-lhs)
        for (lit in rhs) {
            yield(-lit)
        }
    }
}

/** [lhs] <=> ([a] `XOR` [b]) */
fun Solver.iffXor2(lhs: Lit, a: Lit, b: Lit) {
    implyXor2(lhs, a, b)
    addClause(lhs, a, -b)
    addClause(lhs, -a, b)
}

/** [lhs] <=> ([a] `XOR` [b] `XOR` [c]) */
fun Solver.iffXor3(lhs: Lit, a: Lit, b: Lit, c: Lit) {
    addClause(-lhs, -a, -b, c)
    addClause(-lhs, -a, b, -c)
    addClause(-lhs, a, -b, -c)
    addClause(-lhs, a, b, c)
    addClause(lhs, -a, -b, -c)
    addClause(lhs, -a, b, c)
    addClause(lhs, a, -b, c)
    addClause(lhs, a, b, -c)
}

/** [lhs] <=> ([a] `XOR` [b] `XOR` [c]) */
fun Solver.iffMaj3(lhs: Lit, a:Lit, b:Lit, c:Lit) {
    addClause(-lhs, a, b)
    addClause(-lhs, a, c)
    addClause(-lhs, b, c)
    addClause(lhs, -a, -b)
    addClause(lhs, -a, -c)
    addClause(lhs, -b, -c)
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

fun Solver.maybeNumberOfDecisions(): Long = when (this) {
    is MiniSatSolver -> backend.numberOfDecisions
    is GlucoseSolver -> backend.numberOfDecisions
    is CadicalSolver -> backend.numberOfDecisions
    else -> -1
}

fun Solver.maybeNumberOfConflicts(): Long = when (this) {
    is MiniSatSolver -> backend.numberOfConflicts
    is GlucoseSolver -> backend.numberOfConflicts
    is CadicalSolver -> backend.numberOfConflicts
    else -> -1
}

fun Solver.maybeNumberOfPropagations(): Long = when (this) {
    is MiniSatSolver -> backend.numberOfPropagations
    is GlucoseSolver -> backend.numberOfPropagations
    is CadicalSolver -> backend.numberOfPropagations
    else -> -1
}
