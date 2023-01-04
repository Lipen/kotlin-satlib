@file:Suppress("FunctionName", "LocalVariableName")

package com.github.lipen.satlib.jna

import com.sun.jna.Library
import com.sun.jna.Pointer
import com.sun.jna.PointerType

private typealias minisat_Var = Int
private typealias minisat_Lit = Int
private typealias minisat_lbool = Int

interface LibMinisat : Library {
    fun minisat_signature(): String
    fun minisat_init(): CMinisat
    fun minisat_release(ptr: CMinisat)

    fun minisat_newVar(ptr: CMinisat): minisat_Var
    fun minisat_newLit(ptr: CMinisat): minisat_Lit

    fun minisat_mkLit(x: minisat_Var): minisat_Lit
    fun minisat_mkLit_args(x: minisat_Var, sign: Int): minisat_Lit
    fun minisat_negate(p: minisat_Lit): minisat_Lit

    fun minisat_var(p: minisat_Lit): minisat_Var
    fun minisat_sign(p: minisat_Lit): Boolean

    fun minisat_addClause(ptr: CMinisat, len: Int, ps: Pointer /* Lit* */): Boolean
    fun minisat_addClause_begin(ptr: CMinisat)
    fun minisat_addClause_addLit(ptr: CMinisat, p: minisat_Lit)
    fun minisat_addClause_commit(ptr: CMinisat): Boolean

    fun minisat_simplify(ptr: CMinisat): Boolean

    fun minisat_solve(ptr: CMinisat, len: Int, ps: Pointer /* Lit* */): Boolean
    fun minisat_limited_solve(ptr: CMinisat, len: Int, ps: Pointer /* Lit* */): minisat_lbool
    fun minisat_solve_begin(ptr: CMinisat)
    fun minisat_solve_addLit(ptr: CMinisat, p: minisat_Lit)
    fun minisat_solve_commit(ptr: CMinisat): Boolean
    fun minisat_limited_solve_commit(ptr: CMinisat): minisat_lbool

    fun minisat_okay(ptr: CMinisat): Boolean

    fun minisat_setPolarity(ptr: CMinisat, v: minisat_Var, b: Int)
    fun minisat_setDecisionVar(ptr: CMinisat, v: minisat_Var, b: Int)

    fun minisat_get_l_True(): minisat_lbool
    fun minisat_get_l_False(): minisat_lbool
    fun minisat_get_l_Undef(): minisat_lbool

    fun minisat_value_Var(ptr: CMinisat, x: minisat_Var): minisat_lbool
    fun minisat_value_Lit(ptr: CMinisat, p: minisat_Lit): minisat_lbool
    fun minisat_modelValue_Var(ptr: CMinisat, x: minisat_Var): minisat_lbool
    fun minisat_modelValue_Lit(ptr: CMinisat, p: minisat_Lit): minisat_lbool

    fun minisat_num_assigns(ptr: CMinisat): Int
    fun minisat_num_clauses(ptr: CMinisat): Int
    fun minisat_num_learnts(ptr: CMinisat): Int
    fun minisat_num_vars(ptr: CMinisat): Int
    fun minisat_num_freeVars(ptr: CMinisat): Int

    fun minisat_conflict_len(ptr: CMinisat): Int
    fun minisat_conflict_nthLit(ptr: CMinisat, i: Int): minisat_Lit

    fun minisat_set_conf_budget(ptr: CMinisat, x: Int)
    fun minisat_set_prop_budget(ptr: CMinisat, x: Int)
    fun minisat_no_budget(ptr: CMinisat)

    fun minisat_interrupt(ptr: CMinisat)
    fun minisat_clearInterrupt(ptr: CMinisat)

    fun minisat_setFrozen(ptr: CMinisat, v: minisat_Var, b: Boolean)
    fun minisat_isEliminated(ptr: CMinisat, v: minisat_Var): Boolean
    fun minisat_eliminate(ptr: CMinisat, turn_off_elim: Boolean): Boolean

    fun minisat_set_verbosity(ptr: CMinisat, v: Int)

    fun minisat_num_conflicts(ptr: CMinisat): Long
    fun minisat_num_decisions(ptr: CMinisat): Long
    fun minisat_num_restarts(ptr: CMinisat): Long
    fun minisat_num_propagations(ptr: CMinisat): Long

    class CMinisat : PointerType()

    companion object {
        val INSTANCE: LibMinisat by lazy(::load)

        fun load(name: String = "minisat"): LibMinisat = loadLibraryDefault(name)
    }
}

fun LibMinisat.minisat_addClause(
    ptr: LibMinisat.CMinisat,
    clause: Iterable<minisat_Lit>,
): Boolean {
    minisat_addClause_begin(ptr)
    for (lit in clause) {
        minisat_addClause_addLit(ptr, lit)
    }
    return minisat_addClause_commit(ptr)
}

fun LibMinisat.minisat_solve(
    ptr: LibMinisat.CMinisat,
    assumptions: Iterable<minisat_Lit> = emptyList(),
): Boolean {
    minisat_solve_begin(ptr)
    for (lit in assumptions) {
        minisat_solve_addLit(ptr, lit)
    }
    return minisat_solve_commit(ptr)
}

fun LibMinisat.minisat_limited_solve(
    ptr: LibMinisat.CMinisat,
    assumptions: Iterable<minisat_Lit> = emptyList(),
): minisat_lbool {
    minisat_solve_begin(ptr)
    for (lit in assumptions) {
        minisat_solve_addLit(ptr, lit)
    }
    return minisat_limited_solve_commit(ptr)
}

fun main() {
    val lib = LibMinisat.load("minisat")
    println("library = $lib")
    println("signature = ${lib.minisat_signature()}")

    val ptr = lib.minisat_init()
    println("ptr = $ptr")

    println("Allocating new literal")
    val x = lib.minisat_newLit(ptr)
    println("x = $x")
    println("-x = ${lib.minisat_negate(x)}")
    val y = lib.minisat_newLit(ptr)
    println("y = $y")
    println("-y = ${lib.minisat_negate(y)}")

    fun addClause(clause: Iterable<minisat_Lit>) {
        val lits = clause.toList()
        println("Adding clause $lits")
        lib.minisat_addClause(ptr, lits)
    }

    addClause(listOf(lib.minisat_negate(x)))

    // SAT
    println("Solving...")
    println("res = ${lib.minisat_solve(ptr)} (must be SAT)")

    // UNSAT under assumptions
    println("Solving under assumption [$x]")
    println("res = ${lib.minisat_solve(ptr, listOf(x))} (must be UNSAT)")

    // SAT again
    println("Solving again without assumptions...")
    println("res = ${lib.minisat_solve(ptr)} (must be SAT)")

    // UNSAT
    addClause(listOf(x))
    println("res = ${lib.minisat_solve(ptr)} (must be UNSAT)")

    lib.minisat_release(ptr)

    println("All done!")
}
