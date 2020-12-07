package com.github.lipen.satlib.card

import com.github.lipen.satlib.op.allSolutions
import com.github.lipen.satlib.op.imply
import com.github.lipen.satlib.solver.MiniSatSolver
import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.utils.AssumptionsProvider
import com.github.lipen.satlib.utils.Lit
import com.github.lipen.satlib.utils.LitArray
import com.github.lipen.satlib.utils.SequenceScopeLit
import com.github.lipen.satlib.utils.convert
import com.github.lipen.satlib.utils.newBoolVarArray
import com.github.lipen.satlib.utils.toList_
import com.github.lipen.satlib.utils.useWith

@Suppress("MemberVisibilityCanBePrivate")
class Cardinality private constructor(
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
            println("De-assuming the upper bound (null)")
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
            println("De-assuming the upper bound (null)")
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

private fun main() {
    fun Iterable<Boolean>.toBinaryString(): String =
        joinToString("") { if (it) "1" else "0" }

    MiniSatSolver().useWith {
        val n = 5
        val ub = 4
        val lb = 2
        val lits = context("lits") {
            newBoolVarArray(n)
        }

        for (i in 1 until n) {
            imply(lits[i], lits[i + 1])
        }

        // addClause(lits[5])

        val totalizer = declareCardinality(lits.values)
        println("totalizer = ${totalizer.totalizer}")
        totalizer.assumeUpperBoundLessThanOrEqual(ub)
        totalizer.assumeLowerBoundGreaterThanOrEqual(lb)
        // totalizer.declareUpperBoundLessThanOrEqual(ub)
        // totalizer.declareLowerBoundGreaterThanOrEqual(lb)

        for ((i, model) in allSolutions(lits.values).withIndex()) {
            println("Solution #${i + 1}")
            val litsModel = lits.convert(model)
            check(litsModel.shape.size == 1)
            // println("       123456789")
            println("lits = ${litsModel.values.toBinaryString()} (count = ${litsModel.values.count { it }})")
        }
    }
}
