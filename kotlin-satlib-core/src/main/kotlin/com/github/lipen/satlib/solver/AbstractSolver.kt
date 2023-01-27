package com.github.lipen.satlib.solver

import com.github.lipen.satlib.core.Context
import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.newContext
import com.github.lipen.satlib.utils.toList_
import java.io.File

private val log = mu.KotlinLogging.logger {}

abstract class AbstractSolver : Solver {
    final override var context: Context = newContext()
    final override var numberOfVariables: Int = 0
        private set
    final override var numberOfClauses: Int = 0
        private set
    final override val assumptions: MutableList<Lit> = mutableListOf()

    final override fun reset() {
        context = newContext()
        numberOfVariables = 0
        numberOfClauses = 0
        assumptions.clear()
        _reset()
    }

    final override fun close() {
        _close()
    }

    final override fun interrupt() {
        _interrupt()
    }

    final override fun comment(comment: String) {
        log.trace { "// $comment" }
        _comment(comment)
    }

    final override fun dumpDimacs(file: File) {
        log.debug { "dumpDimacs(file = $file)" }
        _dumpDimacs(file)
    }

    final override fun newLiteral(): Lit {
        val outer = ++numberOfVariables
        return _newLiteral(outer)
    }

    final override fun addClause(literals: List<Lit>) {
        ++numberOfClauses
        val pool = literals.toList_()
        // log.trace { "addClause($pool)" }
        _addClause(pool)
    }

    final override fun solve(): Boolean {
        val res = _solve()
        assumptions.clear()
        return res
    }

    override fun toString(): String {
        return this::class.java.simpleName
    }

    protected abstract fun _reset()
    protected abstract fun _close()
    protected abstract fun _interrupt()
    protected abstract fun _dumpDimacs(file: File)

    protected abstract fun _comment(comment: String)
    protected abstract fun _newLiteral(outer: Lit): Lit
    protected abstract fun _addClause(literals: List<Lit>)
    protected abstract fun _solve(): Boolean
}
