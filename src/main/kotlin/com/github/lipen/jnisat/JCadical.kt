/**
 * Copyright (c) 2020, Darya Grechishkina, Konstantin Chukharev, ITMO University
 */

package com.github.lipen.jnisat

import com.github.lipen.jnisat.JCadical.Companion.SolveResult

@Suppress("FunctionName")
class JCadical : AutoCloseable {
    private var handle: Long = 0

    var numberOfVariables: Int = 0
        private set
    var numberOfClauses: Int = 0
        private set

    init {
        handle = cadical_create()
        if (handle == 0L) {
            throw OutOfMemoryError()
        }
    }

    override fun close() {
        if (handle != 0L) {
            cadical_delete(handle)
            handle = 0
        }
    }

    fun newVariable(): Int = ++numberOfVariables

    fun frozen(lit: Int): Boolean {
        TODO()
    }

    fun freeze(lit: Int) {
        TODO()
    }

    fun melt(lit: Int) {
        TODO()
    }

    fun fixed(lit: Int): Int {
        TODO()
    }

    fun add(lit: Int) {
        cadical_add(handle, lit)
    }

    fun assume(lit: Int) {
        cadical_assume(handle, lit)
    }

    @Deprecated("Clause must contain at least one literal!", ReplaceWith("addClause(...)"))
    fun addClause(): Nothing = error("Clause cannot be empty!")

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
        ++numberOfClauses
        cadical_add_clause(handle, literals)
    }

    fun solve(): SolveResult {
        return SolveResult[cadical_solve(handle)]
    }

    fun solve(lit1: Int): SolveResult {
        return SolveResult[cadical_solve(handle, lit1)]
    }

    fun solve(lit1: Int, lit2: Int): SolveResult {
        return SolveResult[cadical_solve(handle, lit1, lit2)]
    }

    fun solve(lit1: Int, lit2: Int, lit3: Int): SolveResult {
        return SolveResult[cadical_solve(handle, lit1, lit2, lit3)]
    }

    fun solve(vararg assumptions: Int): SolveResult {
        return SolveResult[cadical_solve(handle, assumptions)]
    }

    fun getValue(lit: Int): Boolean {
        val value = cadical_get_value(handle, lit)
        check(value != 0) { "cadical_get_value($lit) returned 0" }
        return value > 0
    }

    fun getModel(): BooleanArray {
        val model = cadical_get_model(handle)
            ?: throw OutOfMemoryError("cadical_get_model returned NULL")
        return model.map { it > 0 }.toBooleanArray()
    }

    /* Native */

    private external fun cadical_create(): Long
    private external fun cadical_delete(handle: Long)
    private external fun cadical_add(handle: Long, lit: Int)
    private external fun cadical_assume(handle: Long, lit: Int)
    private external fun cadical_add_clause(handle: Long, literals: IntArray)
    private external fun cadical_solve(handle: Long): Int
    private external fun cadical_solve(handle: Long, lit1: Int): Int
    private external fun cadical_solve(handle: Long, lit1: Int, lit2: Int): Int
    private external fun cadical_solve(handle: Long, lit1: Int, lit2: Int, lit3: Int): Int
    private external fun cadical_solve(handle: Long, assumptions: IntArray): Int
    private external fun cadical_get_value(handle: Long, lit: Int): Int
    private external fun cadical_get_model(handle: Long): IntArray?

    companion object {
        init {
            Loader.load("jcadical")
        }

        enum class SolveResult(val code: Int) {
            UNSOLVED(0),
            SATISFIABLE(10),
            UNSATISFIABLE(20);

            companion object {
                private val lookup = values().associateBy(SolveResult::code)
                operator fun get(code: Int): SolveResult = lookup.getValue(code)
            }
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

        check(solve() == SolveResult.SATISFIABLE) { "Unexpected UNSAT" }

        // Answer must be: x = -1, y = 1, z = -1
        println("x = ${getValue(x)}, y = ${getValue(y)}, z = ${getValue(z)}")

        check(solve(y) == SolveResult.SATISFIABLE)
        check(solve(-y) == SolveResult.UNSATISFIABLE)

        val t = newVariable()
        check(solve(t) == SolveResult.SATISFIABLE)
        check(solve(-t) == SolveResult.SATISFIABLE)
    }
}
