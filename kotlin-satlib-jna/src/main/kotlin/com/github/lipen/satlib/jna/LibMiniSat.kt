@file:Suppress("FunctionName", "LocalVariableName", "ClassName")

package com.github.lipen.satlib.jna

import com.sun.jna.DefaultTypeMapper
import com.sun.jna.Library
import com.sun.jna.Pointer
import com.sun.jna.PointerType

// TODO: Rename these classes to PascalCase:

// Note: `minisat_Var` and `minisat_Lit` could be "inline classes",
//   but JNA does not support mapping them.
data class minisat_Var(val value: Int) {
    override fun toString(): String = "Var($value)"
}

data class minisat_Lit(val value: Int) {
    override fun toString(): String = "Lit($value)"
}

enum class minisat_lbool(val value: Int) {
    True(1), False(0), Undef(-1);

    companion object {
        private val map = entries.associateBy { it.value }
        fun from(value: Int): minisat_lbool? = map[value]
    }
}

interface LibMiniSat : Library {
    fun minisat_signature(): String
    fun minisat_init(): CMiniSat
    fun minisat_release(ptr: CMiniSat)

    fun minisat_newVar(ptr: CMiniSat): minisat_Var
    fun minisat_newLit(ptr: CMiniSat): minisat_Lit

    fun minisat_mkLit(x: minisat_Var): minisat_Lit
    fun minisat_mkLit_args(x: minisat_Var, sign: Int): minisat_Lit
    fun minisat_negate(p: minisat_Lit): minisat_Lit

    fun minisat_var(p: minisat_Lit): minisat_Var
    fun minisat_sign(p: minisat_Lit): Boolean

    fun minisat_setPolarity(ptr: CMiniSat, v: minisat_Var, b: Int)
    fun minisat_setDecisionVar(ptr: CMiniSat, v: minisat_Var, b: Int)
    fun minisat_setFrozen(ptr: CMiniSat, v: minisat_Var, b: Boolean)
    fun minisat_isEliminated(ptr: CMiniSat, v: minisat_Var): Boolean
    fun minisat_eliminate(ptr: CMiniSat, turn_off_elim: Boolean): Boolean

    fun minisat_addClause(ptr: CMiniSat, len: Int, ps: Pointer /* Lit* */): Boolean
    fun minisat_addClause_begin(ptr: CMiniSat)
    fun minisat_addClause_addLit(ptr: CMiniSat, p: minisat_Lit)
    fun minisat_addClause_commit(ptr: CMiniSat): Boolean

    fun minisat_simplify(ptr: CMiniSat): Boolean

    fun minisat_solve(ptr: CMiniSat, len: Int, ps: Pointer /* Lit* */): Boolean
    fun minisat_limited_solve(ptr: CMiniSat, len: Int, ps: Pointer /* Lit* */): minisat_lbool
    fun minisat_solve_begin(ptr: CMiniSat)
    fun minisat_solve_addLit(ptr: CMiniSat, p: minisat_Lit)
    fun minisat_solve_commit(ptr: CMiniSat): Boolean
    fun minisat_limited_solve_commit(ptr: CMiniSat): minisat_lbool

    fun minisat_okay(ptr: CMiniSat): Boolean

    fun minisat_get_l_True(): minisat_lbool
    fun minisat_get_l_False(): minisat_lbool
    fun minisat_get_l_Undef(): minisat_lbool

    fun minisat_value_Var(ptr: CMiniSat, x: minisat_Var): minisat_lbool
    fun minisat_value_Lit(ptr: CMiniSat, p: minisat_Lit): minisat_lbool
    fun minisat_modelValue_Var(ptr: CMiniSat, x: minisat_Var): minisat_lbool
    fun minisat_modelValue_Lit(ptr: CMiniSat, p: minisat_Lit): minisat_lbool

    fun minisat_num_assigns(ptr: CMiniSat): Int
    fun minisat_num_clauses(ptr: CMiniSat): Int
    fun minisat_num_learnts(ptr: CMiniSat): Int
    fun minisat_num_vars(ptr: CMiniSat): Int
    fun minisat_num_freeVars(ptr: CMiniSat): Int

    fun minisat_conflict_len(ptr: CMiniSat): Int
    fun minisat_conflict_nthLit(ptr: CMiniSat, i: Int): minisat_Lit

    fun minisat_set_conf_budget(ptr: CMiniSat, x: Int)
    fun minisat_set_prop_budget(ptr: CMiniSat, x: Int)
    fun minisat_no_budget(ptr: CMiniSat)

    fun minisat_interrupt(ptr: CMiniSat)
    fun minisat_clearInterrupt(ptr: CMiniSat)

    fun minisat_set_verbosity(ptr: CMiniSat, v: Int)
    fun minisat_set_random_var_freq(ptr: CMiniSat, freq: Double)
    fun minisat_set_random_seed(ptr: CMiniSat, seed: Double)

    fun minisat_num_conflicts(ptr: CMiniSat): Long
    fun minisat_num_decisions(ptr: CMiniSat): Long
    fun minisat_num_restarts(ptr: CMiniSat): Long
    fun minisat_num_propagations(ptr: CMiniSat): Long

    class CMiniSat : PointerType()

    companion object {
        val INSTANCE: LibMiniSat by lazy(::load)

        fun load(name: String = "minisat"): LibMiniSat {
            val mapper = DefaultTypeMapper()
            mapper.addTypeConverter(minisat_Var::class.java, converterForVar)
            mapper.addTypeConverter(minisat_Lit::class.java, converterForLit)
            mapper.addTypeConverter(minisat_lbool::class.java, converterForLBool)
            val options = mapOf(
                Library.OPTION_TYPE_MAPPER to mapper,
            )
            return loadLibrary(name, options)
            // .also { lib ->
            //     check(lib.minisat_get_l_True() == minisat_lbool.True)
            //     check(lib.minisat_get_l_False() == minisat_lbool.False)
            //     check(lib.minisat_get_l_Undef() == minisat_lbool.Undef)
            // }
        }

        private val converterForVar by lazy {
            typeConverter<minisat_Var, Int>(
                fromNative = { nativeValue, _ -> minisat_Var(nativeValue) },
                toNative = { value, _ -> value.value }
            )
        }
        private val converterForLit by lazy {
            typeConverter<minisat_Lit, Int>(
                fromNative = { nativeValue, _ -> minisat_Lit(nativeValue) },
                toNative = { value, _ -> value.value }
            )
        }
        private val converterForLBool by lazy {
            typeConverter<minisat_lbool, Int>(
                fromNative = { nativeValue, _ ->
                    minisat_lbool.from(nativeValue)
                        ?: error("Bad value: '$nativeValue'")
                },
                toNative = { value, _ -> value.value }
            )
        }
    }
}

fun LibMiniSat.minisat_addClause(
    ptr: LibMiniSat.CMiniSat,
    clause: Iterable<minisat_Lit>,
): Boolean {
    minisat_addClause_begin(ptr)
    for (lit in clause) {
        minisat_addClause_addLit(ptr, lit)
    }
    return minisat_addClause_commit(ptr)
}

fun LibMiniSat.minisat_solve(
    ptr: LibMiniSat.CMiniSat,
    assumptions: Iterable<minisat_Lit> = emptyList(),
): Boolean {
    minisat_solve_begin(ptr)
    for (lit in assumptions) {
        minisat_solve_addLit(ptr, lit)
    }
    return minisat_solve_commit(ptr)
}

fun LibMiniSat.minisat_limited_solve(
    ptr: LibMiniSat.CMiniSat,
    assumptions: Iterable<minisat_Lit> = emptyList(),
): minisat_lbool {
    minisat_solve_begin(ptr)
    for (lit in assumptions) {
        minisat_solve_addLit(ptr, lit)
    }
    return minisat_limited_solve_commit(ptr)
}

fun main() {
    val lib = LibMiniSat.load("minisat")
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
    val res1 = lib.minisat_solve(ptr)
    println("res = $res1 (must be SAT)")
    println("x = ${lib.minisat_value_Lit(ptr, x)}")
    println("-x = ${lib.minisat_value_Lit(ptr, lib.minisat_negate(x))}")
    check(res1)

    // UNSAT under assumptions
    println("Solving under assumption [$x]")
    val assumptions = listOf(x)
    val res2 = lib.minisat_solve(ptr, assumptions)
    println("res = $res2 (must be UNSAT)")
    println("x = ${lib.minisat_value_Lit(ptr, x)}")
    check(!res2)

    // SAT again
    println("Solving again without assumptions...")
    val res3 = lib.minisat_solve(ptr)
    println("res = $res3 (must be SAT)")
    println("x = ${lib.minisat_value_Lit(ptr, x)}")
    check(res3)

    // UNSAT
    addClause(listOf(x))
    val res4 = lib.minisat_solve(ptr)
    println("res = $res4 (must be UNSAT)")
    println("x = ${lib.minisat_value_Lit(ptr, x)}")
    check(!res4)

    lib.minisat_release(ptr)

    println("All done!")
}
