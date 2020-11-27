package com.github.lipen.satlib.solver

import com.github.lipen.satlib.utils.Context
import com.github.lipen.satlib.utils.Lit
import com.github.lipen.satlib.utils.LitArray
import com.github.lipen.satlib.utils.Model
import com.github.lipen.satlib.utils.SequenceScopeLit
import com.github.lipen.satlib.utils.toList_
import okio.BufferedSink
import java.io.File

@Suppress("FunctionName")
interface Solver : AutoCloseable {
    var context: Context

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
    fun addClause(literals: List<Lit>)

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
    fun solve(assumptions: List<Lit>): Boolean

    fun interrupt()

    fun getValue(lit: Lit): Boolean
    fun getModel(): Model

    fun dumpDimacs(sink: BufferedSink)
    fun dumpDimacs(file: File)

    // companion object {
    //     const val trueLiteral: Lit = Int.MAX_VALUE
    //     const val falseLiteral: Lit = -trueLiteral
    // }
}

inline fun Solver.switchContext(newContext: Context, block: () -> Unit) {
    val oldContext = this.context
    this.context = newContext
    block()
    this.context = oldContext
}

fun Solver.addClause(literals: Iterable<Lit>) {
    addClause(literals.toList_())
}

fun Solver.addClause(literals: Sequence<Lit>) {
    addClause(literals.asIterable())
}

fun Solver.addClause(block: SequenceScopeLit) {
    addClause(sequence(block).constrainOnce())
}

fun Solver.solve(assumptions: Iterable<Lit>): Boolean {
    return solve(assumptions.toList_())
}
