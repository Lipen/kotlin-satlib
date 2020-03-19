package com.github.lipen.satlib.solver

import com.github.lipen.jnisat.JMiniSat
import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.LitArray
import com.github.lipen.satlib.core.RawAssignment
import com.github.lipen.satlib.core.RawAssignment1
import com.github.lipen.satlib.utils.toList_

@Suppress("MemberVisibilityCanBePrivate")
class MiniSatSolver @JvmOverloads constructor(
    val backend: JMiniSat = JMiniSat()
) : Solver {
    override val numberOfVariables: Int get() = backend.numberOfVariables
    override val numberOfClauses: Int get() = backend.numberOfClauses

    override fun reset() {
        backend.reset()
    }

    override fun close() {
        backend.close()
    }

    override fun newVariable(): Lit {
        return backend.newVariable()
    }

    override fun comment(comment: String) {}

    @Suppress("OverridingDeprecatedMember")
    override fun addClause() {
        @Suppress("deprecation")
        backend.addClause()
    }

    override fun addClause(lit: Lit) {
        backend.addClause(lit)
    }

    override fun addClause(lit1: Lit, lit2: Lit) {
        backend.addClause(lit1, lit2)
    }

    override fun addClause(lit1: Lit, lit2: Lit, lit3: Lit) {
        backend.addClause(lit1, lit2, lit3)
    }

    override fun addClause_(literals: LitArray) {
        backend.addClause_(literals)
    }

    override fun addClause(literals: Iterable<Lit>) {
        addClause_(literals.toList_().toIntArray())
    }

    override fun solve(): Boolean {
        return backend.solve()
    }

    override fun solve(lit: Lit): Boolean {
        return backend.solve(lit)
    }

    override fun solve(lit1: Lit, lit2: Lit): Boolean {
        return backend.solve(lit1, lit2)
    }

    override fun solve(lit1: Lit, lit2: Lit, lit3: Lit): Boolean {
        return backend.solve(lit1, lit2, lit3)
    }

    override fun solve_(assumptions: LitArray): Boolean {
        return backend.solve_(assumptions)
    }

    override fun solve(assumptions: Iterable<Lit>): Boolean {
        return solve_(assumptions.toList_().toIntArray())
    }

    override fun getValue(lit: Lit): Boolean {
        return backend.getValue(lit)
    }

    override fun getModel(): RawAssignment {
        return RawAssignment1(backend.getModel())
    }
}
