package com.github.lipen.satlib.op

import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.Model
import com.github.lipen.satlib.core.sign
import com.github.lipen.satlib.solver.Solver

private val log = mu.KotlinLogging.logger {}

fun Solver.allSolutions(
    essential: List<Lit>? = null,
): Sequence<Model> = sequence {
    val essentialLits = essential ?: (1..numberOfVariables)

    while (solve()) {
        val model = getModel()
        yield(model)

        val refutationLits = essentialLits.map { lit -> lit sign !model[lit] }
        log.trace { "refutationLits = ${refutationLits.toList()}" }
        addClause(refutationLits)
    }
    log.trace { "No more solutions" }
}

fun Solver.allSolutions(
    refutation: (Model) -> Iterable<Lit>,
): Sequence<Model> = sequence {
    while (solve()) {
        val model = getModel()
        yield(model)

        val refutationLits = refutation(model)
        log.trace { "refutationLits = ${refutationLits.toList()}" }
        addClause(refutationLits)
    }
    log.trace { "No more solutions" }
}
