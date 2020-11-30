package com.github.lipen.satlib.solver

import com.github.lipen.satlib.utils.Context
import com.github.lipen.satlib.utils.Lit
import com.github.lipen.satlib.utils.LitArray
import com.github.lipen.satlib.utils.newContext
import com.github.lipen.satlib.utils.toList_
import com.github.lipen.satlib.utils.write
import com.github.lipen.satlib.utils.writeln
import okio.Buffer
import okio.BufferedSink
import okio.buffer
import okio.sink
import java.io.File

@Suppress("FunctionName")
abstract class AbstractSolver : Solver {
    private val buffer: Buffer = Buffer()
    private val _assumptions: MutableList<Lit> = mutableListOf()

    final override lateinit var context: Context
    final override var numberOfVariables: Int = 0
        private set
    final override var numberOfClauses: Int = 0
        private set
    final override val assumptions: List<Lit> = _assumptions

    final override fun reset() {
        context = newContext()
        numberOfVariables = 0
        numberOfClauses = 0
        _assumptions.clear()
        buffer.clear()
        _reset()
    }

    final override fun close() {
        buffer.close()
        _close()
    }

    final override fun comment(comment: String) {
        for (line in comment.lineSequence()) {
            buffer.write("c ").writeln(line)
        }
        _comment(comment)
    }

    final override fun newLiteral(): Lit {
        val outerNumberOfVariables = ++numberOfVariables
        return _newLiteral(outerNumberOfVariables)
    }

    @Suppress("OverridingDeprecatedMember")
    final override fun addClause() {
        ++numberOfClauses
        buffer.writeln("0")
        _addClause()
    }

    final override fun addClause(lit: Lit) {
        ++numberOfClauses
        buffer.writeln("$lit 0")
        _addClause(lit)
    }

    final override fun addClause(lit1: Lit, lit2: Lit) {
        ++numberOfClauses
        buffer.writeln("$lit1 $lit2 0")
        _addClause(lit1, lit2)
    }

    final override fun addClause(lit1: Lit, lit2: Lit, lit3: Lit) {
        ++numberOfClauses
        buffer.writeln("$lit1 $lit2 $lit3 0")
        _addClause(lit1, lit2, lit3)
    }

    final override fun addClause(literals: LitArray) {
        ++numberOfClauses
        for (lit in literals) {
            buffer.write(lit.toString()).write(" ")
        }
        buffer.writeln("0")
        _addClause(literals)
    }

    final override fun addClause(literals: Iterable<Lit>) {
        ++numberOfClauses
        val pool = literals.toList_()
        for (lit in pool) {
            buffer.write(lit.toString()).write(" ")
        }
        buffer.writeln("0")
        _addClause(pool)
    }

    final override fun addAssumptions(literals: LitArray) {
        addAssumptions(literals.asIterable())
    }

    final override fun addAssumptions(literals: Iterable<Lit>) {
        _assumptions.addAll(literals)
    }

    final override fun clearAssumptions() {
        _assumptions.clear()
    }

    final override fun solve(): Boolean =
        if (assumptions.isEmpty()) {
            buffer.writeln("c solve")
            _solve()
        } else {
            solve(assumptions)
        }

    final override fun solve(assumptions: LitArray): Boolean {
        buffer.writeln("c solve ${assumptions.joinToString(" ")}")
        return _solve(assumptions)
    }

    final override fun solve(assumptions: Iterable<Lit>): Boolean {
        val pool = assumptions.toList_()
        buffer.writeln("c solve ${pool.joinToString(" ")}")
        return _solve(pool)
    }

    final override fun dumpDimacs(sink: BufferedSink) {
        sink.writeln("p cnf $numberOfVariables $numberOfClauses")
        buffer.copyTo(sink.buffer)
    }

    final override fun dumpDimacs(file: File) {
        file.sink().buffer().use {
            dumpDimacs(it)
        }
    }

    override fun toString(): String {
        return this::class.java.simpleName
    }

    protected abstract fun _reset()
    protected abstract fun _close()

    protected abstract fun _comment(comment: String)

    protected abstract fun _newLiteral(outerNumberOfVariables: Int): Lit

    protected abstract fun _addClause()
    protected abstract fun _addClause(lit: Lit)
    protected abstract fun _addClause(lit1: Lit, lit2: Lit)
    protected abstract fun _addClause(lit1: Lit, lit2: Lit, lit3: Lit)
    protected abstract fun _addClause(literals: LitArray)
    protected open fun _addClause(literals: List<Lit>) {
        _addClause(literals.toIntArray())
    }

    protected abstract fun _solve(): Boolean
    protected abstract fun _solve(assumptions: LitArray): Boolean
    protected open fun _solve(assumptions: List<Lit>): Boolean {
        return _solve(assumptions.toIntArray())
    }
}
