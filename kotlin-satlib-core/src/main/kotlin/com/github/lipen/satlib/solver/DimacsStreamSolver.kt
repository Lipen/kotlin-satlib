package com.github.lipen.satlib.solver

import com.github.lipen.satlib.utils.Lit
import com.github.lipen.satlib.utils.LitArray
import com.github.lipen.satlib.utils.Model
import com.github.lipen.satlib.utils.parseDimacsOutput
import com.github.lipen.satlib.utils.useWith
import okio.buffer
import okio.sink
import okio.source

@Suppress("MemberVisibilityCanBePrivate")
class DimacsStreamSolver(
    val command: String,
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
        val process = Runtime.getRuntime().exec(command)
        val processInput = process.outputStream.sink().buffer()
        dumpDimacs(processInput)
        // Note: process' stdin must be closed in order to start the solving process
        processInput.close()
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
        return "${this::class.java.simpleName}(\"$command\")"
    }

    companion object {
        private val ASSUMPTIONS_NOT_SUPPORTED: String =
            "${DimacsStreamSolver::class.java.simpleName} does not support solving with assumptions"
        private val INTERRUPTION_NOT_SUPPORTED: String =
            "${DimacsStreamSolver::class.java.simpleName} does not support interruption"
    }
}

fun main() {
    DimacsStreamSolver("cryptominisat5").useWith {
        testSolverWithoutAssumptions()
    }
}
