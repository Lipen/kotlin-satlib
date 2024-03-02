@file:Suppress("MemberVisibilityCanBePrivate")

package com.github.lipen.satlib.solver.jna

import com.github.lipen.satlib.core.Context
import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.Model
import com.github.lipen.satlib.core.newContext
import com.github.lipen.satlib.jna.LibKissat
import com.github.lipen.satlib.jna.kissat_add_clause
import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.utils.useWith
import com.sun.jna.Pointer
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

class KissatSolver(
    val initialSeed: Int? = null, // internal default is 0
) : Solver {
    val native: LibKissat = LibKissat.INSTANCE
    val ptr: LibKissat.Kissat = native.kissat_init()

    init {
        if (initialSeed != null) native.kissat_set_option(ptr, "seed", initialSeed)
    }

    override var context: Context = newContext()
    override var numberOfVariables: Int = 0
        private set
    override var numberOfClauses: Int = 0
        private set

    override val assumptions: MutableList<Lit> = mutableListOf()

    override fun reset() {
        context = newContext()
        numberOfVariables = 0
        numberOfClauses = 0

        if (ptr.pointer != Pointer.NULL) native.kissat_release(ptr)
        ptr.pointer = native.kissat_init().pointer
        // if (ptr.pointer == Pointer.NULL) throw OutOfMemoryError("kissat_init returned NULL")
        if (initialSeed != null) native.kissat_set_option(ptr, "seed", initialSeed)
    }

    override fun close() {
        if (ptr.pointer != Pointer.NULL) {
            native.kissat_release(ptr)
            ptr.pointer = Pointer.NULL
        }
    }

    override fun interrupt() {
        native.kissat_terminate(ptr)
    }

    override fun dumpDimacs(file: File) {
        // TODO: native.kissat_write_dimacs(ptr, file.path)
        TODO()
    }

    override fun comment(comment: String) {
        logger.debug { "c $comment" }
    }

    override fun newLiteral(): Lit {
        return ++numberOfVariables
    }

    override fun addClause(literals: List<Lit>) {
        ++numberOfClauses
        native.kissat_add_clause(ptr, literals)
    }

    override fun solve(): Boolean {
        if (assumptions.isNotEmpty()) {
            throw UnsupportedOperationException(ASSUMPTIONS_NOT_SUPPORTED)
        }
        return when (val res = native.kissat_solve(ptr)) {
            0 -> false // UNSOLVED
            10 -> true // SATISFIABLE
            20 -> false // UNSATISFIABLE
            else -> error("kissat_solve returned $res")
        }
    }

    override fun getValue(lit: Lit): Boolean {
        return when (val res = native.kissat_value(ptr, lit)) {
            lit -> true
            -lit -> false
            else -> error("kissat_val(lit = $lit) returned $res")
        }
    }

    override fun getModel(): Model {
        val data = List(numberOfVariables) { i -> getValue(i + 1) }
        return Model.from(data, zerobased = true)
    }

    companion object {
        private const val NAME = "KissatSolver"
        private const val ASSUMPTIONS_NOT_SUPPORTED =
            "$NAME does not support solving with assumptions"
    }
}

@Suppress("DuplicatedCode")
private fun main() {
    KissatSolver().useWith {
        val tie = newLiteral()
        val shirt = newLiteral()
        println("tie = $tie")
        println("shirt = $shirt")

        println("Adding clauses...")
        addClause(listOf(-tie, shirt))
        addClause(listOf(tie, shirt))
        addClause(listOf(-tie, -shirt))

        println("Solving...")
        val res1 = solve()
        println("Result: ${if (res1) "SAT" else "UNSAT"}")
        check(res1)
        println("tie = ${getValue(tie)}")
        println("shirt = ${getValue(shirt)}")
        check(!getValue(tie))
        check(getValue(shirt))
    }
    println()
    println("All done!")
}
