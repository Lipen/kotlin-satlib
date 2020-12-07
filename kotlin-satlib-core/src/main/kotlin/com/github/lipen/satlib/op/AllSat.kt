package com.github.lipen.satlib.op

import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.utils.Lit
import com.github.lipen.satlib.utils.Model
import com.github.lipen.satlib.utils.sign

private val log = mu.KotlinLogging.logger {}

fun Solver.allSolutions(
    essentialLiterals: List<Lit>? = null,
): Sequence<Model> = sequence {
    while (solve()) {
        val model = getModel()
        yield(model)

        val essential = essentialLiterals ?: (1..numberOfVariables)
        val refutation = essential.map { i -> i sign !model[i] }
        log.debug { "refutation = $refutation" }
        addClause(refutation)
    }
    log.debug { "No more solutions" }
}
