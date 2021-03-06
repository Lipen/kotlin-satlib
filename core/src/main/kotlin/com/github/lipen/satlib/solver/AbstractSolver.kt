package com.github.lipen.satlib.solver

import com.github.lipen.satlib.core.AssumptionsObservable
import com.github.lipen.satlib.core.Context
import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.LitArray
import com.github.lipen.satlib.core.newContext
import com.github.lipen.satlib.utils.toList_
import java.io.File

private val log = mu.KotlinLogging.logger {}

@Suppress("FunctionName")
abstract class AbstractSolver : Solver {
    final override var context: Context = newContext()
    final override var numberOfVariables: Int = 0
        private set
    final override var numberOfClauses: Int = 0
        private set
    final override val assumptionsObservable: AssumptionsObservable = AssumptionsObservable()

    final override fun reset() {
        context = newContext()
        numberOfVariables = 0
        numberOfClauses = 0
        assumptionsObservable.clear()
        _reset()
    }

    final override fun close() {
        _close()
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
        val outerNumberOfVariables = ++numberOfVariables
        return _newLiteral(outerNumberOfVariables)
    }

    @Suppress("OverridingDeprecatedMember")
    final override fun addClause() {
        // log.trace { "addClause()" }
        ++numberOfClauses
        _addClause()
    }

    final override fun addClause(lit: Lit) {
        // log.trace { "addClause($lit)" }
        ++numberOfClauses
        _addClause(lit)
    }

    final override fun addClause(lit1: Lit, lit2: Lit) {
        // log.trace { "addClause($lit1, $lit2)" }
        ++numberOfClauses
        _addClause(lit1, lit2)
    }

    final override fun addClause(lit1: Lit, lit2: Lit, lit3: Lit) {
        // log.trace { "addClause($lit1, $lit2, $lit3)" }
        ++numberOfClauses
        _addClause(lit1, lit2, lit3)
    }

    final override fun addClause(literals: LitArray) {
        // log.trace { "addClause(${literals.asList()})" }
        ++numberOfClauses
        _addClause(literals)
    }

    final override fun addClause(literals: Iterable<Lit>) {
        ++numberOfClauses
        val pool = literals.toList_()
        // log.trace { "addClause($pool)" }
        _addClause(pool)
    }

    final override fun solve(): Boolean {
        val assumptions = assumptionsObservable.collect()
        return if (assumptions.isEmpty()) {
            log.debug { "solve()" }
            _solve()
        } else {
            solve(assumptions)
        }
    }

    final override fun solve(assumptions: LitArray): Boolean {
        log.debug { "solve(assumptions = ${assumptions.asList()})" }
        return _solve(assumptions)
    }

    final override fun solve(assumptions: Iterable<Lit>): Boolean {
        val pool = assumptions.toList_()
        log.debug { "solve(assumptions = $pool)" }
        return _solve(pool)
    }

    override fun toString(): String {
        return this::class.java.simpleName
    }

    protected abstract fun _reset()
    protected abstract fun _close()
    protected abstract fun _dumpDimacs(file: File)

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
