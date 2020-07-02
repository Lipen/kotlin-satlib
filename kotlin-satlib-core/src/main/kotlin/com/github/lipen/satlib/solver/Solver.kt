package com.github.lipen.satlib.solver

import com.github.lipen.satlib.op.encodeOneHot
import com.github.lipen.satlib.utils.BoolVarArray
import com.github.lipen.satlib.utils.DomainVar
import com.github.lipen.satlib.utils.DomainVarArray
import com.github.lipen.satlib.utils.IntVar
import com.github.lipen.satlib.utils.IntVarArray
import com.github.lipen.satlib.utils.Lit
import com.github.lipen.satlib.utils.LitArray
import com.github.lipen.satlib.utils.RawAssignment
import com.github.lipen.satlib.utils.toList_
import okio.BufferedSink
import java.io.File

@Suppress("FunctionName")
interface Solver : AutoCloseable {
    val numberOfVariables: Int
    val numberOfClauses: Int

    fun reset()
    override fun close()

    fun newLiteral(): Lit

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

    fun dumpDimacs(sink: BufferedSink)
    fun dumpDimacs(file: File)
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
    encodeOneHot: Boolean = true,
    init: (T) -> Lit = { newLiteral() }
): DomainVar<T> {
    val v = DomainVar.new(domain, init)
    if (encodeOneHot) encodeOneHot(v)
    return v
}

fun Solver.newIntVar(
    domain: Iterable<Int>,
    encodeOneHot: Boolean = true,
    init: (Int) -> Lit = { newLiteral() }
): IntVar = newDomainVar(domain, encodeOneHot, init)

fun <T> Solver.newDomainVarArray(
    vararg shape: Int,
    encodeOneHot: Boolean = true,
    init: (T) -> Lit = { newLiteral() },
    domain: (IntArray) -> Iterable<T>
): DomainVarArray<T> = DomainVarArray.create(shape) { index -> newDomainVar(domain(index), encodeOneHot, init) }

fun Solver.newIntVarArray(
    vararg shape: Int,
    encodeOneHot: Boolean = true,
    init: (Int) -> Lit = { newLiteral() },
    domain: (IntArray) -> Iterable<Int>
): IntVarArray = IntVarArray.create(shape) { index -> newIntVar(domain(index), encodeOneHot, init) }

fun Solver.newBoolVarArray(
    vararg shape: Int,
    init: (IntArray) -> Lit = { newLiteral() }
): BoolVarArray = BoolVarArray.create_(shape, init)
