package com.github.lipen.satlib.solver

import com.github.lipen.satlib.core.Context
import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.Model
import com.github.lipen.satlib.core.newContext
import com.github.lipen.satlib.jna.LibCadical
import com.github.lipen.satlib.jna.ccadical_add_clause
import com.github.lipen.satlib.jna.ccadical_solve
import com.github.lipen.satlib.utils.useWith
import com.sun.jna.Pointer
import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

@Suppress("MemberVisibilityCanBePrivate")
class CadicalSolver(
    val initialSeed: Int? = null, // internal default is 0
) : Solver2 {
    val native: LibCadical = LibCadical.INSTANCE
    val ptr: LibCadical.CCadical = native.ccadical_init()

    init {
        if (initialSeed != null) native.ccadical_set_option(ptr, "seed", initialSeed)
    }

    override var context: Context = newContext()

    override var numberOfVariables: Int = 0
        private set
    override var numberOfClauses: Int = 0
        private set

    override val assumptions: MutableList<Lit> = mutableListOf()

    override fun close() {
        if (ptr.pointer != Pointer.NULL) {
            native.ccadical_release(ptr)
            ptr.pointer = Pointer.NULL
        }
    }

    override fun reset() {
        context = newContext()
        numberOfVariables = 0
        numberOfClauses = 0
        assumptions.clear()

        if (ptr.pointer != Pointer.NULL) native.ccadical_release(ptr)
        ptr.pointer = native.ccadical_init().pointer
        // if (ptr.pointer == Pointer.NULL) throw OutOfMemoryError("ccadical_init returned NULL")
        if (initialSeed != null) native.ccadical_set_option(ptr, "seed", initialSeed)
    }

    override fun interrupt() {
        native.ccadical_terminate(ptr)
    }

    override fun dumpDimacs(file: File) {
        native.ccadical_write_dimacs(ptr, file.path)
    }

    override fun comment(comment: String) {
        logger.debug { "c $comment" }
    }

    override fun newLiteral(): Lit {
        return ++numberOfVariables
    }

    override fun addClause(literals: List<Lit>) {
        ++numberOfClauses
        native.ccadical_add_clause(ptr, literals)
    }

    override fun solve(): Boolean {
        val res = native.ccadical_solve(ptr, assumptions)
        assumptions.clear()
        return when (res) {
            0 -> false // UNSOLVED
            10 -> true // SATISFIABLE
            20 -> false // UNSATISFIABLE
            else -> error("ccadical_solve returned $res")
        }
    }

    override fun getValue(lit: Lit): Boolean {
        return when (val res = native.ccadical_val(ptr, lit)) {
            lit -> true
            -lit -> false
            else -> error("ccadical_val(lit = $lit) returned $res")
        }
    }

    override fun getModel(): Model {
        val data = List(numberOfVariables) { i -> getValue(i + 1) }
        return Model.from(data, false)
    }
}

@Suppress("DuplicatedCode")
fun main() {
    CadicalSolver().useWith {
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

        println()
        println("Solving with assumption TIE=true...")
        val res2 = solve(listOf(tie))
        println("Result: ${if (res2) "SAT" else "UNSAT"}")
        check(!res2)
        println("tie failed: ${native.ccadical_failed(ptr, tie)}")
        println("shirt failed: ${native.ccadical_failed(ptr, shirt)}")
        check(native.ccadical_failed(ptr, tie))
        check(!native.ccadical_failed(ptr, shirt))

        println()
        println("Solving with assumption SHIRT=false...")
        val res3 = solve(listOf(-shirt))
        println("Result: ${if (res3) "SAT" else "UNSAT"}")
        check(!res3)
        println("tie failed: ${native.ccadical_failed(ptr, tie)}")
        println("-shirt failed: ${native.ccadical_failed(ptr, -shirt)}")
        check(!native.ccadical_failed(ptr, tie))
        check(native.ccadical_failed(ptr, -shirt))
    }
    println()
    println("All done!")
}
