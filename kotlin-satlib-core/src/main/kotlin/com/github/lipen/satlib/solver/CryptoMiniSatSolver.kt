package com.github.lipen.satlib.solver

import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.LitArray
import com.github.lipen.satlib.core.Model
import com.github.lipen.satlib.solver.jni.JCryptoMiniSat
import com.github.lipen.satlib.utils.useWith

@Suppress("MemberVisibilityCanBePrivate")
class CryptoMiniSatSolver @JvmOverloads constructor(
    val backend: JCryptoMiniSat = JCryptoMiniSat(),
) : AbstractSolver() {
    override fun _reset() {
        backend.reset()
    }

    override fun _close() {
        backend.close()
    }

    override fun _comment(comment: String) {}

    override fun _newLiteral(outerNumberOfVariables: Int): Lit {
        backend.newVariable()
        return outerNumberOfVariables
    }

    override fun _addClause() {
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
        backend.addClause(literals)
    }

    override fun _solve(): Boolean {
        return backend.solve()
    }

    override fun _solve(assumptions: LitArray): Boolean {
        return backend.solve(assumptions)
    }

    override fun interrupt() {
        backend.interrupt()
    }

    override fun getValue(lit: Lit): Boolean {
        return backend.getValue(lit)
    }

    override fun getModel(): Model {
        return Model.from(backend.getModel(), zerobased = true)
    }
}

private fun main() {
    CryptoMiniSatSolver().useWith {
        testSolverWithAssumptions()
    }
}
