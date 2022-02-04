package com.github.lipen.satlib.nexus.eqcheck

import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.sign
import com.github.lipen.satlib.nexus.utils.bit
import com.github.lipen.satlib.nexus.utils.pow
import com.github.lipen.satlib.solver.Solver
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

internal data class BucketEvaluationResult(
    val bucket: List<Lit>,
    val saturation: Double,
    val domain: List<Int>,
)

internal fun Solver.evalBucket(bucket: List<Lit>): BucketEvaluationResult {
    val domain = mutableListOf<Int>()
    val n = 2.pow(bucket.size)

    for (f in 0 until n) {
        val assumptions = bucket.mapIndexed { i, lit -> lit sign f.bit(i) }
        val res = solve(assumptions)
        if (res) {
            domain.add(f)
        }
    }

    val saturation = domain.size.toDouble() / n.toDouble()
    return BucketEvaluationResult(bucket, saturation, domain)
}
