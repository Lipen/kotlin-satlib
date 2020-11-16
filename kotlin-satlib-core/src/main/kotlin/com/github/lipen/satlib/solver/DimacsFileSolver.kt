package com.github.lipen.satlib.solver

import com.github.lipen.satlib.op.exactlyOne
import com.github.lipen.satlib.utils.Lit
import com.github.lipen.satlib.utils.LitArray
import com.github.lipen.satlib.utils.Model
import com.github.lipen.satlib.utils.Model0
import com.github.lipen.satlib.utils.lineSequence
import com.github.lipen.satlib.utils.useWith
import okio.BufferedSource
import okio.buffer
import okio.source
import java.io.File

@Suppress("MemberVisibilityCanBePrivate")
class DimacsFileSolver @JvmOverloads constructor(
    val command: String,
    val file: File = createTempFile(),
) : AbstractSolver() {
    private var _model: Model? = null

    override var numberOfVariables: Int = 0
        private set
    override var numberOfClauses: Int = 0
        private set

    override fun _reset() {
        numberOfVariables = 0
        numberOfClauses = 0
    }

    override fun _close() {}

    override fun newLiteral(): Lit {
        return ++numberOfVariables
    }

    override fun _comment(comment: String) {}

    @Suppress("OverridingDeprecatedMember")
    override fun _addClause() {
        ++numberOfClauses
    }

    override fun _addClause(lit: Lit) {
        ++numberOfClauses
    }

    override fun _addClause(lit1: Lit, lit2: Lit) {
        ++numberOfClauses
    }

    override fun _addClause(lit1: Lit, lit2: Lit, lit3: Lit) {
        ++numberOfClauses
    }

    override fun _addClause(literals: LitArray) {
        _addClause(literals.toList())
    }

    override fun _addClause(literals: List<Lit>) {
        ++numberOfClauses
    }

    override fun _solve(): Boolean {
        dumpDimacs(file)
        val process = Runtime.getRuntime().exec(command.format(file))
        val processOutput = process.inputStream.source().buffer()
        _model = parseDimacsOutput(processOutput)
        return _model != null
    }

    override fun _solve(lit: Lit): Boolean {
        throw UnsupportedOperationException(ASSUMPTIONS_NOT_SUPPORTED)
    }

    override fun _solve(lit1: Lit, lit2: Lit): Boolean {
        throw UnsupportedOperationException(ASSUMPTIONS_NOT_SUPPORTED)
    }

    override fun _solve(lit1: Lit, lit2: Lit, lit3: Lit): Boolean {
        throw UnsupportedOperationException(ASSUMPTIONS_NOT_SUPPORTED)
    }

    override fun _solve(assumptions: LitArray): Boolean {
        throw UnsupportedOperationException(ASSUMPTIONS_NOT_SUPPORTED)
    }

    override fun _solve(assumptions: List<Lit>): Boolean {
        throw UnsupportedOperationException(ASSUMPTIONS_NOT_SUPPORTED)
    }

    override fun interrupt() {
        throw UnsupportedOperationException(INTERRUPTION_NOT_SUPPORTED)
    }

    override fun getValue(lit: Lit): Boolean {
        return getModel()[lit]
    }

    override fun getModel(): Model {
        return _model ?: error("Model is null because the solver is not in the SAT state")
    }

    override fun toString(): String {
        return "${this::class.java.simpleName}(\"$command\", \"$file\")"
    }

    companion object {
        private val ASSUMPTIONS_NOT_SUPPORTED: String =
            "${DimacsFileSolver::class.java.simpleName} does not support solving with assumptions"
        private val INTERRUPTION_NOT_SUPPORTED: String =
            "${DimacsFileSolver::class.java.simpleName} does not support interruption"
    }
}

private fun parseDimacsOutput(source: BufferedSource): Model? {
    val answer = source.lineSequence().firstOrNull { it.startsWith("s ") }
        ?: error("No answer from solver")
    return when {
        "UNSAT" in answer -> null
        "INDETERMINATE" in answer -> null
        "SAT" in answer ->
            source
                .lineSequence()
                .filter { it.startsWith("v ") }
                .flatMap { it.drop(2).trim().splitToSequence(' ') }
                .map { it.toInt() }
                .takeWhile { it != 0 }
                .map { it > 0 }
                .toList()
                .toBooleanArray()
                .also { check(it.isNotEmpty()) { "Model is empty" } }
                .let { Model0(it) }
        else -> error("Bad answer (neither SAT nor UNSAT) from solver: '$answer'")
    }
}

fun main() {
    DimacsFileSolver("cryptominisat5 %s", File("cnf")).useWith {
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
