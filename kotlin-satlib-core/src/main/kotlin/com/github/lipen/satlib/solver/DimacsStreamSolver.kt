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
    val command: () -> String,
) : AbstractSolver() {
    private var _model: Model? = null

    override fun _reset() {}

    override fun _close() {}

    override fun _comment(comment: String) {}

    override fun _newLiteral(outerNumberOfVariables: Int): Lit {
        return outerNumberOfVariables
    }

    override fun _addClause() {}

    override fun _addClause(lit: Lit) {}

    override fun _addClause(lit1: Lit, lit2: Lit) {}

    override fun _addClause(lit1: Lit, lit2: Lit, lit3: Lit) {}

    override fun _addClause(literals: LitArray) {
        _addClause(literals.asList())
    }

    override fun _addClause(literals: List<Lit>) {}

    override fun _solve(): Boolean {
        val command = command()
        val process = Runtime.getRuntime().exec(command)
        val processInput = process.outputStream.sink().buffer()
        dumpDimacs(processInput)
        // Note: process' stdin must be closed in order to start the solving process
        processInput.close()
        val processOutput = process.inputStream.source().buffer()
        _model = parseDimacsOutput(processOutput)
        return _model != null
    }

    override fun _solve(assumptions: LitArray): Boolean {
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

    companion object {
        private const val ASSUMPTIONS_NOT_SUPPORTED: String =
            "DimacsStreamSolver does not support solving with assumptions"
        private const val INTERRUPTION_NOT_SUPPORTED: String =
            "DimacsStreamSolver does not support interruption"
    }
}

private fun main() {
    DimacsStreamSolver { "cryptominisat5" }.useWith {
        testSolverWithoutAssumptions()
    }
}
