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

    fun addClause(literals: IntArray) {
        ++numberOfClauses
        cadical_add_clause(handle, literals)
    }

    @JvmName("addClauseVararg")
    fun addClause(vararg literals: Int) {
        addClause(literals)
    }

    @Deprecated(
        "Assumption must contain at least one literal!",
        ReplaceWith("addAssumption(...)")
    )
    fun addAssumption(): Nothing = error("Assumption cannot be empty!")

    fun addAssumption(lit1: Int) {
        assume(lit1)
    }

    fun addAssumption(lit1: Int, lit2: Int) {
        assume(lit1); assume(lit2)
    }

    fun addAssumption(lit1: Int, lit2: Int, lit3: Int) {
        assume(lit1); assume(lit2); assume(lit3)
    }

    fun addAssumption(literals: IntArray) {
        cadical_add_assumption(handle, literals)
    }

    @JvmName("addAssumptionVararg")
    fun addAssumption(vararg literals: Int) {
        addAssumption(literals)
    }

    fun solve(): SolveResult {
        return SolveResult.of(cadical_solve(handle))
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
    private external fun cadical_add_assumption(handle: Long, assumptions: IntArray)
    private external fun cadical_solve(handle: Long): Int
    private external fun cadical_get_value(handle: Long, lit: Int): Int
    private external fun cadical_get_model(handle: Long): IntArray?

    companion object {
        init {
            Loader.load("jcadical")
        }

        enum class SolveResult {
            UNSOLVED,
            SATISFIABLE,
            UNSATISFIABLE;

            companion object {
                fun of(code: Int): SolveResult = when (code) {
                    0 -> UNSOLVED
                    10 -> SATISFIABLE
                    20 -> UNSATISFIABLE
                    else -> error("Bad solver exit code $code")
                }
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
