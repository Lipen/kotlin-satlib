package com.github.lipen.satlib.solver

import com.github.lipen.satlib.core.Context
import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.LitArray
import com.github.lipen.satlib.core.Model
import java.io.File

interface Solver2 : AutoCloseable {
    var context: Context

    val numberOfVariables: Int
    val numberOfClauses: Int

    val assumptions: MutableList<Lit>

    override fun close()
    fun reset()
    fun interrupt()
    fun dumpDimacs(file: File)

    fun comment(comment: String)
    fun newLiteral(): Lit
    fun addClause(literals: List<Lit>)
    fun solve(): Boolean
    fun getValue(lit: Lit): Boolean
    fun getModel(): Model
}

fun Solver2.assume(literals: Iterable<Lit>) {
    assumptions.addAll(literals)
}

fun Solver2.assume(literals: Sequence<Lit>) {
    assume(literals.asIterable())
}

fun Solver2.assume(literals: LitArray) {
    assume(literals.asList())
}

@JvmName("assumeVararg")
fun Solver2.assume(vararg literals: Lit) {
    assume(literals)
}

fun Solver2.solve(assumptions: Iterable<Lit>): Boolean {
    assume(assumptions)
    return solve()
}

fun Solver2.solve(assumptions: Sequence<Lit>): Boolean {
    return solve(assumptions.asIterable())
}

fun Solver2.solve(assumptions: LitArray): Boolean {
    return solve(assumptions.asList())
}

@JvmName("solveVararg")
fun Solver2.solve(vararg assumptions: Lit): Boolean {
    return solve(assumptions)
}
