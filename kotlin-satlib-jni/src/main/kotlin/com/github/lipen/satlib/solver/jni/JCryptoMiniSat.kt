/**
 * Copyright © 2020, Darya Grechishkina, Konstantin Chukharev, ITMO University
 */

package com.github.lipen.satlib.solver.jni

import com.github.lipen.satlib.utils.useWith
import java.io.File
import kotlin.math.absoluteValue

@Suppress("FunctionName", "MemberVisibilityCanBePrivate", "unused")
class JCryptoMiniSat : AutoCloseable {
    private var handle: Long = 0

    var numberOfVariables: Int = 0
        private set
    var numberOfClauses: Int = 0
        private set

    init {
        reset()
    }

    fun reset() {
        if (handle != 0L) cms_delete(handle)
        handle = cms_create()
        if (handle == 0L) throw OutOfMemoryError()
        numberOfVariables = 0
        numberOfClauses = 0
    }

    override fun close() {
        if (handle != 0L) {
            cms_delete(handle)
            handle = 0
        }
    }

    fun newVariable(): Int {
        cms_new_variable(handle)
        return ++numberOfVariables
    }

    // Note: returns one of {0,10,20}
    fun simplify(): Int {
        return cms_simplify(handle)
    }

    fun simplify(lit1: Int): Int {
        return cms_simplify(handle, lit1)
    }

    fun simplify(lit1: Int, lit2: Int): Int {
        return cms_simplify(handle, lit1, lit2)
    }

    fun simplify(lit1: Int, lit2: Int, lit3: Int): Int {
        return cms_simplify(handle, lit1, lit2, lit3)
    }

    fun simplify(vararg literals: Int): Int {
        return cms_simplify(handle, literals)
    }

    fun interrupt() {
        return cms_interrupt(handle)
    }

    fun writeDimacs(path: String) {
        return cms_write_dimacs(handle, path)
    }

    fun writeDimacs(file: File) {
        writeDimacs(file.path)
    }

    @Deprecated(
        "Clause must contain at least one literal!",
        ReplaceWith("addClause(...)")
    )
    fun addClause() {
        ++numberOfClauses
    }

    fun addClause(lit1: Int) {
        ++numberOfClauses
        cms_add_clause(handle, lit1)
    }

    fun addClause(lit1: Int, lit2: Int) {
        ++numberOfClauses
        cms_add_clause(handle, lit1, lit2)
    }

    fun addClause(lit1: Int, lit2: Int, lit3: Int) {
        ++numberOfClauses
        cms_add_clause(handle, lit1, lit2, lit3)
    }

    fun addClause(vararg literals: Int) {
        addClause_(literals)
    }

    fun addClause_(literals: IntArray) {
        ++numberOfClauses
        cms_add_clause(handle, literals)
    }

    private fun convertSolveResult(value: Int): Boolean {
        return when (value) {
            0 -> false // UNSOLVED
            10 -> true // SATISFIABLE
            20 -> false // UNSATISFIABLE
            else -> error("cms_solve returned '$value'")
        }
    }

    fun solve(): Boolean {
        return convertSolveResult(cms_solve(handle))
    }

    fun solve(lit1: Int): Boolean {
        return convertSolveResult(cms_solve(handle, lit1))
    }

    fun solve(lit1: Int, lit2: Int): Boolean {
        return convertSolveResult(cms_solve(handle, lit1, lit2))
    }

    fun solve(lit1: Int, lit2: Int, lit3: Int): Boolean {
        return convertSolveResult(cms_solve(handle, lit1, lit2, lit3))
    }

    fun solve(vararg literals: Int): Boolean {
        return solve_(literals)
    }

    fun solve_(literals: IntArray): Boolean {
        return convertSolveResult(cms_solve(handle, literals))
    }

    fun getValue(lit: Int): Boolean {
        require(lit != 0) { "Literal must be non-zero" }
        return when (val value = cms_get_value(handle, lit.absoluteValue)) {
            LBOOL_TRUE -> true
            LBOOL_FALSE -> false
            LBOOL_UNDEF -> error("cms_get_value returned l_Undef")
            else -> error("cms_get_value returned $value")
        } xor (lit < 0)
    }

    /** Note: resulting array is 1-based. */
    fun getModel(): BooleanArray {
        return cms_get_model(handle)
            ?: throw OutOfMemoryError("cms_get_model returned NULL")
    }

    fun setThreadNumber(n: Int) {
        cms_set_num_threads(handle, n)
    }

    fun setMaxTime(time: Double) {
        cms_set_max_time(handle, time)
    }

    fun setTimeoutAllCalls(time: Double) {
        cms_set_timeout_all_calls(handle, time)
    }

    fun setDefaultPolarity(polarity: Boolean) {
        cms_set_default_polarity(handle, polarity)
    }

    fun noSimplify() {
        cms_no_simplify(handle)
    }

    fun noSimplifyAtStartup() {
        cms_no_simplify_at_startup(handle)
    }

    private external fun cms_create(): Long
    private external fun cms_delete(handle: Long)
    private external fun cms_interrupt(handle: Long)
    private external fun cms_write_dimacs(handle: Long, path: String)
    private external fun cms_new_variable(handle: Long)
    private external fun cms_add_clause(handle: Long, lit1: Int)
    private external fun cms_add_clause(handle: Long, lit1: Int, lit2: Int)
    private external fun cms_add_clause(handle: Long, lit1: Int, lit2: Int, lit3: Int)
    private external fun cms_add_clause(handle: Long, literals: IntArray)
    private external fun cms_solve(handle: Long): Int
    private external fun cms_solve(handle: Long, lit1: Int): Int
    private external fun cms_solve(handle: Long, lit1: Int, lit2: Int): Int
    private external fun cms_solve(handle: Long, lit1: Int, lit2: Int, lit3: Int): Int
    private external fun cms_solve(handle: Long, literals: IntArray): Int
    private external fun cms_simplify(handle: Long): Int
    private external fun cms_simplify(handle: Long, lit1: Int): Int
    private external fun cms_simplify(handle: Long, lit1: Int, lit2: Int): Int
    private external fun cms_simplify(handle: Long, lit1: Int, lit2: Int, lit3: Int): Int
    private external fun cms_simplify(handle: Long, literals: IntArray): Int
    private external fun cms_get_value(handle: Long, lit: Int): Byte
    private external fun cms_get_model(handle: Long): BooleanArray?
    private external fun cms_set_num_threads(handle: Long, n: Int)
    private external fun cms_set_max_time(handle: Long, time: Double)
    private external fun cms_set_timeout_all_calls(handle: Long, time: Double)
    private external fun cms_set_default_polarity(handle: Long, time: Boolean)
    private external fun cms_no_simplify(handle: Long)
    private external fun cms_no_simplify_at_startup(handle: Long)

    companion object {
        init {
            Loader.load("jcms")
        }

        private const val LBOOL_TRUE: Byte = 0
        private const val LBOOL_FALSE: Byte = 1
        private const val LBOOL_UNDEF: Byte = 2
    }
}

fun main() {
    @Suppress("DuplicatedCode")
    JCryptoMiniSat().useWith {
        val x = newVariable()
        val y = newVariable()
        val z = newVariable()

        addClause(-x)
        addClause(-z)
        addClause(x, y, z)

        println("Solving...")
        check(solve()) { "Unexpected UNSAT" }
        println("x = ${getValue(x)}, y = ${getValue(y)}, z = ${getValue(z)}")
        println("model = ${getModel().drop(1)}")

        println("Solving with assumptions...")
        check(solve(y))
        check(!solve(-y))

        val t = newVariable()
        check(solve(t))
        check(solve(-t))
        println("Solving with assumptions: OK")
    }
}
