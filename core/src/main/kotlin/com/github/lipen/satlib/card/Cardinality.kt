package com.github.lipen.satlib.card

import com.github.lipen.satlib.core.AssumptionsProvider
import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.LitArray
import com.github.lipen.satlib.core.SequenceScopeLit
import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.utils.toList_

private val log = mu.KotlinLogging.logger {}

@Suppress("MemberVisibilityCanBePrivate")
class Cardinality(
    private val solver: Solver,
    val totalizer: List<Lit>, // "output variables"
) {
    private var assumptionsUB: List<Lit> = emptyList()
    private var assumptionsLB: List<Lit> = emptyList()
    private val assumptionsProvider = AssumptionsProvider { assumptionsUB + assumptionsLB }

    var declaredUpperBound: Int? = null
        private set
    var declaredLowerBound: Int? = null
        private set

    init {
        require(totalizer.isNotEmpty())
        solver.assumptionsObservable.register(assumptionsProvider)
    }

    fun unregister() {
        solver.assumptionsObservable.unregister(assumptionsProvider)
    }

    fun declareUpperBoundLessThan(newUpperBound: Int?) {
        declaredUpperBound?.let { curUpperBound ->
            check(newUpperBound != null && newUpperBound <= curUpperBound) { "Cannot soften UB" }
        }

        if (newUpperBound == null) return

        solver.declareComparatorLessThan(totalizer, newUpperBound, declaredUpperBound)
        declaredUpperBound = newUpperBound
    }

    fun declareUpperBoundLessThanOrEqual(newUpperBound: Int?) {
        declareUpperBoundLessThan(newUpperBound?.let { it + 1 })
    }

    fun declareLowerBoundGreaterThan(newLowerBound: Int?) {
        declareLowerBoundGreaterThanOrEqual(newLowerBound?.let { it + 1 })
    }

    fun declareLowerBoundGreaterThanOrEqual(newLowerBound: Int?) {
        declaredLowerBound?.let { curLowerBound ->
            check(newLowerBound != null && newLowerBound >= curLowerBound) { "Cannot soften LB" }
        }

        if (newLowerBound == null) return

        solver.declareComparatorGreaterThanOrEqual(totalizer, newLowerBound, declaredLowerBound)
        declaredLowerBound = newLowerBound
    }

    fun assumeUpperBoundLessThan(newUpperBound: Int?) {
        assumptionsUB = if (newUpperBound == null) {
            log.debug { "De-assuming the upper bound (null)" }
            emptyList()
        } else {
            require(newUpperBound <= totalizer.size) {
                "Upper bound ($newUpperBound) is too large (size = ${totalizer.size})"
            }
            (newUpperBound..totalizer.size).map { i -> -totalizer[i - 1] }
        }
    }

    fun assumeUpperBoundLessThanOrEqual(newUpperBound: Int?) {
        assumeUpperBoundLessThan(newUpperBound?.let { it + 1 })
    }

    fun assumeLowerBoundGreaterThan(newLowerBound: Int?) {
        assumeLowerBoundGreaterThanOrEqual(newLowerBound?.let { it + 1 })
    }

    fun assumeLowerBoundGreaterThanOrEqual(newLowerBound: Int?) {
        assumptionsLB = if (newLowerBound == null) {
            log.debug { "De-assuming the upper bound (null)" }
            emptyList()
        } else {
            require(newLowerBound >= 1) {
                "Lower bound ($newLowerBound) is too small"
            }
            (1..newLowerBound).map { i -> totalizer[i - 1] }
        }
    }

    companion object {
        fun declare(solver: Solver, literals: List<Lit>): Cardinality {
            val totalizer = solver.declareTotalizer(literals)
            return Cardinality(solver, totalizer)
        }
    }
}

fun Solver.declareCardinality(literals: Iterable<Lit>): Cardinality =
    Cardinality.declare(
        solver = this,
        literals = literals.toList_()
    )

fun Solver.declareCardinality(literals: LitArray): Cardinality =
    declareCardinality(literals.asList())

@JvmName("declareCardinalityVararg")
fun Solver.declareCardinality(vararg literals: Int): Cardinality =
    declareCardinality(literals)

fun Solver.declareCardinality(literals: Sequence<Lit>): Cardinality =
    declareCardinality(literals.asIterable())

fun Solver.declareCardinality(literals: SequenceScopeLit): Cardinality =
    declareCardinality(sequence(literals).constrainOnce())
