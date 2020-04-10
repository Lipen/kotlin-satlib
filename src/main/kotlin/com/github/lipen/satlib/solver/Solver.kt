package com.github.lipen.satlib.solver

import com.github.lipen.satlib.core.BoolVarArray
import com.github.lipen.satlib.core.DomainVar
import com.github.lipen.satlib.core.DomainVarArray
import com.github.lipen.satlib.core.IntVar
import com.github.lipen.satlib.core.IntVarArray
import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.LitArray
import com.github.lipen.satlib.core.RawAssignment
import com.github.lipen.satlib.op.encodeOneHot
import com.github.lipen.satlib.utils.toList_

@Suppress("FunctionName")
interface Solver : AutoCloseable {
    val numberOfVariables: Int
    val numberOfClauses: Int

    fun reset()
    override fun close()

    fun newVariable(): Lit

    fun comment(comment: String)

    @Deprecated(
        "Clause must contain at least one literal!",
        ReplaceWith("addClause(...)")
    )
    fun addClause()
    fun addClause(lit: Lit)
    fun addClause(lit1: Lit, lit2: Lit)
    fun addClause(lit1: Lit, lit2: Lit, lit3: Lit)
    fun addClause(vararg literals: Lit): Unit = addClause_(literals)
    fun addClause_(literals: LitArray)
    fun addClause_(literals: List<Lit>)

    fun assume(vararg literals: Lit): Unit = assume_(literals)
    fun assume_(literals: LitArray)
    fun assume_(literals: List<Lit>)
    fun clearAssumptions()

    // Note:
    //  - The `solve()` method must use the assumptions passed via the `assume` method.
    //  - Other `solve(...)` methods must use *only* passed assumptions.
    fun solve(): Boolean
    fun solve(lit: Lit): Boolean
    fun solve(lit1: Lit, lit2: Lit): Boolean
    fun solve(lit1: Lit, lit2: Lit, lit3: Lit): Boolean
    fun solve(vararg assumptions: Lit): Boolean = solve_(assumptions)
    fun solve_(assumptions: LitArray): Boolean
    fun solve_(assumptions: List<Lit>): Boolean

    fun interrupt()

    fun getValue(lit: Lit): Boolean
    fun getModel(): RawAssignment
}

fun Solver.addClause(literals: Iterable<Lit>) {
    addClause_(literals.toList_())
}

fun Solver.addClause(literals: Sequence<Lit>) {
    addClause(literals.asIterable())
}

fun Solver.addClause(block: suspend SequenceScope<Lit>.() -> Unit) {
    addClause(sequence(block).constrainOnce())
}

fun Solver.solve(assumptions: Iterable<Lit>): Boolean {
    return solve_(assumptions.toList_())
}

fun <T> Solver.newDomainVar(
    domain: Iterable<T>,
    init: (T) -> Lit = { newVariable() }
): DomainVar<T> = DomainVar(domain, init).also { encodeOneHot(it) }

fun Solver.newIntVar(
    domain: Iterable<Int>,
    init: (Int) -> Lit = { newVariable() }
): IntVar = newDomainVar(domain, init)

fun <T> Solver.newDomainVarArray(
    vararg shape: Int,
    init: (T) -> Lit = { newVariable() },
    domain: (IntArray) -> Iterable<T>
): DomainVarArray<T> = DomainVarArray.create(shape) { index -> newDomainVar(domain(index), init) }

fun Solver.newIntVarArray(
    vararg shape: Int,
    init: (Int) -> Lit = { newVariable() },
    domain: (IntArray) -> Iterable<Int>
): IntVarArray = IntVarArray.create(shape) { index -> newIntVar(domain(index), init) }

fun Solver.newBoolVarArray(
    vararg shape: Int,
    init: (IntArray) -> Lit = { newVariable() }
): BoolVarArray = BoolVarArray.create(shape, init)
