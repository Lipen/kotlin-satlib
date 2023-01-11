@file:Suppress("MemberVisibilityCanBePrivate")

package com.github.lipen.satlib.jni.solver

import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.Model
import com.github.lipen.satlib.jni.JCadical
import com.github.lipen.satlib.solver.AbstractSolver
import java.io.File

class CadicalSolver @JvmOverloads constructor(
    val backend: JCadical = JCadical(),
) : AbstractSolver() {
    constructor(initialSeed: Int?) : this(backend = JCadical(initialSeed))

    override fun _reset() {
        backend.reset()
    }

    override fun _close() {
        backend.close()
    }

    override fun _interrupt() {
        backend.terminate()
    }

    override fun _dumpDimacs(file: File) {
        backend.writeDimacs(file)
    }

    override fun _comment(comment: String) {}

    override fun _newLiteral(outer: Lit): Lit {
        return outer
    }

    override fun _addClause(literals: List<Lit>) {
        backend.addClause(literals.toIntArray())
    }

    override fun _solve(): Boolean {
        return backend.solve()
    }

    override fun getValue(lit: Lit): Boolean {
        return backend.getValue(lit)
    }

    override fun getModel(): Model {
        return Model.from(backend.getModel(), zerobased = false)
    }
}
