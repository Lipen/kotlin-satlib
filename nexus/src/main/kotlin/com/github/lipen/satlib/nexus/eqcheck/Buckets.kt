package com.github.lipen.satlib.nexus.eqcheck

import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.sign
import com.github.lipen.satlib.nexus.utils.bit
import com.github.lipen.satlib.nexus.utils.pow
import com.github.lipen.satlib.solver.Solver
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

internal fun bucketValuation(lits: List<Lit>, f: Long) : List<Lit> {
    return lits.mapIndexed { i, lit -> lit sign f.bit(lits.size - i - 1) }
}

internal fun valuationIndex(valuation: List<Lit>): Long {
    return valuation.mapIndexed { i, v -> if (v > 0) 2L.pow(valuation.size - i - 1) else 0L }.sum()
}

internal data class Bucket(
    val lits: List<Lit>,
    val domain: List<Long>,
) {
    val saturation: Double = domain.size.toDouble() / 2.pow(lits.size)

    fun decomposition(): List<List<Lit>> {
        return domain.map { f -> bucketValuation(lits, f) }
    }
}

internal fun Solver.evalBucket(lits: List<Lit>): Bucket {
    val domain = mutableListOf<Long>()
    val n = 2.pow(lits.size)

    for (f in 0L until n) {
        val assumptions = bucketValuation(lits, f)
        val res = solve(assumptions)
        if (res) {
            domain.add(f)
        } /*else {
            logger.debug {
                "UNSAT on f=$f for assumptions=$assumptions"
            }
        }*/
    }

    return Bucket(lits, domain)
}
