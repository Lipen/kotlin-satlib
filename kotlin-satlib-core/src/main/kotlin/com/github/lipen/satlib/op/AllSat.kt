package com.github.lipen.satlib.op

import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.Model
import com.github.lipen.satlib.core.sign
import com.github.lipen.satlib.solver.Solver
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

fun Solver.allSolutions(
    essential: List<Lit>? = null,
): Sequence<Model> = sequence {
    val essentialLits = essential ?: (1..numberOfVariables)

    while (solve()) {
        val model = getModel()
        yield(model)

        val refutationLits = essentialLits.map { i -> i sign !model[i] }
        logger.trace { "refutationLits = ${refutationLits.toList()}" }
        addClause(refutationLits)
    }
    logger.trace { "No more solutions" }
}

fun Solver.allSolutions(
    refutation: (Model) -> List<Lit>,
): Sequence<Model> = sequence {
    while (solve()) {
        val model = getModel()
        yield(model)

        val refutationLits = refutation(model)
        logger.trace { "refutationLits = $refutationLits" }
        addClause(refutationLits)
    }
    logger.trace { "No more solutions" }
}
