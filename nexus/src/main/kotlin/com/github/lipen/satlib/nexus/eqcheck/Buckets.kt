package com.github.lipen.satlib.nexus.eqcheck

import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.sign
import com.github.lipen.satlib.nexus.utils.bit
import com.github.lipen.satlib.nexus.utils.pow
import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.solver.solve
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

internal fun bucketValuation(lits: List<Lit>, f: Long): List<Lit> {
    return lits.mapIndexed { i, lit -> lit sign f.bit(lits.size - i - 1) }
}

internal fun valuationIndex(valuation: List<Lit>): Long {
    return valuation.mapIndexed { i, v -> if (v > 0) 2L.pow(valuation.size - i - 1) else 0L }.sum()
}

internal data class Bucket(
    val lits: List<Lit>,
    val domain: List<Long>,
) {
    init {
        require(lits.size <= 63)
    }

    val saturation: Double = domain.size.toDouble() / 2L.pow(lits.size)

    fun decomposition(): List<List<Lit>> {
        return domain.map { f -> bucketValuation(lits, f) }
    }
}

internal fun Solver.evalAllValuations(lits: List<Lit>): Bucket {
    require(lits.size <= 60)

    val domain = mutableListOf<Long>()
    val n = 2L.pow(lits.size)

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
