package com.github.lipen.satlib.solver

import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.LitArray
import com.github.lipen.satlib.core.Model
import java.io.File

class MockSolver(
    private val __comment: (String) -> Unit = {},
    private val __newLiteral: (outerNumberOfVariables: Int) -> Lit = { it },
    private val __addClause: (List<Lit>) -> Unit = {},
    private val __solve: () -> Boolean = { TODO() },
    private val __reset: () -> Unit = {},
    private val __close: () -> Unit = {},
    private val __dumpDimacs: (File) -> Unit = {},
    private val __interrupt: () -> Unit = {},
    private val __getModel: () -> Model = { TODO() },
) : AbstractSolver() {
    override fun _reset() {
        __reset()
    }

    override fun _close() {
        __close()
    }

    override fun _dumpDimacs(file: File) {
        __dumpDimacs(file)
    }

    override fun _comment(comment: String) {
        __comment(comment)
    }

    override fun _newLiteral(outerNumberOfVariables: Int): Lit {
        return __newLiteral(outerNumberOfVariables)
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

    override fun _solve(assumptions: LitArray): Boolean {
        return _solve(assumptions.asList())
    }

    override fun _solve(assumptions: List<Lit>): Boolean {
        return __solve()
    }

    override fun interrupt() {
        __interrupt()
    }

    override fun getValue(lit: Lit): Boolean {
        return getModel()[lit]
    }

    override fun getModel(): Model {
        return __getModel()
    }
}
