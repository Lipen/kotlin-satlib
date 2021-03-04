package com.github.lipen.satlib.solver

import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.LitArray
import com.github.lipen.satlib.core.Model
import com.github.lipen.satlib.utils.parseDimacsOutput
import com.github.lipen.satlib.utils.write
import com.github.lipen.satlib.utils.writeln
import okio.Buffer
import okio.BufferedSink
import okio.buffer
import okio.sink
import okio.source
import java.io.File

@Suppress("MemberVisibilityCanBePrivate")
class DimacsStreamSolver(
    val command: () -> String,
) : AbstractSolver() {
    private val buffer = Buffer()
    private var _model: Model? = null

    override fun _reset() {
        buffer.clear()
        _model = null
    }

    override fun _close() {
        buffer.close()
    }

    fun writeDimacs(sink: BufferedSink) {
        sink.writeln("p cnf $numberOfVariables $numberOfClauses")
        buffer.copyTo(sink.buffer)
    }

    override fun _dumpDimacs(file: File) {
        file.sink().buffer().use {
            writeDimacs(it)
        }
    }

    override fun _comment(comment: String) {
        for (line in comment.lineSequence()) {
            buffer.write("c ").writeln(line)
        }
    }

    override fun _newLiteral(outerNumberOfVariables: Int): Lit {
        return outerNumberOfVariables
    }

    override fun _addClause() {}

    override fun _addClause(lit: Lit) {
        buffer.writeln("$lit 0")
    }

    override fun _addClause(lit1: Lit, lit2: Lit) {
        buffer.writeln("$lit1 $lit2 0")
    }

    override fun _addClause(lit1: Lit, lit2: Lit, lit3: Lit) {
        buffer.writeln("$lit1 $lit2 $lit3 0")
    }

    override fun _addClause(literals: LitArray) {
        _addClause(literals.asList())
    }

    override fun _addClause(literals: List<Lit>) {
        for (lit in literals) {
            buffer.write(lit.toString()).write(" ")
        }
        buffer.writeln("0")
    }

    override fun _solve(): Boolean {
        buffer.writeln("c solve")
        val command = command()
        val process = Runtime.getRuntime().exec(command)
        process.outputStream.sink().buffer().use {
            writeDimacs(it)
            // Note: process' stdin must be closed in order to start the solving process
        }
        process.inputStream.source().buffer().use { out ->
            _model = parseDimacsOutput(out)
            return _model != null
        }
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
        private const val NAME = "DimacsStreamSolver"
        private const val ASSUMPTIONS_NOT_SUPPORTED =
            "$NAME does not support solving with assumptions"
        private const val INTERRUPTION_NOT_SUPPORTED =
            "$NAME does not support interruption"
    }
}
