@file:Suppress("FunctionName", "LocalVariableName", "ClassName")

package com.github.lipen.satlib.jna

import com.sun.jna.DefaultTypeMapper
import com.sun.jna.Library
import com.sun.jna.Pointer
import com.sun.jna.PointerType

data class glucose_Var(val value: Int) {
    override fun toString(): String = "Var($value)"
}

data class glucose_Lit(val value: Int) {
    override fun toString(): String = "Lit($value)"
}

enum class glucose_lbool(val value: Int) {
    True(1), False(0), Undef(-1);

    companion object {
        private val map = entries.associateBy { it.value }
        fun from(value: Int): glucose_lbool? = map[value]
    }
}

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

    fun glucose_setPolarity(ptr: CGlucose, v: glucose_Var, b: Int)
    fun glucose_setDecisionVar(ptr: CGlucose, v: glucose_Var, b: Int)
    fun glucose_setFrozen(ptr: CGlucose, v: glucose_Var, b: Boolean)
    fun glucose_isEliminated(ptr: CGlucose, v: glucose_Var): Boolean
    fun glucose_eliminate(ptr: CGlucose, turn_off_elim: Boolean): Boolean

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

    fun glucose_setIncremental(ptr: CGlucose)
    fun glucose_set_verbosity(ptr: CGlucose, v: Int)
    fun glucose_set_random_var_freq(ptr: CGlucose, freq: Double)
    fun glucose_set_random_seed(ptr: CGlucose, seed: Double)

    fun glucose_num_conflicts(ptr: CGlucose): Long
    fun glucose_num_decisions(ptr: CGlucose): Long
    fun glucose_num_restarts(ptr: CGlucose): Long
    fun glucose_num_propagations(ptr: CGlucose): Long

    class CGlucose : PointerType()

    companion object {
        val INSTANCE: LibGlucose by lazy(::load)

        fun load(name: String = "glucose"): LibGlucose {
            val mapper = DefaultTypeMapper()
            mapper.addTypeConverter<glucose_Var>(converterForVar)
            mapper.addTypeConverter<glucose_Lit>(converterForLit)
            mapper.addTypeConverter<glucose_lbool>(converterForLBool)
            val options = mapOf(
                Library.OPTION_TYPE_MAPPER to mapper,
            )
            return loadLibrary(name, options)
            // .also { lib ->
            //     check(lib.glucose_get_l_True() == glucose_lbool.True)
            //     check(lib.glucose_get_l_False() == glucose_lbool.False)
            //     check(lib.glucose_get_l_Undef() == glucose_lbool.Undef)
            // }
        }

        private val converterForVar by lazy {
            typeConverter<glucose_Var, Int>(
                fromNative = { nativeValue, _ -> glucose_Var(nativeValue) },
                toNative = { value, _ -> value.value }
            )
        }
        private val converterForLit by lazy {
            typeConverter<glucose_Lit, Int>(
                fromNative = { nativeValue, _ -> glucose_Lit(nativeValue) },
                toNative = { value, _ -> value.value }
            )
        }
        private val converterForLBool by lazy {
            typeConverter<glucose_lbool, Int>(
                fromNative = { nativeValue, _ ->
                    glucose_lbool.from(nativeValue)
                        ?: error("Bad value: '$nativeValue'")
                },
                toNative = { value, _ -> value.value }
            )
        }
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

    println("Setting incremental mode")
    lib.glucose_setIncremental(ptr)

    println("Setting verbosity")
    lib.glucose_set_verbosity(ptr, 1)

    println("Turning off elimination")
    lib.glucose_eliminate(ptr, true)

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
    val res1 = lib.glucose_solve(ptr)
    println("res = $res1 (must be SAT)")
    println("x = ${lib.glucose_value_Lit(ptr, x)}")
    println("-x = ${lib.glucose_value_Lit(ptr, lib.glucose_negate(x))}")
    check(res1)

    // UNSAT under assumptions
    val assumptions = listOf(x)
    println("Solving under assumptions $assumptions")
    val res2 = lib.glucose_solve(ptr, assumptions)
    println("res = $res2 (must be UNSAT)")
    println("x = $x = ${lib.glucose_value_Lit(ptr, x)}")
    check(!res2)

    // SAT again
    println("Solving again without assumptions...")
    val res3 = lib.glucose_solve(ptr)
    println("res = $res3 (must be SAT)")
    println("x = $x = ${lib.glucose_value_Lit(ptr, x)}")
    check(res3)

    // UNSAT
    addClause(listOf(x))
    val res4 = lib.glucose_solve(ptr)
    println("res = $res4 (must be UNSAT)")
    println("x = $x = ${lib.glucose_value_Lit(ptr, x)}")
    check(!res4)

    lib.glucose_release(ptr)

    println("All done!")
}
