@file:Suppress("MemberVisibilityCanBePrivate", "LocalVariableName")

package com.github.lipen.satlib.solver.jna

import com.github.lipen.satlib.core.Context
import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.Model
import com.github.lipen.satlib.core.newContext
import com.github.lipen.satlib.jna.LibMiniSat
import com.github.lipen.satlib.jna.minisat_Lit
import com.github.lipen.satlib.jna.minisat_Var
import com.github.lipen.satlib.jna.minisat_addClause
import com.github.lipen.satlib.jna.minisat_lbool
import com.github.lipen.satlib.jna.minisat_limited_solve
import com.github.lipen.satlib.jna.minisat_solve
import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.solver.solve
import com.github.lipen.satlib.utils.useWith
import com.sun.jna.Pointer
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import kotlin.math.absoluteValue

private val logger = KotlinLogging.logger {}

class MiniSatSolver(
    val initialSeed: Double? = null, // internal default is 0
) : Solver {
    val native: LibMiniSat = LibMiniSat.INSTANCE
    val ptr: LibMiniSat.CMiniSat = native.minisat_init()

    init {
        if (initialSeed != null) native.minisat_set_random_seed(ptr, initialSeed)
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
        assumptions.clear()

        if (ptr.pointer != Pointer.NULL) native.minisat_release(ptr)
        ptr.pointer = native.minisat_init().pointer
        if (ptr.pointer == Pointer.NULL) throw OutOfMemoryError("minisat_init returned NULL")
        if (initialSeed != null) native.minisat_set_random_seed(ptr, initialSeed)
    }

    override fun close() {
        if (ptr.pointer != Pointer.NULL) {
            native.minisat_release(ptr)
            ptr.pointer = Pointer.NULL
        }
    }

    override fun interrupt() {
        native.minisat_interrupt(ptr)
    }

    fun clearInterrupt() {
        native.minisat_clearInterrupt(ptr)
    }

    override fun dumpDimacs(file: File) {
        // TODO: native.minisat_write_dimacs(ptr, file.path)
        TODO()
    }

    override fun comment(comment: String) {
        logger.debug { "c $comment" }
    }

    override fun newLiteral(): Lit {
        val lit = ++numberOfVariables
        val minisatLit = native.minisat_newLit(ptr)
        check(lit == fromMinisat(minisatLit))
        return lit
    }

    fun setPolarity(v: Int, b: Boolean) {
        require(v > 0)
        native.minisat_setPolarity(ptr, minisat_Var(v - 1), if (b) 1 else 0)
    }

    fun setDecision(v: Int, b: Boolean) {
        require(v > 0)
        native.minisat_setDecisionVar(ptr, minisat_Var(v - 1), if (b) 1 else 0)
    }

    fun setFrozen(v: Int, b: Boolean) {
        require(v > 0)
        native.minisat_setFrozen(ptr, minisat_Var(v - 1), b)
    }

    fun isEliminated(v: Int): Boolean {
        require(v > 0)
        return native.minisat_isEliminated(ptr, minisat_Var(v - 1))
    }

    fun eliminate(turn_off_elim: Boolean = false): Boolean {
        return native.minisat_eliminate(ptr, turn_off_elim)
    }

    override fun addClause(literals: List<Lit>) {
        ++numberOfClauses
        native.minisat_addClause(ptr, literals.map { toMinisat(it) })
    }

    override fun solve(): Boolean {
        val res = native.minisat_solve(ptr, assumptions.map { toMinisat(it) })
        assumptions.clear()
        return res
    }

    fun solve_limited(): Boolean? {
        val res = native.minisat_limited_solve(ptr, assumptions.map { toMinisat(it) })
        assumptions.clear()
        return when (res) {
            minisat_lbool.True -> true
            minisat_lbool.False -> false
            minisat_lbool.Undef -> null
        }
    }

    fun getAssignedValue(lit: Lit): Boolean? {
        return when (native.minisat_value_Lit(ptr, toMinisat(lit))) {
            minisat_lbool.True -> true
            minisat_lbool.False -> false
            minisat_lbool.Undef -> null
        }
    }

    override fun getValue(lit: Lit): Boolean {
        return when (native.minisat_modelValue_Lit(ptr, toMinisat(lit))) {
            minisat_lbool.True -> true
            minisat_lbool.False -> false
            minisat_lbool.Undef -> false // FIXME?
        }
    }

    override fun getModel(): Model {
        val data = List(numberOfVariables) { getValue(it + 1) }
        return Model.from(data, zerobased = true)
    }
}

// External Lit to internal minisat_Lit
fun toMinisat(lit: Lit): minisat_Lit {
    require(lit != 0)
    val v = lit.absoluteValue - 1
    val sign = if (lit < 0) 1 else 0
    return minisat_Lit((v shl 1) + sign)
}

// Internal minisat_Lit to external Lit
fun fromMinisat(lit: minisat_Lit): Lit {
    require(lit.value >= 0)
    val v = (lit.value shr 1) + 1 // 1-based variable index
    return if (lit.value and 1 == 1) -v else v
}

@Suppress("DuplicatedCode")
private fun main() {
    MiniSatSolver().useWith {
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

        println()
        println("Solving with assumption SHIRT=false...")
        val res3 = solve(listOf(-shirt))
        println("Result: ${if (res3) "SAT" else "UNSAT"}")
        check(!res3)
    }
    println()
    println("All done!")
}
