/**
 * Copyright © 2020, Darya Grechishkina, Konstantin Chukharev, ITMO University
 */

package com.github.lipen.jnisat

@Suppress("FunctionName", "MemberVisibilityCanBePrivate", "unused")
class JCadical : AutoCloseable {
    private var handle: Long = 0

    var numberOfVariables: Int = 0
        private set
    var numberOfClauses: Int = 0
        private set

    init {
        reset()
    }

    fun reset() {
        if (handle != 0L) cadical_delete(handle)
        handle = cadical_create()
        if (handle == 0L) throw OutOfMemoryError()
        numberOfVariables = 0
        numberOfClauses = 0
    }

    override fun close() {
        if (handle != 0L) {
            cadical_delete(handle)
            handle = 0
        }
    }

    fun setOption(name: String, value: Int): Boolean {
        return cadical_set(handle, name, value)
    }

    fun setLongOption(arg: String): Boolean {
        return cadical_set_long_option(handle, arg)
    }

    fun newVariable(): Int = ++numberOfVariables

    fun frozen(lit: Int): Boolean {
        return cadical_frozen(handle, lit)
    }

    fun freeze(lit: Int) {
        cadical_freeze(handle, lit)
    }

    fun melt(lit: Int) {
        cadical_melt(handle, lit)
    }

    fun fixed(lit: Int): Int {
        return cadical_fixed(handle, lit)
    }

    fun failed(lit: Int): Boolean {
        return cadical_failed(handle, lit)
    }

    fun optimize(value: Int) {
        cadical_optimize(handle, value)
    }

    fun simplify() {
        cadical_simplify(handle)
    }

    fun terminate() {
        cadical_terminate(handle)
    }

    fun add(lit: Int) {
        cadical_add(handle, lit)
    }

    fun assume(lit: Int) {
        cadical_assume(handle, lit)
    }

    @Deprecated(
        "Clause must contain at least one literal!",
        ReplaceWith("addClause(...)")
    )
    fun addClause() {
        ++numberOfClauses
        add(0)
    }

    fun addClause(lit1: Int) {
        ++numberOfClauses
        add(lit1); add(0)
    }

    fun addClause(lit1: Int, lit2: Int) {
        ++numberOfClauses
        add(lit1); add(lit2); add(0)
    }

    fun addClause(lit1: Int, lit2: Int, lit3: Int) {
        ++numberOfClauses
        add(lit1); add(lit2); add(lit3); add(0)
    }

    fun addClause(vararg literals: Int) {
        addClause_(literals)
    }

    fun addClause_(literals: IntArray) {
        ++numberOfClauses
        cadical_add_clause(handle, literals)
    }

    @Deprecated(
        "Assumption should contain at least one literal",
        ReplaceWith("addAssumption(...)")
    )
    fun addAssumption() {
    }

    fun addAssumption(lit1: Int) {
        assume(lit1)
    }

    fun addAssumption(lit1: Int, lit2: Int) {
        assume(lit1); assume(lit2)
    }

    fun addAssumption(lit1: Int, lit2: Int, lit3: Int) {
        assume(lit1); assume(lit2); assume(lit3)
    }

    fun addAssumption(vararg literals: Int) {
        addAssumption_(literals)
    }

    fun addAssumption_(literals: IntArray) {
        cadical_add_assumption(handle, literals)
    }

    fun solve(): Boolean {
        return when (val result = cadical_solve(handle)) {
            10 -> true
            20 -> false
            else -> error("cadical_solve returned $result")
        }
    }

    fun solve(lit1: Int): Boolean {
        addAssumption(lit1)
        return solve()
    }

    fun solve(lit1: Int, lit2: Int): Boolean {
        addAssumption(lit1, lit2)
        return solve()
    }

    fun solve(lit1: Int, lit2: Int, lit3: Int): Boolean {
        addAssumption(lit1, lit2, lit3)
        return solve()
    }

    fun solve(vararg literals: Int): Boolean {
        return solve_(literals)
    }

    fun solve_(assumptions: IntArray): Boolean {
        addAssumption_(assumptions)
        return solve()
    }

    fun getValue(lit: Int): Boolean {
        return cadical_get_value(handle, lit)
    }

    fun getModel(): BooleanArray {
        return cadical_get_model(handle)
            ?: throw OutOfMemoryError("cadical_get_model returned NULL")
    }

    private external fun cadical_create(): Long
    private external fun cadical_delete(handle: Long)
    private external fun cadical_set(handle: Long, name: String, value: Int): Boolean
    private external fun cadical_set_long_option(handle: Long, arg: String): Boolean
    private external fun cadical_frozen(handle: Long, lit: Int): Boolean
    private external fun cadical_freeze(handle: Long, lit: Int)
    private external fun cadical_melt(handle: Long, lit: Int)
    private external fun cadical_fixed(handle: Long, lit: Int): Int
    private external fun cadical_failed(handle: Long, lit: Int): Boolean
    private external fun cadical_optimize(handle: Long, value: Int)
    private external fun cadical_simplify(handle: Long)
    private external fun cadical_terminate(handle: Long)
    private external fun cadical_add(handle: Long, lit: Int)
    private external fun cadical_assume(handle: Long, lit: Int)
    private external fun cadical_add_clause(handle: Long, literals: IntArray)
    private external fun cadical_add_assumption(handle: Long, literals: IntArray)
    private external fun cadical_solve(handle: Long): Int
    private external fun cadical_get_value(handle: Long, lit: Int): Boolean
    private external fun cadical_get_model(handle: Long): BooleanArray?

    companion object {
        init {
            Loader.load("jcadical")
        }
    }
}

fun main() {
    fun <T : AutoCloseable, R> T.useWith(block: T.() -> R): R = use(block)

    JCadical().useWith {
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
        addAssumption(y)
        check(solve())
        addAssumption(-y)
        check(!solve())

        val t = newVariable()
        addAssumption(t)
        check(solve())
        addAssumption(-t)
        check(solve())
        println("Solving with assumptions: OK")
    }
}
