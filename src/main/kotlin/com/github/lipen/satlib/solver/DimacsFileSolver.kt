package com.github.lipen.satlib.solver

import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.LitArray
import com.github.lipen.satlib.core.RawAssignment
import com.github.lipen.satlib.core.RawAssignment0
import com.github.lipen.satlib.utils.lineSequence
import com.github.lipen.satlib.utils.useWith
import com.github.lipen.satlib.utils.write
import com.github.lipen.satlib.utils.writeln
import okio.Buffer
import okio.BufferedSource
import okio.buffer
import okio.sink
import okio.source
import java.io.File

@Suppress("MemberVisibilityCanBePrivate")
class DimacsFileSolver(
    val command: String,
    val file: File
) : Solver {
    private val buffer = Buffer()
    private var _model: RawAssignment? = null

    override var numberOfVariables: Int = 0
        private set
    override var numberOfClauses: Int = 0
        private set

    override fun reset() {
        buffer.clear()
        numberOfVariables = 0
        numberOfClauses = 0
    }

    override fun close() {
        buffer.close()
    }

    override fun newVariable(): Lit {
        return ++numberOfVariables
    }

    override fun comment(comment: String) {
        buffer.write("c ").writeln(comment)
    }

    @Suppress("OverridingDeprecatedMember")
    override fun addClause() {
        ++numberOfClauses
        buffer.writeln("0")
    }

    override fun addClause(lit: Lit) {
        ++numberOfClauses
        buffer.writeln("$lit 0")
    }

    override fun addClause(lit1: Lit, lit2: Lit) {
        ++numberOfClauses
        buffer.writeln("$lit1 $lit2 0")
    }

    override fun addClause(lit1: Lit, lit2: Lit, lit3: Lit) {
        ++numberOfClauses
        buffer.writeln("$lit1 $lit2 $lit3 0")
    }

    override fun addClause_(literals: LitArray) {
        addClause(literals.asIterable())
    }

    override fun addClause(literals: Iterable<Lit>) {
        ++numberOfClauses
        for (x in literals)
            buffer.write(x.toString()).write(" ")
        buffer.writeln("0")
    }

    override fun solve(): Boolean {
        file.sink().buffer().use {
            it.writeln("p cnf $numberOfVariables $numberOfClauses")
            buffer.copyTo(it.buffer)
        }

        val process = Runtime.getRuntime().exec(command.format(file))
        val processOutput = process.inputStream.source().buffer()
        _model = parseDimacsOutput(processOutput)
        return _model != null
    }

    override fun solve(lit: Lit): Boolean {
        throw UnsupportedOperationException(ASSUMPTIONS_NOT_SUPPORTED)
    }

    override fun solve(lit1: Lit, lit2: Lit): Boolean {
        throw UnsupportedOperationException(ASSUMPTIONS_NOT_SUPPORTED)
    }

    override fun solve(lit1: Lit, lit2: Lit, lit3: Lit): Boolean {
        throw UnsupportedOperationException(ASSUMPTIONS_NOT_SUPPORTED)
    }

    override fun solve_(assumptions: LitArray): Boolean {
        throw UnsupportedOperationException(ASSUMPTIONS_NOT_SUPPORTED)
    }

    override fun solve(assumptions: Iterable<Lit>): Boolean {
        throw UnsupportedOperationException(ASSUMPTIONS_NOT_SUPPORTED)
    }

    override fun getValue(lit: Lit): Boolean {
        return getModel()[lit]
    }

    override fun getModel(): RawAssignment {
        return _model ?: error("Model is null because the solver is not in the SAT state")
    }

    companion object {
        private val ASSUMPTIONS_NOT_SUPPORTED: String =
            "${DimacsFileSolver::class.java.simpleName} does not support solving with assumptions"
    }
}

private fun parseDimacsOutput(source: BufferedSource): RawAssignment? {
    val answer = source.lineSequence().firstOrNull { it.startsWith("s ") }
        ?: error("No answer from solver")
    return when {
        "UNSAT" in answer -> null
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
                .let { RawAssignment0(it) }
        else -> error("Bad answer (neither SAT nor UNSAT) from solver: '$answer'")
    }
}

fun main() {
    DimacsFileSolver("cryptominisat5 %s", File("cnf")).useWith {
        val x = newVariable()
        val y = newVariable()

        addClause(x)
        addClause(-y)

        check(solve())
        println("model = ${getModel()}")
    }
}
