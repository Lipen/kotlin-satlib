package com.github.lipen.satlib.solver

import com.github.lipen.satlib.core.AssumptionsObservable
import com.github.lipen.satlib.core.Context
import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.LitArray
import com.github.lipen.satlib.core.Model
import com.github.lipen.satlib.core.SequenceScopeLit
import okio.BufferedSink
import java.io.File

@Suppress("FunctionName")
interface Solver : AutoCloseable {
    var context: Context
    val numberOfVariables: Int
    val numberOfClauses: Int
    val assumptionsObservable: AssumptionsObservable

    fun reset()
    override fun close()

    fun comment(comment: String)

    fun newLiteral(): Lit

    @Deprecated(
        "Clause must contain at least one literal!",
        ReplaceWith("addClause(...)")
    )
    fun addClause()
    fun addClause(lit: Lit)
    fun addClause(lit1: Lit, lit2: Lit)
    fun addClause(lit1: Lit, lit2: Lit, lit3: Lit)
    fun addClause(literals: LitArray)
    fun addClause(literals: Iterable<Lit>)

    // Note:
    //  - `solve()` method must use assumptions passed via the `addAssumptions` method.
    //  - `solve(assumptions)` methods must use *only* the passed `assumptions`.
    fun solve(): Boolean
    fun solve(assumptions: LitArray): Boolean
    fun solve(assumptions: Iterable<Lit>): Boolean

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

fun Solver.addClause(vararg literals: Lit) {
    addClause(literals)
}

fun Solver.addClause(literals: Sequence<Lit>) {
    addClause(literals.asIterable())
}

fun Solver.addClause(block: SequenceScopeLit) {
    addClause(sequence(block).constrainOnce())
}

fun Solver.solve(vararg assumptions: Lit): Boolean {
    return solve(assumptions)
}

// fun Solver.solve(literals: Sequence<Lit>) {
//     solve(literals.asIterable())
// }
//
// fun Solver.solve(block: SequenceScopeLit) {
//     solve(sequence(block).constrainOnce())
// }
