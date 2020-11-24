package com.github.lipen.satlib.solver

import com.github.lipen.satlib.solver.jni.JCadical
import com.github.lipen.satlib.utils.Lit
import com.github.lipen.satlib.utils.LitArray
import com.github.lipen.satlib.utils.Model
import com.github.lipen.satlib.utils.Model1
import com.github.lipen.satlib.utils.useWith

@Suppress("MemberVisibilityCanBePrivate")
class CadicalSolver @JvmOverloads constructor(
    val backend: JCadical = JCadical(),
) : AbstractSolver() {
    override val numberOfVariables: Int get() = backend.numberOfVariables
    override val numberOfClauses: Int get() = backend.numberOfClauses

    override fun _reset() {
        backend.reset()
    }

    override fun _close() {
        backend.close()
    }

    override fun newLiteral(): Lit {
        return backend.newVariable()
    }

    override fun _comment(comment: String) {}

    @Suppress("OverridingDeprecatedMember")
    override fun _addClause() {
        @Suppress("deprecation")
        backend.addClause()
    }

    override fun _addClause(lit: Lit) {
        backend.addClause(lit)
    }

    override fun _addClause(lit1: Lit, lit2: Lit) {
        backend.addClause(lit1, lit2)
    }

    override fun _addClause(lit1: Int, lit2: Int, lit3: Int) {
        backend.addClause(lit1, lit2, lit3)
    }

    override fun _addClause(literals: IntArray) {
        backend.addClause_(literals)
    }

    override fun _addClause(literals: List<Int>) {
        _addClause(literals.toIntArray())
    }

    override fun _solve(): Boolean {
        return backend.solve()
    }

    override fun _solve(lit: Lit): Boolean {
        return backend.solve(lit)
    }

    override fun _solve(lit1: Lit, lit2: Lit): Boolean {
        return backend.solve(lit1, lit2)
    }

    override fun _solve(lit1: Lit, lit2: Lit, lit3: Lit): Boolean {
        return backend.solve(lit1, lit2, lit3)
    }

    override fun _solve(assumptions: LitArray): Boolean {
        return backend.solve_(assumptions)
    }

    override fun _solve(assumptions: List<Lit>): Boolean {
        return _solve(assumptions.toIntArray())
    }

    override fun interrupt() {
        backend.terminate()
    }

    override fun getValue(lit: Lit): Boolean {
        return backend.getValue(lit)
    }

    override fun getModel(): Model {
        return Model1(backend.getModel())
    }
}

fun main() {
    CadicalSolver().useWith {
        testSolverWithAssumptions()
    }
}
