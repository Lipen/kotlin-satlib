@file:Suppress("MemberVisibilityCanBePrivate", "LocalVariableName")

package com.github.lipen.satlib.solver.jna

import com.github.lipen.satlib.core.Context
import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.Model
import com.github.lipen.satlib.core.newContext
import com.github.lipen.satlib.jna.LibGlucose
import com.github.lipen.satlib.jna.glucose_Lit
import com.github.lipen.satlib.jna.glucose_Var
import com.github.lipen.satlib.jna.glucose_addClause
import com.github.lipen.satlib.jna.glucose_lbool
import com.github.lipen.satlib.jna.glucose_limited_solve
import com.github.lipen.satlib.jna.glucose_solve
import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.solver.solve
import com.github.lipen.satlib.utils.useWith
import com.sun.jna.Pointer
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import kotlin.math.absoluteValue

private val logger = KotlinLogging.logger {}

class GlucoseSolver(
    val initialSeed: Double? = null, // internal default is 0
) : Solver {
    val native: LibGlucose = LibGlucose.INSTANCE
    val ptr: LibGlucose.CGlucose = native.glucose_init()

    init {
        if (initialSeed != null) native.glucose_set_random_seed(ptr, initialSeed)
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

        if (ptr.pointer != Pointer.NULL) native.glucose_release(ptr)
        ptr.pointer = native.glucose_init().pointer
        if (ptr.pointer == Pointer.NULL) throw OutOfMemoryError("glucose_init returned NULL")
        if (initialSeed != null) native.glucose_set_random_seed(ptr, initialSeed)
    }

    override fun close() {
        if (ptr.pointer != Pointer.NULL) {
            native.glucose_release(ptr)
            ptr.pointer = Pointer.NULL
        }
    }

    override fun interrupt() {
        native.glucose_interrupt(ptr)
    }

    fun clearInterrupt() {
        native.glucose_clearInterrupt(ptr)
    }

    override fun dumpDimacs(file: File) {
        // TODO: native.glucose_write_dimacs(ptr, file.path)
        TODO()
    }

    override fun comment(comment: String) {
        logger.debug { "c $comment" }
    }

    override fun newLiteral(): Lit {
        val lit = ++numberOfVariables
        val glucoseLit = native.glucose_newLit(ptr)
        check(lit == fromGlucose(glucoseLit))
        return lit
    }

    fun setPolarity(v: Int, b: Boolean) {
        require(v > 0)
        native.glucose_setPolarity(ptr, glucose_Var(v - 1), if (b) 1 else 0)
    }

    fun setDecision(v: Int, b: Boolean) {
        require(v > 0)
        native.glucose_setDecisionVar(ptr, glucose_Var(v - 1), if (b) 1 else 0)
    }

    fun setFrozen(v: Int, b: Boolean) {
        require(v > 0)
        native.glucose_setFrozen(ptr, glucose_Var(v - 1), b)
    }

    fun isEliminated(v: Int): Boolean {
        require(v > 0)
        return native.glucose_isEliminated(ptr, glucose_Var(v - 1))
    }

    fun eliminate(turn_off_elim: Boolean = false): Boolean {
        return native.glucose_eliminate(ptr, turn_off_elim)
    }

    override fun addClause(literals: List<Lit>) {
        ++numberOfClauses
        native.glucose_addClause(ptr, literals.map { toGlucose(it) })
    }

    override fun solve(): Boolean {
        val res = native.glucose_solve(ptr, assumptions.map { toGlucose(it) })
        assumptions.clear()
        return res
    }

    fun solve_limited(): Boolean? {
        val res = native.glucose_limited_solve(ptr, assumptions.map { toGlucose(it) })
        assumptions.clear()
        return when (res) {
            glucose_lbool.True -> true
            glucose_lbool.False -> false
            glucose_lbool.Undef -> null
        }
    }

    fun getAssignedValue(lit: Lit): Boolean? {
        return when (native.glucose_value_Lit(ptr, toGlucose(lit))) {
            glucose_lbool.True -> true
            glucose_lbool.False -> false
            glucose_lbool.Undef -> null
        }
    }

    override fun getValue(lit: Lit): Boolean {
        return when (native.glucose_modelValue_Lit(ptr, toGlucose(lit))) {
            glucose_lbool.True -> true
            glucose_lbool.False -> false
            glucose_lbool.Undef -> false // FIXME?
        }
    }

    override fun getModel(): Model {
        val data = List(numberOfVariables) { getValue(it + 1) }
        return Model.from(data, zerobased = true)
    }
}

// External Lit to internal glucose_Lit
fun toGlucose(lit: Lit): glucose_Lit {
    require(lit != 0)
    val v = lit.absoluteValue - 1
    val sign = if (lit < 0) 1 else 0
    return glucose_Lit((v shl 1) + sign)
}

// Internal glucose_Lit to external Lit
fun fromGlucose(lit: glucose_Lit): Lit {
    require(lit.value >= 0)
    val v = (lit.value shr 1) + 1 // 1-based variable index
    return if (lit.value and 1 == 1) -v else v
}

@Suppress("DuplicatedCode")
fun main() {
    GlucoseSolver().useWith {
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
