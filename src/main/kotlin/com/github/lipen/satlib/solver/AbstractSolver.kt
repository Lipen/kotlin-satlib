package com.github.lipen.satlib.solver

import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.LitArray
import com.github.lipen.satlib.utils.write
import com.github.lipen.satlib.utils.writeln
import okio.Buffer
import okio.BufferedSink
import okio.buffer
import okio.sink
import java.io.File

@Suppress("MemberVisibilityCanBePrivate", "FunctionName")
abstract class AbstractSolver : Solver {
    override val buffer: Buffer = Buffer()

    final override fun reset() {
        buffer.clear()
        _reset()
    }

    final override fun close() {
        buffer.close()
        _close()
    }

    final override fun comment(comment: String) {
        for (line in comment.lineSequence())
            buffer.write("c ").writeln(line)
        _comment(comment)
    }

    @Suppress("OverridingDeprecatedMember")
    final override fun addClause() {
        buffer.writeln("0")
        _addClause()
    }

    final override fun addClause(lit: Lit) {
        buffer.writeln("$lit 0")
        _addClause(lit)
    }

    final override fun addClause(lit1: Lit, lit2: Lit) {
        buffer.writeln("$lit1 $lit2 0")
        _addClause(lit1, lit2)
    }

    final override fun addClause(lit1: Lit, lit2: Lit, lit3: Lit) {
        buffer.writeln("$lit1 $lit2 $lit3 0")
        _addClause(lit1, lit2, lit3)
    }

    final override fun addClause(vararg literals: Lit): Unit = addClause_(literals)

    final override fun addClause_(literals: LitArray) {
        for (lit in literals)
            buffer.write(lit.toString()).write(" ")
        buffer.writeln("0")
        _addClause_(literals)
    }

    final override fun addClause_(literals: List<Lit>) {
        for (lit in literals)
            buffer.write(lit.toString()).write(" ")
        buffer.writeln("0")
        _addClause_(literals)
    }

    final override fun solve(): Boolean {
        buffer.writeln("c solve")
        return _solve()
    }

    final override fun solve(lit: Lit): Boolean {
        buffer.writeln("c solve $lit")
        return _solve(lit)
    }

    final override fun solve(lit1: Lit, lit2: Lit): Boolean {
        buffer.writeln("c solve $lit1 $lit2")
        return _solve(lit1, lit2)
    }

    final override fun solve(lit1: Lit, lit2: Lit, lit3: Lit): Boolean {
        buffer.writeln("c solve $lit1 $lit2 $lit3")
        return _solve(lit1, lit2, lit3)
    }

    final override fun solve(vararg assumptions: Lit): Boolean = solve_(assumptions)

    final override fun solve_(assumptions: LitArray): Boolean {
        buffer.writeln("c solve ${assumptions.joinToString(" ")}")
        return _solve_(assumptions)
    }

    final override fun solve_(assumptions: List<Lit>): Boolean {
        buffer.write("c solve")
        for (lit in assumptions)
            buffer.write(" $lit")
        buffer.writeln("")
        return _solve_(assumptions)
    }

    fun dumpDimacs(sink: BufferedSink) {
        sink.writeln("p cnf $numberOfVariables $numberOfClauses")
        buffer.copyTo(sink.buffer)
    }

    fun dumpDimacs(file: File) {
        file.sink().buffer().use {
            dumpDimacs(it)
        }
    }

    protected abstract fun _reset()
    protected abstract fun _close()
    protected abstract fun _comment(comment: String)

    protected abstract fun _addClause()
    protected abstract fun _addClause(lit: Lit)
    protected abstract fun _addClause(lit1: Lit, lit2: Lit)
    protected abstract fun _addClause(lit1: Lit, lit2: Lit, lit3: Lit)
    protected abstract fun _addClause_(literals: LitArray)
    protected abstract fun _addClause_(literals: List<Lit>)

    protected abstract fun _solve(): Boolean
    protected abstract fun _solve(lit: Lit): Boolean
    protected abstract fun _solve(lit1: Lit, lit2: Lit): Boolean
    protected abstract fun _solve(lit1: Lit, lit2: Lit, lit3: Lit): Boolean
    protected abstract fun _solve_(assumptions: LitArray): Boolean
    protected abstract fun _solve_(assumptions: List<Lit>): Boolean
}
