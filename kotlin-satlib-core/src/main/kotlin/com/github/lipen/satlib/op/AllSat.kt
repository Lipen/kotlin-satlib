package com.github.lipen.satlib.op

import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.Model
import com.github.lipen.satlib.core.sign
import com.github.lipen.satlib.solver.Solver

private val log = mu.KotlinLogging.logger {}

fun Solver.allSolutions(
    essentialLiterals: List<Lit>? = null,
): Sequence<Model> = sequence {
    val essential = essentialLiterals ?: (1..numberOfVariables)

    while (solve()) {
        val model = getModel()
        yield(model)

        val refutation = essential.map { i -> i sign !model[i] }
        log.debug { "refutation = $refutation" }
        addClause(refutation)
    }
    log.debug { "No more solutions" }
}
