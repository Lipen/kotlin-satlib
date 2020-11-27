package com.github.lipen.satlib.solver

import com.github.lipen.satlib.utils.Lit
import com.github.lipen.satlib.utils.LitArray
import com.github.lipen.satlib.utils.Model

class MockSolver(
    private val __comment: (String) -> Unit = {},
    private val __newLiteral: () -> Lit = { TODO() },
    private val __addClause: (List<Lit>) -> Unit = {},
    private val __solve: () -> Boolean = { TODO() },
    private val __reset: () -> Unit = {},
    private val __close: () -> Unit = {},
) : AbstractSolver() {
    override var numberOfVariables: Int = 0
        private set
    override var numberOfClauses: Int = 0
        private set

    override fun _reset() {
        __reset()
    }

    override fun _close() {
        __close()
    }

    override fun _comment(comment: String) {
        __comment(comment)
    }

    override fun newLiteral(): Lit {
        return __newLiteral()
    }

    override fun _addClause() {
        _addClause(intArrayOf())
    }

    override fun _addClause(lit: Lit) {
        _addClause(intArrayOf(lit))
    }

    override fun _addClause(lit1: Lit, lit2: Lit) {
        _addClause(intArrayOf(lit1, lit2))
    }

    override fun _addClause(lit1: Lit, lit2: Lit, lit3: Lit) {
        _addClause(intArrayOf(lit1, lit2, lit3))
    }

    override fun _addClause(literals: LitArray) {
        _addClause(literals.asList())
    }

    override fun _addClause(literals: List<Lit>) {
        __addClause(literals)
    }

    override fun _solve(): Boolean {
        return _solve(intArrayOf())
    }

    override fun _solve(lit: Lit): Boolean {
        return _solve(intArrayOf(lit))
    }

    override fun _solve(lit1: Lit, lit2: Lit): Boolean {
        return _solve(intArrayOf(lit1, lit2))
    }

    override fun _solve(lit1: Lit, lit2: Lit, lit3: Lit): Boolean {
        return _solve(intArrayOf(lit1, lit2, lit3))
    }

    override fun _solve(assumptions: LitArray): Boolean {
        return _solve(assumptions.asList())
    }

    override fun _solve(assumptions: List<Lit>): Boolean {
        return __solve()
    }

    override fun interrupt() {
        TODO()
    }

    override fun getValue(lit: Lit): Boolean {
        TODO()
    }

    override fun getModel(): Model {
        TODO()
    }
}
