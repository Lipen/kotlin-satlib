package com.github.lipen.satlib.card

import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.solver.Solver

fun Solver.declareTotalizer(variables: Iterable<Lit>): List<Lit> {
    val queue = ArrayDeque<List<Lit>>()

    for (e in variables) {
        queue.addLast(listOf(e))
    }

    comment("Totalizer(${queue.size})")

    while (queue.size != 1) {
        val a = queue.removeFirst()
        val b = queue.removeFirst()

        val m1 = a.size
        val m2 = b.size
        val m = m1 + m2

        val r = List(m) { newLiteral() }
        queue.addLast(r)

        for (alpha in 0..m1) {
            for (beta in 0..m2) {
                val sigma = alpha + beta
                // TODO: rewrite without intermediate `c1` and `c2` variables
                val c1: List<Lit>? = when {
                    sigma == 0 -> null
                    alpha == 0 -> listOf(-b[beta - 1], r[sigma - 1])
                    beta == 0 -> listOf(-a[alpha - 1], r[sigma - 1])
                    else -> listOf(-a[alpha - 1], -b[beta - 1], r[sigma - 1])
                }
                val c2: List<Lit>? = when {
                    sigma == m -> null
                    alpha == m1 -> listOf(b[beta], -r[sigma])
                    beta == m2 -> listOf(a[alpha], -r[sigma])
                    else -> listOf(a[alpha], b[beta], -r[sigma])
                }
                c1?.let { addClause(it) }
                c2?.let { addClause(it) }
            }
        }
    }

    val totalizer = queue.removeFirst()
    return totalizer
    // return newBoolVarArray(totalizer.size) { (i) -> totalizer[i - 1] }
}
