@file:Suppress("MemberVisibilityCanBePrivate", "LocalVariableName")

package com.github.lipen.satlib.solver.jni

import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.Model
import com.github.lipen.satlib.jni.JMiniSat
import com.github.lipen.satlib.solver.AbstractSolver
import java.io.File

class MiniSatSolver @JvmOverloads constructor(
    val simpStrategy: SimpStrategy = SimpStrategy.ONCE,
    val backend: JMiniSat = JMiniSat(),
) : AbstractSolver() {
    private var simplified = false

    constructor(
        simpStrategy: SimpStrategy = SimpStrategy.ONCE,
        initialSeed: Double? = null,
        initialRandomVarFreq: Double? = null,
        initialRandomPolarities: Boolean = false,
        initialRandomInitialActivities: Boolean = false,
    ) : this(
        simpStrategy = simpStrategy,
        backend = JMiniSat(
            initialSeed = initialSeed,
            initialRandomVarFreq = initialRandomVarFreq,
            initialRandomPolarities = initialRandomPolarities,
            initialRandomInitialActivities = initialRandomInitialActivities
        )
    )

    init {
        if (simpStrategy == SimpStrategy.NEVER) {
            backend.eliminate(turn_off_elim = true)
        }
    }

    override fun _reset() {
        backend.reset()
        if (simpStrategy == SimpStrategy.NEVER) {
            backend.eliminate(turn_off_elim = true)
        }
        simplified = false
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

    override fun _newLiteral(outer: Int): Lit {
        return backend.newVariable()
    }

    override fun _addClause(literals: List<Lit>) {
        backend.addClause_(literals.toIntArray())
    }

    private fun <T> runMatchingSimpStrategy(block: (do_simp: Boolean, turn_off_simp: Boolean) -> T): T {
        return when (simpStrategy) {
            SimpStrategy.ONCE -> block(!simplified, !simplified).also { simplified = true }
            SimpStrategy.ALWAYS -> block(true, false)
            SimpStrategy.NEVER -> block(false, false)
        }
    }

    override fun _solve(): Boolean {
        return runMatchingSimpStrategy { do_simp, turn_off_simp ->
            if (assumptions.isEmpty()) {
                backend.solve(do_simp, turn_off_simp)
            } else {
                backend.solve(assumptions.toIntArray(), do_simp, turn_off_simp)
            }
        }
    }

    override fun getValue(lit: Lit): Boolean {
        return backend.getValue(lit)
    }

    override fun getModel(): Model {
        return Model.from(backend.getModel(), zerobased = false)
    }

    companion object {
        enum class SimpStrategy {
            NEVER, ONCE, ALWAYS;
        }
    }
}
