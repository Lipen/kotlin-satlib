package com.github.lipen.satlib.solver

import com.github.lipen.satlib.solver.jni.JMiniSat
import com.github.lipen.satlib.utils.Lit
import com.github.lipen.satlib.utils.LitArray
import com.github.lipen.satlib.utils.Model
import com.github.lipen.satlib.utils.Model0
import com.github.lipen.satlib.utils.useWith

@Suppress("MemberVisibilityCanBePrivate", "FunctionName")
class MiniSatSolver @JvmOverloads constructor(
    val simpStrategy: SimpStrategy = SimpStrategy.ONCE,
    val backend: JMiniSat = JMiniSat(),
) : AbstractSolver() {
    private var simplified = false

    init {
        reset_()
    }

    private fun reset_() {
        backend.reset()
        simplified = false
        if (simpStrategy == SimpStrategy.NEVER) {
            backend.eliminate(turn_off_elim = true)
        }
    }

    override fun _reset() {
        reset_()
    }

    override fun _close() {
        backend.close()
    }

    override fun _newLiteral(outerNumberOfVariables: Int): Lit {
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

    override fun _addClause(lit1: Lit, lit2: Lit, lit3: Lit) {
        backend.addClause(lit1, lit2, lit3)
    }

    override fun _addClause(literals: LitArray) {
        backend.addClause_(literals)
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
            backend.solve(do_simp, turn_off_simp)
        }
    }

    override fun _solve(assumptions: LitArray): Boolean {
        return runMatchingSimpStrategy { do_simp, turn_off_simp ->
            backend.solve(assumptions, do_simp, turn_off_simp)
        }
    }

    override fun interrupt() {
        backend.interrupt()
    }

    override fun getValue(lit: Lit): Boolean {
        return backend.getValue(lit)
    }

    override fun getModel(): Model {
        return Model0(backend.getModel())
    }

    companion object {
        enum class SimpStrategy {
            NEVER, ONCE, ALWAYS;
        }
    }
}

private fun main() {
    MiniSatSolver().useWith {
        testSolverWithAssumptions()
    }
}
