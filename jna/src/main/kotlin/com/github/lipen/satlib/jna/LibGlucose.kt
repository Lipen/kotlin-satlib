@file:Suppress("FunctionName", "LocalVariableName")

package com.github.lipen.satlib.jna

import com.sun.jna.Library
import com.sun.jna.Pointer
import com.sun.jna.PointerType

private typealias glucose_Var = Int
private typealias glucose_Lit = Int
private typealias glucose_lbool = Int

interface LibGlucose : Library {
    fun glucose_signature(): String
    fun glucose_init(): CGlucose
    fun glucose_release(ptr: CGlucose)

    fun glucose_newVar(ptr: CGlucose): glucose_Var
    fun glucose_newLit(ptr: CGlucose): glucose_Lit

    fun glucose_mkLit(x: glucose_Var): glucose_Lit
    fun glucose_mkLit_args(x: glucose_Var, sign: Int): glucose_Lit
    fun glucose_negate(p: glucose_Lit): glucose_Lit

    fun glucose_var(p: glucose_Lit): glucose_Var
    fun glucose_sign(p: glucose_Lit): Boolean

    fun glucose_addClause(ptr: CGlucose, len: Int, ps: Pointer /* Lit* */): Boolean
    fun glucose_addClause_begin(ptr: CGlucose)
    fun glucose_addClause_addLit(ptr: CGlucose, p: glucose_Lit)
    fun glucose_addClause_commit(ptr: CGlucose): Boolean

    fun glucose_simplify(ptr: CGlucose): Boolean

    fun glucose_solve(ptr: CGlucose, len: Int, ps: Pointer /* Lit* */): Boolean
    fun glucose_limited_solve(ptr: CGlucose, len: Int, ps: Pointer /* Lit* */): glucose_lbool
    fun glucose_solve_begin(ptr: CGlucose)
    fun glucose_solve_addLit(ptr: CGlucose, p: glucose_Lit)
    fun glucose_solve_commit(ptr: CGlucose): Boolean
    fun glucose_limited_solve_commit(ptr: CGlucose): glucose_lbool

    fun glucose_okay(ptr: CGlucose): Boolean

    fun glucose_setPolarity(ptr: CGlucose, v: glucose_Var, b: Int)
    fun glucose_setDecisionVar(ptr: CGlucose, v: glucose_Var, b: Int)

    fun glucose_get_l_True(): glucose_lbool
    fun glucose_get_l_False(): glucose_lbool
    fun glucose_get_l_Undef(): glucose_lbool

    fun glucose_value_Var(ptr: CGlucose, x: glucose_Var): glucose_lbool
    fun glucose_value_Lit(ptr: CGlucose, p: glucose_Lit): glucose_lbool
    fun glucose_modelValue_Var(ptr: CGlucose, x: glucose_Var): glucose_lbool
    fun glucose_modelValue_Lit(ptr: CGlucose, p: glucose_Lit): glucose_lbool

    fun glucose_num_assigns(ptr: CGlucose): Int
    fun glucose_num_clauses(ptr: CGlucose): Int
    fun glucose_num_learnts(ptr: CGlucose): Int
    fun glucose_num_vars(ptr: CGlucose): Int
    fun glucose_num_freeVars(ptr: CGlucose): Int

    fun glucose_conflict_len(ptr: CGlucose): Int
    fun glucose_conflict_nthLit(ptr: CGlucose, i: Int): glucose_Lit

    fun glucose_set_conf_budget(ptr: CGlucose, x: Int)
    fun glucose_set_prop_budget(ptr: CGlucose, x: Int)
    fun glucose_no_budget(ptr: CGlucose)

    fun glucose_interrupt(ptr: CGlucose)
    fun glucose_clearInterrupt(ptr: CGlucose)

    fun glucose_setFrozen(ptr: CGlucose, v: glucose_Var, b: Boolean)
    fun glucose_isEliminated(ptr: CGlucose, v: glucose_Var): Boolean
    fun glucose_eliminate(ptr: CGlucose, turn_off_elim: Boolean): Boolean

    fun glucose_set_verbosity(ptr: CGlucose, v: Int)

    fun glucose_num_conflicts(ptr: CGlucose): Long
    fun glucose_num_decisions(ptr: CGlucose): Long
    fun glucose_num_restarts(ptr: CGlucose): Long
    fun glucose_num_propagations(ptr: CGlucose): Long

    class CGlucose : PointerType()

    companion object {
        val INSTANCE: LibGlucose by lazy(::load)

        fun load(name: String = "glucose"): LibGlucose = loadLibraryDefault(name)
    }
}

fun LibGlucose.glucose_addClause(
    ptr: LibGlucose.CGlucose,
    clause: Iterable<glucose_Lit>,
): Boolean {
    glucose_addClause_begin(ptr)
    for (lit in clause) {
        glucose_addClause_addLit(ptr, lit)
    }
    return glucose_addClause_commit(ptr)
}

fun LibGlucose.glucose_solve(
    ptr: LibGlucose.CGlucose,
    assumptions: Iterable<glucose_Lit> = emptyList(),
): Boolean {
    glucose_solve_begin(ptr)
    for (lit in assumptions) {
        glucose_solve_addLit(ptr, lit)
    }
    return glucose_solve_commit(ptr)
}

fun LibGlucose.glucose_limited_solve(
    ptr: LibGlucose.CGlucose,
    assumptions: Iterable<glucose_Lit> = emptyList(),
): glucose_lbool {
    glucose_solve_begin(ptr)
    for (lit in assumptions) {
        glucose_solve_addLit(ptr, lit)
    }
    return glucose_limited_solve_commit(ptr)
}

fun main() {
    val lib = LibGlucose.load("glucose")
    println("library = $lib")
    println("signature = ${lib.glucose_signature()}")

    val ptr = lib.glucose_init()
    println("ptr = $ptr")

    println("Allocating new literal")
    val x = lib.glucose_newLit(ptr)
    println("x = $x")
    println("-x = ${lib.glucose_negate(x)}")
    val y = lib.glucose_newLit(ptr)
    println("y = $y")
    println("-y = ${lib.glucose_negate(y)}")

    fun addClause(clause: Iterable<glucose_Lit>) {
        val lits = clause.toList()
        println("Adding clause $lits")
        lib.glucose_addClause(ptr, lits)
    }

    addClause(listOf(lib.glucose_negate(x)))

    // SAT
    println("Solving...")
    println("res = ${lib.glucose_solve(ptr)} (must be SAT)")

    // UNSAT under assumptions
    println("Solving under assumption [$x]")
    println("res = ${lib.glucose_solve(ptr, listOf(x))} (must be UNSAT)")

    // SAT again
    println("Solving again without assumptions...")
    println("res = ${lib.glucose_solve(ptr)} (must be SAT)")

    // UNSAT
    addClause(listOf(x))
    println("res = ${lib.glucose_solve(ptr)} (must be UNSAT)")

    lib.glucose_release(ptr)

    println("All done!")
}
