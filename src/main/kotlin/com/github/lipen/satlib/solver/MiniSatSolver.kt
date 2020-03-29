package com.github.lipen.satlib.solver

import com.github.lipen.jnisat.JMiniSat
import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.LitArray
import com.github.lipen.satlib.core.RawAssignment
import com.github.lipen.satlib.core.RawAssignment1
import com.github.lipen.satlib.utils.useWith
import com.github.lipen.satlib.utils.write
import com.github.lipen.satlib.utils.writeln
import okio.Buffer
import okio.buffer
import okio.sink
import java.io.File

@Suppress("MemberVisibilityCanBePrivate", "FunctionName")
class MiniSatSolver @JvmOverloads constructor(
    val simpStrategy: SimpStrategy = SimpStrategy.ONCE,
    val backend: JMiniSat = JMiniSat()
) : Solver {
    private val buffer = Buffer()
    private var simplified = false
    override val numberOfVariables: Int get() = backend.numberOfVariables
    override val numberOfClauses: Int get() = backend.numberOfClauses

    init {
        reset_()
    }

    private fun reset_() {
        backend.reset()
        buffer.clear()
        simplified = false
        if (simpStrategy == SimpStrategy.NEVER) {
            backend.eliminate(turn_off_elim = true)
        }
    }

    override fun reset() {
        reset_()
    }

    override fun close() {
        backend.close()
    }

    override fun newVariable(): Lit {
        return backend.newVariable(frozen = false)
    }

    override fun comment(comment: String) {
        for (line in comment.lineSequence())
            buffer.write("c ").writeln(line)
    }

    @Suppress("OverridingDeprecatedMember")
    override fun addClause() {
        buffer.writeln("0")
        @Suppress("deprecation")
        backend.addClause()
    }

    override fun addClause(lit: Lit) {
        buffer.writeln("$lit 0")
        backend.addClause(lit)
    }

    override fun addClause(lit1: Lit, lit2: Lit) {
        buffer.writeln("$lit1 $lit2 0")
        backend.addClause(lit1, lit2)
    }

    override fun addClause(lit1: Lit, lit2: Lit, lit3: Lit) {
        buffer.writeln("$lit1 $lit2 $lit3 0")
        backend.addClause(lit1, lit2, lit3)
    }

    override fun addClause_(literals: LitArray) {
        for (lit in literals)
            buffer.write(lit.toString()).write(" ")
        buffer.writeln("0")
        backend.addClause_(literals)
    }

    override fun addClause(literals: List<Lit>) {
        addClause_(literals.toIntArray())
    }

    private fun <T> runMatchingSimpStrategy(block: (do_simp: Boolean, turn_off_simp: Boolean) -> T): T {
        return when (simpStrategy) {
            SimpStrategy.ONCE -> block(!simplified, !simplified).also { simplified = true }
            SimpStrategy.ALWAYS -> block(true, false)
            SimpStrategy.NEVER -> block(false, false)
        }
    }

    override fun solve(): Boolean {
        return runMatchingSimpStrategy { do_simp, turn_off_simp ->
            backend.solve(do_simp, turn_off_simp)
        }
    }

    override fun solve(lit: Lit): Boolean {
        return runMatchingSimpStrategy { do_simp, turn_off_simp ->
            backend.solve(lit, do_simp, turn_off_simp)
        }
    }

    override fun solve(lit1: Lit, lit2: Lit): Boolean {
        return runMatchingSimpStrategy { do_simp, turn_off_simp ->
            backend.solve(lit1, lit2, do_simp, turn_off_simp)
        }
    }

    override fun solve(lit1: Lit, lit2: Lit, lit3: Lit): Boolean {
        return runMatchingSimpStrategy { do_simp, turn_off_simp ->
            backend.solve(lit1, lit2, lit3, do_simp, turn_off_simp)
        }
    }

    override fun solve_(assumptions: LitArray): Boolean {
        return runMatchingSimpStrategy { do_simp, turn_off_simp ->
            backend.solve_(assumptions, do_simp, turn_off_simp)
        }
    }

    override fun solve(assumptions: List<Lit>): Boolean {
        return solve_(assumptions.toIntArray())
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

    fun dumpDimacs(file: File) {
        println("Dumping cnf to <$file>...")
        file.sink().buffer().use {
            it.writeln("p cnf $numberOfVariables $numberOfClauses")
            buffer.copyTo(it.buffer)
        }
    }

    companion object {
        enum class SimpStrategy {
            NEVER, ONCE, ALWAYS;
        }
    }
}

fun main() {
    MiniSatSolver().useWith {
        val x = newVariable()
        val y = newVariable()

        addClause(x)
        addClause(-y)

        check(solve())
        println("model = ${getModel()}")
    }
}
