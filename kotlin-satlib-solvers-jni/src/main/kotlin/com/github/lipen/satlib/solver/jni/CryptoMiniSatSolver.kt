@file:Suppress("MemberVisibilityCanBePrivate")

package com.github.lipen.satlib.solver.jni

import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.Model
import com.github.lipen.satlib.jni.JCryptoMiniSat
import com.github.lipen.satlib.solver.AbstractSolver
import java.io.File

class CryptoMiniSatSolver @JvmOverloads constructor(
    val backend: JCryptoMiniSat = JCryptoMiniSat(),
) : AbstractSolver() {
    constructor(numberOfThreads: Int) : this(backend = JCryptoMiniSat(numberOfThreads))

    override fun _reset() {
        backend.reset()
    }

    override fun _close() {
        backend.close()
    }

    override fun _interrupt() {
        backend.interrupt()
    }

    override fun _dumpDimacs(file: File) {
        backend.writeDimacs(file)
    }

    override fun _comment(comment: String) {}

    override fun _newLiteral(outer: Lit): Lit {
        backend.newVariable()
        return outer
    }

    override fun _addClause(literals: List<Lit>) {
        backend.addClause(literals.toIntArray())
    }

    override fun _solve(): Boolean {
        return if (assumptions.isEmpty()) {
            backend.solve()
        } else {
            backend.solve(assumptions.toIntArray())
        }
    }

    override fun getValue(lit: Lit): Boolean {
        return backend.getValue(lit)
    }

    override fun getModel(): Model {
        return Model.from(backend.getModel(), zerobased = true)
    }
}
