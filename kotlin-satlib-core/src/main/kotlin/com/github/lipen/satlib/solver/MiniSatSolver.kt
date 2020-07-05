package com.github.lipen.satlib.solver

import com.github.lipen.satlib.op.exactlyOne
import com.github.lipen.satlib.solver.jni.JMiniSat
import com.github.lipen.satlib.utils.Lit
import com.github.lipen.satlib.utils.LitArray
import com.github.lipen.satlib.utils.RawAssignment
import com.github.lipen.satlib.utils.RawAssignment1
import com.github.lipen.satlib.utils.useWith

@Suppress("MemberVisibilityCanBePrivate", "FunctionName")
class MiniSatSolver @JvmOverloads constructor(
    val simpStrategy: SimpStrategy = SimpStrategy.ONCE,
    val backend: JMiniSat = JMiniSat()
) : AbstractSolver() {
    private var simplified = false

    override val numberOfVariables: Int get() = backend.numberOfVariables
    override val numberOfClauses: Int get() = backend.numberOfClauses

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

    override fun _addClause(lit1: Lit, lit2: Lit, lit3: Lit) {
        backend.addClause(lit1, lit2, lit3)
    }

    override fun _addClause(literals: LitArray) {
        backend.addClause_(literals)
    }

    override fun _addClause(literals: List<Lit>) {
        _addClause(literals.toIntArray())
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

    override fun _solve(lit: Lit): Boolean {
        return runMatchingSimpStrategy { do_simp, turn_off_simp ->
            backend.solve(lit, do_simp, turn_off_simp)
        }
    }

    override fun _solve(lit1: Lit, lit2: Lit): Boolean {
        return runMatchingSimpStrategy { do_simp, turn_off_simp ->
            backend.solve(lit1, lit2, do_simp, turn_off_simp)
        }
    }

    override fun _solve(lit1: Lit, lit2: Lit, lit3: Lit): Boolean {
        return runMatchingSimpStrategy { do_simp, turn_off_simp ->
            backend.solve(lit1, lit2, lit3, do_simp, turn_off_simp)
        }
    }

    override fun _solve(assumptions: LitArray): Boolean {
        return runMatchingSimpStrategy { do_simp, turn_off_simp ->
            backend.solve_(assumptions, do_simp, turn_off_simp)
        }
    }

    override fun _solve(assumptions: List<Lit>): Boolean {
        return _solve(assumptions.toIntArray())
    }

    override fun interrupt() {
        backend.interrupt()
    }

    override fun getValue(lit: Lit): Boolean {
        return backend.getValue(lit)
    }

    override fun getModel(): RawAssignment {
        return RawAssignment1(backend.getModel())
    }

    companion object {
        enum class SimpStrategy {
            NEVER, ONCE, ALWAYS;
        }
    }
}

fun main() {
    MiniSatSolver().useWith {
        val x = newLiteral()
        val y = newLiteral()
        val z = newLiteral()

        println("Encoding exactlyOne(x, y, z)")
        exactlyOne(x, y, z)

        println("nVars = $numberOfVariables")
        println("nClauses = $numberOfClauses")

        check(solve())
        println("model = ${getModel()}")

        println("Solving with assumptions...")
        check(solve(x)); println("model = ${getModel()}"); check(getValue(x))
        check(solve(y)); println("model = ${getModel()}"); check(getValue(y))
        check(solve(z)); println("model = ${getModel()}"); check(getValue(z))
        println("Solving with assumptions: OK")
    }
}
