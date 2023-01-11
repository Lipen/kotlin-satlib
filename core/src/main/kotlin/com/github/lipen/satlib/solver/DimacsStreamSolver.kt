package com.github.lipen.satlib.solver

import com.github.lipen.satlib.core.Context
import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.Model
import com.github.lipen.satlib.core.newContext
import com.github.lipen.satlib.utils.parseDimacsOutput
import com.github.lipen.satlib.utils.write
import com.github.lipen.satlib.utils.writeln
import mu.KotlinLogging
import okio.Buffer
import okio.BufferedSink
import okio.buffer
import okio.sink
import okio.source
import java.io.File

private val logger = KotlinLogging.logger {}

@Suppress("MemberVisibilityCanBePrivate")
class DimacsStreamSolver(
    val command: () -> String,
) : Solver {
    override var context: Context = newContext()
    override var numberOfVariables: Int = 0
        private set
    override var numberOfClauses: Int = 0
        private set
    override val assumptions: MutableList<Lit> = mutableListOf()

    private val buffer = Buffer()
    private var model: Model? = null

    override fun reset() {
        logger.debug { "reset()" }
        context = newContext()
        numberOfVariables = 0
        numberOfClauses = 0
        assumptions.clear()
        buffer.clear()
        model = null
    }

    override fun close() {
        logger.debug { "close()" }
        buffer.close()
    }

    override fun interrupt() {
        logger.debug { "close()" }
        throw UnsupportedOperationException(INTERRUPTION_NOT_SUPPORTED)
    }

    fun writeDimacs(sink: BufferedSink) {
        sink.writeln("p cnf $numberOfVariables $numberOfClauses")
        buffer.copyTo(sink.buffer)
    }

    override fun dumpDimacs(file: File) {
        logger.debug { "dumpDimacs(file = $file)" }
        file.sink().buffer().use {
            writeDimacs(it)
        }
    }

    override fun comment(comment: String) {
        logger.trace { "// $comment" }
        for (line in comment.lineSequence()) {
            buffer.write("c ").writeln(line)
        }
    }

    override fun newLiteral(): Lit {
        return ++numberOfVariables
    }

    override fun addClause(literals: List<Lit>) {
        logger.trace { "addClause($literals)" }
        ++numberOfClauses
        for (lit in literals) {
            buffer.write(lit.toString()).write(" ")
        }
        buffer.writeln("0")
    }

    override fun solve(): Boolean {
        logger.debug { "solve()" }
        if (assumptions.isNotEmpty()) {
            throw UnsupportedOperationException(ASSUMPTIONS_NOT_SUPPORTED)
        }
        buffer.writeln("c solve")
        val command = command()
        val process = Runtime.getRuntime().exec(command)
        process.outputStream.sink().buffer().use {
            writeDimacs(it)
            // Note: process' stdin must be closed in order to start the solving process
        }
        process.inputStream.source().buffer().use { out ->
            model = parseDimacsOutput(out)
            return model != null
        }
    }

    override fun getValue(lit: Lit): Boolean {
        return getModel()[lit]
    }

    override fun getModel(): Model {
        return model ?: error("Model is null because the solver is not in the SAT state")
    }

    companion object {
        private const val NAME = "DimacsStreamSolver"
        private const val ASSUMPTIONS_NOT_SUPPORTED =
            "$NAME does not support solving with assumptions"
        private const val INTERRUPTION_NOT_SUPPORTED =
            "$NAME does not support interruption"
    }
}
